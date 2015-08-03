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
 * 抓取网页内容
 * 
 * @author liaobo
 * 
 */
public class Spider {

	// 网页链接抓取器
	URLFetcher uRLFetcher;

	// 内容抓取器
	ContentFetcher contentFetcher;

	// 网页抓取器
	BaseContentExtraction conTentFetcher;

	public Spider() {
		uRLFetcher = URLFetcher.getInstance();
		contentFetcher = ContentFetcher.getInstance();
	}

	/**
	 * spider 主函数入口
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Spider spider = new Spider();
		try {
			// 抓取url种子

			// spider.uRLFetcher.crawling(); // 抓取内容
			spider.contentFetcher.crawling(); // 将抓取到的内容上传
			String contentFilePath = SpiderContext.getContentFile();
			String questionFilePath = SpiderContext.getQuestionFile();

			if (StringUtils.isNotBlank(contentFilePath)) {
				SendFile
						.uploadFile(CommonConstants.UPLOAD_URL, contentFilePath);
				KenZip.zip("images.zip", "images");
				SendFile.uploadFile(CommonConstants.UPLOAD_URL, "images.zip");
				// 删除本地的图片文件
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
