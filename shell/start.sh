#!/bin/sh
# author: yanggui
# purpose: dump push materials from redis
. /etc/profile
export LANG=en_US.UTF-8

MAILLIST="liaobo,fuqiang5,jinling5"
SUBJECT="物料推送"
CONTENT="物料推送失败！"

BASIC_PATH="/Users/liaobo/workspace_j2ee/spider"
CP=$BASIC_PATH/lib/sunshine_spider_offgrid.jar
for file in `ls ${BASIC_PATH}/lib`
do
	if [ -f ${BASIC_PATH}/lib/$file ]
	then
		CP=${CP}:${BASIC_PATH}/lib/$file
	fi
done
echo $CP
PROS="-s /Users/liaobo/workspace_j2ee/sunshine_spider_offgrid/spider/seeds.txt -d /Users/liaobo/workspace_j2ee/sunshine_spider_offgrid/src/com/boliao/sunshine/properties/lastDateRecord.properties  -b /Users/liaobo/workspace_j2ee/spider"
CLASS=com.boliao.sunshine.main.SpiderLauncher
java -cp $CP $CLASS $PROS
