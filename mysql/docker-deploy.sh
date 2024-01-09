#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"

cd $work_dir
docker-compose -p webauthn up -d