#!/bin/sh -x

CONFIG_FILE=./$(hostname -s)-config.json
JAVA_OPTS=

pkill -U $(id -u) java

sleep 2

export PATH="${HOME}/jdk-13+33-jre/bin/:$PATH"
if [ $(hostname) = 'raspberrypi' ]; then
  JAVA_OPTS="-Xmx64m"
fi

export PATH="${HOME}/bin:$PATH"

nohup java -XX:SharedArchiveFile=./netty.http.jsa -Dconfig.file.name=$CONFIG_FILE $JAVA_OPTS -jar build/libs/*.jar 2>&1 | simplerotate logs &
