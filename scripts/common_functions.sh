#!/usr/bin/env bash

sbt_cmd() {
  local project_root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/.."
  $project_root/sbt/sbt "$@"
}

docker_compose_cmd() {
  local project_root="$(dirname $0)/../.."
  pushd
  cd $project_root
  docker-compose "$@"
  popd
}

shutdown_web_app() {
  local project_root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/.."
  if ! test -e "$project_root/api/target/docker/stage/Dockerfile"; then
   # We need the Dockerfile for the API to use docker-compose and can create it with sbt
   sbt_cmd api/docker:stage
  fi
  docker-compose stop
}


ensure_docker_compose_override_exists() {
  local scripts_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  local project_root="$scripts_dir/.."
  if ! test -f "$project_root/docker-compose.override.yml"; then
    echo
    cp sample-docker-compose.override.yml docker-compose.override.yml

    echo "Created file: docker-compose.override.yml"
    echo "Change this to configure your installation."
    echo "Web frontend is now configured to run on: http//:localhost:8080"
  fi
}

wait_for_db() {
  echo -n "Waiting for DB server to start."
  until docker-compose exec db pg_isready > /dev/null;
    do sleep 1;
  done
  echo " [done]"
}

wait_for_url() {
  msg="$1"
  url="$2"
  echo -n "$msg"
  until curl -sf "$url" > /dev/null; do :; sleep 1; done
  echo " [done]"
}



ensure_only_db_is_running() {
  shutdown_web_app
  ensure_docker_compose_override_exists
  docker-compose up -d db
  wait_for_db
}

# Because MacOs misses realpath (and also readlink)
# https://stackoverflow.com/a/18443300
combat_realpath() {
  local OURPWD=$PWD
  cd "$(dirname "$1")"
  local LINK=$(readlink "$(basename "$1")")
  while [ "$LINK" ]; do
    cd "$(dirname "$LINK")"
    local LINK=$(readlink "$(basename "$1")")
  done
  local REALPATH="$PWD/$(basename "$1")"
  cd "$OURPWD"
  echo "$REALPATH"
}

has_project_model_bundle() {
  local scripts_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  local project_root="$scripts_dir/.."
  test -d "$project_root/pgdata" || test -d "$project_root/imgdata"
  return $?
}


abort_if_model_already_loaded() {
  if has_project_model_bundle; then
    echo "Error: Has already a model bundle loaded."
    echo
    echo "You must remove this model bundle before you can continue."
    echo "Run: wsd model:delete"
    exit 1
  fi
}

abort_if_no_model_loaded() {
  if has_project_model_bundle; then :
  else
    echo "Error: No model bundle loaded in project."
    echo
    echo "To continue you must load a model bundle, this can be done by either"
    echo "downloading or building the toy or full model, see the 'wsd model:*' commands."
    exit 1
  fi
}

export -f sbt_cmd
export -f shutdown_web_app
export -f ensure_only_db_is_running
export -f combat_realpath
export -f has_project_model_bundle
export -f abort_if_model_already_loaded
export -f ensure_docker_compose_override_exists
export -f wait_for_db
export -f wait_for_url