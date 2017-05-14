#!/bin/bash

source ".env"

golden_related_jq_cmd='(.hypernyms | join(","))'

while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -e|--env)
    source ".${2}.env"
    shift # past argument
    ;;
    -hh|--with-hyperhypernyms)
    golden_related_jq_cmd='([.hypernyms, .hyperhypernyms] | flatten | join(","))'
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done


function join_by { local IFS="$1"; shift; echo "$*"; }

# TODO
#

# "9col" seems to be the naming for this format, see
# https://github.com/tudarmstadt-lt/lefex/blob/master/src/main/java/de/tudarmstadt/lt/jst/ExtractLexicalSampleFeatures/LexicalSampleDataset.java#L16
fields9col=(context_id target target_pos target_position gold_sense_ids predict_sense_ids golden_related predict_related context)
jq_array_fields=".$(join_by "," "${fields9col[@]}" | sed 's/,/,\./g')"
tsv_header_fields=$(join_by $'\t' "${fields9col[@]}")
for path in $OUTPUT_FOLDER/*.json;
do
  wordfile=$(basename $path)
  word=${wordfile%.json}
  cat $path | jq -c -r '[.[] | {
     "context_id": "",
     "target":"'"$word"'",
     "target_pos": "",
     "target_position": "",
     "gold_sense_ids": ("'"$word"'#" + .synsetId),
     "predict_sense_ids":"",
     "golden_related": '"${golden_related_jq_cmd}"',
     "predict_related": "",
     "context":.glosses[]}
     ] | unique | .[] | ['"$jq_array_fields"'] | @tsv'
done | awk -v TEXT="$tsv_header_fields" 'BEGIN {print TEXT; i=0}{print i $0; i++}'
