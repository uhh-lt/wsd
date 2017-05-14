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
# sense_id  synset_id  hypernyms hyperhypernyms

# Using https://github.com/jehiah/json2csv
fields="word,id,related_words"
for path in $OUTPUT_FOLDER/*.json;
do
  wordfile=$(basename $path)
  word=${wordfile%.json}
  cat $path | jq -c -r '.[] | [
     "'"$word"'",
     "'"$word"'#" + .synsetId,
     '"${golden_related_jq_cmd}"'
     ] | @tsv'
done | sort | uniq | awk -v TEXT="$(echo $fields | tr ',' '\t')" 'BEGIN {print TEXT}{print}'