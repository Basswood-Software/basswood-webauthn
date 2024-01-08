#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly project_dir="$(cd ${work_dir}/../.. && pwd)"
readonly target_dir="${project_dir}/target"
readonly service_name=authenticator

cd $project_dir
echo "Building the maven project....."
mvn clean install assembly:single -DskipTests

if (($? != 0));then
  echo "Build failure" >&2
  exit 1
fi

tarball=$(ls ${target_dir}/*.tar.gz);
if [ -z "${tarball}" ];then
  echo "No tarball found. Possibly build failure" >&2
  exit 2
fi
tarball_basename=$(basename ${tarball})
startup_script=$(tar -tf $tarball | grep startup.sh)
if [ -z "$startup_script" ];then
  echo "No startup.sh script is found in the tarball" >&2
  exit 3
fi

log_config_file=$(tar -tf $tarball | grep log4j2-spring.xml)
echo "log_config_file=$log_config_file"
echo "Building docker image....."

docker container rm $service_name
docker image rm basswood/$service_name
docker build --build-arg TAR_BALL=${tarball_basename} --build-arg STARTUP_SCRIPT=${startup_script} --build-arg LOGGING_CONFIG_ARG=${log_config_file} -f ${work_dir}/Dockerfile -t basswood/$service_name .