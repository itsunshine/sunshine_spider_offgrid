/**
 * 
 */
package com.boliao.sunshine.fetchers;

import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.utils.LogUtil;

/**
 * 内容抓取类的基类，提供抓取器需要的公共方法
 * 
 * @author Liaobo
 * 
 */
public class BaseFetcher {

	// 日志记录器
	private static Logger logger = Logger.getLogger(URLFetcher.class);

	// 系统异常，日志记录器
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

}
