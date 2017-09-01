#!/usr/bin/env bash

model_scripts_dir="$(dirname $0)"

source "$model_scripts_dir/common_functions.sh"

project_root=$(combat_realpath "$model_scripts_dir/../..")
logfile="$project_root/logs/delete-model.log"

echo "Deleting loaded model bundle"
echo "Logs are written to '$logfile'"
echo
echo "Shutting down web app"
shutdown_web_app >> $logfile
echo
read -p "Do you want to create a backup? (Y|n) " -r
echo
if [[ $REPLY =~ ^[nN]$ ]]
then
  echo "Proceeding with NO backup!!!"
  echo
  sleep 1
else
  echo "----- STARTING BACKUP -----"
  echo
  $model_scripts_dir/backup_model.sh
  echo
  echo "----- BACKUP FINISHED -----"
  echo
fi

echo "!!! THIS WILL PERMANENTLY DELETE ALL EXISTING MODELS !!!"
echo "This will remove the 'pgdata' and 'imgdata' folders."
echo
read -p "Please type 'YES' (all uppercase) and hit enter to confirm: " -r

if [[ $REPLY == "YES" ]]
then
  echo
  docker run -v "$project_root:/project" alpine \
    rm -rf /project/pgdata /project/imgdata
  echo "The model bundle has been successfully deleted."
else
 echo
 echo "Aborted. Nothing has been deleted!"
fi







