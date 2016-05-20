#!/bin/sh
# author: yanggui
# purpose: dump push materials from redis
. /etc/profile
export LANG=en_US.UTF-8

BASIC_PATH="/Users/liaobo/zip/data"
CP=lib/sunshine_spider_offgrid.jar
for file in `ls lib`
do
        if [ -f lib/$file ]
        then
                CP=${CP}:lib/$file
        fi
done
echo $CP
PROS="-r -b ${BASIC_PATH}"
CLASS=com.boliao.sunshine.main.SpiderLauncher
java -cp $CP $CLASS $PROS
