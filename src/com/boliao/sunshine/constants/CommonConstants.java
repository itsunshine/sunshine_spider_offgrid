/**
 * 
 */
package com.boliao.sunshine.constants;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * 普通常量类
 * 
 * @author Liaobo
 * 
 */
public class CommonConstants {
	// 文件格式的日期字符串
	public static final SimpleDateFormat filefmt = new SimpleDateFormat(
			"yyyyMMdd");

	// ibm host
	public static final String IBM = "www.ibm.com";

	// ITEYE host
	public static final String ITEYE = "www.iteye.com";

	// 内容配置开始后缀
	public static final String CONTENT_START_PREFIX = ".content.start";

	// 内容配置结束后缀
	public static final String CONTENT_END_PREFIX = ".content.end";

	// 页码内容配置开始后缀
	public static final String PAGECONTENT_START_PREFIX = ".pagecontent.start";

	// 页码内容配置结束后缀
	public static final String PAGECONTENT_END_PREFIX = ".pagecontent.end";

	// 分隔符
	public static final String SEPARATOR = ";";

	// filter
	public static final String URL_FILTER = ".url.filter";

	// 存储url地址的临时目录
	public static final String SEEDS_DIR = "temp" + File.separator;

	// 上传网站的url地址
	public static final String UPLOAD_URL = "http://localhost:8989/sunshine_new/views/upload.do";
}
