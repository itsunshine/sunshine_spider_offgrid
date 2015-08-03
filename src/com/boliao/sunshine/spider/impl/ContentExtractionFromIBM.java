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
 * ��IBMץȡ����
 * 
 * @author liaobo
 * 
 */
public class ContentExtractionFromIBM extends BaseContentExtraction {

	private final Logger logger = Logger
			.getLogger(ContentExtractionFromIBM.class);

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy �� MM �� dd ��");
	private final SimpleDateFormat articleSDF = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	@Override
	public void execute() {

		try {
			// ����URL��ַ����ȡ��ҳ����
			String html = HttpUtil.getHtmlContent(url);

			if (html == null) {
				logger.error("�޷���ȡ��" + url + "����ַ������");
				throw new RuntimeException("�޷���ȡ��" + url + "����ַ������");
			}

			Article a = new Article();

			// �������µ���Դ
			a.setSource(url);

			// ����ҳ���ݽ��з�������ȡ
			// �������µı���
			TitleTag titleTag = ParseUtils.parseTag(html, TitleTag.class, null,
					null);
			a.setTitle(titleTag.getTitle());

			// �������µĹؼ���
			MetaTag keywordTag = ParseUtils.parseTag(html, MetaTag.class,
					"name", "Keywords");
			/*
			 * if (keywordTag.getMetaContent().length() > 255) {
			 * a.setIntro(keywordTag.getMetaContent().substring(0, 255)); }
			 */

			// �������µļ��
			MetaTag introTag = ParseUtils.parseTag(html, MetaTag.class, "name",
					"Abstract");
			a.setIntro(introTag.getMetaContent());

			// �������µ�����
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

			// �������´�����ʱ��
			String createTime = getCreateTime(html);
			a.setCreateTime(createTime);
			a.setDeployTime(createTime);
			a.setUpdateTime(createTime);
			// �������µ�����
			String content = StringUtils.substringBetween(html,
					"<!-- 1_1_COLUMN_BEGIN -->", "<!-- 1_1_COLUMN_END -->");

			// ��ѯ���µ���������������ͼƬ�������ص�uploadĿ¼��Ȼ�󴴽�Attachment�������õ�Article������
			List<ImageTag> imageTags = ParseUtils.parseTags(content,
					ImageTag.class);

			// �õ�ͼƬ���ڵ�·��Ŀ¼
			String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
			if (imageTags != null) {
				String imageName = String.valueOf(a.getTitle().hashCode());
				imageName = imageName.replace("-", "��");
				for (ImageTag it : imageTags) {

					// �����<img>��ǩ�е�src��ֵ
					String imageUrl = it.getImageURL();

					// ͼƬ�ľ���·��
					String absoluteUrl = baseUrl + imageUrl;

					// : "���±���/xxx.jpg"
					// String imageName =
					// a.getTitle().replaceAll("/|\\\\|\\:|\\*|\\?|\\||\\<|>| ",
					// "")+"/" + imageUrl;
					// ��ͼƬ���浽uploadĿ¼
					// ����ȷ�������浽���ص�ͼƬ��·��
					String imageLocalFile = "images" + File.separator
							+ imageName + File.separator + imageUrl;

					// ���ͼƬ�Ѿ������ص����أ���������
					if (!new File(imageLocalFile).exists()) {
						// ����ͼƬ����Ϣ
						byte[] image = HttpUtils.getImage(httpclient,
								absoluteUrl);
						// ֱ��ʹ��new FileOutputStream(imageLocalFile)���ַ�ʽ������һ��
						// �ļ�����������ڵ�������ǣ��������ļ����ڵ�Ŀ¼�����ڣ��򴴽�����
						// ����������׳��쳣��
						// ���ԣ�ʹ�ø����Ĺ�����������һ���ļ������:FileUtils.openOutputStream(new
						// File(imageLocalFile))
						// ͨ��������������ļ����ڵĸ�Ŀ¼�����ڵ�ʱ�򣬽��Զ����������еĸ�Ŀ¼
						FileOutputStream fos = FileUtils
								.openOutputStream(new File(imageLocalFile));
						IOUtils.write(image, FileUtils
								.openOutputStream(new File(imageLocalFile)));
						// �ر���Ӧ�������
						fos.close();
						System.out.println("ͼƬ��" + absoluteUrl + "��������");
					}
				}
				// �޸�content�е�����ͼƬ��src��ֵ
				// ��src��ֵ������ǰ׺��upload_image/���±���/ͼƬ.jpg
				content = ParseUtils.modifyImageUrl(content, "/" + cont
						+ "/images/" + imageName + "/");
			}

			// ɾ��<hr>��"����ҳ"�����ӱ�ǩ
			content = ParseUtils.reomveTags(content, Div.class, "class",
					"ibm-alternate-rule");
			content = ParseUtils.reomveTags(content, ParagraphTag.class,
					"class", "ibm-ind-link ibm-back-to-top");

			a.setContent(content);

			// �����¶������HttpContext
			List<Article> articles = new ArrayList<Article>();
			articles.add(a);
			context.setAttribute("results", articles);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ������µĴ���ʱ��
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
