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
 * ���Ա�����Ƹ����ҳ���ȡ��Ϣ��ץȡ��
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromALIbabaHR extends BaseContentExtraction {

	// ��־��¼��
	private final Logger logger = Logger.getLogger(ContentExtractionFromALIbabaHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// �Ա���Ƹҳ��վ���ַ�������
	public final String SITE = "ALIBABAHR";
	// ��˾����
	public final String COMPANYNAME = "����Ͱ͹�˾";

	// ���Ա���Ƹҳ��ץȡ����ƸҪ�����ݵ�ץȡ��
	public static ContentExtractionFromALIbabaHR contentExtractionFromALIbabaHR = new ContentExtractionFromALIbabaHR();

	// ����Ƹ������ϸ���л�ȡ�������������
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w]+)(\\<br\\>)?");

	private ContentExtractionFromALIbabaHR() {
	}

	// ȡ���Ա���ƸҪ��ҳ�棬��Ƹ���ݵ�ץȡ��
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
			// ����URL��ַ����ȡ��ҳ����
			String htmlContent = HttpUtil.getHtmlContent(jobDemandArt.getSource());

			if (htmlContent == null) {
				LogUtil.warn(logger, "�޷���ȡ��" + jobDemandArt.getSource() + "����ַ������");
				throw new RuntimeException("�޷���ȡ��" + jobDemandArt.getSource() + "����ַ������");
			}
			String useFullContent = getUseFullContent(htmlContent);
			jobDemandArt.setTitle(obtainTitle(useFullContent));
			jobDemandArt.setContent(obtainContent(useFullContent, jobDemandArt));
			jobDemandArt.setCompanyName(COMPANYNAME);
			// ��ȡ��Ƹ����
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
			LogUtil.error(errorLogger, "��ȡ��ҳ����ʧ�ܣ�" + jobDemandArt.getSource(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * ��ȡ���������õ���Ϣ
	 * 
	 * @param htmlContent
	 *            ��ȫ����ҳ����
	 * @return ���õĹ���������Ϣ
	 */
	private String getUseFullContent(String htmlContent) {
		// ����ҳ�г�ȡ������һҳ�ĵ�ַ
		String usefullContent = SpideContentUtil.getContent(htmlContent, CommonConstants.JOBCONTENT_START_PREFIX, CommonConstants.JOBCONTENT_END_PREFIX, SITE, ConfigService
				.getInstance());
		return usefullContent;
	}

	/**
	 * ��ȡ�����Ĺ������ݣ������µ�����ģ������ַ����滻
	 * 
	 * @param usefullContent
	 * @return
	 * @throws IOException
	 */
	private String obtainContent(String usefullContent, JobDemandArt jobDemandArt) throws IOException {
		List<TableRow> trList = ParseUtils.parseTags(usefullContent, TableRow.class);
		// �������������ʡ�ѧ�������ŵ���Ƹ��Ϣ
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
			// ����Ҫ��
			if (i == 0) {
				String jobDesc = paragraphTag.getText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					rpList.add(m.group(1));
				}
			}
			// ����ְ��
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
	 * ������ҳ���ݣ���ȡ��Ҫ�ı���
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
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-���������̨�����߼�����ʦ�����ڣ�</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "-ȫ�渺��IDC����������ʩ��ά����������ȷ���������İ�ȫ���ɿ�����Ч����<br>-�ƶ���˾IDC������ʩ��ά�����ƶȡ������ֲᡢӦ���������̺�Ӧ������Ԥ��";
		Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w]+)(\\<br\\>)?");
		Matcher m = pattern.matcher(testStr);
		while (m.find()) {
			System.out.println(m.group(1));
		}
	}
}
