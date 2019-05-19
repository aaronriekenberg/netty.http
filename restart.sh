#!/bin/sh -x

CONFIG_FILE=./$(hostname -s)-config.json
JAVA_OPTS='-Xmx64m'

pkill -U $(id -u) java

sleep 2

export PATH="$HOME/jdk-11.0.3+7/bin/:$PATH"

nohup java -Dconfig.file.name=$CONFIG_FILE $JAVA_OPTS -jar build/libs/*.jar 2>&1 | svlogd logs &
