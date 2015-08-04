/**
 * 
 */
package com.boliao.sunshine.spider;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.spider.impl.ContentExtractionFromBaiduHR;
import com.boliao.sunshine.spider.impl.ContentExtractionFromTencentHR;

/**
 * 
 * @author liaobo.lb
 * 
 */
public abstract class BaseContentExtraction {

	// 存放在解释过程中的状态信息，比如：Article对象列表
	protected String cont = "sunshine_new";
	protected final static String RESULTS = "results";

	private static Map<String, BaseContentExtraction> baseContentExtractions = new HashMap<String, BaseContentExtraction>();
	static {
		baseContentExtractions.put(CommonConstants.TENCENTHR, ContentExtractionFromTencentHR.getInstance());
		baseContentExtractions.put(CommonConstants.BAIDUHR, ContentExtractionFromBaiduHR.getInstance());
	}

	// 根据URL网址，创建相应的Spider对象
	public static BaseContentExtraction getInstance(String url) {
		try {
			// 根据URL选择不同的子类
			URL u = new URL(url);
			String host = u.getHost(); // 得到的是：www.ibm.com这样的主机地址串
			return baseContentExtractions.get(host); // 创建Spider对象
		} catch (Exception e) {
			throw new RuntimeException("无法找到【" + url + "】对应的爬虫！");
		}
	}

	// 收集文章
	public List<Object> collect(JobDemandArt jobDemandArt) {

		// 执行收集过程
		List<Object> articles = execute(jobDemandArt);

		// 返回文章列表
		return articles;
	}

	public void execute() {
	};

	public abstract List<Object> execute(JobDemandArt jobDemandArt);
}
