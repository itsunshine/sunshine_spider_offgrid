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
 * �����࣬������������ļ�
 * 
 * @author liaobo.lb
 * 
 */
public class ConfigService {

	Properties htmlTmpParseconfigPros = new Properties();
	Properties lastDateRecordPros = new Properties();
	static ConfigService configService = new ConfigService();
	// properties�ļ��Ĵ��·��
	private static boolean isOnGrid;

	// ����ָ�������ļ�·��ģʽ
	public static boolean isLocalSpecifiedMode;

	// grid�������ļ�·��
	private static final String defaultTmpCfgFile = "com/boliao/sunshine/properties/htmlParseTempConfig.properties";
	public String defaultLastDateRecordFile = "com/boliao/sunshine/properties/lastDateRecord.properties";

	// �����ļ����µ�·��
	private String localHtmlTempCfg;// ��ʱ����
	private String localLastDateRecord;

	/**
	 * ���������ļ�
	 */
	public void init() {
		try {
			// ���ؽ�������վ���������
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
	 * �ṩ���÷���ʵ��
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
	 * ����nameȡ��value��ֵ
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
	 * ȡ�ö�Ӧkey���������
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
	 * ������������
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
	 * �����µ�ְλ���ڣ���¼��properties�ļ���
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
	 * ��ץȡ���������ڳ־û���Ӳ����
	 */
	public void storeRecentDate(String filePath) {
		try {
			// ���������ļ�·���ǿգ��ʹ洢��Ĭ�ϼ�¼�������ڵ��ļ���
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
