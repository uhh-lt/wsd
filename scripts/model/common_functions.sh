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

export -f docker_sbt_cmd
export -f shutdown_web_app