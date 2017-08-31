#!/usr/bin/env bash

origin=http://panchenko.me/data/joint
target=data/training

ddt_archive=ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv.gz
wget $origin/ddt/$ddt_archive -P $target
tar -xzvf $target/ddt_archive -C $target

wget $origin/word-feature-counts/45g-lmi.csv.gz -P $target
tar -xzvf $target/45g-lmi.csv.gz -C $target
cat $target/45g-lmi.csv | grep -v '元' > $target/45g-lmi.csv-cleaned.csv
mv $target/45g-lmi.csv-cleaned.csv $target/45g-lmi.csv

wget $origin/cosets/P80_T100_Elog_N1_Htfidf/clusters.csv -P $target
mv $target/clusters.csv $target/P80_T100_Elog_N1_Htfidf_clusters.csv
wget $origin/cosets/P80_T0_Ecount_N0_Htfidf/clusters.csv -P $target
mv $target/clusters.csv $target/P80_T0_Ecount_N0_Htfidf_clusters.csv