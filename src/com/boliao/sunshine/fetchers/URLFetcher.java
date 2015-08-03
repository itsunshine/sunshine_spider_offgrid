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
 * ��ҳurlץȡ��
 * 
 * @author liaobo
 * 
 */
public class URLFetcher {

	String[] seeds = null;

	String filter = null;
	Map<String, List<String>> lastUrls = null;

	// ץȡ��ҳurl��ʵ��
	private static URLFetcher urlFetcher = new URLFetcher();

	// ��ͬ��վ���ݵ�parser
	private final Map<String, ParserAdapter> parserMap = new HashMap<String, ParserAdapter>();

	/**
	 * ���췽������ʼ��ץȡ����
	 */
	private URLFetcher() {
		seeds = ConfigService.getInstance().getSeeds();
		parserMap.put(CommonConstants.IBM, IBMParser.getInstance());
		parserMap.put(CommonConstants.ITEYE, ITeyeParser.getInstance());
		lastUrls = new HashMap<String, List<String>>();
	}

	/**
	 * ���urlץȡ��
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

	/* ʹ������ url ��ʼ�� URL ���� */
	private void initCrawlerWithSeeds(String[] seeds) {
		for (int i = 0; i < seeds.length; i++)
			LinkDB.addUnvisitedUrl(seeds[i]);
	}

	/**
	 * ��ץȡ������������ڣ��־û���Ӳ��
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

	/** ��ȡ�ļ������������ */
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

	/* ��ȡ���� */
	public void crawling() throws Exception {
		if (seeds == null) {
			throw new RuntimeException("the seeds can not be null !");
		}
		// �������ϴ�ץȡ��ʱ���
		setRecentDate();
		// ץȡurl���ӵ�ַ
		spideUrls(seeds);
		// ���ñ���ץȡ���������
		for (String seed : seeds) {
			URL url = new URL(seed);
			String host = url.getHost();
			markRecentDate(host);
		}

	}

	public void spideUrls(String[] seeds) throws Exception {
		// ��ʼ�� URL ����
		initCrawlerWithSeeds(seeds);
		// ѭ����������ץȡ�����Ӳ�����ץȡ����ҳ������ 1000
		while (!LinkDB.unVisitedUrlsEmpty()
				&& LinkDB.getVisitedUrlNum() <= 1000) {
			// ��ͷ URL ����
			String visitUrl = LinkDB.unVisitedUrlDeQueue();
			if (visitUrl == null)
				continue;
			System.out.println("start to deal with this url : " + visitUrl);

			// ������ҳ
			String htmlContent = HttpUtil.getHtmlContent(visitUrl);

			// ���û��ץȡ����ҳ����������
			if (StringUtils.isBlank(htmlContent))
				continue;

			URL url = new URL(visitUrl);
			String host = url.getHost();
			BaseParser parser = parserMap.get(host);
			// ������ץȡ���ݵ�url����ץpage��url
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

			// �洢Ҫץȡ���ݵ���ҳ���ӵ�ַ
			FileUtils.saveToLocal(sb.toString().getBytes(), filePath, true);
			// �µ�δ���ʵ� URL ���
			for (String link : pageLinks) {
				link = StringUtil.transformUrl(link);
				LinkDB.addUnvisitedUrl(url.getProtocol() + "://"
						+ url.getHost() + link);
			}
			System.out.println("the url is done : " + visitUrl);
		}
	}

	// TODO �Ժ����ƣ���һ�εģ��Ȳ���
	public List<String> getLastUrls() {
		// File file = new File();
		return null;
	}

	/**
	 * ����ǰ���ļ�·��
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

	// main �������
	public static void main(String[] args) throws Exception {
		URLFetcher uRLFetcher = new URLFetcher();
		uRLFetcher.crawling();
	}
}
