/**
 * 
 */
package com.boliao.sunshine.spider.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.spider.BaseContentExtraction;
import com.boliao.sunshine.utils.ParseUtils;
import com.boliao.sunshine.utils.SpideContentUtil;
import com.boliao.sunshine.utils.VelUtil;

/**
 * 从腾讯的招聘内容页面获取信息的抓取器
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromTencentHR extends BaseContentExtraction {

	// 日志记录器
	private final Logger logger = Logger.getLogger(ContentExtractionFromTencentHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// 腾旭招聘页面站点字符串常量
	public final String SITE = "TENCENTHR";
	// 公司名字
	public final String COMPANYNAME = "腾讯公司";

	// 从腾讯招聘页面抓取，招聘要求内容的抓取器
	public static ContentExtractionFromTencentHR contentExtractionFromTencentHR = new ContentExtractionFromTencentHR();

	// 从招聘的内容细节中获取具体的文字内容
	public static Pattern pattern = Pattern.compile("(\\<li\\>(.*?)\\</li\\>)+?");

	private ContentExtractionFromTencentHR() {
	}

	// 取得腾讯招聘要求页面，招聘内容的抓取器
	public static ContentExtractionFromTencentHR getInstance() {
		if (contentExtractionFromTencentHR == null) {
			synchronized (ContentExtractionFromTencentHR.class) {
				if (contentExtractionFromTencentHR == null) {
					contentExtractionFromTencentHR = new ContentExtractionFromTencentHR();
				}
			}
		}
		return contentExtractionFromTencentHR;
	}

	@Override
	public List<Object> execute(JobDemandArt jobDemandArt) {
		try {
			// 根据URL地址，获取网页内容
			String htmlContent = HttpUtil.getHtmlContent(jobDemandArt.getSource());

			if (htmlContent == null) {
				LogUtil.warn(logger, "无法获取【" + jobDemandArt.getSource() + "】网址的内容");
				throw new RuntimeException("无法获取【" + jobDemandArt.getSource() + "】网址的内容");
			}
			String useFullContent = getUseFullContent(htmlContent);
			jobDemandArt.setTitle(obtainTitle(useFullContent));
			jobDemandArt.setContent(obtainContent(useFullContent, jobDemandArt));
			jobDemandArt.setCompanyName(COMPANYNAME);
			List<Object> jobs = new ArrayList<Object>();
			jobs.add(jobDemandArt);
			return jobs;
		} catch (Exception e) {
			LogUtil.error(errorLogger, "获取网页内容失败：" + jobDemandArt.getSource(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取内容中有用的信息
	 * 
	 * @param htmlContent
	 *            完全的网页内容
	 * @return 有用的工作内容信息
	 */
	private String getUseFullContent(String htmlContent) {
		// 从网页中抽取出，下一页的地址
		String usefullContent = SpideContentUtil.getContent(htmlContent, CommonConstants.JOBCONTENT_START_PREFIX, CommonConstants.JOBCONTENT_END_PREFIX, SITE, ConfigService
				.getInstance());
		return usefullContent;
	}

	/**
	 * 获取真正的工作内容
	 * 
	 * @param usefullContent
	 * @return
	 * @throws IOException
	 */
	private String obtainContent(String usefullContent, JobDemandArt jobDemandArt) throws IOException {
		String jobDemandContent = ParseUtils.reomveTags(usefullContent, TableRow.class, "class", "h");
		jobDemandContent = ParseUtils.reomveTags(jobDemandContent, TableRow.class, "class", "c bottomline");
		Set<Integer> indexes = new HashSet<Integer>(2);
		indexes.add(2);
		indexes.add(3);
		jobDemandContent = ParseUtils.removeNodesByIndexes(jobDemandContent, indexes, TableRow.class);

		return replaceNewConTemp(jobDemandContent, jobDemandArt);
	}

	/**
	 * 用新的模板，对工作职责和要求进行替换。
	 * 
	 * @param jobDemandContent
	 *            工作要求内容
	 * @return
	 * @throws IOException
	 */
	private String replaceNewConTemp(String jobDemandContent, JobDemandArt jobDemandArt) throws IOException {
		String[] conStrs = StringUtils.substringsBetween(jobDemandContent, "<div class=\"lightblue\">", "</ul>");
		Matcher m = pattern.matcher(conStrs[0]);
		List<String> rpList = new ArrayList<String>(8);
		while (m.find()) {
			rpList.add(m.group(2));
		}
		m = pattern.matcher(conStrs[1]);
		List<String> cdList = new ArrayList<String>(8);
		while (m.find()) {
			cdList.add(m.group(2));
		}
		return VelUtil.cstJobCd(jobDemandArt.getTitle(), rpList, cdList);
	}

	/**
	 * 根据网页内容，获取需要的标题
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String obtainTitle(String htmlContent) {
		TableColumn titleCol = ParseUtils.parseTag(htmlContent, TableColumn.class, "id", "sharetitle");
		String title = titleCol.getChild(0).getText();
		return title;
	}

	public static void main(String[] args) {
		// String content =
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-公共组件后台开发高级工程师（深圳）</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "工作职责：</div> <ul class=\"squareli\"><li>负责ORACLE数据库的规划、设计和实施，包括高可用集群、容灾、备份策略、性能优化、存储管理等；</li><li>负责ORACLE集群的日常管理，可用性监控、备份管理、容量管理、事件处理、性能优化、容灾演习等；</li><li>负责对最终用户和开发测试同事的问题响应及技术支持，比如问题解决、数据库解决方案、环境克隆等；</li><li>负责数据库相关产品的架构优化和升级(版本升级、高可用性建设、主数据建设等)，负责相关流程的建设和文档化建设。</li>";
		Matcher m = pattern.matcher(testStr);
		while (m.find()) {
			System.out.println(m.group(2));
		}
	}
}
