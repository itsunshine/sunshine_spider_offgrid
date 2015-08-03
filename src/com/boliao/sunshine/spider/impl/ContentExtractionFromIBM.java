package com.boliao.sunshine.spider.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.TitleTag;

import com.boliao.sunshine.biz.model.Article;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.spider.BaseContentExtraction;
import com.boliao.sunshine.utils.HttpUtils;
import com.boliao.sunshine.utils.ParseUtils;

/**
 * 从IBM抓取内容
 * 
 * @author liaobo
 * 
 */
public class ContentExtractionFromIBM extends BaseContentExtraction {

	private final Logger logger = Logger
			.getLogger(ContentExtractionFromIBM.class);

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy 年 MM 月 dd 日");
	private final SimpleDateFormat articleSDF = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	@Override
	public void execute() {

		try {
			// 根据URL地址，获取网页内容
			String html = HttpUtil.getHtmlContent(url);

			if (html == null) {
				logger.error("无法获取【" + url + "】网址的内容");
				throw new RuntimeException("无法获取【" + url + "】网址的内容");
			}

			Article a = new Article();

			// 设置文章的来源
			a.setSource(url);

			// 对网页内容进行分析和提取
			// 设置文章的标题
			TitleTag titleTag = ParseUtils.parseTag(html, TitleTag.class, null,
					null);
			a.setTitle(titleTag.getTitle());

			// 设置文章的关键字
			MetaTag keywordTag = ParseUtils.parseTag(html, MetaTag.class,
					"name", "Keywords");
			/*
			 * if (keywordTag.getMetaContent().length() > 255) {
			 * a.setIntro(keywordTag.getMetaContent().substring(0, 255)); }
			 */

			// 设置文章的简介
			MetaTag introTag = ParseUtils.parseTag(html, MetaTag.class, "name",
					"Abstract");
			a.setIntro(introTag.getMetaContent());

			// 设置文章的作者
			List<Div> authors = ParseUtils.parseTags(html, Div.class, "class",
					"author");
			String author = "";
			for (int i = 0; i < authors.size(); i++) {
				if (i != 0) {
					author = author + ",";
				}
				Div div = authors.get(i);
				author = author
						+ ParseUtils.parseTag(div.getStringText(),
								LinkTag.class).getStringText();
			}
			a.setAuthor(author);

			// 设置文章创建的时间
			String createTime = getCreateTime(html);
			a.setCreateTime(createTime);
			a.setDeployTime(createTime);
			a.setUpdateTime(createTime);
			// 设置文章的内容
			String content = StringUtils.substringBetween(html,
					"<!-- 1_1_COLUMN_BEGIN -->", "<!-- 1_1_COLUMN_END -->");

			// 查询文章的内容中所包含的图片，并下载到upload目录，然后创建Attachment对象，设置到Article对象中
			List<ImageTag> imageTags = ParseUtils.parseTags(content,
					ImageTag.class);

			// 得到图片所在的路径目录
			String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
			if (imageTags != null) {
				String imageName = String.valueOf(a.getTitle().hashCode());
				imageName = imageName.replace("-", "负");
				for (ImageTag it : imageTags) {

					// 这个是<img>标签中的src的值
					String imageUrl = it.getImageURL();

					// 图片的绝对路径
					String absoluteUrl = baseUrl + imageUrl;

					// : "文章标题/xxx.jpg"
					// String imageName =
					// a.getTitle().replaceAll("/|\\\\|\\:|\\*|\\?|\\||\\<|>| ",
					// "")+"/" + imageUrl;
					// 把图片保存到upload目录
					// 首先确定，保存到本地的图片的路径
					String imageLocalFile = "images" + File.separator
							+ imageName + File.separator + imageUrl;

					// 如果图片已经被下载到本地，则不再下载
					if (!new File(imageLocalFile).exists()) {
						// 下载图片的信息
						byte[] image = HttpUtils.getImage(httpclient,
								absoluteUrl);
						// 直接使用new FileOutputStream(imageLocalFile)这种方式，创建一个
						// 文件输出流，存在的问题就是：如果这个文件所在的目录不存在，则创建不了
						// 输出流，会抛出异常！
						// 所以，使用辅助的工具类来创建一个文件输出流:FileUtils.openOutputStream(new
						// File(imageLocalFile))
						// 通过这个方法，当文件所在的父目录不存在的时候，将自动创建其所有的父目录
						FileOutputStream fos = FileUtils
								.openOutputStream(new File(imageLocalFile));
						IOUtils.write(image, FileUtils
								.openOutputStream(new File(imageLocalFile)));
						// 关闭相应的输出流
						fos.close();
						System.out.println("图片【" + absoluteUrl + "】已下载");
					}
				}
				// 修改content中的所有图片的src的值
				// 将src的值，加上前缀：upload_image/文章标题/图片.jpg
				content = ParseUtils.modifyImageUrl(content, "/" + cont
						+ "/images/" + imageName + "/");
			}

			// 删除<hr>和"回首页"的链接标签
			content = ParseUtils.reomveTags(content, Div.class, "class",
					"ibm-alternate-rule");
			content = ParseUtils.reomveTags(content, ParagraphTag.class,
					"class", "ibm-ind-link ibm-back-to-top");

			a.setContent(content);

			// 将文章对象放入HttpContext
			List<Article> articles = new ArrayList<Article>();
			articles.add(a);
			context.setAttribute("results", articles);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获得文章的创建时间
	 * 
	 * @param content
	 * @return
	 * @throws ParseException
	 */
	private String getCreateTime(String content) throws ParseException {
		List<ParagraphTag> ps = ParseUtils.parseTags(content,
				ParagraphTag.class, "class", "dw-summary-date");
		String result = null;
		for (ParagraphTag tag : ps) {
			String dateStr = tag.getChildrenHTML();
			result = articleSDF.format(sdf.parse(dateStr));
		}
		return result;
	}
}
