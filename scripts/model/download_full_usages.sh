#!/bin/bash

set -o nounset # Error on referencing undefined variables, shorthand: set -n
set -o errexit # Abort on error, shorthand: set -e

wget -c http://panchenko.me/data/joint/corpora/usages-wiki-ddt-mwe-313k.csv.gz -P data
gunzip -c data/usages-wiki-ddt-mwe-313k.csv.gz > data/usages-wiki-ddt-mwe-313k.csv