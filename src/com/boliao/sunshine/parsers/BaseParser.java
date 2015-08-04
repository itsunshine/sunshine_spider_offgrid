package com.boliao.sunshine.parsers;

import java.util.List;

import com.boliao.sunshine.biz.model.JobDemandArt;

/**
 * ��ҳ��������ӿ�
 * 
 * @author liaobo
 * 
 */
public interface BaseParser {

	/**
	 * ����content���ݣ���ȡ����
	 * 
	 * @param content
	 * @return
	 */
	public List<JobDemandArt> getLinks(String content);

	/**
	 * ���ҳ�������
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getPageLinks(String content);

	/**
	 * ����ϴ����µ�����
	 * 
	 * @return
	 */
	public String getLastDateStr();

	/**
	 * �����������
	 * 
	 * @return
	 */
	public String getMaxDateStr();

	/**
	 * ��ȡվ�����ֳ���
	 * 
	 * @return
	 */
	public String getSite();
}
