#!/usr/bin/env bash

wget http://ltdata1.informatik.uni-hamburg.de/joint/wsd/20170719_wsd_db.tar
tar -xf 20170719_wsd_db.tar
# Adjust UID, 999 is the postgres user in the wsd_db docker container
docker run -v "$(pwd)/pgdata:/pgdata" alpine chown -R 999:999 /pgdata/data