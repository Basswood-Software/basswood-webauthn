#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly project_root="$(cd ${work_dir}/../.. && pwd)"
readonly docker_dir="${project_root}/opt/docker"
readonly webauthn_dir="${project_root}/webauthn"
readonly authenticator_dir="${project_root}/authenticator"

echo "Building: docker-build.sh......."
# Build webauthn module
${webauthn_dir}/opt/docker/docker-build.sh
if (($? != 0 ));then
  echo "webauthn module build failed."
  exit 1
fi

# Build authenticator module
${authenticator_dir}/opt/docker/docker-build.sh
if (($? != 0 ));then
  echo "authenticator module build failed."
  exit 2
fi

# cleanup dangling volumes
count=$(docker volume ls -qf dangling=true|wc -l|tr -d ' ')
if (($count > 0));then
  docker volume rm $(docker volume ls -qf dangling=true)
fi

# Bring up database first
if [ ! -f "${docker_dir}/setup-database.sh" ];then
  echo "Database setup script at ${docker_dir}/setup-database.sh not found"
  exit 3
fi
if [ ! -f "${docker_dir}/database.sql" ];then
  echo "Database setup sql script at ${docker_dir}/database.sql not found"
  exit 4
fi
cd $docker_dir
docker-compose -p webauthn up -d mysqldb
echo "Waiting for database to come up ...."
sleep 5

#Setup db
${docker_dir}/setup-database.sh
if (($? != 0 ));then
  echo "database setup script exited with error."
  exit 5
fi
# Bring up rest of the services
cd $docker_dir
docker-compose -p webauthn up -d