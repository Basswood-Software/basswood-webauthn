#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly docker_dir="$(dirname $work_dir)/docker"
cd $docker_dir
#echo "Shutting down docker containers......."
#docker-compose stop
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
docker-compose -p webauthn up -d