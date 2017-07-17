#!/usr/bin/env bash

set -o nounset # Error on referencing undefined variables, shorthand: set -n
set -o errexit # Abort on error, shorthand: set -e

lmi=${1:-"data/training/45g-lmi.csv"}
ddt_t=${2:-"data/training/ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv"}
ddt_c=${3:-"data/training/P80_T0_Ecount_N0_Htfidf_clusters.csv"}

model_location=${4:-data/models}

spark_submit_cmd() {
  scripts/spark_submit_jar.sh $@
}
docker_sbt_cmd() {
  docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt "$@" -ivy .ivy2
}

import_db_entities() {
    # Fill DB table with entities
    # @See: de.tudarmstadt.lt.wsd.common.DetectEntities

    mwe_file=data/voc-mwe6446031-dbpedia-babelnet-wordnet-dela.csv
    comm -12 \
      <( cat "$mwe_file" | tr '[:upper:]' '[:lower:]' | sort | uniq ) \
      <( cat "$ddt_t" | awk -F'\t' '{print $1}' | tr '[:upper:]' '[:lower:]' | sort | uniq ) \
        > data/voc-mwe-dbpedia-common-with-ddt.csv

    docker cp data/voc-mwe-dbpedia-common-with-ddt.csv \
        wsd_import_db:/voc.csv

    sql_cmd="""
    CREATE TABLE entities (text text);
    COPY entities FROM '/voc.csv' DELIMITER E'\t' CSV;
    CREATE INDEX entities_text_index ON entities (text); -- takes a few minutes
    """

    docker exec -it wsd_import_db psql wsp_default -U postgres -c "$sql_cmd"
}

import_db_babelnet_ids() {
    # Adding babelnet_id column and fill with data:
    # @See: de.tudarmstadt.lt.wsd.common.model.Sense

    rdf_file=data/ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-pos-closure_babelnet.rdf
    csv_file=ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-pos-closure_babelnet.csv

    grep -v '@' "$rdf_file" | \
      sed 's/__/ /g' | tr ':' ' ' | \
      awk 'NF == 8 && match($3, /_[0-9]*$/) {print $2"#"substr($3, RSTART+1, RLENGTH-1)"\t"$6":"substr($7,2)}' \
      > "$csv_file"

    docker cp "$csv_file" wsd_import_db:/data.csv

    sql_cmd="""
    CREATE TEMP TABLE temporary (sense_id text, babelnet_id text);
    COPY temporary FROM '/data.csv' DELIMITER E'\t' CSV;
    ALTER TABLE senses ADD COLUMN babelnet_id TEXT DEFAULT NULL NULL;

    UPDATE senses AS s SET babelnet_id = t.babelnet_id
    FROM temporary AS t
    WHERE s.sense_id = t.sense_id;

    DROP TABLE temporary;
    """

    docker exec -it wsd_import_db psql wsp_default -U postgres -c "$sql_cmd"
}

import_db_sample_sentences() {
    # Adding babelnet_id column and fill with data:
    # @See: de.tudarmstadt.lt.wsd.common.model.SampleSentence

    csv_file=data/sample_sentences_traditional_inventory.csv

    docker cp "$csv_file" wsd_import_db:/data.csv

    sql_cmd="""
    DROP TABLE sample_sentences
    CREATE TABLE sample_sentences (
        sentence_id INT,
        sense_id TEXT,
        inventory TEXT,
        sense_position TEXT,
        sentence TEXT
    );

    COPY sample_sentences(sentence_id, sense_id, sense_position, sentence)
        FROM '/data.csv'
        DELIMITER E'\t'
        CSV HEADER;

    UPDATE  sample_sentences SET inventory = 'traditional';

    CREATE INDEX sample_sentences_sense_index ON sample_sentences (sense_id, inventory);
    """

    docker exec -it wsd_db_1 psql wsp_default -U postgres -c "$sql_cmd"

}

build_model() {

  mkdir -p /tmp/spark-events
  rm -rdf "$model_location"

  sbt spark/assembly

  spark_submit_cmd "create -n cosets1k_coocwords -c $ddt_c -f $lmi -p $model_location"
  spark_submit_cmd "create -n cosets1k_coocdeps -c $ddt_c -f $lmi -p $model_location"
  spark_submit_cmd "create -n cosets1k_self -c $ddt_c -f $lmi -p $model_location"
  
  spark_submit_cmd "create -n traditional_coocwords -c $ddt_t -f $lmi -p $model_location"
  spark_submit_cmd "create -n traditional_coocdeps -c $ddt_t -f $lmi -p $model_location"
  spark_submit_cmd "create -n traditional_self -c $ddt_t -f $lmi -p $model_location"
  
}

shutdown_web_app() {
  sbt api/docker:stage # We need the Dockerfile for the API to use docker-compose
  # docker-compose exec db psql -U postgres -c "\l" # List databases
  docker-compose down
}

remove_import_db_container() {
  name="wsd_import_db"
  if [[ -n $(docker ps -a | awk '$NF=="'$name'"{print}') ]]; then docker rm -f "$name"; fi > /dev/null
}

start_import_db() {
  name="wsd_import_db"
  remove_import_db_container # make sure no old container is running
  shutdown_web_app
  # Create new container
  docker run -v $(pwd)/pgdata/data:/var/lib/postgresql/data -p "5432:5432" --name=$name -d postgres:9.5.5 > /dev/null
  until docker exec -it $name psql -U postgres -c "select 1" -d postgres; do echo "Waiting for DB to startup"; sleep 1; done > /dev/null
  echo "DB is ready"
}

export_db() {
  docker exec -it $name dropdb -U postgres wsp_default --if-exists
  docker exec -it $name createdb -U postgres wsp_default
  spark_submit_cmd "exportdb"
}

build_model
start_import_db
export_db
import_db_babelnet_ids
import_db_entities
remove_import_db_container
