/**
 * 
 */
package com.boliao.sunshine.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 配置类，负责解析配置文件
 * 
 * @author liaobo.lb
 * 
 */
public class ConfigService {

	Properties webPro = new Properties();
	Properties configPros = new Properties();
	String seeds[] = null;
	Set<String> webSites = null;
	static ConfigService configService = new ConfigService();

	/**
	 * 加载配置文件
	 */
	private void init() {
		try {
			// 加载要抓取的网站
			webPro
					.load(ConfigService.class
							.getClassLoader()
							.getResourceAsStream(
									"com/boliao/sunshine/properties/website.properties"));
			// 加载解析该网站的相关配置
			configPros
					.load(ConfigService.class
							.getClassLoader()
							.getResourceAsStream(
									"com/boliao/sunshine/properties/config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ConfigService() {
		init();
	}

	/**
	 * 提供配置服务实例
	 * 
	 * @return
	 */
	public static ConfigService getInstance() {
		if (configService == null) {
			synchronized (ConfigService.class) {
				if (configService == null) {
					configService = new ConfigService();
				}
			}
		}
		return configService;
	}

	/**
	 * 获得种子url
	 * 
	 * @return
	 */
	public String[] getSeeds() {
		Set<Object> keySet = webPro.keySet();
		if (seeds != null) {
			return seeds;
		}
		seeds = new String[keySet.size()];
		Object[] keys = keySet.toArray();
		for (int i = 0; i < keySet.size(); i++) {
			String key = (String) keys[i];
			String seed = webPro.getProperty(key.toString());
			seeds[i] = seed;
		}
		return seeds;
	}

	/**
	 * 获取网站，所有的键值
	 * 
	 * @return
	 */
	public Set<String> getWebSit() {
		if (webSites != null && webSites.size() != 0) {
			return webSites;
		}
		webSites = new HashSet<String>();
		for (Object key : webPro.keySet()) {
			webSites.add(key.toString());
		}
		return webSites;
	}

	/**
	 * 根据name取得value的值
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public String getString(String name, String defaultValue) {
		Object val = webPro.get(name);
		if (val == null) {
			val = configPros.get(name);
			if (val == null)
				return defaultValue;
		}
		return val.toString();
	}

	public static void main(String[] args) {
		ConfigService configService = new ConfigService();
		configService.init();
	}

}
