#!/usr/bin/env bash


model_scripts_dir=$(dirname $0)

source "$model_scripts_dir/common_functions.sh"

project_root=$(combat_realpath "$model_scripts_dir/../..")
backup_folder="$project_root/data/backup"

download_db() {
  echo "Downloading DB backup"
  latest_pgdata=20170831_wsd_db.tar
  wget -nc http://ltdata1.informatik.uni-hamburg.de/joint/wsd/$latest_pgdata -P $backup_folder

  echo "Extracting DB backup"
  docker run -v "$project_root:/project" -v "$backup_folder:/backup" alpine \
    tar -xf "/backup/$latest_pgdata" -C "/project"

  status=$?

  if [ "$status" -eq 0 ]; then
    echo "Adjusting file ownership"
    # Adjust UID, 999 is the postgres user in the wsd_db docker container
    docker run -v "$project_root/pgdata:/pgdata" alpine chown -R 999:999 /pgdata/data
  fi

  return "$status"
}

download_images() {

  echo "Downloading images backup"

  latest_imgdata=20170831_all_senses_imgdata.tgz
  wget -nc http://ltdata1.informatik.uni-hamburg.de/joint/wsd/$latest_imgdata -P $backup_folder

  echo "Extracting images backup"
  docker run -v "$project_root:/project" -v "$backup_folder:/backup" alpine \
    tar -xzf "/backup/$latest_imgdata" -C "/project"

  status=$?

  if [ "$status" -eq 0 ]; then
    echo "Adjusting file ownerships"
    # Adjust UID, 1 is the daemon user in the wsd_api docker container
    docker run -v "$project_root/imgdata:/imgdata" alpine chown -R 1:1 /imgdata/bing
  fi

  return "$status"
}



if download_db && download_images; then
  echo
  echo "Model successfully downloaded"
  echo "You can now start the web application: wsd web-app:start"
else
  echo
  echo "An error occurred, please inspect output above to recover."
  exit 1
fi



