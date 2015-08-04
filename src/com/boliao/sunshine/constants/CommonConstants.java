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
	public static final SimpleDateFormat filefmt = new SimpleDateFormat("yyyyMMdd");

	// ibm host
	public static final String IBM = "www.ibm.com";

	// ITEYE host
	public static final String ITEYE = "www.iteye.com";
	public static final String TENCENTHR = "hr.tencent.com";
	public static final String BAIDUHR = "talent.baidu.com";
	public static final String TAOBAOHR = "job.alibaba.com";

	// �������ÿ�ʼ��׺
	public static final String CONTENT_START_PREFIX = ".content.start";

	// ����Ҫ���������ý�����׺
	public static final String JOBCONTENT_END_PREFIX = ".jobcontent.end";

	// ����Ҫ���������ÿ�ʼ��׺
	public static final String JOBCONTENT_START_PREFIX = ".jobcontent.start";

	// �������ý�����׺
	public static final String CONTENT_END_PREFIX = ".content.end";

	// ҳ���������ÿ�ʼ��׺
	public static final String PAGECONTENT_START_PREFIX = ".pagecontent.start";

	// ҳ���������ý�����׺
	public static final String PAGECONTENT_END_PREFIX = ".pagecontent.end";

	// ץȡ�ļ�¼�����һ����Ӧ������
	public static final String LAST_RECORD_DATE = ".lastOne";

	// �ָ���
	public static final String SEPARATOR = ";";

	// filter
	public static final String URL_FILTER = ".url.filter";

	// �洢url��ַ����ʱĿ¼
	public static final String SEEDS_DIR = "temp" + File.separator;

	// �洢url��ַ����ʱĿ¼
	public static final String RECOVERY_SEEDS_DIR = "recovery" + File.separator + "temp" + File.separator;

	// �ϴ���վ��url��ַ
	public static final String UPLOAD_URL = "http://localhost:8989/sunshine_new/views/upload.do";

	// ��������Ŀ¼
	public static final String USER_DIR = "user.dir";

	// urlץȡʧ�ܵ�Ŀ¼
	public static final String URL_FETCH_ERROR = "urlFetchErrors";

	// contentץȡʧ�ܵ�Ŀ¼
	public static final String CON_FETCH_ERROR = "conFetchErrors";

	// ץȡʧ��ʱ���������
	public static final String ERROR_DATE = "failedDt";

	// ץȡʧ�ܵ�url
	public static final String ERROR_URL = "failedUrl";

	// �س������ַ���
	public static final String ENTER_STR = "\n";

	// ����Ƹҳ��������ɣ������10
	public static final int N_NUMBER = 10;
}
