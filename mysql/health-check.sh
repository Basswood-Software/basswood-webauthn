#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly username=${MYSQL_USER:-basswood}
readonly password=${MYSQL_PASSWORD:-basswood}
readonly schema=${MYSQL_DATABASE:-webauthn_basswood}

cd $work_dir
count=$(mysql --user=${username} --password=${password} --execute 'show databases;' 2>&1 | grep ${schema} | wc -l)
(($count ==1)) && exit 0 || exit 1