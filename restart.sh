#!/bin/sh -x

CONFIG_FILE=./$(hostname)-config.json

pkill -u $(whoami) java

nohup java -Dconfig.file.name=$CONFIG_FILE -jar build/libs/*.jar 2>&1 | svlogd logs &
