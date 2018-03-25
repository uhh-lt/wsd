#!/usr/bin/env bash
#set -e # Exit on any errors

# This script extracts senses for the given words

## DECLARE VARIABLES

TRADITIONAL=traditional
COSETS=cosets
senses_type=traditional #cosets

cosets1k_senses_file=P80_T100_Elog_N1_Htfidf_clusters.csv
traditional_senses_file=ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv
contexts_file=45g-lmi.csv

tmp="tmp/$(date +%F--%H-%M-%S)"

if [ "$senses_type" = "$TRADITIONAL" ]
then test_senses_file="$tmp/${traditional_senses_file%.*}-test.csv"
else test_senses_file="$tmp/${cosets1k_senses_file%.*}-test.csv"
fi
test_contexts_file="$tmp/${contexts_file%.*}-test.csv"


## ECHO SETTINGS


if [ "$senses_type" = "$TRADITIONAL" ]
then
    echo "Running extraction for traditional sense inventory"
    if [ -z "$1" ]; then echo "Missing parameters for words"; exit 1; fi
    words="$@"
    echo "Extracting test data for: $words"
else
    echo "Running extraction for coset sense inventory"
    if [ -z "$1" ]; then echo "Missing first parameter for num_senses"; exit 1; fi
    num_senses="$1"
    echo "Extracting the first $num_senses senses as test data"
fi

num_cpu=$(cat /proc/cpuinfo | awk '/^processor/{print $3}' | tail -1)
echo "$num_cpu CPUs were detected for parallel processing."
mkdir -p tmp && mkdir "$tmp"
echo "Saving results to folder: $tmp"


## EXTRACT SENSES

senses_folder="$tmp/senses"
mkdir "$senses_folder"

if [ "$senses_type" = "$TRADITIONAL" ]
then
    export senses_folder
    export traditional_senses_file
    function extract_traditional_senses() {
        word=$1
        output_file="$senses_folder/$word.csv"
        cat "$traditional_senses_file" | grep -i -P "^\Q$word\E\t" > "$output_file"
        echo "$(cat "$output_file" | wc -l) senses found for $word"
    }
    export -f extract_traditional_senses
    # TODO: Does not support multi-words
    echo "$words" | tr ' ' '\n' |
        xargs --max-procs="$num_cpu" -I {} bash -c 'extract_traditional_senses "$@"' _ {}

    head -n 1 "$traditional_senses_file" > "$test_senses_file"
    for f in $(ls "$senses_folder"/*); do cat "$f"; done >> "$test_senses_file"
else
    head -n 1 "$cosets1k_senses_file" > "$test_senses_file"
    tail -n +2 "$cosets1k_senses_file" | head -n "$num_senses"  >> "$test_senses_file"
fi

echo "All $(tail -n +2 "$test_senses_file" | wc -l) senses written into file: $test_senses_file"


## EXTRACT CLUSTER WORDS

extract_traditional_cluster_words() {
    awk -F'\t' '{print $3}' \
    | sed 's/,/\n/g' \
    | awk -F':' '{''print $1}' \
    | awk -F'#' '{print $1}' \
    | sort | uniq
}

extract_coset_cluster_words() {
    awk -F'\t' '{print $4}' \
    | sed 's/,/\n/g' \
    | sed 's/^ //g' \
    | awk -F'#' '{print $1}' \
    | sort | uniq
}

cluster_words_file="$tmp/cluster_words.csv"

if [ "$senses_type" = "$TRADITIONAL" ]
then cat "$test_senses_file" | extract_traditional_cluster_words > "$cluster_words_file"
else cat "$test_senses_file" | extract_coset_cluster_words > "$cluster_words_file"
fi

## EXTRACT CONTEXT FEATURES

num_cluster_words=$(tail -n +2 "$cluster_words_file" | wc -l)
echo "Searching context features for $num_cluster_words cluster words."

contexts_folder="$tmp/context_features"
mkdir "$contexts_folder"
export contexts_folder
export contexts_file
grep_contexts_features() {
    c=$1
    output_file="$contexts_folder/$(echo $c | md5sum | cut -f1 -d" ").csv"
    grep -P "^\Q$c\E\t" "$contexts_file" > "$output_file" # escaping variable http://stackoverflow.com/a/2001239
    #echo "$(wc -l "$output_file" | awk '{print $1}') found for '$c'" # For debugging
}
export -f grep_contexts_features

print_progress() {
    item=$(ls "$contexts_folder" | grep -v "__COMPLETED" | wc -l)
    total="$num_cluster_words"
    percent=$(awk "BEGIN { pc=100*${item}/${total}; i=int(pc); print (pc-i<0.5)?i:i+1 }")
    echo -ne "Progress: $percent% ($item/$total)\r";
}

while [ ! -e "$contexts_folder/__COMPLETED" ]; do print_progress; sleep 1; done &
# Fix: Remove any cluster words with quotes
cat "$cluster_words_file" | grep -v '"' | grep -v "'" | xargs --max-procs=$num_cpu -I {} bash -c 'grep_contexts_features "$@"' _ {}

echo "Done!"
touch "$contexts_folder/__COMPLETED"

head -n 1 "$contexts_file" > "$test_contexts_file"
for f in $(ls "$contexts_folder"/*); do cat "$f"; done >> "$test_contexts_file"

## ECHO RESULTS

echo
echo "$(tail -n +2 $test_senses_file | wc -l) senses written to: $test_senses_file"
echo "$(tail -n +2 $test_contexts_file | wc -l) context features written to: $test_contexts_file"
