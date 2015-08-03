package com.boliao.sunshine.parsers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.filters.LinkFilter;
import com.boliao.sunshine.utils.DrawlContent;
import com.boliao.sunshine.utils.HtmlParserTool;
import com.boliao.sunshine.utils.ParseUtils;

/**
 * 解析从ibm抓取下来的内容
 * 
 * @author liaobo
 * 
 */
public class IBMParser extends ParserAdapter {

	private static IBMParser ibmParser = new IBMParser();

	private boolean fetchFLag = true;

	/**
	 * 获得ibmParser实例
	 * 
	 * @return
	 */
	public static IBMParser getInstance() {
		if (ibmParser == null) {
			synchronized (IBMParser.class) {
				if (ibmParser == null) {
					ibmParser = new IBMParser();
				}
			}
		}
		return ibmParser;
	}

	/**
	 * 私有构造方法
	 */
	private IBMParser() {

	}

	/**
	 * 根据htmlContent抽取，要抓取网页内容的url
	 * 
	 * @param htmlContent
	 * @return
	 */
	@Override
	public String parseContent(String htmlContent) {
		// 从网页中抽取出，下一页的地址
		String content = DrawlContent
				.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX,
						CommonConstants.CONTENT_END_PREFIX, ConfigService
								.getInstance());
		return content;
	}

	/**
	 * 根据htmlContent,抽取下一页的链接地址
	 * 
	 * @param htmlContent
	 * @return
	 */
	@Override
	public String parsePageContent(String htmlContent) {
		// 从网页中抽取出，下一页的地址
		String pageContent = DrawlContent.getContent(htmlContent,
				CommonConstants.PAGECONTENT_START_PREFIX,
				CommonConstants.PAGECONTENT_END_PREFIX, ConfigService
						.getInstance());
		return pageContent;
	}

	/**
	 * 获取待抓取的url
	 */
	@Override
	public List<String> getLinks(String htmlContent) {
		List<String> links = new ArrayList<String>();
		if (!fetchFLag) {
			return links;
		}
		LinkFilter filter = DrawlContent.getFilter(htmlContent,
				CommonConstants.URL_FILTER, ConfigService.getInstance());
		String content = parseContent(htmlContent);
		getContent(content, links, filter, false);
		// 获得最大的日期
		getRecentDate(content);
		return links;
	}

	/**
	 * 获得htmlContent里的页面内容
	 * 
	 * @param htmlContent
	 * @param links
	 * @param filter
	 */
	private void getContent(String htmlContent, List<String> links,
			LinkFilter filter, boolean useFilter) {
		// 提取出下载网页中的 URL
		List<TableRow> trs = ParseUtils.parseTags(htmlContent, TableRow.class);
		for (TableRow tr : trs) {
			String text = tr.getStringText();
			List<TableColumn> tds = ParseUtils.parseTags(text,
					TableColumn.class, "class", "dw-nowrap");
			String dateStr = getDateStr(tds);
			if (StringUtils.isBlank(limitDateStr)
					|| dateStr.compareTo(limitDateStr) > 0) {
				List<String> getLinks = HtmlParserTool.generateLinks(text,
						filter, useFilter);
				links.addAll(getLinks);
			} else {
				fetchFLag = false;
				break;
			}

		}
	}

	@Override
	public void getRecentDate(String content) {
		List<TableColumn> tds = ParseUtils.parseTags(content,
				TableColumn.class, "class", "dw-nowrap");
		setMaxDate(tds);
	}

	@Override
	public List<String> getPageLinks(String htmlContent) {
		List<String> links = new ArrayList<String>();
		if (!fetchFLag) {
			return links;
		}
		LinkFilter filter = DrawlContent.getFilter(htmlContent,
				CommonConstants.URL_FILTER, ConfigService.getInstance());
		String pageContent = parsePageContent(htmlContent);
		links = HtmlParserTool.generateLinks(pageContent, filter, true);
		return links;
	}

}
