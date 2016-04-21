/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.ParserException;

import com.boliao.sunshine.biz.constants.JobTypeCnt;
import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.filters.LinkFilter;
import com.boliao.sunshine.utils.ParseUtils;
import com.boliao.sunshine.utils.SpideContentUtil;
import com.boliao.sunshine.utils.URLParserTool;

/**
 * 腾旭招聘页面解析器
 * 
 * @author liaobo
 * 
 */
public class TencentHRParser implements BaseParser {

	// 日志记录器
	private static final Logger logger = Logger.getLogger(TencentHRParser.class);

	// 腾旭招聘页面解析器实例
	private static TencentHRParser tencentHRParser = new TencentHRParser();
	private boolean fetchFLag = true;
	// 腾旭招聘页面站点字符串常量
	public final String SITE = "TENCENTHR";
	// 最新发布的招聘日期
	private String maxDateStr = "";

	/**
	 * 获得腾讯页面解析器实例
	 * 
	 * @return
	 */
	public static TencentHRParser getInstance() {
		if (tencentHRParser == null) {
			synchronized (TencentHRParser.class) {
				if (tencentHRParser == null) {
					tencentHRParser = new TencentHRParser();
				}
			}
		}
		return tencentHRParser;
	}

	/**
	 * 私有构造方法
	 */
	private TencentHRParser() {

	}

