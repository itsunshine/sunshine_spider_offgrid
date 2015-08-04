package com.boliao.sunshine.parsers;

import java.util.List;

import com.boliao.sunshine.biz.model.JobDemandArt;

/**
 * 网页解析服务接口
 * 
 * @author liaobo
 * 
 */
public interface BaseParser {

	/**
	 * 根据content内容，获取链接
	 * 
	 * @param content
	 * @return
	 */
	public List<JobDemandArt> getLinks(String content);

	/**
	 * 获得页码的内容
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getPageLinks(String content);

	/**
	 * 获得上次最新的日期
	 * 
	 * @return
	 */
	public String getLastDateStr();

	/**
	 * 获得最新日期
	 * 
	 * @return
	 */
	public String getMaxDateStr();

	/**
	 * 获取站点名字常量
	 * 
	 * @return
	 */
	public String getSite();
}
