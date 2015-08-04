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
	static Map<String, String> typeFilePathMap = new HashMap<String, String>();

	static final String CON_FILE = "contentFile";
	static final String QUE_FILE = "questionFile";
	static final String JOB_FILE = "jobDemandFile";

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
	 * 招聘要求内容，存储的文件
	 * 
	 * @param filePath
	 */
	public static void setJobDemandFile(String filePath) {
		if (typeFilePathMap == null) {
			typeFilePathMap = new HashMap<String, String>();
		}
		typeFilePathMap.put(JOB_FILE, filePath);
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
	 * 获取工作要求文件地址
	 * 
	 * @return
	 */
	public static String getJobDemandFile() {
		String filePath = typeFilePathMap.get(JOB_FILE);
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
