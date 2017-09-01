#!/usr/bin/env bash

docker_sbt_cmd() {
  docker run -it --rm -v $(pwd):/root hseeberger/scala-sbt sbt "$@" -ivy .ivy2
}

shutdown_web_app() {
  project_root="$(dirname $0)/../.."
  if ! test -e "$project_root/api/target/docker/stage/Dockerfile"; then
   # We need the Dockerfile for the API to use docker-compose and can create it with sbt
   docker_sbt_cmd api/docker:stage
  fi
  docker-compose stop
}

ensure_only_db_is_running() {
  shutdown_web_app
  docker-compose up -d db
  until docker-compose exec db psql -U postgres -c "select 1" -d postgres; do echo "Waiting for DB to startup"; sleep 1; done > /dev/null
  echo "DB is ready"
}

# Because MacOs misses realpath (and also readlink)
# https://stackoverflow.com/a/18443300
combat_realpath() {
  OURPWD=$PWD
  cd "$(dirname "$1")"
  LINK=$(readlink "$(basename "$1")")
  while [ "$LINK" ]; do
    cd "$(dirname "$LINK")"
    LINK=$(readlink "$(basename "$1")")
  done
  REALPATH="$PWD/$(basename "$1")"
  cd "$OURPWD"
  echo "$REALPATH"
}

export -f docker_sbt_cmd
export -f shutdown_web_app
export -f ensure_only_db_is_running
export -f combat_realpath