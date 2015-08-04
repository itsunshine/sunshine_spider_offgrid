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
 * ����Ѷ����Ƹ����ҳ���ȡ��Ϣ��ץȡ��
 * 
 * @author Liaobo
 * 
 */
public class ContentExtractionFromTencentHR extends BaseContentExtraction {

	// ��־��¼��
	private final Logger logger = Logger.getLogger(ContentExtractionFromTencentHR.class);
	private final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// ������Ƹҳ��վ���ַ�������
	public final String SITE = "TENCENTHR";
	// ��˾����
	public final String COMPANYNAME = "��Ѷ��˾";

	// ����Ѷ��Ƹҳ��ץȡ����ƸҪ�����ݵ�ץȡ��
	public static ContentExtractionFromTencentHR contentExtractionFromTencentHR = new ContentExtractionFromTencentHR();

	// ����Ƹ������ϸ���л�ȡ�������������
	public static Pattern pattern = Pattern.compile("(\\<li\\>(.*?)\\</li\\>)+?");

	private ContentExtractionFromTencentHR() {
	}

	// ȡ����Ѷ��ƸҪ��ҳ�棬��Ƹ���ݵ�ץȡ��
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
	 * ��ȡ�����Ĺ�������
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
	 * ���µ�ģ�壬�Թ���ְ���Ҫ������滻��
	 * 
	 * @param jobDemandContent
	 *            ����Ҫ������
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
	 * ������ҳ���ݣ���ȡ��Ҫ�ı���
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
		// "<table class=\"tablelist textl\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr class=\"h\"><td colspan=\"3\" class=\"l2 bold size16\" id=\"sharetitle\">SNG02-���������̨�����߼�����ʦ�����ڣ�</td></tr></tbody></table>";
		// TableColumn titleCol = ParseUtils.parseTag(content,
		// TableColumn.class, "id", "sharetitle");

		String testStr = "����ְ��</div> <ul class=\"squareli\"><li>����ORACLE���ݿ�Ĺ滮����ƺ�ʵʩ�������߿��ü�Ⱥ�����֡����ݲ��ԡ������Ż����洢����ȣ�</li><li>����ORACLE��Ⱥ���ճ����������Լ�ء����ݹ������������¼����������Ż���������ϰ�ȣ�</li><li>����������û��Ϳ�������ͬ�µ�������Ӧ������֧�֣����������������ݿ���������������¡�ȣ�</li><li>�������ݿ���ز�Ʒ�ļܹ��Ż�������(�汾�������߿����Խ��衢�����ݽ����)������������̵Ľ�����ĵ������衣</li>";
		Matcher m = pattern.matcher(testStr);
		while (m.find()) {
			System.out.println(m.group(2));
		}
	}
}
