#!/bin/sh -x

CONFIG_FILE=./$(hostname -s)-config.json

pkill -U $(id -u) java

nohup java -Dconfig.file.name=$CONFIG_FILE -jar build/libs/*.jar 2>&1 | svlogd logs &
