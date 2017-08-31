#!/usr/bin/env bash

logfile=logs/web_app_build.log
mkdir -p logs
echo "Build and then start web application"
echo
echo "Logs can be found in $logfile"

# First prepare the API subproject to build a Docker image from it:
# This will compile the SBT source code and generate a `Dockerfile` to `api/target/docker/stage`, which is used in the docker-compose.yml.
echo "Compile sbt project"
docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt api/docker:stage -ivy .ivy2 >> $logfile
docker-compose up -d --build >> $logfile

wait_for_url() {
  msg="$1"
  url="$2"
  echo -n "$msg"
  until curl -sf "$url" > /dev/null; do echo $?; sleep 1; done
  echo " [done]"
}

api_url=$(docker-compose config | grep WSP_API_PUBLIC_URL | awk -F ': ' '{print $2}')
wait_for_url "Waiting for API server to start on endpoint: '$api_url'" $api_url

echo "Web application successfully updated and started."
