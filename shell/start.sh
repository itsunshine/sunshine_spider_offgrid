#!/bin/sh
# author: liaobo
# purpose: dump push materials from redis
. /etc/profile

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
PROS="-s ${BASIC_PATH}/spider/seeds.txt -d ${BASIC_PATH}/spider/lastDateRecord.properties -b ${DATA_DIR}"
CLASS=com.boliao.sunshine.main.SpiderLauncher
java -cp $CP $CLASS $PROS
# 在ios系统上，由于是utf-8编码，所以需要将utf-8字符转成gbk，
#下面的代码是为了做编码转换,并上传转换好的文件
#filePrefix="${DATA_DIR}/spider/jobDemandArt/"`date '+%Y%m%d'`
#fileSuf="_jobDemandArt"
#nU="${filePrefix}${fileSuf}"
#u="${filePrefix}${fileSuf}U"
#mv ${nU} ${u}
#iconv -f UTF-8 -t GBK ${u} > ${nU}
#curl -F file=${u} http://www.itsunshine.net/views/upload.do