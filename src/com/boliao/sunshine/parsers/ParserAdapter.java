/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;

/**
 * @author liaobo
 * 
 */
public class ParserAdapter implements BaseParser {

	/** 最近的日期 */
	public String maxDate = null;

	/** 该日期以后的内容不抓取 */
	protected String limitDateStr = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.boliao.sunshine.parsers.BaseParser#getLinks(java.lang.String)
	 */
	@Override
	public List<String> getLinks(String content) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.boliao.sunshine.parsers.BaseParser#getPageLinks(java.lang.String)
	 */
	@Override
	public List<String> getPageLinks(String content) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.boliao.sunshine.parsers.BaseParser#getRecentDate(java.lang.String)
	 */
	@Override
	public void getRecentDate(String content) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.boliao.sunshine.parsers.BaseParser#parseContent(java.lang.String)
	 */
	@Override
	public String parseContent(String htmlContent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.boliao.sunshine.parsers.BaseParser#parsePageContent(java.lang.String)
	 */
	@Override
	public String parsePageContent(String htmlContent) {
		return null;
	}

	/**
	 * 设置日期的最大值
	 * 
	 * @param nodes
	 */
	protected void setMaxDate(List<?> nodes) {
		for (Object obj : nodes) {
			Node node = (Node) obj;
			String dateStr = node.getChildren().elementAt(0).toHtml();
			if (StringUtils.isBlank(maxDate) || maxDate.compareTo(dateStr) < 0) {
				maxDate = dateStr;
			}
		}
	}

	public String getDateStr(List<?> nodes) {
		for (Object obj : nodes) {
			Node node = (Node) obj;
			String dateStr = node.getChildren().elementAt(0).toHtml();
			return dateStr;
		}
		return null;
	}

	public void setLimitDateStr(String limitDateStr) {
		this.limitDateStr = limitDateStr;
	}

}
