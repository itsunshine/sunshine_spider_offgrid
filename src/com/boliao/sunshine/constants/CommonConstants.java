/**
 * 
 */
package com.boliao.sunshine.constants;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * ��ͨ������
 * 
 * @author Liaobo
 * 
 */
public class CommonConstants {
	// �ļ���ʽ�������ַ���
	public static final SimpleDateFormat filefmt = new SimpleDateFormat(
			"yyyyMMdd");

	// ibm host
	public static final String IBM = "www.ibm.com";

	// ITEYE host
	public static final String ITEYE = "www.iteye.com";

	// �������ÿ�ʼ��׺
	public static final String CONTENT_START_PREFIX = ".content.start";

	// �������ý�����׺
	public static final String CONTENT_END_PREFIX = ".content.end";

	// ҳ���������ÿ�ʼ��׺
	public static final String PAGECONTENT_START_PREFIX = ".pagecontent.start";

	// ҳ���������ý�����׺
	public static final String PAGECONTENT_END_PREFIX = ".pagecontent.end";

	// �ָ���
	public static final String SEPARATOR = ";";

	// filter
	public static final String URL_FILTER = ".url.filter";

	// �洢url��ַ����ʱĿ¼
	public static final String SEEDS_DIR = "temp" + File.separator;

	// �ϴ���վ��url��ַ
	public static final String UPLOAD_URL = "http://localhost:8989/sunshine_new/views/upload.do";
}
