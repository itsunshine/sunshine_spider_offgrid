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
 * ������Ƹҳ�������
 * 
 * @author liaobo
 * 
 */
public class BaiduHRParser implements BaseParser {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(BaiduHRParser.class);

	// ������Ƹҳ�������ʵ��
	private static BaiduHRParser baiduHRParser = new BaiduHRParser();
	private boolean fetchFLag = true;
	// ������Ƹҳ��վ���ַ�������
	public final String SITE = "BAIDUHR";
	// ���·�������Ƹ����
	private String maxDateStr = "";
	// �ٶ���һҳ��urlģ��
	private final String firstPageUrl = "http://talent.baidu.com/baidu/web/templet1000/index/corpwebPosition1000baidu!getPostListByConditionBaidu?pc.currentPage=1&pc.rowSize=10&releaseTime=0&keyWord=&positionType=0%2F1227%2F10002&trademark=1&workPlaceCode=&positionName=&recruitType=2&brandCode=1&searchType=1&workPlaceNameV=&positionTypeV=0%2F1227%2F10002&keyWordV=";
	// ��һҳ
	private int page = 1;

	/**
	 * �����Ѷҳ�������ʵ��
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
	 * ˽�й��췽��
	 */
	private BaiduHRParser() {

	}

	/**
	 * ����htmlContent��ȡ��Ҫץȡ��ҳ���ݵ�url
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parseContent(String htmlContent) {
		// ȡ��������ְҵ������ݵ�url
		String conContent = SpideContentUtil.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX, CommonConstants.CONTENT_END_PREFIX, SITE, ConfigService.getInstance());
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
		// ���������������Ϊ�գ��򷵻ؿ�
		if (StringUtils.isBlank(content)) {
			throw new RuntimeException("�ٶ���Ƹҳ��,html���ݽ���������");
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
	 * �����Ի�ȡ��Ƹ���ݵ�url���룬�Ѽ���Ƹ���ݵ�������
	 * 
	 * @param content
	 * @param jobDemandArts
	 * @param filter
	 * @param useFilter
	 * @throws ParseException
	 * @throws ParserException
	 */
	private void getContentLinks(String content, List<JobDemandArt> jobDemandArts, LinkFilter filter, boolean useFilter) throws ParseException, ParserException {
		// ��ȡ��������ҳ�е� URL
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
				if (StringUtils.isNotBlank(techStr) && StringUtils.trim(techStr).equals("����")) {
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
		String result = "<td> <a target=\"_blank\" href=\"/baidu/web/templet1000/index/corpwebPosition1000baidu!getOnePosition?postIdEnc=2EE97DB5B5F2A6225A573C7754F66C1A&brandCode=1&recruitType=2&lanType=1&operational=6637AA56FA08745E71A74EA6AC68D5FFF28F462DA4C19FB3FABC8882DE74DA1C0FDB6AEAC9F8C487108CCBE39D45983B54F375AA1CAE83E6A21F36A7DBB429FDA1AA45697C458F4E318F8DFB32711E38D130740ECA46D104EAAE68E6E5E266B2A5F490AF8EE77F247C58B479FA37A1101C8FE41D11E20AFFA9511543837DA597ADA993F4A79495C679D35888897C39FD21D98BCC2FE67575CAED499E9C86325CC3D44DA1C73F4DA945C7FA90CE9460F2\" class=\"col-4\">��ƽ̨��_Webǰ�˸߼�����ʦ</a> </td> <td> <font title=\"����\">����</font> </td> <td> <span title=\"������\"><script type=\"text/javascript\"> var wpStr = \"������\"; if(wpStr.length > 10){ var tempStr = wpStr.substring(0,10)+\"...\"; document.write(tempStr); }else{ document.write(wpStr); } </script></span> </td> <td> 2015-03-14 </td>";
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
		if (pageContent.indexOf("��һҳ") >= 0) {
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
	 * ������µ�����
	 * 
	 * @return
	 */
	public String getMaxDateStr() {
		return maxDateStr;
	}

	/**
	 * ��ȡվ���ַ�������
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
