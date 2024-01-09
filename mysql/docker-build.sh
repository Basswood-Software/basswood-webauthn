#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly service_name=mysqldb

cd $work_dir

docker container rm $service_name
docker image rm basswood/$service_name
docker build -t basswood/$service_name .