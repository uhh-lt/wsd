#!/usr/bin/env bash

origin="http://panchenko.me/data/joint"
target="data/training"

ddt="ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv"
ddt_archive="$ddt.gz"
wget -c "$origin/ddt/$ddt_archive" -P "$target"
gunzip -c "$target/$ddt_archive" > "$target/$ddt"

lmi="45g-lmi.csv"
lmi_archive="$lmi.gz"
wget -c "$origin/word-feature-counts/$lmi.gz" -P "$target"
gunzip -c "$target/$lmi_archive" > "$target"
cat "$target/$lmi" | grep -v '元' > "$target/45g-lmi.csv-cleaned.csv"
mv "$target/45g-lmi.csv-cleaned.csv" "$target/45g-lmi.csv"

wget -c -O "$target/P80_T100_Elog_N1_Htfidf_clusters.csv" "$origin/cosets/P80_T100_Elog_N1_Htfidf/clusters.csv"
wget -c -O "$target/P80_T0_Ecount_N0_Htfidf_clusters.csv" "$origin/cosets/P80_T0_Ecount_N0_Htfidf/clusters.csv"
