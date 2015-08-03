/**
 * 
 */
package com.boliao.sunshine.parsers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;

import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.filters.LinkFilter;
import com.boliao.sunshine.utils.DrawlContent;
import com.boliao.sunshine.utils.HtmlParserTool;
import com.boliao.sunshine.utils.ParseUtils;

/**
 * @author liaobo
 * 
 */
public class ITeyeParser extends ParserAdapter {

	private static ITeyeParser iteyeParser = new ITeyeParser();

	private boolean fetchFLag = true;

	private final static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy��MM��dd�� HH:mm");

	/**
	 * ���ibmParserʵ��
	 * 
	 * @return
	 */
	public static ITeyeParser getInstance() {
		if (iteyeParser == null) {
			synchronized (ITeyeParser.class) {
				if (iteyeParser == null) {
					iteyeParser = new ITeyeParser();
				}
			}
		}
		return iteyeParser;
	}

	/**
	 * ˽�й��췽��
	 */
	private ITeyeParser() {

	}

	/**
	 * ����htmlContent��ȡ��Ҫץȡ��ҳ���ݵ�url
	 * 
	 * @param htmlContent
	 * @return
	 */
	@Override
	public String parseContent(String htmlContent) {
		// ����ҳ�г�ȡ������һҳ�ĵ�ַ
		String content = StringUtils.substringBetween(htmlContent,
				"<li class='active' >�ѽ������</li>", "<div class=\"pagination\"");
		return content;
	}

	/**
	 * ����htmlContent,��ȡ��һҳ�����ӵ�ַ
	 * 
	 * @param htmlContent
	 * @return
	 */
	@Override
	public String parsePageContent(String htmlContent) {
		// ����ҳ�г�ȡ������һҳ�ĵ�ַ
		String pageContent = DrawlContent.getContent(htmlContent,
				CommonConstants.PAGECONTENT_START_PREFIX,
				CommonConstants.PAGECONTENT_END_PREFIX, ConfigService
						.getInstance());

		List<LinkTag> linkTags = ParseUtils.parseTags(pageContent,
				LinkTag.class, "class", "next_page");
		pageContent = linkTags.get(0).toHtml();
		return pageContent;
	}

	@Override
	public List<String> getLinks(String htmlContent) {
		List<String> links = new ArrayList<String>();
		if (!fetchFLag) {
			return links;
		}
		String content = parseContent(htmlContent);
		LinkFilter filter = DrawlContent.getFilter(htmlContent,
				CommonConstants.URL_FILTER, ConfigService.getInstance());
		// ���������������Ϊ�գ��򷵻ؿ�
		if (StringUtils.isBlank(content)) {
			return null;
		}
		try {
			getContent(content, links, filter, true);
			getRecentDate(content);
		} catch (ParseException e) {
			throw new RuntimeException(e.getCause());
		}
		return links;
	}

	private void getContent(String content, List<String> links,
			LinkFilter filter, boolean useFilter) throws ParseException {
		// ��ȡ��������ҳ�е� URL
		List<Div> divs = ParseUtils.parseTags(content, Div.class, "class",
				"question-summary");
		for (Div div : divs) {
			String text = div.getStringText();
			List<Span> tds = ParseUtils.parseTags(text, Span.class, "class",
					"gray");
			String dateStr = getDateStr(tds);
			dateStr = getFormatDateStr(dateStr);
			if (StringUtils.isBlank(limitDateStr)
					|| dateStr.compareTo(limitDateStr) > 0) {
				List<String> getLinks = HtmlParserTool.generateLinks(text,
						filter, useFilter);
				links.addAll(getLinks);
			} else {
				fetchFLag = false;
				break;
			}

		}
	}

	/**
	 * ��ø�ʽ���õ������ַ���
	 * 
	 * @param dateStr
	 * @return
	 */
	private String getFormatDateStr(String dateStr) {
		Date date;
		try {
			date = sdf.parse(dateStr);
			dateStr = sdf.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateStr;
	}

	/**
	 * �������ڵ����ֵ
	 * 
	 * @param nodes
	 */
	@Override
	protected void setMaxDate(List<?> nodes) {
		for (Object obj : nodes) {
			Node node = (Node) obj;
			String dateStr = node.getChildren().elementAt(0).toHtml();
			dateStr = getFormatDateStr(dateStr);
			if (StringUtils.isBlank(maxDate) || maxDate.compareTo(dateStr) < 0) {
				maxDate = dateStr;
			}
		}
	}

	@Override
	public List<String> getPageLinks(String content) {
		List<String> links = new ArrayList<String>();
		if (!fetchFLag) {
			return links;
		}
		String pageContent = parsePageContent(content);
		LinkFilter filter = DrawlContent.getFilter(content,
				CommonConstants.URL_FILTER, ConfigService.getInstance());
		List<LinkTag> linkTags = ParseUtils.parseTags(pageContent,
				LinkTag.class, "class", "next_page");
		pageContent = linkTags.get(0).toHtml();

		links = HtmlParserTool.generateLinks(pageContent, filter, true);
		return links;
	}

	@Override
	public void getRecentDate(String content) {
		List<Span> tds = ParseUtils.parseTags(content, Span.class, "class",
				"gray");
		setMaxDate(tds);
	}

	public static void main(String[] args) throws ParseException {
		String dateStr = "2014��5��08�� 19:16";
		Date d = sdf.parse(dateStr);
		String str = sdf.format(d);
		System.out.println(str);
	}
}
