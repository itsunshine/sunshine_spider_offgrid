/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.nodes.TagNode;
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
public class BaiduHRParser implements BaseParser {

	// 日志记录器
	private static final Logger logger = Logger.getLogger(BaiduHRParser.class);

	// 腾旭招聘页面解析器实例
	private static BaiduHRParser baiduHRParser = new BaiduHRParser();
	private boolean fetchFLag = true;
	// 腾旭招聘页面站点字符串常量
	public final String SITE = "BAIDUHR";
	// 最新发布的招聘日期
	private String maxDateStr = "";
	// 百度下一页的url模板
	private final String firstPageUrl = "http://talent.baidu.com/baidu/web/templet1000/index/corpwebPosition1000baidu!getPostListByConditionBaidu?pc.currentPage=1&pc.rowSize=10&releaseTime=0&keyWord=&positionType=0%2F1227%2F10002&trademark=1&workPlaceCode=&positionName=&recruitType=2&brandCode=1&searchType=1&workPlaceNameV=&positionTypeV=0%2F1227%2F10002&keyWordV=";
	// 第一页
	private int page = 1;

	/**
	 * 获得腾讯页面解析器实例
	 * 
	 * @return
	 */
	public static BaiduHRParser getInstance() {
		if (baiduHRParser == null) {
			synchronized (BaiduHRParser.class) {
				if (baiduHRParser == null) {
					baiduHRParser = new BaiduHRParser();
				}
			}
		}
		return baiduHRParser;
	}

	/**
	 * 私有构造方法
	 */
	private BaiduHRParser() {

	}

	/**
	 * 根据htmlContent抽取，要抓取网页内容的url
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parseContent(String htmlContent) {
		// 取出有真正职业简介内容的url
		String conContent = SpideContentUtil.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX, CommonConstants.CONTENT_END_PREFIX, SITE, ConfigService.getInstance());
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
		String pageContent = SpideContentUtil.getContent(htmlContent, CommonConstants.PAGECONTENT_START_PREFIX, CommonConstants.PAGECONTENT_END_PREFIX, SITE, ConfigService
				.getInstance());
		return pageContent;
	}

	@Override
	public List<JobDemandArt> getLinks(String htmlContent) {
		List<JobDemandArt> jobDemandArts = new ArrayList<JobDemandArt>();
		if (!fetchFLag) {
			return jobDemandArts;
		}
		String content = parseContent(htmlContent);
		// 如果解析出的内容为空，则返回空
		if (StringUtils.isBlank(content)) {
			throw new RuntimeException("百度招聘页面,html内容解析，出错。");
		}
		try {
			getContentLinks(content, jobDemandArts, null, false);
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
	private void getContentLinks(String content, List<JobDemandArt> jobDemandArts, LinkFilter filter, boolean useFilter) throws ParseException, ParserException {
		// 提取出下载网页中的 URL
		List<TableRow> tableRows = ParseUtils.parseTags(content, TableRow.class);
		for (TableRow row : tableRows) {
			String text = row.getStringText();
			String dateStr = row.getChildren().elementAt(7).toPlainTextString();
			String lastDateRecord = ConfigService.getInstance().getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
			if (StringUtils.isBlank(lastDateRecord) || dateStr.compareTo(lastDateRecord) > 0) {
				if (this.maxDateStr.compareTo(dateStr) < 0) {
					this.maxDateStr = dateStr;
				}
				List<String> conLinks = URLParserTool.fetchLinksFromContent(text, filter, useFilter);
				TagNode tn = (TagNode) row.getChildren().elementAt(5).getChildren().elementAt(1);
				String locatStr = tn.getAttribute("title");
				String techStr = row.getChildren().elementAt(3).toPlainTextString();
				JobDemandArt jobDemandArt = new JobDemandArt();
				jobDemandArt.setSource(conLinks.get(0));
				if (StringUtils.isNotBlank(locatStr)) {
					jobDemandArt.setLocation(locatStr);
				}
				if (StringUtils.isNotBlank(techStr) && StringUtils.trim(techStr).equals("技术")) {
					jobDemandArt.setJobType(JobTypeCnt.TECHNOLOGY);
				}
				jobDemandArt.setCreateTime(dateStr);
				jobDemandArts.add(jobDemandArt);
			} else {
				fetchFLag = false;
				break;
			}
		}
	}

	public static void main(String[] args) {
		String result = "<td> <a target=\"_blank\" href=\"/baidu/web/templet1000/index/corpwebPosition1000baidu!getOnePosition?postIdEnc=2EE97DB5B5F2A6225A573C7754F66C1A&brandCode=1&recruitType=2&lanType=1&operational=6637AA56FA08745E71A74EA6AC68D5FFF28F462DA4C19FB3FABC8882DE74DA1C0FDB6AEAC9F8C487108CCBE39D45983B54F375AA1CAE83E6A21F36A7DBB429FDA1AA45697C458F4E318F8DFB32711E38D130740ECA46D104EAAE68E6E5E266B2A5F490AF8EE77F247C58B479FA37A1101C8FE41D11E20AFFA9511543837DA597ADA993F4A79495C679D35888897C39FD21D98BCC2FE67575CAED499E9C86325CC3D44DA1C73F4DA945C7FA90CE9460F2\" class=\"col-4\">云平台部_Web前端高级工程师</a> </td> <td> <font title=\"技术\">技术</font> </td> <td> <span title=\"北京市\"><script type=\"text/javascript\"> var wpStr = \"北京市\"; if(wpStr.length > 10){ var tempStr = wpStr.substring(0,10)+\"...\"; document.write(tempStr); }else{ document.write(wpStr); } </script></span> </td> <td> 2015-03-14 </td>";
		TableRow row = null;
		row.getChildren().elementAt(3);
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
		if (pageContent.indexOf("下一页") >= 0) {
			String replaceStr = replaceStr();
			String nextPageLink = firstPageUrl.replaceAll("pc\\.currentPage=\\d", replaceStr);
			links.add(nextPageLink);
		}
		return links;
	}

	private String replaceStr() {
		synchronized (this) {
			String replace = "pc.currentPage";
			replace = replace + "=" + (++page);
			return replace;
		}
	}

	/**
	 * 获得最新的日期
	 * 
	 * @return
	 */
	public String getMaxDateStr() {
		return maxDateStr;
	}

	/**
	 * 获取站点字符串常量
	 */
	@Override
	public String getSite() {
		return SITE;
	}

	@Override
	public String getLastDateStr() {
		return ConfigService.getInstance().getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
	}
}
