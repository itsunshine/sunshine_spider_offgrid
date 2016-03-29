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
 * 阿里巴巴招聘页面解析器
 * 
 * @author liaobo
 * 
 */
public class ALIbabaHRParser2 implements BaseParser {

	// 日志记录器
	private static final Logger logger = Logger.getLogger(ALIbabaHRParser2.class);

	// 百度招聘页面解析器实例
	private static ALIbabaHRParser2 alibabaHRParser = new ALIbabaHRParser2();

	// 百度招聘页面站点字符串常量
	public final String SITE = "ALIBABAHR";

	// 是否继续抓取的标识位
	private boolean fetchFLag = true;

	// 最新发布的招聘日期
	private String maxDateStr = "";

	// 从招聘的内容细节中获取具体的文字内容
	public static Pattern pattern = Pattern.compile("([\u0391-\uFFE5|\\w|\\s|\\-|\\+|\\/|(|)]+)(\\<br\\/>)?");

	// 过滤抓取内容用的charSet
	public static Set<Character> charSet = new HashSet<Character>();

	// 日期格式化工具
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	// 公司名字
	public final String COMPANYNAME = "阿里巴巴公司";

	// 阿里工作获取页面
	public final String jobUrl = "https://job.alibaba.com/zhaopin/socialPositionList/doList.json?pageSize=10&pageIndex=$pageIndex&first=%E6%8A%80%E6%9C%AF%E7%B1%BB";

	// 工作内容地址信息
	public final String detailUrl = "https://job.alibaba.com/zhaopin/position_detail.htm?positionId=$id";

	static {
		charSet.add('）');
		charSet.add('、');
		charSet.add('：');
		charSet.add('，');
		charSet.add('；');
		charSet.add('.');
	}

	/**
	 * 获得百度页面解析器实例
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
	 * 私有构造方法
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
				throw new RuntimeException("阿里巴巴，工作内容抓取失败！！！");
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
				// 判断当前抓取的数据，是否最新，如果不是最新数据，则不再抓取
				if (StringUtils.isBlank(lastDateRecord) || createTime.compareTo(lastDateRecord) > 0) {
					// 记录最大的日期
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
		// 添加工作职责列表
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
			throw new RuntimeException("阿里巴巴，工作内容抓取失败！！！");
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
	 * 获得上次最新的日期
	 * 
	 * @return
	 */
	public String getLastDateStr() {
		return ConfigService.getInstance().getLastDateRecord(SITE + CommonConstants.LAST_RECORD_DATE, null);
	}

	/**
	 * 获得最新的日期
	 * 
	 */
	public String getMaxDateStr() {
		return maxDateStr;
	}

	public static void main(String[] args) throws Exception {
		ALIbabaHRParser2 tencentHRParser = ALIbabaHRParser2.getInstance();
		String con = "熟悉至少一门编程语言，可以用该语言实现指定的测试产品开发要求，JAVA/Python/Ruby/PHP语言优先，有过测试产品研发成果优先";
		// con = "深入理解Android系统原理，有丰富的移动端开发经验； ";
		con = "如果你加入这个团队，那你的职责可能包括但不限于 \\r<br\\/>1）在架构师、技术专家的带领下，参与系统架构的设计，负责核心模块的开发、性能调优\\r<br\\/>2）参与到业务中去，和业务一起，为用户创造价值\\r<br\\/>3）具体的工作有营销平台、会员平台，组件平台等";
		con = "Java和c++(c)至少一项精通";
		// 添加工作要求列表
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
	 * 获取站点字符串常量
	 */
	@Override
	public String getSite() {
		return SITE;
	}
}
