package com.boliao.sunshine.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.fetch.filters.LinkFilter;

public class URLParserTool {

	// 系统异常，日志记录器
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	// 该方法暂时没用；获取一个网站上的链接,filter 用来过滤链接
	public static Set<String> extracLinks(String url, LinkFilter filter) {

		Set<String> links = new HashSet<String>();
		try {
			Parser parser = new Parser(url);
			parser.setEncoding("gb2312");
			// 过滤 <frame >标签的 filter，用来提取 frame 标签里的 src 属性所表示的链接
			NodeFilter frameFilter = new NodeFilter() {
				public boolean accept(Node node) {
					if (node.getText().startsWith("href=")) {
						return true;
					} else {
						return false;
					}
				}
			};
			// OrFilter 来设置过滤 <a> 标签，和 <frame> 标签
			OrFilter linkFilter = new OrFilter(new NodeClassFilter(LinkTag.class), frameFilter);
			// 得到所有经过过滤的标签
			NodeList list = parser.extractAllNodesThatMatch(linkFilter);
			for (int i = 0; i < list.size(); i++) {
				Node tag = list.elementAt(i);
				if (tag instanceof LinkTag)// <a> 标签
				{
					LinkTag link = (LinkTag) tag;
					String linkUrl = link.getLink();// url
					if (filter.accept(linkUrl))
						links.add(linkUrl);
				} else// <frame> 标签
				{
					// 提取 frame 里 src 属性的链接如 <frame src="test.html"/>
					String frame = tag.getText();
					int start = frame.indexOf("src=");
					frame = frame.substring(start);
					int end = frame.indexOf(" ");
					if (end == -1)
						end = frame.indexOf(">");
					String frameUrl = frame.substring(5, end - 1);
					if (filter.accept(frameUrl))
						links.add(frameUrl);
				}
			}
		} catch (ParserException e) {
			LogUtil.error(errorLogger, URLParserTool.class.getSimpleName() + "：从页面内容中，获取url失败", e);
		}
		return links;
	}

	// 获取一个网站上的链接,filter 用来过滤链接
	public static List<String> fetchLinksFromContent(String content, LinkFilter filter, boolean filterFlag) throws ParserException {

		List<String> links = new ArrayList<String>();
		try {
			Parser parser = new Parser();
			parser.setEncoding("utf-8");
			parser.setInputHTML(content);
			// parser.setEncoding("gb2312");
			// 过滤 <frame >标签的 filter，用来提取 frame 标签里的 src 属性所表示的链接
			NodeFilter frameFilter = new NodeFilter() {
				public boolean accept(Node node) {
					if (node.getText().startsWith("href=")) {
						return true;
					} else {
						return false;
					}
				}
			};
			// OrFilter 来设置过滤 <a> 标签，和 <frame> 标签
			OrFilter linkFilter = new OrFilter(new NodeClassFilter(LinkTag.class), frameFilter);
			// 得到所有经过过滤的标签
			NodeList list = parser.extractAllNodesThatMatch(linkFilter);
			for (int i = 0; i < list.size(); i++) {
				Node tag = list.elementAt(i);
				if (tag instanceof LinkTag)// <a> 标签
				{
					LinkTag link = (LinkTag) tag;
					String linkUrl = link.getLink();// url
					if (filterFlag) {
						if (filter.accept(linkUrl)) {
							links.add(linkUrl);
						}
						continue;
					} else {
						links.add(linkUrl);
					}
				} else// <frame> 标签
				{
					// 提取 frame 里 src 属性的链接如 <frame src="test.html"/>
					String frame = tag.getText();
					int start = frame.indexOf("src=");
					frame = frame.substring(start);
					int end = frame.indexOf(" ");
					if (end == -1)
						end = frame.indexOf(">");
					String frameUrl = frame.substring(5, end - 1);
					if (filterFlag) {
						if (filter.accept(frameUrl)) {
							links.add(frameUrl);
						}
						continue;
					} else {
						links.add(frameUrl);
					}
				}
			}
		} catch (ParserException e) {
			LogUtil.error(errorLogger, URLParserTool.class.getSimpleName() + "：从页面内容中，获取url失败", e);
			throw e;
		}
		return links;
	}

	// 测试的 main 方法
	public static void main(String[] args) {
		Set<String> links = URLParserTool.extracLinks("http://www.twt.edu.cn", new LinkFilter() {
			// 提取以 http://www.twt.edu.cn 开头的链接
			public boolean accept(String url) {
				if (url.startsWith("http://www.twt.edu.cn"))
					return true;
				else
					return false;
			}

		});
		for (String link : links)
			System.out.println(link);
	}
}