/**
 * 
 */
package com.boliao.sunshine.constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import com.boliao.sunshine.parsers.TencentHRParser;

/**
 * 普通常量类
 * 
 * @author Liaobo 1、数据接入平台 2、容器化 3、机器学习平台 4、队列技术
 * 
 */
public class CommonConstants {

	// 文件格式的日期字符串
	public static final SimpleDateFormat filefmt = new SimpleDateFormat("yyyyMMdd");

	// 更新日期字符串格式化工具
	public static final SimpleDateFormat recordDatefmt = new SimpleDateFormat("yyyy-MM-dd");

	// ibm host
	public static final String IBM = "www.ibm.com";

	// ITEYE host
	public static final String ITEYE = "www.iteye.com";

	// 腾讯招聘host
	public static final String TENCENTHR = "hr.tencent.com";
	public static final String BAIDUHR = "talent.baidu.com";
	public static final String TAOBAOHR = "job.alibaba.com";

	// 内容配置开始后缀
	public static final String CONTENT_START_PREFIX = ".content.start";

	// 工作要求内容配置结束后缀
	public static final String JOBCONTENT_END_PREFIX = ".jobcontent.end";

	// 工作要求内容配置开始后缀
	public static final String JOBCONTENT_START_PREFIX = ".jobcontent.start";

	// 内容配置结束后缀
	public static final String CONTENT_END_PREFIX = ".content.end";

	// 页码内容配置开始后缀
	public static final String PAGECONTENT_START_PREFIX = ".pagecontent.start";

	// 页码内容配置结束后缀
	public static final String PAGECONTENT_END_PREFIX = ".pagecontent.end";

	// 抓取的记录，最后一条对应的日期
	public static final String LAST_RECORD_DATE = ".lastOne";

	// 分隔符
	public static final String SEPARATOR = ";";

	// filter
	public static final String URL_FILTER = ".url.filter";

	// 存储url地址的临时目录
	public static final String SEEDS_DIR = "temp" + File.separator;

	// 存储url地址的临时目录
	public static final String RECOVERY_SEEDS_DIR = "recovery" + File.separator + "temp" + File.separator;

	// 上传网站的url地址
	public static final String UPLOAD_URL = "http://localhost:8989/sunshine_new/views/upload.do";
	// public static final String UPLOAD_URL =
	// "http://www.itsunshine.net/views/upload.do";

	// 程序运行目录
	public static final String USER_DIR = "user.dir";

	// url抓取失败的目录
	public static final String URL_FETCH_ERROR = "urlFetchErrors";

	// content抓取失败的目录
	public static final String CON_FETCH_ERROR = "conFetchErrors";

	// 抓取失败时的最近日期
	public static final String ERROR_DATE = "failedDt";

	// 抓取失败的url
	public static final String ERROR_URL = "failedUrl";

	// 回车换行字符串
	public static final String ENTER_STR = "\n";

	// 将招聘页面里的若干，翻译成10
	public static final int N_NUMBER = 10;

	// 需要先获取html页面内容再解析的，parsers
	public static Set<Class> OBTAIN_HTML_PARSERS = new HashSet<Class>();

	// 加入需要准备html内容的parser
	static {
		OBTAIN_HTML_PARSERS.add(TencentHRParser.class);
	}
}
