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

	// ����ڽ��͹����е�״̬��Ϣ�����磺Article�����б�
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

	// ����URL��ַ��������Ӧ��Spider����
	public static BaseContentExtraction getInstance(String url) {
		try {
			// ����URLѡ��ͬ������
			URL u = new URL(url);
			String host = u.getHost(); // �õ����ǣ�www.ibm.com������������ַ��
			return baseContentExtractions.get(host).newInstance(); // ����Spider����
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("�޷��ҵ���" + url + "����Ӧ�����棡");
		}
	}

	// �ռ�����
	public List<Object> collect(String url) {

		// ����HttpClient
		this.httpclient = new DefaultHttpClient();
		this.context = new BasicHttpContext();
		this.url = url;

		// �����������
		// httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
		// new HttpHost("192.168.1.1", 808));

		// ִ���ռ�����
		execute();

		httpclient.getConnectionManager().shutdown();

		// ��ȡ�ռ���������
		List<Object> articles = (List<Object>) context.getAttribute("results");

		// ���������б�
		return articles;
	}

	public abstract void execute();
}
