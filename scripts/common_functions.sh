#!/usr/bin/env bash

sbt_cmd() {
  project_root="$(dirname $0)/../.."
  $project_root/sbt/sbt "$@"
}

shutdown_web_app() {
  local project_root="$(dirname $0)/../.."
  if ! test -e "$project_root/api/target/docker/stage/Dockerfile"; then
   # We need the Dockerfile for the API to use docker-compose and can create it with sbt
   sbt_cmd api/docker:stage
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
  local model_scripts_dir=$(dirname $0)
  local project_root="$model_scripts_dir/../.."
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