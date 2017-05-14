#!/usr/bin/env bash

source .env

echo "Calculating statistics..."

number_of_words=$(
  ls "$OUTPUT_FOLDER"/*.json | wc -l
)

number_of_senses=$(
  for f in "$OUTPUT_FOLDER"/*.json
  do
    cat "$f" | jq '.[] | keys' | grep ']'
  done | wc -l
)

number_of_glosses=$(
  for f in "$OUTPUT_FOLDER"/*.json
  do
    cat "$f" | jq '.[] | .glosses' | grep -v ']' | grep -v '\['
  done | wc -l
)

number_of_hypernyms=$(
  for f in "$OUTPUT_FOLDER"/*.json
  do
    cat "$f" | jq '.[] | .hypernyms | length'
  done | awk '{s+=$1} END {print s}'
)

number_of_hyperhypernyms=$(
  for f in "$OUTPUT_FOLDER"/*.json
  do
    cat "$f" | jq '.[] | .hyperhypernyms | length'
  done | awk '{s+=$1} END {print s}'
)




echo
echo "Downloaded data for $number_of_words words"
echo "with $number_of_senses senses and"
echo "$number_of_glosses contexts (with duplicates) and"
echo "$number_of_hypernyms hypernyms and"
echo "$number_of_hyperhypernyms hyperhypernyms"
