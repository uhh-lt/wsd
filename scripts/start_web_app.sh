#!/usr/bin/env bash

scripts_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$scripts_dir/common_functions.sh"
project_root=$(combat_realpath "$scripts_dir/..")

logfile="$project_root/data/logs/web_app_build.log"

mkdir -p logs
echo "Build and then start web application."

ensure_docker_compose_override_exists
echo

# First prepare the API subproject to build a Docker image from it:
# This will compile the SBT source code and generate a `Dockerfile` to `api/target/docker/stage`, which is used in the docker-compose.yml.
echo "Compile sbt project"
docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt api/docker:stage -ivy .ivy2
echo
echo "Build docker images"
docker-compose build
echo
echo "Start docker containers."
docker-compose up -d
wait_for_db
api_url=$(docker-compose config | grep WSP_API_PUBLIC_URL | awk -F ': ' '{print $2}')
wait_for_url "Waiting for API server to start on endpoint: '$api_url'" $api_url
echo
echo "Web application successfully updated and started."
