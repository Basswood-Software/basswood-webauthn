#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly project_root="$(cd ${work_dir}/.. && pwd)"
readonly docker_dir="${project_root}/docker"
readonly mysql_dir="${project_root}/mysql"
readonly webauthn_dir="${project_root}/webauthn"
readonly authenticator_dir="${project_root}/authenticator"

echo "Building: docker-build.sh......."
# Build mysql module
${mysql_dir}/docker-build.sh
if (($? != 0 ));then
  echo "mysql module build failed."
  exit 1
fi

# Build webauthn module
${webauthn_dir}/docker/docker-build.sh
if (($? != 0 ));then
  echo "webauthn module build failed."
  exit 2
fi

# Build authenticator module
${authenticator_dir}/docker/docker-build.sh
if (($? != 0 ));then
  echo "authenticator module build failed."
  exit 3
fi

# cleanup dangling volumes
count=$(docker volume ls -qf dangling=true|wc -l|tr -d ' ')
if (($count > 0));then
  docker volume rm $(docker volume ls -qf dangling=true)
fi

cd $docker_dir
# Bring up the database first
docker-compose -p webauthn up -d mysqldb
sleep 10

# Bring up rest of the services
docker-compose -p webauthn up -d