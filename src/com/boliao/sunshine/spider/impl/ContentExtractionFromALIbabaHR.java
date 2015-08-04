/**
 * 
 */
package com.boliao.sunshine.spider.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.DefinitionList;
import org.htmlparser.tags.DefinitionListBullet;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TableRow;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.biz.utils.StringHelperUtil;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.spider.BaseContentExtraction;
import com.boliao.sunshine.utils.ParseUtils;
import com.boliao.sunshine.utils.SpideContentUtil;
import com.boliao.sunshine.utils.VelUtil;

/**
 * 从淘宝的招聘内容页面获取信息的抓取器
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromALIbabaHR extends BaseContentExtraction {

	// 日志记录器
	private final Logger logger = Logger.getLogger(ContentExtractionFromALIbabaHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// 淘宝招聘页面站点字符串常量
	public final String SITE = "ALIBABAHR";
	// 公司名字
	public final String COMPANYNAME = "阿里巴巴公司";

	// 从淘宝招聘页面抓取，招聘要求内容的抓取器
	public static ContentExtractionFromALIbabaHR contentExtractionFromALIbabaHR = new ContentExtractionFromALIbabaHR();

	// 从招聘的内容细节中获取具体的文字内容
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w]+)(\\<br\\>)?");

	private ContentExtractionFromALIbabaHR() {
	}

	// 取得淘宝招聘要求页面，招聘内容的抓取器
	public static ContentExtractionFromALIbabaHR getInstance() {
		if (contentExtractionFromALIbabaHR == null) {
			synchronized (ContentExtractionFromALIbabaHR.class) {
				if (contentExtractionFromALIbabaHR == null) {
					contentExtractionFromALIbabaHR = new ContentExtractionFromALIbabaHR();
				}
			}
		}
		return contentExtractionFromALIbabaHR;
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
			// 获取招聘人数
			List<DefinitionList> definitionLists = ParseUtils.parseTags(useFullContent, DefinitionList.class, "class", "hrs_jobInfo");
			DefinitionList definitionList = definitionLists.get(0);
			String definCon = definitionList.getText();
			List<DefinitionListBullet> definitionListBullets = ParseUtils.parseTags(definCon, DefinitionListBullet.class);
			for (int i = 0; i < definitionListBullets.size(); i++) {
				if (i == 4) {
					DefinitionListBullet definitionListBullet = definitionListBullets.get(i);
					String numStr = definitionListBullet.getChildrenHTML();
					if (numStr.matches("\\d+")) {
						jobDemandArt.setHrNumber(new Integer(numStr.trim()));
					}
				}
			}
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
	 * 获取真正的工作内容，并用新的内容模板进行字符串替换
	 * 
	 * @param usefullContent
	 * @return
	 * @throws IOException
	 */
	private String obtainContent(String usefullContent, JobDemandArt jobDemandArt) throws IOException {
		List<TableRow> trList = ParseUtils.parseTags(usefullContent, TableRow.class);
		// 解析出工作性质、学历、部门等招聘信息
		for (int i = 0; i < trList.size(); i++) {
			if (i == 0) {
				String jobTime = trList.get(i).getChildren().elementAt(11).toPlainTextString();
				jobDemandArt.setJobTime(StringUtils.trim(jobTime));
			} else if (i == 1) {
				String education = trList.get(i).getChildren().elementAt(7).toPlainTextString();
				String department = trList.get(i).getChildren().elementAt(3).toPlainTextString();
				jobDemandArt.setEducation(StringUtils.trim(education));
				jobDemandArt.setDepartmentName(StringUtils.trim(department));
			}
		}

		List<ParagraphTag> paragraphTags = ParseUtils.parseTags(usefullContent, ParagraphTag.class, "class", "detail-content");
		List<String> rpList = new ArrayList<String>(8);
		List<String> cdList = new ArrayList<String>(8);
		for (int i = 0; i < paragraphTags.size(); i++) {
			ParagraphTag paragraphTag = paragraphTags.get(i);
			// 工作要求
			if (i == 0) {
				String jobDesc = paragraphTag.getText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					rpList.add(m.group(1));
				}
			}
			// 工作职责
			if (i == 1) {
				String jobDesc = paragraphTag.getText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					cdList.add(m.group(1));
				}
			}
		}
		return VelUtil.cstJobCd(COMPANYNAME, rpList, cdList);
	}

	/**
	 * 根据网页内容，获取需要的标题
	 * 
	 * @param htmlContent
	 * @return jobTitle
	 */
	private String obtainTitle(String htmlContent) {
		HeadingTag headingTag = ParseUtils.parseTag(htmlContent, HeadingTag.class, "class", "bg-title");
		String title = headingTag.getChild(0).getText();
		return title;
	}

	public static void main(String[] args) {
		// String content =
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-公共组件后台开发高级工程师（深圳）</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "-全面负责IDC机房基础设施运维及管理工作，确保数据中心安全、可靠、高效运行<br>-制定公司IDC基础设施运维管理制度、操作手册、应急管理流程和应急操作预案";
		Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w]+)(\\<br\\>)?");
		Matcher m = pattern.matcher(testStr);
		while (m.find()) {
			System.out.println(m.group(1));
		}
	}
}
