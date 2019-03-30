#!/bin/sh -x

CONFIG_FILE=./$(hostname -s)-config.json

pkill -U $(id -u) java

if [ $(uname) = 'OpenBSD' ]; then
  export PATH="/usr/local/jdk-1.8.0/bin/:$PATH"
fi

nohup java -Dconfig.file.name=$CONFIG_FILE -jar build/libs/*.jar 2>&1 | svlogd logs &
