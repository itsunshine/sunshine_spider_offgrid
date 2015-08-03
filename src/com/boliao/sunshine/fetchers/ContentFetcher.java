package com.boliao.sunshine.fetchers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.boliao.sunshine.biz.constants.TypeConstants;
import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.context.SpiderContext;
import com.boliao.sunshine.spider.BaseContentExtraction;

/**
 * 
 * @author liaobo.lb
 * 
 */
public class ContentFetcher {
	private static ContentFetcher contentFetcher = new ContentFetcher();

	Map<String, String> configMap = new HashMap<String, String>();

	private ContentFetcher() {
		configMap.put("www.ibm.com", TypeConstants.ARTICLE);
		configMap.put("www.iteye.com", TypeConstants.QUESTION);
	}

	// 获得实例对象
	public static ContentFetcher getInstance() {
		if (contentFetcher == null) {
			synchronized (ContentFetcher.class) {
				if (contentFetcher == null) {
					contentFetcher = new ContentFetcher();
				}
			}
		}
		return contentFetcher;
	}

	// 搜集内容的方法
	public void collect(String url) {
		BaseContentExtraction baseContentExtraction = BaseContentExtraction
				.getInstance(url);
		try {
			URL u = new URL(url);
			String host = u.getHost();
			String type = configMap.get(host);
			List<Object> articles = baseContentExtraction.collect(url);
			if (articles != null) {
				for (Object a : articles) {
					if (type.equals(TypeConstants.ARTICLE)) {
						JSONObject obj = JSONObject.fromObject(a);
						writeToObjFile(obj.toString(), type);
					} else if (type.equals(TypeConstants.QUESTION)) {
						JSONArray array = JSONArray.fromObject(a);
						writeToObjFile(array.toString(), type);
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 创建存储json对象的文件
	 * 
	 * @param jsonStr
	 * @return
	 */
	private void writeToObjFile(String jsonStr, String type) {
		String filePath = "spider" + File.separator + type + File.separator
				+ CommonConstants.filefmt.format(new Date()) + "_" + type;
		if (type.equals(TypeConstants.ARTICLE)) {
			SpiderContext.setContentFile(filePath);
		} else if (type.equals(TypeConstants.QUESTION)) {
			SpiderContext.setQuestionFile(filePath);
		}

		try {
			FileUtils.createFile(filePath);
			jsonStr = jsonStr + "\n";
			FileUtils.saveToLocal(jsonStr.getBytes(), filePath, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 抓取页面内容
	 * 
	 * @param line
	 */
	public void spideContent(String line) {
		if (line == null)
			return;
		collect(line);
	}

	/**
	 * 主流程
	 * 
	 * @throws IOException
	 */
	public void crawling() throws IOException {
		String filePath = CommonConstants.SEEDS_DIR;
		File seedsDir = new File(filePath);
		File[] files = seedsDir.listFiles();
		InputStreamReader isr = null;
		BufferedReader br = null;
		for (File file : files) {

			if (file.isDirectory()) {
				File[] seedFiles = file.listFiles();
				for (File f : seedFiles) {
					if (f.getName().equals("persistence.txt"))
						continue;
					isr = new InputStreamReader(new FileInputStream(f),
							"gb2312");
					br = new BufferedReader(isr);
					String line = br.readLine();
					while (line != null) {
						System.out.println("start to deal with this url : "
								+ line);
						spideContent(line);
						System.out.println("the url is done : " + line);
						line = br.readLine();
					}
					isr.close();
					br.close();
					// 删除种子文件
				}
			}

		}
	}

}
