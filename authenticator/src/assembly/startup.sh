#!/usr/bin/env bash
readonly work_dir="$(cd $(dirname $0) && pwd)"
readonly jarfile=$(find $work_dir -name *.jar)
if [ -z "$jarfile" ];then
  echo "Executable Jar not found" >&2
  exit 1
fi
java -jar $jarfile