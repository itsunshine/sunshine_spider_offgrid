/**
 * 
 */
package com.boliao.sunshine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

/**
 * 配置类，负责解析配置文件
 * 
 * @author liaobo.lb
 * 
 */
public class ConfigService {

	Properties htmlTmpParseconfigPros = new Properties();
	Properties lastDateRecordPros = new Properties();
	static ConfigService configService = new ConfigService();
	// properties文件的存放路径
	private static boolean isOnGrid;

	// 本地指定配置文件路径模式
	public static boolean isLocalSpecifiedMode;

	// grid下配置文件路径
	private static final String defaultTmpCfgFile = "com/boliao/sunshine/properties/htmlParseTempConfig.properties";
	public String defaultLastDateRecordFile = "com/boliao/sunshine/properties/lastDateRecord.properties";

	// 本地文件夹下的路径
	private String localHtmlTempCfg;// 暂时无用
	private String localLastDateRecord;

	/**
	 * 加载配置文件
	 */
	public void init() {
		try {
			// 加载解析该网站的相关配置
			if (isOnGrid) {
				FileReader fr = new FileReader("htmlParseTempConfig.properties");
				htmlTmpParseconfigPros.load(fr);
				fr.close();
				fr = new FileReader("lastDateRecord.properties");
				lastDateRecordPros.load(fr);
				fr.close();
			} else if (isLocalSpecifiedMode) {
				htmlTmpParseconfigPros
						.load(ConfigService.class.getClassLoader().getResourceAsStream(defaultTmpCfgFile));
				FileReader fr = new FileReader(localLastDateRecord);
				lastDateRecordPros.load(fr);
				fr.close();
			} else {
				htmlTmpParseconfigPros
						.load(ConfigService.class.getClassLoader().getResourceAsStream(defaultTmpCfgFile));
				FileInputStream inputStream = new FileInputStream(defaultLastDateRecordFile);
				lastDateRecordPros.load(inputStream);
				inputStream.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ConfigService() {
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
	 * 根据name取得value的值
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getFlagString(String key, String defaultValue) {
		Object val = htmlTmpParseconfigPros.get(key);
		if (val == null)
			return defaultValue;
		return val.toString();
	}

	/***
	 * 取得对应key的最后日期
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getLastDateRecord(String key, String defaultValue) {
		Object val = lastDateRecordPros.get(key);
		if (val == null)
			return defaultValue;
		return val.toString();
	}

	/**
	 * 清除缓存的配置
	 */
	public void clearProperties() {
		lastDateRecordPros.clear();
		htmlTmpParseconfigPros.clear();
	}

	/**
	 * @param isOnGrid
	 *            the isOnGrid to set
	 */
	public static void setOnGrid(boolean onGrid) {
		isOnGrid = onGrid;
	}

	/***
	 * 将最新的职位日期，记录到properties文件上
	 * 
	 * @param key
	 * @param dateStr
	 */
	public void flushRecentRecord(String key, String dateStr) {
		if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(dateStr)) {
			lastDateRecordPros.put(key, dateStr);
		}
	}

	/**
	 * 将抓取的最新日期持久化到硬盘上
	 */
	public void storeRecentDate(String filePath) {
		try {
			// 如果传入的文件路径是空，就存储在默认记录最新日期的文件里
			if (StringUtils.isBlank(filePath)) {
				URL url = ConfigService.class.getClassLoader().getResource(this.defaultLastDateRecordFile);
				filePath = url.getFile();
			}
			File file = new File(filePath);
			if (file.exists()) {
				file.delete();
			}
			FileOutputStream fos = new FileOutputStream(filePath);
			lastDateRecordPros.store(fos, null);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param localHtmlTempCfg
	 *            the localHtmlTempCfg to set
	 */
	public void setLocalHtmlTempCfg(String localHtmlTempCfg) {
		this.localHtmlTempCfg = localHtmlTempCfg;
	}

	/**
	 * @param localLastDateRecord
	 *            the localLastDateRecord to set
	 */
	public void setLocalLastDateRecord(String localLastDateRecord) {
		this.localLastDateRecord = localLastDateRecord;
	}

	public static void main(String[] args) {
		ConfigService configService = new ConfigService();
		configService.defaultLastDateRecordFile = "/Users/liaobo/workspace_j2ee/sunshine_spider_offgrid/src/com/boliao/sunshine/properties/lastDateRecord.properties";
		configService.init();
	}
}
