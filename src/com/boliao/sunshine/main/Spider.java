/**
 * 
 */
package com.boliao.sunshine.main;

import org.apache.commons.lang.StringUtils;

import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.biz.utils.KenZip;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.context.SpiderContext;
import com.boliao.sunshine.fetchers.ContentFetcher;
import com.boliao.sunshine.fetchers.URLFetcher;
import com.boliao.sunshine.spider.BaseContentExtraction;
import com.boliao.sunshine.upload.SendFile;

/**
 * ץȡ��ҳ����
 * 
 * @author liaobo
 * 
 */
public class Spider {

	// ��ҳ����ץȡ��
	URLFetcher uRLFetcher;

	// ����ץȡ��
	ContentFetcher contentFetcher;

	// ��ҳץȡ��
	BaseContentExtraction conTentFetcher;

	public Spider() {
		uRLFetcher = URLFetcher.getInstance();
		contentFetcher = ContentFetcher.getInstance();
	}

	/**
	 * spider ���������
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Spider spider = new Spider();
		try {
			// ץȡurl����

			// spider.uRLFetcher.crawling(); // ץȡ����
			spider.contentFetcher.crawling(); // ��ץȡ���������ϴ�
			String contentFilePath = SpiderContext.getContentFile();
			String questionFilePath = SpiderContext.getQuestionFile();

			if (StringUtils.isNotBlank(contentFilePath)) {
				SendFile
						.uploadFile(CommonConstants.UPLOAD_URL, contentFilePath);
				KenZip.zip("images.zip", "images");
				SendFile.uploadFile(CommonConstants.UPLOAD_URL, "images.zip");
				// ɾ�����ص�ͼƬ�ļ�
				FileUtils.deleteDir("images.zip");
				FileUtils.deleteDir("images");
				FileUtils.createDir("images");
			}
			if (StringUtils.isNotBlank(questionFilePath)) {
				SendFile.uploadFile(CommonConstants.UPLOAD_URL,
						questionFilePath);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
