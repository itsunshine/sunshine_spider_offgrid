#!/bin/sh
# author: liaobo
# purpose: dump push materials from redis
. /etc/profile
export LANG=en_US.UTF-8

BASIC_PATH="/Users/liaobo/zip/spider"
DATA_DIR="/Users/liaobo/zip/data"
CP=$BASIC_PATH/lib/sunshine_spider_offgrid.jar
for file in `ls ${BASIC_PATH}/lib`
do
	if [ -f ${BASIC_PATH}/lib/$file ]
	then
		CP=${CP}:${BASIC_PATH}/lib/$file
	fi
done
echo $CP
PROS="-r -b ${DATA_DIR}"
CLASS=com.boliao.sunshine.main.SpiderLauncher
java -cp $CP $CLASS $PROS
