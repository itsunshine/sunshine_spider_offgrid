package com.boliao.sunshine.fetchers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.datastru.LinkDB;
import com.boliao.sunshine.parsers.BaseParser;
import com.boliao.sunshine.parsers.IBMParser;
import com.boliao.sunshine.parsers.ITeyeParser;
import com.boliao.sunshine.parsers.ParserAdapter;
import com.boliao.sunshine.utils.StringUtil;

/**
 * 网页url抓取器
 * 
 * @author liaobo
 * 
 */
public class URLFetcher {

	String[] seeds = null;

	String filter = null;
	Map<String, List<String>> lastUrls = null;

	// 抓取网页url的实例
	private static URLFetcher urlFetcher = new URLFetcher();

	// 不同网站内容的parser
	private final Map<String, ParserAdapter> parserMap = new HashMap<String, ParserAdapter>();

	/**
	 * 构造方法，初始化抓取种子
	 */
	private URLFetcher() {
		seeds = ConfigService.getInstance().getSeeds();
		parserMap.put(CommonConstants.IBM, IBMParser.getInstance());
		parserMap.put(CommonConstants.ITEYE, ITeyeParser.getInstance());
		lastUrls = new HashMap<String, List<String>>();
	}

	/**
	 * 获得url抓取器
	 * 
	 * @return
	 */
	public static URLFetcher getInstance() {
		if (urlFetcher == null) {
			synchronized (URLFetcher.class) {
				if (urlFetcher == null) {
					urlFetcher = new URLFetcher();
				}
			}
		}
		return urlFetcher;
	}

	/* 使用种子 url 初始化 URL 队列 */
	private void initCrawlerWithSeeds(String[] seeds) {
		for (int i = 0; i < seeds.length; i++)
			LinkDB.addUnvisitedUrl(seeds[i]);
	}

	/**
	 * 将抓取到的最近的日期，持久化到硬盘
	 */
	private void markRecentDate(String host) {
		try {
			String filePath = CommonConstants.SEEDS_DIR + host + File.separator
					+ "persistence.txt";
			FileUtils.createFile(filePath);
			String dateStr = (parserMap.get(host)).maxDate;
			if (StringUtils.isBlank(dateStr))
				return;
			FileUtils.saveToLocal(dateStr.getBytes(), filePath, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/** 读取文件中最近的日期 */
	private void setRecentDate() {
		Set<String> keySet = parserMap.keySet();
		for (String key : keySet) {
			String filePath = CommonConstants.SEEDS_DIR + key + File.separator
					+ "persistence.txt";

			String dateStr = FileUtils.readFile(filePath);
			ParserAdapter parserAdapter = parserMap.get(key);
			parserAdapter.setLimitDateStr(dateStr);
		}
	}

	/* 爬取方法 */
	public void crawling() throws Exception {
		if (seeds == null) {
			throw new RuntimeException("the seeds can not be null !");
		}
		// 先设置上次抓取的时间点
		setRecentDate();
		// 抓取url链接地址
		spideUrls(seeds);
		// 设置本次抓取的最近日期
		for (String seed : seeds) {
			URL url = new URL(seed);
			String host = url.getHost();
			markRecentDate(host);
		}

	}

	public void spideUrls(String[] seeds) throws Exception {
		// 初始化 URL 队列
		initCrawlerWithSeeds(seeds);
		// 循环条件：待抓取的链接不空且抓取的网页不多于 1000
		while (!LinkDB.unVisitedUrlsEmpty()
				&& LinkDB.getVisitedUrlNum() <= 1000) {
			// 队头 URL 出队
			String visitUrl = LinkDB.unVisitedUrlDeQueue();
			if (visitUrl == null)
				continue;
			System.out.println("start to deal with this url : " + visitUrl);

			// 下载网页
			String htmlContent = HttpUtil.getHtmlContent(visitUrl);

			// 如果没有抓取到网页内容则跳过
			if (StringUtils.isBlank(htmlContent))
				continue;

			URL url = new URL(visitUrl);
			String host = url.getHost();
			BaseParser parser = parserMap.get(host);
			// 必须先抓取内容的url，再抓page的url
			List<String> contentLinks = parser.getLinks(htmlContent);
			if (contentLinks == null || contentLinks.isEmpty()) {
				break;
			}
			List<String> pageLinks = parser.getPageLinks(htmlContent);
			String filePath = CommonConstants.SEEDS_DIR + url.getHost()
					+ File.separator
					+ CommonConstants.filefmt.format(new Date()) + ".txt";
			FileUtils.createFile(filePath);
			StringBuilder sb = new StringBuilder();
			for (String link : contentLinks) {
				link = StringUtil.transformUrl(link);
				if (link.startsWith("http")) {
					sb.append(link).append("\n");
				} else {
					sb.append(url.getProtocol() + "://" + url.getHost() + link)
							.append("\n");
				}

			}

			// 存储要抓取内容的网页链接地址
			FileUtils.saveToLocal(sb.toString().getBytes(), filePath, true);
			// 新的未访问的 URL 入队
			for (String link : pageLinks) {
				link = StringUtil.transformUrl(link);
				LinkDB.addUnvisitedUrl(url.getProtocol() + "://"
						+ url.getHost() + link);
			}
			System.out.println("the url is done : " + visitUrl);
		}
	}

	// TODO 以后完善，第一次的，先不用
	public List<String> getLastUrls() {
		// File file = new File();
		return null;
	}

	/**
	 * 几天前的文件路径
	 * 
	 * @param days
	 * @return
	 */
	public String getFilePath(int days, URL url) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, days);
		String filePath = CommonConstants.SEEDS_DIR + url.getHost()
				+ File.separator + CommonConstants.filefmt.format(c.getTime())
				+ ".txt";
		return filePath;
	}

	// main 方法入口
	public static void main(String[] args) throws Exception {
		URLFetcher uRLFetcher = new URLFetcher();
		uRLFetcher.crawling();
	}
}
