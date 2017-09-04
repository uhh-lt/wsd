#!/usr/bin/env bash

scripts_dir="$(dirname $0)/.."
source "$scripts_dir/common_functions.sh"
project_root=$(combat_realpath "$scripts_dir/..")

if has_project_model_bundle; then
  echo
  echo "Project has model bundle loaded."
  echo
else
  echo
  echo "No model bundle loaded."
  echo
fi

echo "This is the contents of your backup folder (data/backup):"
ls -lh $project_root/data/backup