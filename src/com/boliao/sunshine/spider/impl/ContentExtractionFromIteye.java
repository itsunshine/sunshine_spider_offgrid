package com.boliao.sunshine.spider.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.Span;

import com.boliao.sunshine.biz.model.Answer;
import com.boliao.sunshine.biz.model.Question;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.spider.BaseContentExtraction;
import com.boliao.sunshine.utils.ParseUtils;

public class ContentExtractionFromIteye extends BaseContentExtraction {

	private final Logger logger = Logger
			.getLogger(ContentExtractionFromIteye.class);

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy年MM月dd日 HH:mm");
	private final SimpleDateFormat qaSDF = new SimpleDateFormat(
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

			Question a = new Question();
			Answer an = new Answer();

			// 设置文章的来源
			a.setSource(url);

			// 对网页内容进行分析和提取
			// 设置文章的标题

			// 设置文章的关键字
			MetaTag keywordTag = ParseUtils.parseTag(html, MetaTag.class,
					"name", "keywords");

			// 设置文章的作者
			List<Span> authors = ParseUtils.parseTags(html, Span.class,
					"class", "user_blog");

			String questionAuthor = "";
			String answerAuthor = "";
			for (int i = 0; i < 2; i++) {
				String span = authors.get(i).getChildrenHTML();
				LinkTag tag = ParseUtils.parseTag(span, LinkTag.class);
				if (i == 0)
					questionAuthor = tag.getChildrenHTML();
				if (i == 1) {
					answerAuthor = tag.getChildrenHTML();
					break;
				}
			}
			a.setAuthor(questionAuthor);
			an.setAuthor(answerAuthor);

			// 设置问题的内容
			List<Div> questionDivs = ParseUtils.parseTags(html, Div.class,
					"class", "sproblem_right");
			Div questionDiv = questionDivs.get(0);
			String questionContent = questionDiv.toHtml();
			// 从内容里获取问题，创建时间
			String createTime = getCreateTime(questionContent);
			questionContent = ParseUtils.reomveTags(questionContent,
					Span.class, "class", "score");
			questionContent = ParseUtils.reomveTags(questionContent, Div.class,
					"class", "ask_label");
			questionContent = ParseUtils.reomveTags(questionContent, Div.class,
					"class", "user_info fr");
			// 替换掉<a> 标签
			List<HeadingTag> hs = ParseUtils.parseTags(questionContent,
					HeadingTag.class, "class", "close");
			String linkTitile = (hs.get(0)).getChild(0).toHtml();
			String strTitle = (hs.get(0)).getChild(0).getFirstChild().toHtml();
			questionContent = StringUtils.replace(questionContent, linkTitile,
					strTitle);
			a.setTitle(strTitle);
			a.setContent(questionContent);
			a.setCreateTime(createTime);
			a.setUpdateTime(createTime);

			// 设置答案的内容
			Div answerDiv = ParseUtils.parseTag(html, Div.class, "class",
					"accept_solution");
			String answerContent = answerDiv.toHtml();
			// 获取答案的时间
			// createTime = getCreateTime(answerContent);
			createTime = qaSDF.format(new Date());
			an.setContent(answerContent);
			an.setCreateTime(createTime);
			an.setUpdateTime(createTime);

			// 将文章对象放入HttpContext
			List<List<Object>> modelList = new ArrayList<List<Object>>();
			List<Object> models = new ArrayList<Object>();
			models.add(a);
			models.add(an);
			modelList.add(models);

			context.setAttribute("results", modelList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获得问题或答案的创建时间
	 * 
	 * @param content
	 * @return
	 * @throws ParseException
	 */
	public String getCreateTime(String content) throws ParseException {
		Span span = ParseUtils.parseTag(content, Span.class, "class", "gray");
		String createTime = span.getChildrenHTML();
		createTime = qaSDF.format(sdf.parse(createTime));
		return createTime;
	}
}
