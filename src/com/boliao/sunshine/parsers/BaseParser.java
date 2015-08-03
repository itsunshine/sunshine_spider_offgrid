package com.boliao.sunshine.parsers;

import java.util.List;

/**
 * ��ҳ��������ӿ�
 * 
 * @author liaobo
 * 
 */
public interface BaseParser {

	/**
	 * ����htmlContent��ȡ��Ҫץȡ��ҳ���ݵ�url
	 * 
	 * @param htmlContent
	 * @return
	 */
	public String parseContent(String htmlContent);

	/**
	 * ����htmlContent,��ȡ��һҳ�����ӵ�ַ
	 * 
	 * @param htmlContent
	 * @return
	 */
	public String parsePageContent(String htmlContent);

	/**
	 * ����content���ݣ���ȡ����
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getLinks(String content);

	/**
	 * ���ҳ�������
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getPageLinks(String content);

	/**
	 * ������һƪ���µ�����
	 * 
	 * @param content
	 * @return ���ڸ�ʽ���ַ���
	 */
	public void getRecentDate(String content);
}
