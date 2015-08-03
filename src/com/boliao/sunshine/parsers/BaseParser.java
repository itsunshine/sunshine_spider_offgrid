package com.boliao.sunshine.parsers;

import java.util.List;

/**
 * 网页解析服务接口
 * 
 * @author liaobo
 * 
 */
public interface BaseParser {

	/**
	 * 根据htmlContent抽取，要抓取网页内容的url
	 * 
	 * @param htmlContent
	 * @return
	 */
	public String parseContent(String htmlContent);

	/**
	 * 根据htmlContent,抽取下一页的链接地址
	 * 
	 * @param htmlContent
	 * @return
	 */
	public String parsePageContent(String htmlContent);

	/**
	 * 根据content内容，获取链接
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getLinks(String content);

	/**
	 * 获得页码的内容
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getPageLinks(String content);

	/**
	 * 获得最近一篇文章的日期
	 * 
	 * @param content
	 * @return 日期格式的字符串
	 */
	public void getRecentDate(String content);
}
