/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.utils.VelUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * ����Ͱ���Ƹҳ�������
 * 
 * @author liaobo
 * 
 */
public class ALIbabaHRParser2 implements BaseParser {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(ALIbabaHRParser2.class);

	// �ٶ���Ƹҳ�������ʵ��
	private static ALIbabaHRParser2 alibabaHRParser = new ALIbabaHRParser2();

	// �ٶ���Ƹҳ��վ���ַ�������
	public final String SITE = "ALIBABAHR";

	// �Ƿ����ץȡ�ı�ʶλ
	private boolean fetchFLag = true;

	// ���·�������Ƹ����
	private String maxDateStr = "";

	// ����Ƹ������ϸ���л�ȡ�������������
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\s|\\-|\\+|\\/|(|)]+)(\\<br\\/>)?");

	// ����ץȡ�����õ�charSet
	public static Set<Character> charSet = new HashSet<Character>();

	// ���ڸ�ʽ������
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	// ��˾����
	public final String COMPANYNAME = "����Ͱ͹�˾";

	// ���﹤����ȡҳ��
	public final String jobUrl = "https://job.alibaba.com/zhaopin/socialPositionList/doList.json?pageSize=10&pageIndex=$pageIndex&first=%E6%8A%80%E6%9C%AF%E7%B1%BB";

	// �������ݵ�ַ��Ϣ
	public final String detailUrl = "https://job.alibaba.com/zhaopin/position_detail.htm?positionId=$id";

	static {
		charSet.add('��');
		charSet.add('��');
		charSet.add('��');
		charSet.add('��');
		charSet.add('��');
		charSet.add('.');
	}

	/**
	 * ��ðٶ�ҳ�������ʵ��
	 * 
	 * @return
	 */
	public static ALIbabaHRParser2 getInstance() {
		if (alibabaHRParser == null) {
			synchronized (ALIbabaHRParser2.class) {
				if (alibabaHRParser == null) {
					alibabaHRParser = new ALIbabaHRParser2();
				}
			}
		}
		return alibabaHRParser;
	}

	/**
	 * ˽�й��췽��
	 */
	private ALIbabaHRParser2() {

	}

	@Override
	public List<JobDemandArt> getLinks(String htmlContent) {
		List<JobDemandArt> jobDemandArts = new ArrayList<JobDemandArt>();
		if (StringUtils.isBlank(htmlContent)) {
			return jobDemandArts;
		}
		try {
			JSONObject JobDemandAndArtJson = JSONObject.fromObject(htmlContent);
			boolean isSuccess = JobDemandAndArtJson.getBoolean("isSuccess");
			if (!isSuccess) {
				throw new RuntimeException("����Ͱͣ���������ץȡʧ�ܣ�����");
			}
			JSONArray datasArray = JobDemandAndArtJson.getJSONObject("returnValue").getJSONArray("datas");
			int size = datasArray.size();
			String lastDateRecord = ConfigService.getInstance()
					.getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
			for (int i = 0; i < size; i++) {
				JSONObject obj = datasArray.getJSONObject(i);
				String department = obj.getString("departmentName");
				String title = obj.getString("name");
				String location = obj.getString("workLocation");
				int recuitNumber = obj.getInt("recruitNumber");
				String degree = obj.getString("degree");
				long gmtCreate = obj.getLong("gmtModified");
				String requirement = obj.getString("requirement");
				String description = obj.getString("description");
				String content = this.obtainContent(title, requirement, description);
				Date date = new Date(gmtCreate);
				String createTime = sdf.format(date);
				String id = obj.getString("id");
				// �жϵ�ǰץȡ�����ݣ��Ƿ����£���������������ݣ�����ץȡ
				if (StringUtils.isBlank(lastDateRecord) || createTime.compareTo(lastDateRecord) > 0) {
					// ��¼��������
					if (this.maxDateStr.compareTo(createTime) < 0) {
						this.maxDateStr = createTime;
					}
					JobDemandArt jobDemandArt = new JobDemandArt();
					jobDemandArt.setCompanyName(COMPANYNAME);
					jobDemandArt.setCreateTime(createTime);
					jobDemandArt.setDepartmentName(department);
					jobDemandArt.setEducation(degree);
					jobDemandArt.setHrNumber(recuitNumber);
					jobDemandArt.setLocation(location);
					jobDemandArt.setTitle(title);
					jobDemandArt.setContent(content);
					jobDemandArt.setSource(detailUrl.replace("$id", id));
					jobDemandArts.add(jobDemandArt);
				} else {
					fetchFLag = false;
					break;
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return jobDemandArts;
	}

	/**
	 * �ӹ��������У�ȡ������Ҫ��
	 * 
	 * @param jobDesc
	 * @return
	 * @throws IOException
	 */
	private String obtainContent(String title, String jobDesc, String jobResp) throws IOException {
		List<String> rpList = new ArrayList<String>(8);
		List<String> cdList = new ArrayList<String>(8);
		// ��ӹ���Ҫ���б�
		Matcher m = pattern.matcher(jobDesc);
		while (m.find()) {
			String str = m.group(0);
			if (str.length() > 4) {
				char index = str.charAt(0);
				char symbol = str.charAt(1);
				if (StringUtils.isNumeric(String.valueOf(index)) && charSet.contains(symbol)) {
					str = str.substring(2, str.length());
				} else if (StringUtils.isNumeric(String.valueOf(index))) {
					str = str.substring(1, str.length());
				}
				str = str.trim();
				rpList.add(str);

			}
		}
		// ��ӹ���ְ���б�
		m = pattern.matcher(jobResp);
		while (m.find()) {
			String str = m.group(0);
			str = str.trim();
			if (str.length() > 4) {
				char index = str.charAt(0);
				char symbol = str.charAt(1);
				if (StringUtils.isNumeric(String.valueOf(index)) && charSet.contains(symbol)) {
					str = str.substring(2, str.length());
				} else if (StringUtils.isNumeric(String.valueOf(index))) {
					str = str.substring(1, str.length());
				}
				str = str.trim();
				cdList.add(str);
			}
		}
		return VelUtil.cstJobCd(title, rpList, cdList);
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
		if (StringUtils.isBlank(htmlContent) || !fetchFLag) {
			return links;
		}
		JSONObject JobDemandAndArtJson = JSONObject.fromObject(htmlContent);
		boolean isSuccess = JobDemandAndArtJson.getBoolean("isSuccess");
		if (!isSuccess) {
			throw new RuntimeException("����Ͱͣ���������ץȡʧ�ܣ�����");
		}
		int totalPage = JobDemandAndArtJson.getJSONObject("returnValue").getInt("totalPage");
		int pageIndex = JobDemandAndArtJson.getJSONObject("returnValue").getInt("pageIndex");
		if (totalPage == pageIndex) {
			return links;
		}
		links.add(jobUrl.replace("$pageIndex", String.valueOf(++pageIndex)));
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
		ALIbabaHRParser2 tencentHRParser = ALIbabaHRParser2.getInstance();
		String con = "��Ϥ����һ�ű�����ԣ������ø�����ʵ��ָ���Ĳ��Բ�Ʒ����Ҫ��JAVA/Python/Ruby/PHP�������ȣ��й����Բ�Ʒ�з��ɹ�����";
		// con = "�������Androidϵͳԭ���зḻ���ƶ��˿������飻 ";
		con = "������������Ŷӣ������ְ����ܰ����������� \\r<br\\/>1���ڼܹ�ʦ������ר�ҵĴ����£�����ϵͳ�ܹ�����ƣ��������ģ��Ŀ��������ܵ���\\r<br\\/>2�����뵽ҵ����ȥ����ҵ��һ��Ϊ�û������ֵ\\r<br\\/>3������Ĺ�����Ӫ��ƽ̨����Աƽ̨�����ƽ̨��";
		con = "Java��c++(c)����һ�ͨ";
		// ��ӹ���Ҫ���б�
		Matcher m = pattern.matcher(con);
		while (m.find()) {
			String str = m.group(0);
			if (str.length() > 4) {
				char index = str.charAt(0);
				char symbol = str.charAt(1);
				if (StringUtils.isNumeric(String.valueOf(index)) && charSet.contains(symbol)) {
					str = str.substring(2, str.length());
				} else if (StringUtils.isNumeric(String.valueOf(index))) {
					str = str.substring(1, str.length());
				}
				str = str.trim();
				System.out.println(str);
			}
		}
	}

	/**
	 * ��ȡվ���ַ�������
	 */
	@Override
	public String getSite() {
		return SITE;
	}
}
