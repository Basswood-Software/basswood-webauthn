#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
cd $work_dir
readonly container_name=mysqldb
readonly rootpwd=basswood
readonly username=basswood
readonly password=basswood
readonly schema=webauthn_basswood
readonly script=database.sql

# wait for database to come up
count=0
while ! docker exec -i ${container_name} mysql -u root -p${rootpwd} --execute 'show databases'; do
    count=$((count+1))
    if (($count == 5)); then
      echo "It is taking a while for the db to come up. Giving up..."
      exit 1
    else
        echo "Waiting for database service to come up..."
        sleep 5
    fi
done

docker exec -i ${container_name} mysql -u root -p${rootpwd} << EOF
  CREATE SCHEMA IF NOT EXISTS ${schema};

  CREATE USER IF NOT EXISTS '${username}'@'%' IDENTIFIED BY '${password}';
  GRANT ALL PRIVILEGES ON ${schema}.* TO '${username}'@'%';

  CREATE USER IF NOT EXISTS '${username}'@'localhost' IDENTIFIED BY '${password}';
  GRANT ALL PRIVILEGES ON ${schema}.* TO '${username}'@'localhost';
EOF

# Now create tables.
docker exec -i ${container_name} mysql -u ${username} -p${password} ${schema} < ${script}