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
 * 腾旭招聘页面解析器
 * 
 * @author liaobo
 * 
 */
public class BaiduHRParser2 implements BaseParser {

	// 日志记录器
	private static final Logger logger = Logger.getLogger(BaiduHRParser2.class);

	// 腾旭招聘页面解析器实例
	private static BaiduHRParser2 baiduHRParser = new BaiduHRParser2();
	private final boolean fetchFLag = true;
	// 腾旭招聘页面站点字符串常量
	public final String SITE = "BAIDUHR";
	// 最新发布的招聘日期
	private String maxDateStr = "";
	// 公司名字
	public final String COMPANYNAME = "百度";
	// 百度下一页的url模板
	private final String jobUrl = "http://talent.baidu.com/baidu/web/httpservice/getPostList?workPlace=0%2F4%2F7%2F9&recruitType=2&pageSize=10&curPage=$pageIndex&keyWord=&_=1453215195631";
	// 工作详细页url
	private final static String detailUrl = "http://talent.baidu.com/external/baidu/index.html#/jobDetail/2/$id";
	// 第一页
	private final int page = 1;

	// 从招聘的内容细节中获取具体的文字内容
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\/\\+,\\s]+)(\\s)*(\\<br\\>)?");

	/**
	 * 获得腾讯页面解析器实例
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
	 * 私有构造方法
	 */
	private BaiduHRParser2() {

	}

	/**
	 * 根据htmlContent抽取，要抓取网页内容的url
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parseContent(String htmlContent) {
		// 取出有真正职业简介内容的url
		String conContent = SpideContentUtil.getContent(htmlContent, CommonConstants.CONTENT_START_PREFIX, CommonConstants.CONTENT_END_PREFIX, SITE, ConfigService.getInstance());
		return conContent;
	}

	/**
	 * 根据htmlContent,抽取下一页的链接地址
	 * 
	 * @param htmlContent
	 * @return
	 */
	private String parsePageContent(String htmlContent) {
		// 从网页中抽取出，下一页的地址
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
				if (StringUtils.equals("若干", numStr)) {

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
				// 记录最大的日期
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
	 * 从工作内容中，取出工作要求
	 * 
	 * @param jobDesc
	 * @return
	 * @throws IOException
	 */
	private String obtainContent(String title, String jobDesc, String jobResp) throws IOException {
		List<String> rpList = new ArrayList<String>(8);
		List<String> cdList = new ArrayList<String>(8);
		// 添加工作要求列表
		Matcher m = pattern.matcher(jobDesc);
		while (m.find()) {
			rpList.add(m.group(1));
		}
		// 添加工作职责列表
		m = pattern.matcher(jobResp);
		while (m.find()) {
			cdList.add(m.group(1));
		}
		return VelUtil.cstJobCd(title, rpList, cdList);
	}

	public static void main(String[] args) {
		String result = "<td> <a target=\"_blank\" href=\"/baidu/web/templet1000/index/corpwebPosition1000baidu!getOnePosition?postIdEnc=2EE97DB5B5F2A6225A573C7754F66C1A&brandCode=1&recruitType=2&lanType=1&operational=6637AA56FA08745E71A74EA6AC68D5FFF28F462DA4C19FB3FABC8882DE74DA1C0FDB6AEAC9F8C487108CCBE39D45983B54F375AA1CAE83E6A21F36A7DBB429FDA1AA45697C458F4E318F8DFB32711E38D130740ECA46D104EAAE68E6E5E266B2A5F490AF8EE77F247C58B479FA37A1101C8FE41D11E20AFFA9511543837DA597ADA993F4A79495C679D35888897C39FD21D98BCC2FE67575CAED499E9C86325CC3D44DA1C73F4DA945C7FA90CE9460F2\" class=\"col-4\">云平台部_Web前端高级工程师</a> </td> <td> <font title=\"技术\">技术</font> </td> <td> <span title=\"北京市\"><script type=\"text/javascript\"> var wpStr = \"北京市\"; if(wpStr.length > 10){ var tempStr = wpStr.substring(0,10)+\"...\"; document.write(tempStr); }else{ document.write(wpStr); } </script></span> </td> <td> 2015-03-14 </td>";
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
	 * 获得最新的日期
	 * 
	 * @return
	 */
	public String getMaxDateStr() {
		return maxDateStr;
	}

	/**
	 * 获取站点字符串常量
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
