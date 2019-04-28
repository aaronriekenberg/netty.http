#!/bin/sh -x

CONFIG_FILE=./$(hostname -s)-config.json

pkill -U $(id -u) java

if [ $(hostname) = 'raspberrypi' ]; then
  export PATH="/home/pi/jdk-11.0.3+7/bin/:$PATH"
fi

nohup java -Dconfig.file.name=$CONFIG_FILE -jar build/libs/*.jar 2>&1 | svlogd logs &
