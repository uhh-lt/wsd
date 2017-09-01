#!/usr/bin/env bash

model_scripts_dir=$(dirname $0)
source "$model_scripts_dir/common_functions.sh"
project_root=$(combat_realpath "$model_scripts_dir/../..")

if has_project_model_bundle; then
  echo
  echo "Model bundle exists."
  echo
else
  echo
  echo "No model bundle exist."
  echo
fi

echo "This is the contents of your backup folder (data/backup):"
ls -lh $project_root/data/backup