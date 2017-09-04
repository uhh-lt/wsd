#!/usr/bin/env bash

logfile=logs/web_app_build.log
mkdir -p logs
echo "Build and then start web application"
echo
echo "Logs can be found in $logfile"

if ! test -f "docker-compose.override.yml"; then
echo
  cp sample-docker-compose.override.yml docker-compose.override.yml

  echo "Created file:  docker-compose.override.yml"
  echo "Change this to configure your installation"
  echo "Web frontend is now configured to run on: http//:localhost:8080"

fi

echo
# First prepare the API subproject to build a Docker image from it:
# This will compile the SBT source code and generate a `Dockerfile` to `api/target/docker/stage`, which is used in the docker-compose.yml.
echo "Compile sbt project"
docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt api/docker:stage -ivy .ivy2 >> $logfile
echo
echo "Build docker images"
docker-compose build >> $logfile
echo
echo "Start docker containers"
docker-compose up -d

wait_for_url() {
  msg="$1"
  url="$2"
  echo -n "$msg"
  until curl -sf "$url" > /dev/null; do :; sleep 1; done
  echo " [done]"
}
echo
api_url=$(docker-compose config | grep WSP_API_PUBLIC_URL | awk -F ': ' '{print $2}')
wait_for_url "Waiting for API server to start on endpoint: '$api_url'" $api_url
echo
echo "Web application successfully updated and started."
