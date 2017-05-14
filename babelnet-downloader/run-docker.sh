#!/bin/bash

docker build -t "babelnet-downloader" .
docker run \
  --rm \
  -v $(pwd)/var:"/opt/downloader/var" \
  babelnet-downloader
