#!/bin/sh

pgrep -u $(whoami) java > /dev/null 2>&1
if [ $? -eq 1 ]; then
  cd ~/netty.http
  ./restart.sh > /dev/null 2>&1
fi
