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
 * �Ӱٶȵ���Ƹ����ҳ���ȡ��Ϣ��ץȡ��
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromBaiduHR extends BaseContentExtraction {

	// ��־��¼��
	private final Logger logger = Logger.getLogger(ContentExtractionFromBaiduHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// �ٶ���Ƹҳ��վ���ַ�������
	public final String SITE = "BAIDUHR";
	// ��˾����
	public final String COMPANYNAME = "�ٶȹ�˾";

	// �Ӱٶ���Ƹҳ��ץȡ����ƸҪ�����ݵ�ץȡ��
	public static ContentExtractionFromBaiduHR contentExtractionFromBaiduHR = new ContentExtractionFromBaiduHR();

	// ����Ƹ������ϸ���л�ȡ�������������
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\/\\+,\\s]+)(\\s)*(\\<br\\>)?");

	private ContentExtractionFromBaiduHR() {
	}

	// ȡ�ðٶ���ƸҪ��ҳ�棬��Ƹ���ݵ�ץȡ��
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
					if (StringUtils.equals("����", StringUtils.trim(numStr))) {
						jobDemandArt.setHrNumber(CommonConstants.N_NUMBER);
					}
				}
			}
			List<Object> jobs = new ArrayList<Object>();
			// ����ǰٶȹ�˾��ץȡ���ӣ��ͽ�operational������ַ���ȫȥ����
			String source = jobDemandArt.getSource();
			source = source.substring(0, source.indexOf("&operational"));
			jobDemandArt.setSource(source);
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
		List<Div> jobDivs = ParseUtils.parseTags(usefullContent, Div.class, "style", "word-break:break-all;word-spacing: normal;");
		List<String> rpList = new ArrayList<String>(8);
		List<String> cdList = new ArrayList<String>(8);
		for (int i = 0; i < jobDivs.size(); i++) {
			Div div = jobDivs.get(i);
			// ����Ҫ��
			if (i == 0) {
				String jobDesc = div.getStringText();
				jobDesc = StringHelperUtil.removeBlankWord(jobDesc);
				Matcher m = pattern.matcher(jobDesc);
				while (m.find()) {
					rpList.add(m.group(1));

				}
			}
			// ����ְ��
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
	 * ������ҳ���ݣ���ȡ��Ҫ�ı���
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
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-���������̨�����߼�����ʦ�����ڣ�</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "-ȫ�渺��IDC����������ʩ��ά����������ȷ���������İ�ȫ���ɿ�����Ч����   <br>   -�ƶ���˾IDC������ʩ��ά�����ƶȡ������ֲᡢӦ���������̺�Ӧ������Ԥ��";
		testStr = "����2��������ƿ������飬������Ϥһ�ֱ�����ԣ�PHP/Python/Java�� ";
		testStr = "��ͨLinux/Unixƽ̨�ϵ�Java/C/C++��̣���Ϥ�ű���̣���Bashx, Python�ȣ�";
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
		// if (StringUtils.equals("����", StringUtils.trim(numStr))) {
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
				// ����������������
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
