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
 * ������Ƹҳ�������
 * 
 * @author liaobo
 * 
 */
public class TencentHRParser implements BaseParser {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(TencentHRParser.class);

	// ������Ƹҳ�������ʵ��
	private static TencentHRParser tencentHRParser = new TencentHRParser();
	private boolean fetchFLag = true;
	// ������Ƹҳ��վ���ַ�������
	public final String SITE = "TENCENTHR";
	// ���·�������Ƹ����
	private String maxDateStr = "";

	/**
	 * �����Ѷҳ�������ʵ��
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
	 * ˽�й��췽��
	 */
	private TencentHRParser() {

	}

	/**
	 * ����htmlContent��ȡ��Ҫץȡ��ҳ���ݵ�url
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parseContent(String htmlContent) {
		// ȡ��������ְҵ������ݵ�url
		String conContent = SpideContentUtil.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX,
				CommonConstants.CONTENT_END_PREFIX, SITE, ConfigService.getInstance());
		return conContent;
	}

	/**
	 * ����htmlContent,��ȡ��һҳ�����ӵ�ַ
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parsePageContent(String htmlContent) {
		// ����ҳ�г�ȡ������һҳ�ĵ�ַ
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
		// ���������������Ϊ�գ��򷵻ؿ�
		if (StringUtils.isBlank(content)) {
			throw new RuntimeException("��Ѷ��Ƹҳ��,html���ݽ���������");
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
	 * �����Ի�ȡ��Ƹ���ݵ�url���룬�Ѽ���Ƹ���ݵ�������
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
		// ��ȡ��������ҳ�е� URL
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
					if (StringUtils.isNotBlank(techStr) && techStr.equals("������")) {
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
					if (StringUtils.isNotBlank(techStr) && techStr.equals("������")) {
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
	 * ����ϴ����µ�����
	 * 
	 * @return
	 */
	public String getLastDateStr() {
		return ConfigService.getInstance().getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
	}

	/**
	 * ������µ�����
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
	 * ��ȡվ���ַ�������
	 */
	@Override
	public String getSite() {
		return SITE;
	}
}
