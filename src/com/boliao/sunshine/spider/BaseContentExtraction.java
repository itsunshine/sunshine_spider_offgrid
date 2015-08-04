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

	// ����ڽ��͹����е�״̬��Ϣ�����磺Article�����б�
	protected String cont = "sunshine_new";
	protected final static String RESULTS = "results";

	private static Map<String, BaseContentExtraction> baseContentExtractions = new HashMap<String, BaseContentExtraction>();
	static {
		baseContentExtractions.put(CommonConstants.TENCENTHR, ContentExtractionFromTencentHR.getInstance());
		baseContentExtractions.put(CommonConstants.BAIDUHR, ContentExtractionFromBaiduHR.getInstance());
	}

	// ����URL��ַ��������Ӧ��Spider����
	public static BaseContentExtraction getInstance(String url) {
		try {
			// ����URLѡ��ͬ������
			URL u = new URL(url);
			String host = u.getHost(); // �õ����ǣ�www.ibm.com������������ַ��
			return baseContentExtractions.get(host); // ����Spider����
		} catch (Exception e) {
			throw new RuntimeException("�޷��ҵ���" + url + "����Ӧ�����棡");
		}
	}

	// �ռ�����
	public List<Object> collect(JobDemandArt jobDemandArt) {

		// ִ���ռ�����
		List<Object> articles = execute(jobDemandArt);

		// ���������б�
		return articles;
	}

	public void execute() {
	};

	public abstract List<Object> execute(JobDemandArt jobDemandArt);
}
