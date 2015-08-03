/**
 * 
 */
package com.boliao.sunshine.context;

import java.util.HashMap;
import java.util.Map;

/**
 * spider的上下文
 * 
 * @author liaobo
 * 
 */
public class SpiderContext {
	/** 线程上下文 */
	static ThreadLocal<Map<String, String>> context = new ThreadLocal<Map<String, String>>();

	static final String CON_FILE = "contentFile";
	static final String QUE_FILE = "questionFile";

	/**
	 * 设置contentFile的路径
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
	 * 设置questionFile的路径
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
	 * 获取questionFile的地址
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
	 * 获取contentFile的地址
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
