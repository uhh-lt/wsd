#!/usr/bin/python3

import sys
import csv
from collections import defaultdict


result = defaultdict(dict)
fieldnames = set()

id=0
for csvfile in sys.argv[1:]:
  with open(csvfile, newline="") as infile:
    reader = csv.DictReader(infile, dialect=csv.excel_tab)
    for row in reader:
      for key in row:
        fieldnames.add(key) # wasteful, but I don't care enough
        result[id][key] = row[key]
      id+=1

writer = csv.DictWriter(sys.stdout, fieldnames)
writer.writeheader()
for item in result:
  writer.writerow(result[item])
