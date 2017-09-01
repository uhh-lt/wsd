#!/usr/bin/env bash

model_scripts_dir=$(dirname $0)
source $model_scripts_dir/common_functions.sh

combat_readlink() {
 if [ "$(uname -s)" = 'Linux' ]; then
    readlink -f "$1"
 else
    readlink "$1"
  fi
}

project_root=$(combat_readlink "$model_scripts_dir/../..")
logfile="$project_root/logs/backup.log"
backup_folder="$project_root/data/backup"
pgdata_folder="$project_root/pgdata"
imgdata_folder="$project_root/imgdata"

echo "Backing up model"
echo "Logs are written to '$logfile'"

echo "Shutting down web app"
shutdown_web_app >> $logfile

today="$( date +"%Y%m%d" )"
pgdata_backup="${today}_pgdata.tar"
imgdata_backup="${today}_imgdata.tgz"

if test -e "$backup_folder/$pgdata_backup"; then
  echo "Backup '$backup_folder/$pgdata_backup' already exists."
  echo ""
  echo "Aborted"
  exit 1
fi
if test -e "$backup_folder/$imgdata_backup"; then
  echo "Backup '$backup_folder/$imgdata_backup' already exists."
  echo ""
  echo "Aborted"
  exit 1
fi

echo "Creating backup '$pgdata_backup' in '$backup_folder'"

docker run -v "$pgdata_folder:/pgdata" -v "$backup_folder:/backup" alpine \
 tar -c /pgdata -f "backup/$pgdata_backup"

echo "Creating backup '$imgdata_backup' in '$backup_folder'"

docker run -v "$imgdata_folder:/imgdata" -v "$backup_folder:/backup" alpine \
 tar -cz /imgdata -f "backup/$imgdata_backup"





