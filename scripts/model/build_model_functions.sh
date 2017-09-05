#!/usr/bin/env bash

set -o nounset # Error on referencing undefined variables, shorthand: set -n
set -o errexit # Abort on error, shorthand: set -e

scripts_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/.."
source "$scripts_dir/common_functions.sh"

toy_lmi="data/training/45g-lmi-test-both.csv"
toy_ddt_t="data/training/ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure-test-python.csv"
toy_ddt_c="data/training/P80_T0_Ecount_N0_Htfidf_clusters-test-cosets-2.csv"
toy_usages="data/sample_sentences_traditional_inventory.csv"

lmi=${1:-$toy_lmi}
ddt_t=${2:-$toy_ddt_t}
ddt_c=${3:-$toy_ddt_c}
usages=${4:-$toy_usages}

model_location=${4:-"data/models"}

spark_submit_cmd() {
  scripts/spark_submit_jar.sh $@
}

import_db_entities() {

    # Fill DB table with entities
    # @See: de.tudarmstadt.lt.wsd.common.DetectEntities

    #mwe_file=data/voc-mwe6446031-dbpedia-babelnet-wordnet-dela.csv
    #comm -12 \
    #  <( cat "$mwe_file" | tr '[:upper:]' '[:lower:]' | sort | uniq ) \
    #  <( cat "$ddt_t" | awk -F'\t' '{print $1}' | tr '[:upper:]' '[:lower:]' | sort | uniq ) \
    #    > data/voc-mwe-dbpedia-common-with-ddt.csv

    db_container=$(docker-compose ps db | tail -n 1 | awk '{print $1}')
    docker cp data/voc-mwe-dbpedia-common-with-ddt.csv $db_container:/voc.csv

    sql_cmd="""
    CREATE TABLE entities (text text);
    COPY entities FROM '/voc.csv' DELIMITER E'\t' CSV;
    CREATE INDEX entities_text_index ON entities (text); -- takes a few minutes
    """

    docker-compose exec db psql wsp_default -U postgres -c "$sql_cmd"
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

    db_container=$(docker-compose ps db | tail -n 1 | awk '{print $1}')
    docker cp "$csv_file" $db_container:/babelnet_ids.csv

    sql_cmd="""
    CREATE TEMP TABLE temporary (sense_id text, babelnet_id text);
    COPY temporary FROM '/babelnet_ids.csv' DELIMITER E'\t' CSV;
    ALTER TABLE senses ADD COLUMN babelnet_id TEXT DEFAULT NULL NULL;

    UPDATE senses AS s SET babelnet_id = t.babelnet_id
    FROM temporary AS t
    WHERE s.sense_id = t.sense_id;

    DROP TABLE temporary;
    """

    docker-compose exec db psql wsp_default -U postgres -c "$sql_cmd"
}

import_db_usage_examples() {
    # Adding usage examples for senses:
    # @See: de.tudarmstadt.lt.wsd.common.model.SampleSentence

    cleaned="data/cleaned-$usages"

    # echo "Count tabs in each line and group by occurrence"
    # awk '{print gsub(/\t/, "")}' $usages | sort | uniq -c

    # Clean file, steps are explained here:
    # 1) number of tabs per line: https://stackoverflow.com/a/15518345
    # 2) delete lines by number in file: https://stackoverflow.com/a/11369748
    awk 'NR==FNR{l[$0];next;} !(FNR in l)' \
      <(awk '{print gsub(/\t/, "") " " NR}'  $usages | grep '^2' | awk '{print $2}') \
      $usages \
      > $cleaned

    # echo "Show that output is cleaned"
    # awk '{print gsub(/\t/, "")}' cleaned | sort | uniq -c

    # Importing into DB

    csv_file=$cleaned

    db_container=$(docker-compose ps db | tail -n 1 | awk '{print $1}')
    docker cp "$csv_file" $db_container:/usages.csv

    sql_cmd="""
        DROP TABLE IF EXISTS sample_sentences;
        CREATE TABLE sample_sentences (
            sentence_id INT,
            sense_id TEXT,
            inventory TEXT,
            sense_position TEXT,
            sentence TEXT
        );

        COPY sample_sentences(sentence_id, sense_id, sense_position, sentence)
            FROM '/usages.csv'
            DELIMITER E'\t'
            QUOTE E'\b' -- https://stackoverflow.com/a/20402913
            CSV HEADER;

        UPDATE  sample_sentences SET inventory = 'traditional';

        CREATE INDEX sample_sentences_sense_index ON sample_sentences (sense_id, inventory);
    """

    docker-compose exec db psql wsp_default -U postgres -c "$sql_cmd"
}

build_model() {

  mkdir -p /tmp/spark-events
  rm -rdf "$model_location"

  sbt_cmd spark/assembly

  spark_submit_cmd "create -n cosets1k_coocwords -c $ddt_c -f $lmi -p $model_location"
  spark_submit_cmd "create -n cosets1k_coocdeps -c $ddt_c -f $lmi -p $model_location"
  spark_submit_cmd "create -n cosets1k_self -c $ddt_c -f $lmi -p $model_location"
  
  spark_submit_cmd "create -n traditional_coocwords -c $ddt_t -f $lmi -p $model_location"
  spark_submit_cmd "create -n traditional_coocdeps -c $ddt_t -f $lmi -p $model_location"
  spark_submit_cmd "create -n traditional_self -c $ddt_t -f $lmi -p $model_location"
}

export_db() {
  docker-compose exec db dropdb -U postgres wsp_default --if-exists
  docker-compose exec db createdb -U postgres wsp_default
  spark_submit_cmd "exportdb"
}

run() {

  start_timer() {
    start=$SECONDS
  }

  echo_timer() {
    echo "it took $($start - $SECONDS) secs"
  }

  echo "Creating a model bundle from training data."
  echo
  echo "This command will do the following steps:"
  echo "1. Creates all models with Spark (Parquet format)"
  echo "2. Exports the models to the DB (Postgres format)"
  echo "3. Applies further migrations to the DB"
  echo
  echo "----- (1) STARTING MODEL CREATION WITH SPARK -----"; start_timer
  echo
  build_model
  echo
  echo
  echo "----- (1) FINISHED MODEL CREATION WITH SPARK ($(echo_timer)) -----"
  echo
  echo "----- (2) STARTING EXPORT MODELS TO DB -----"; start_timer
  echo
  #echo "Prepare DB"
  #ensure_only_db_is_running
  echo
  echo "Export models to DB"
  export_db
  echo
  echo "----- (2) FINISHED EXPORT MODELS TO DB ($(echo_timer)) -----"
  echo
  echo "----- (3) STARTING APPLY MIGRATIONS TO DB -----"; start_timer
  echo
  echo "Import Babelnet IDs of senses"
  import_db_babelnet_ids
  echo
  echo "Import named entities into DB"
  import_db_entities
  echo
  echo "Import usage examples into DB"
  import_db_usage_examples
  echo
  echo "----- (3) FINISHED APPLY MIGRATIONS TO DB ($(echo_timer)) -----"
  echo
  echo "Done."
}

export -f run
export -f build_model
export -f export_db
export -f import_db_babelnet_ids
export -f import_db_entities
export -f import_db_usage_examples


