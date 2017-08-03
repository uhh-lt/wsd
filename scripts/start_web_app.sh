#!/usr/bin/env bash

# First prepare the API subproject to build a Docker image from it:
# This will compile the SBT source code and generate a `Dockerfile` to `api/target/docker/stage`, which is used in the docker-compose.yml.
docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt api/docker:stage -ivy .ivy2

docker-compose up -d --build
echo
echo Web application successfully updated and started.
