/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.TableRow;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.utils.SpideContentUtil;
import com.boliao.sunshine.utils.VelUtil;

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
	private final boolean fetchFLag = true;
	// ������Ƹҳ��վ���ַ�������
	public final String SITE = "BAIDUHR";
	// ���·�������Ƹ����
	private String maxDateStr = "";
	// ��˾����
	public final String COMPANYNAME = "�ٶ�";
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
		if (StringUtils.isBlank(htmlContent)) {
			return jobDemandArts;
		}
		try {
			JSONObject JobDemandAndArtJson = JSONObject.fromObject(htmlContent);
			JSONArray datasArray = JobDemandAndArtJson.getJSONArray("postList");
			int size = datasArray.size();
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
		if (StringUtils.isBlank(htmlContent)) {
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