	/**
	 * 根据htmlContent抽取，要抓取网页内容的url
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parseContent(String htmlContent) {
		// 取出有真正职业简介内容的url
		String conContent = SpideContentUtil.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX,
				CommonConstants.CONTENT_END_PREFIX, SITE, ConfigService.getInstance());
		return conContent;
	}

	/**
	 * 根据htmlContent,抽取下一页的链接地址
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parsePageContent(String htmlContent) {
		// 从网页中抽取出，下一页的地址
		String pageContent = SpideContentUtil.getContent(htmlContent, CommonConstants.PAGECONTENT_START_PREFIX,
				CommonConstants.PAGECONTENT_END_PREFIX, SITE, ConfigService.getInstance());
		return pageContent;
	}

	@Override
	public List<JobDemandArt> getLinks(String htmlContent, boolean isRecovery) {
		List<JobDemandArt> jobDemandArts = new ArrayList<JobDemandArt>();
		if (!fetchFLag) {
			return jobDemandArts;
		}
		String content = parseContent(htmlContent);
		// 如果解析出的内容为空，则返回空
		if (StringUtils.isBlank(content)) {
			throw new RuntimeException("腾讯招聘页面,html内容解析，出错。");
		}
		try {
			getContentLinks(content, jobDemandArts, null, false, isRecovery);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (ParserException e) {
			throw new RuntimeException(e);
		}
		return jobDemandArts;
	}

	/**
	 * 将可以获取招聘内容的url填入，搜集招聘内容的链表中
	 * 
	 * @param content
	 * @param jobDemandArts
	 * @param filter
	 * @param useFilter
	 * @throws ParseException
	 * @throws ParserException
	 */
	private void getContentLinks(String content, List<JobDemandArt> jobDemandArts, LinkFilter filter, boolean useFilter,
			boolean isRecovery) throws ParseException, ParserException {
		// 提取出下载网页中的 URL
		List<TableRow> oddRows = ParseUtils.parseTags(content, TableRow.class, "class", "odd");
		List<TableRow> evenRows = ParseUtils.parseTags(content, TableRow.class, "class", "even");
		List<TableRow> rows = new ArrayList<TableRow>();
		rows.addAll(oddRows);
		rows.addAll(evenRows);
		for (TableRow row : rows) {
			String text = row.getStringText();
			String dateStr = row.getChildren().elementAt(9).toPlainTextString();
			String lastDateRecord = ConfigService.getInstance()
					.getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
			if (!isRecovery) {
				if ((StringUtils.isBlank(lastDateRecord) || dateStr.compareTo(lastDateRecord) > 0)) {
					if (this.maxDateStr.compareTo(dateStr) < 0) {
						this.maxDateStr = dateStr;
					}
					List<String> conLinks = URLParserTool.fetchLinksFromContent(text, filter, useFilter);
					String numStr = row.getChildren().elementAt(5).toPlainTextString();
					String locatStr = row.getChildren().elementAt(7).toPlainTextString();
					String techStr = row.getChildren().elementAt(3).toPlainTextString();
					JobDemandArt jobDemandArt = new JobDemandArt();
					jobDemandArt.setSource(conLinks.get(0));
					if (StringUtils.isNotBlank(numStr)) {
						jobDemandArt.setHrNumber(Integer.parseInt(numStr));
					}
					if (StringUtils.isNotBlank(locatStr)) {
						jobDemandArt.setLocation(locatStr);
					}
					if (StringUtils.isNotBlank(techStr) && techStr.equals("技术类")) {
						jobDemandArt.setJobType(JobTypeCnt.TECHNOLOGY);
					}
					jobDemandArt.setCreateTime(dateStr);
					jobDemandArts.add(jobDemandArt);
				} else {
					fetchFLag = false;
					break;
				}
			} else {
				if (dateStr.compareTo(lastDateRecord) == 0) {
					List<String> conLinks = URLParserTool.fetchLinksFromContent(text, filter, useFilter);
					String numStr = row.getChildren().elementAt(5).toPlainTextString();
					String locatStr = row.getChildren().elementAt(7).toPlainTextString();
					String techStr = row.getChildren().elementAt(3).toPlainTextString();
					JobDemandArt jobDemandArt = new JobDemandArt();
					jobDemandArt.setSource(conLinks.get(0));
					if (StringUtils.isNotBlank(numStr)) {
						jobDemandArt.setHrNumber(Integer.parseInt(numStr));
					}
					if (StringUtils.isNotBlank(locatStr)) {
						jobDemandArt.setLocation(locatStr);
					}
					if (StringUtils.isNotBlank(techStr) && techStr.equals("技术类")) {
						jobDemandArt.setJobType(JobTypeCnt.TECHNOLOGY);
					}
					jobDemandArt.setCreateTime(dateStr);
					jobDemandArts.add(jobDemandArt);
				} else if (dateStr.compareTo(lastDateRecord) < 0) {
					fetchFLag = false;
					break;
				}
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.boliao.sunshine.parsers.BaseParser#getPageLinks(java.lang.String)
	 */
	@Override
	public List<String> getPageLinks(String htmlContent) {
		List<String> links = new ArrayList<String>();
		if (!fetchFLag) {
			return links;
		}
		String pageContent = parsePageContent(htmlContent);
		List<LinkTag> linkTags = ParseUtils.parseTags(pageContent, LinkTag.class, "id", "next");
		String nextPageLink = linkTags.get(0).getLink();
		if (StringUtils.equals(nextPageLink, ";")) {
			return links;
		}
		links.add(nextPageLink);
		return links;
	}

	/**
	 * 获得上次最新的日期
	 * 
	 * @return
	 */
	public String getLastDateStr() {
		return ConfigService.getInstance().getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
	}

	/**
	 * 获得最新的日期
	 * 
	 */
	public String getMaxDateStr() {
		return maxDateStr;
	}

	public static void main(String[] args) throws Exception {
		FileInputStream inputStream = new FileInputStream("E:/html/tencentHr.html");
		InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		StringBuilder sb = new StringBuilder();
		while (true) {
			if (line == null)
				continue;
			if (line.contains("</html>"))
				break;
			sb.append(line);
			line = br.readLine();
		}
		String htmlContent = sb.toString();
		System.out.println(htmlContent);
		TencentHRParser tencentHRParser = TencentHRParser.getInstance();
		// List<JobDemandArt> contentLinks =
		// tencentHRParser.getLinks(htmlContent);
		// for (String str : contentLinks) {
		// System.out.println(str);
		// }
		//
		// List<String> pageList = tencentHRParser.getPageLinks(htmlContent);
		// System.out.println("===================================================================");
		// for (String link : pageList) {
		// System.out.println(link);
		// }
		// if (StringUtils.isNotEmpty(tencentHRParser.getMaxDateStr())) {
		// ConfigService.getInstance().flushRecentRecord(tencentHRParser.getSite()
		// + CommonConstants.LAST_RECORD_DATE, tencentHRParser.getMaxDateStr());
		// ConfigService.getInstance().storeRecentDate("D:/workspace/spiderServiceUtil/src/com/boliao/sunshine/properties/lastDateRecord.properties");
		// }
	}

	/**
	 * 获取站点字符串常量
	 */
	@Override
	public String getSite() {
		return SITE;
	}
}
