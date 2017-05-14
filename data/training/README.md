Please run the following commands to download and prepare the training data into this folder:

```bash
wget http://panchenko.me/data/joint/ddt/ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv.gz
tar -xzvf ddt-mwe-45g-8m-thr-agressive2-cw-e0-N200-n200-minsize5-isas-cmb-313k-hyper-filter-closure.csv.gz
wget http://panchenko.me/data/joint/word-feature-counts/45g-lmi.csv.gz
tar -xzvf 45g-lmi.csv.gz
cat 45g-lmi.csv | grep -v '元' > 45g-lmi.csv-cleaned.csv
mv 45g-lmi.csv-cleaned.csv 45g-lmi.csv
wget http://panchenko.me/data/joint/cosets/P80_T100_Elog_N1_Htfidf/clusters.csv
mv clusters.csv P80_T100_Elog_N1_Htfidf_clusters.csv
http://panchenko.me/data/joint/cosets/P80_T0_Ecount_N0_Htfidf/clusters.csv
mv clusters.csv P80_T0_Ecount_N0_Htfidf_clusters.csv
```

