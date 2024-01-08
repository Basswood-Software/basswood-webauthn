#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly docker_dir="$(dirname $work_dir)/docker"

cd $docker_dir
echo "Building: docker-build.sh......."
count=$(docker volume ls -qf dangling=true|wc -l|tr -d ' ')
if (($count > 0));then
  docker volume rm $(docker volume ls -qf dangling=true)
fi

$docker_dir/docker-build.sh
if (($? != 0 ));then
  echo "docker-build.sh has failed."
  exit 1
fi

# Bring up database first
cd $docker_dir
docker-compose -p webauthn up -d mysqldb
echo "Waiting for database to come up ...."
sleep 5 # wait for database to come up

#Setup db
if [ -f "${work_dir}/setup-database.sh" ];then
  echo "Setting up database ...."
  ${work_dir}/setup-database.sh
fi

# Bring up rest of the services
cd $docker_dir
docker-compose -p webauthn up -d