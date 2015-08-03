/**
 * 
 */
package com.boliao.sunshine.context;

import java.util.HashMap;
import java.util.Map;

/**
 * spider��������
 * 
 * @author liaobo
 * 
 */
public class SpiderContext {
	/** �߳������� */
	static ThreadLocal<Map<String, String>> context = new ThreadLocal<Map<String, String>>();

	static final String CON_FILE = "contentFile";
	static final String QUE_FILE = "questionFile";

	/**
	 * ����contentFile��·��
	 * 
	 * @param filePath
	 */
	public static void setContentFile(String filePath) {
		Map<String, String> map = context.get();
		if (map == null) {
			map = new HashMap<String, String>();
			context.set(map);
		}
		map.put(CON_FILE, filePath);
	}

	/**
	 * ����questionFile��·��
	 * 
	 * @param filePath
	 */
	public static void setQuestionFile(String filePath) {
		Map<String, String> map = context.get();
		if (map == null) {
			map = new HashMap<String, String>();
			context.set(map);
		}
		map.put(QUE_FILE, filePath);
	}

	/**
	 * ��ȡquestionFile�ĵ�ַ
	 * 
	 * @return
	 */
	public static String getQuestionFile() {
		Map<String, String> map = context.get();
		if (map == null) {
			map = new HashMap<String, String>();
			context.set(map);
		}
		String filePath = map.get(QUE_FILE);
		return filePath;
	}

	/**
	 * ��ȡcontentFile�ĵ�ַ
	 * 
	 * @return
	 */
	public static String getContentFile() {
		Map<String, String> map = context.get();
		if (map == null) {
			map = new HashMap<String, String>();
			context.set(map);
		}
		String filePath = map.get(CON_FILE);
		return filePath;
	}

}
