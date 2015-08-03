/**
 * 
 */
package com.boliao.sunshine.spider;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.boliao.sunshine.spider.impl.ContentExtractionFromIBM;
import com.boliao.sunshine.spider.impl.ContentExtractionFromIteye;
import com.boliao.sunshine.spider.impl.ContentExtractionFromOracle;

/**
 * 
 * @author liaobo.lb
 * 
 */
public abstract class BaseContentExtraction {

	// 存放在解释过程中的状态信息，比如：Article对象列表
	protected HttpContext context;
	protected HttpClient httpclient;

	protected String url;

	protected String cont = "sunshine_new";

	private static Map<String, Class<? extends BaseContentExtraction>> baseContentExtractions = new HashMap<String, Class<? extends BaseContentExtraction>>();
	static {
		baseContentExtractions.put("www.ibm.com",
				ContentExtractionFromIBM.class);
		baseContentExtractions.put("www.oracle.com",
				ContentExtractionFromOracle.class);
		baseContentExtractions.put("www.iteye.com",
				ContentExtractionFromIteye.class);
	}

	// 根据URL网址，创建相应的Spider对象
	public static BaseContentExtraction getInstance(String url) {
		try {
			// 根据URL选择不同的子类
			URL u = new URL(url);
			String host = u.getHost(); // 得到的是：www.ibm.com这样的主机地址串
			return baseContentExtractions.get(host).newInstance(); // 创建Spider对象
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("无法找到【" + url + "】对应的爬虫！");
		}
	}

	// 收集文章
	public List<Object> collect(String url) {

		// 创建HttpClient
		this.httpclient = new DefaultHttpClient();
		this.context = new BasicHttpContext();
		this.url = url;

		// 设置网络代理
		// httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
		// new HttpHost("192.168.1.1", 808));

		// 执行收集过程
		execute();

		httpclient.getConnectionManager().shutdown();

		// 获取收集到的文章
		List<Object> articles = (List<Object>) context.getAttribute("results");

		// 返回文章列表
		return articles;
	}

	public abstract void execute();
}
