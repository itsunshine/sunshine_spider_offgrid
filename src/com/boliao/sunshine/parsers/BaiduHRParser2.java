/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.utils.SpideContentUtil;
import com.boliao.sunshine.utils.VelUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * ������Ƹҳ�������
 * 
 * @author liaobo
 * 
 */
public class BaiduHRParser2 implements BaseParser {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(BaiduHRParser2.class);

	// ������Ƹҳ�������ʵ��
	private static BaiduHRParser2 baiduHRParser = new BaiduHRParser2();
	private boolean fetchFLag = true;
	// ������Ƹҳ��վ���ַ�������
	public final String SITE = "BAIDUHR";
	// ���·�������Ƹ����
	private String maxDateStr = "";
	// ��˾����
	public final String COMPANYNAME = "�ٶȹ�˾";
	// �ٶ���һҳ��urlģ��
	private final String jobUrl = "http://talent.baidu.com/baidu/web/httpservice/getPostList?workPlace=0%2F4%2F7%2F9&recruitType=2&pageSize=10&curPage=$pageIndex&keyWord=&_=1453215195631";
	// ������ϸҳurl
	private final static String detailUrl = "http://talent.baidu.com/external/baidu/index.html#/jobDetail/2/$id";
	// ��һҳ
	private final int page = 1;

	// ����Ƹ������ϸ���л�ȡ�������������
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\/\\+,\\s]+)(\\s)*(\\<br\\>)?");

	/**
	 * �����Ѷҳ�������ʵ��
	 * 
	 * @return
	 */
	public static BaiduHRParser2 getInstance() {
		if (baiduHRParser == null) {
			synchronized (BaiduHRParser2.class) {
				if (baiduHRParser == null) {
					baiduHRParser = new BaiduHRParser2();
				}
			}
		}
		return baiduHRParser;
	}

	/**
	 * ˽�й��췽��
	 */
	private BaiduHRParser2() {

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
		if (StringUtils.isBlank(htmlContent)) {
			return jobDemandArts;
		}
		try {
			JSONObject JobDemandAndArtJson = JSONObject.fromObject(htmlContent);
			JSONArray datasArray = JobDemandAndArtJson.getJSONArray("postList");
			int size = datasArray.size();
			String lastDateRecord = ConfigService.getInstance()
					.getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
			for (int i = 0; i < size; i++) {
				JSONObject obj = datasArray.getJSONObject(i);
				String title = obj.getString("name");
				String location = obj.getString("workPlace");
				String numStr = obj.getString("recruitNum");
				int recuitNumber = 10;
				if (StringUtils.equals("����", numStr)) {

				}
				recuitNumber = StringUtils.isNumeric(numStr) ? Integer.parseInt(numStr) : 10;
				String degree = "";
				if (obj.containsKey("education")) {
					degree = obj.getString("education");
				}
				String requirement = obj.getString("serviceCondition");
				String description = obj.getString("workContent");
				String content = this.obtainContent(title, requirement, description);
				String createTime = obj.getString("publishDate");
				String id = obj.getString("postId");
				// �жϵ�ǰץȡ�����ݣ��Ƿ����£���������������ݣ�����ץȡ
				if (!isRecovery) {
					if ((StringUtils.isBlank(lastDateRecord) || createTime.compareTo(lastDateRecord) > 0)) {
						// ��¼��������
						if (this.maxDateStr.compareTo(createTime) < 0) {
							this.maxDateStr = createTime;
						}
						JobDemandArt jobDemandArt = new JobDemandArt();
						jobDemandArt.setCompanyName(COMPANYNAME);
						jobDemandArt.setCreateTime(createTime);
						jobDemandArt.setDepartmentName("");
						jobDemandArt.setLocation(location);
						jobDemandArt.setEducation(degree);
						jobDemandArt.setHrNumber(recuitNumber);
						jobDemandArt.setTitle(title);
						jobDemandArt.setContent(content);
						jobDemandArt.setSource(detailUrl.replace("$id", id));
						jobDemandArts.add(jobDemandArt);
					} else {
						fetchFLag = false;
						break;
					}
				} else {
					if (createTime.compareTo(lastDateRecord) == 0) {
						JobDemandArt jobDemandArt = new JobDemandArt();
						jobDemandArt.setCompanyName(COMPANYNAME);
						jobDemandArt.setCreateTime(createTime);
						jobDemandArt.setDepartmentName("");
						jobDemandArt.setLocation(location);
						jobDemandArt.setEducation(degree);
						jobDemandArt.setHrNumber(recuitNumber);
						jobDemandArt.setTitle(title);
						jobDemandArt.setContent(content);
						jobDemandArt.setSource(detailUrl.replace("$id", id));
						jobDemandArts.add(jobDemandArt);
					} else if (createTime.compareTo(lastDateRecord) < 0) {
						fetchFLag = false;
						break;
					}
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
			rpList.add(m.group(1));
		}
		// ��ӹ���ְ���б�
		m = pattern.matcher(jobResp);
		while (m.find()) {
			cdList.add(m.group(1));
		}
		return VelUtil.cstJobCd(title, rpList, cdList);
	}

	public static void main(String[] args) {
		String con = "-���Ƽ�����ѧ����3�꼰���ϻ�������Ʒ����Ӫ��ҵ���� <br>-ҵ��������������룬�ж�����Ʒ���˼·����Ұ���������ж������ж��� <br>-�߼��������ó�����ҵ�����ϵͳ���� <br>-���Ŷӹ�ͨ����ǿ���ܹ�׼ȷ������� <br>-��ͨAxure��PPT��Visio�Ȳ�Ʒ�����ù��� <br>-���ҹ�������ǿ�������õ�ִ���� <br>-�нϺõĹ�ͨ�������Ŷ�Э������ <br>-��O2O��Ӫ�����µ������о�������";
		Matcher m = pattern.matcher(con);
		while (m.find()) {
			String str = m.group(1);
			str.trim();
			if (str.length() > 4) {
				System.out.println(str);
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
		if (StringUtils.isBlank(htmlContent) || !fetchFLag) {
			return links;
		}
		JSONObject JobDemandAndArtJson = JSONObject.fromObject(htmlContent);
		int totalPage = JobDemandAndArtJson.getInt("totalPage");
		int pageIndex = JobDemandAndArtJson.getInt("currentPage");
		if (totalPage == pageIndex) {
			return links;
		}
		links.add(jobUrl.replace("$pageIndex", String.valueOf(++pageIndex)));
		return links;
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
