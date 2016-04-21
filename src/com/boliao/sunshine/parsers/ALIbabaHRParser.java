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
 * ����Ͱ���Ƹҳ�������
 * 
 * @author liaobo
 * 
 */
public class ALIbabaHRParser implements BaseParser {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(ALIbabaHRParser.class);

	// �ٶ���Ƹҳ�������ʵ��
	private static ALIbabaHRParser alibabaHRParser = new ALIbabaHRParser();
	private boolean fetchFLag = true;
	// �ٶ���Ƹҳ��վ���ַ�������
	public final String SITE = "ALIBABAHR";
	// ���·�������Ƹ����
	private String maxDateStr = "";

	/**
	 * ��ðٶ�ҳ�������ʵ��
	 * 
	 * @return
	 */
	public static ALIbabaHRParser getInstance() {
		if (alibabaHRParser == null) {
			synchronized (ALIbabaHRParser.class) {
				if (alibabaHRParser == null) {
					alibabaHRParser = new ALIbabaHRParser();
				}
			}
		}
		return alibabaHRParser;
	}

	/**
	 * ˽�й��췽��
	 */
	private ALIbabaHRParser() {

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
			throw new RuntimeException("������Ƹҳ��,html���ݽ���������");
		}
		try {
			content = ParseUtils.reomveTags(content, TableRow.class, "style", "display:none");
			getContentLinks(content, jobDemandArts, null, false);
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
	private void getContentLinks(String content, List<JobDemandArt> jobDemandArts, LinkFilter filter, boolean useFilter)
			throws ParseException, ParserException {
		// ��ȡ��������ҳ�е� URL
		List<TableRow> rows = ParseUtils.parseTags(content, TableRow.class);
		for (TableRow row : rows) {
			String text = row.getStringText();
			String dateStr = row.getChildren().elementAt(9).toPlainTextString();
			String lastDateRecord = ConfigService.getInstance()
					.getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
			if (StringUtils.isBlank(lastDateRecord) || dateStr.compareTo(lastDateRecord) > 0) {
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
		List<LinkTag> linkTags = ParseUtils.parseTags(pageContent, LinkTag.class, "title", "��һҳ");
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
		ALIbabaHRParser tencentHRParser = ALIbabaHRParser.getInstance();
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
