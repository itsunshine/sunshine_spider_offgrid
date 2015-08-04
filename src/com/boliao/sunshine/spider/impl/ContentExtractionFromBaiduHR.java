/**
 * 
 */
package com.boliao.sunshine.spider.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.DefinitionList;
import org.htmlparser.tags.DefinitionListBullet;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;

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
 * 从百度的招聘内容页面获取信息的抓取器
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromBaiduHR extends BaseContentExtraction {

	// 日志记录器
	private final Logger logger = Logger.getLogger(ContentExtractionFromBaiduHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// 百度招聘页面站点字符串常量
	public final String SITE = "BAIDUHR";
	// 公司名字
	public final String COMPANYNAME = "百度公司";

	// 从百度招聘页面抓取，招聘要求内容的抓取器
	public static ContentExtractionFromBaiduHR contentExtractionFromBaiduHR = new ContentExtractionFromBaiduHR();

	// 从招聘的内容细节中获取具体的文字内容
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\/\\+,\\s]+)(\\s)*(\\<br\\>)?");

	private ContentExtractionFromBaiduHR() {
	}

	// 取得百度招聘要求页面，招聘内容的抓取器
	public static ContentExtractionFromBaiduHR getInstance() {
		if (contentExtractionFromBaiduHR == null) {
			synchronized (ContentExtractionFromBaiduHR.class) {
				if (contentExtractionFromBaiduHR == null) {
					contentExtractionFromBaiduHR = new ContentExtractionFromBaiduHR();
				}
			}
		}
		return contentExtractionFromBaiduHR;
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
			String definCon = definitionList.getChildrenHTML();
			List<DefinitionListBullet> definitionListBullets = ParseUtils.parseTags(StringUtils.trim(definCon), DefinitionListBullet.class);
			for (int i = 0; i < definitionListBullets.size(); i++) {
				if (i == 5) {
					DefinitionListBullet definitionListBullet = definitionListBullets.get(i);
					String numStr = definitionListBullet.getChildrenHTML();
					numStr = StringUtils.trim(numStr);
					if (numStr.matches("\\d+")) {
						jobDemandArt.setHrNumber(new Integer(numStr.trim()));
					}
					if (StringUtils.equals("若干", StringUtils.trim(numStr))) {
						jobDemandArt.setHrNumber(CommonConstants.N_NUMBER);
					}
				}
			}
			List<Object> jobs = new ArrayList<Object>();
			// 如果是百度公司的抓取链接，就将operational后面的字符串全去掉。
			String source = jobDemandArt.getSource();
			source = source.substring(0, source.indexOf("&operational"));
			jobDemandArt.setSource(source);
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
		List<Div> jobDivs = ParseUtils.parseTags(usefullContent, Div.class, "style", "word-break:break-all;word-spacing: normal;");
		List<String> rpList = new ArrayList<String>(8);
		List<String> cdList = new ArrayList<String>(8);
		for (int i = 0; i < jobDivs.size(); i++) {
			Div div = jobDivs.get(i);
			// 工作要求
			if (i == 0) {
				String jobDesc = div.getStringText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					rpList.add(m.group(1));

				}
			}
			// 工作职责
			if (i == 1) {
				String jobDesc = div.getStringText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					cdList.add(m.group(1));
				}
			}
		}

		return VelUtil.cstJobCd(jobDemandArt.getTitle(), rpList, cdList);
	}

	/**
	 * 根据网页内容，获取需要的标题
	 * 
	 * @param htmlContent
	 * @return jobTitle
	 */
	private String obtainTitle(String htmlContent) {
		HeadingTag headingTag = ParseUtils.parseTag(htmlContent, HeadingTag.class, "class", "hrs_grayBorderTitle");
		String title = headingTag.getChild(0).getText();
		return title;
	}

	public static void main(String[] args) throws Exception {
		// String content =
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-公共组件后台开发高级工程师（深圳）</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "-全面负责IDC机房基础设施运维及管理工作，确保数据中心安全、可靠、高效运行   <br>   -制定公司IDC基础设施运维管理制度、操作手册、应急管理流程和应急操作预案";
		testStr = "具有2年以上设计开发经验，至少熟悉一种编程语言（PHP/Python/Java） ";
		testStr = "精通Linux/Unix平台上的Java/C/C++编程，熟悉脚本编程（如Bashx, Python等）";
		Matcher m = pattern.matcher(testStr);
		while (m.find()) {
			System.out.println(m.group(1));
		}

		// File file = new
		// File("D:/workspace/spiderServiceUtil/spider/jobDemandArt/20150318_jobDemandArt");
		// dealWithJobArt(file);
		// String source = "http://sodfak?s=1&a=2&operational=6637";
		// source = source.substring(0, source.indexOf("&operational"));
		// System.out.println(source);

		// FileReader fileReader = new FileReader("test.txt");
		// BufferedReader br = new BufferedReader(fileReader);
		// StringBuilder sb = new StringBuilder();
		// String line = br.readLine();
		// while (line != null) {
		// sb.append(line);
		// line = br.readLine();
		// }
		// fileReader.close();
		// br.close();
		// String testStr = sb.toString();
		// List<DefinitionList> definitionLists = ParseUtils.parseTags(testStr,
		// DefinitionList.class, "class", "hrs_jobInfo");
		// DefinitionList definitionList = definitionLists.get(0);
		// String definCon = definitionList.getChildrenHTML();
		// List<DefinitionListBullet> definitionListBullets =
		// ParseUtils.parseTags(definCon, DefinitionListBullet.class);
		// for (int i = 0; i < definitionListBullets.size(); i++) {
		// if (i == 5) {
		// DefinitionListBullet definitionListBullet =
		// definitionListBullets.get(i);
		// String numStr = definitionListBullet.getChildrenHTML();
		// numStr = StringUtils.trim(numStr);
		// if (numStr.matches("\\d+")) {
		// System.out.println(numStr.trim());
		// }
		// if (StringUtils.equals("若干", StringUtils.trim(numStr))) {
		// System.out.println(CommonConstants.N_NUMBER);
		// }
		// }
		// }
	}

	public static void dealWithJobArt(File filePath) {
		FileInputStream fr = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			fr = new FileInputStream(filePath);
			isr = new InputStreamReader(fr, "gbk");
			br = new BufferedReader(isr);
			String line = br.readLine();
			List<JobDemandArt> jobDemandArtList = new ArrayList<JobDemandArt>(100);
			while (line != null) {
				line = line.replace("'", "&apos;");
				System.out.println(line);
				System.out.println(line.substring(456, 500));
				JSONObject obj = JSONObject.fromObject(line);
				JobDemandArt jobDemandArt = (JobDemandArt) JSONObject.toBean(obj, JobDemandArt.class);
				jobDemandArtList.add(jobDemandArt);
				// 对数据做批量插入
				if (jobDemandArtList.size() == 100) {
					jobDemandArtList.clear();
				}
				line = br.readLine();
			}
			jobDemandArtList.clear();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fr != null) {
					fr.close();
				}
				if (isr != null) {
					isr.close();
				}
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
