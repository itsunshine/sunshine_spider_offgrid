package com.boliao.sunshine.fetchers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.biz.model.JobDemandArtEnd;
import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.biz.utils.HttpUtil;
import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.biz.utils.StringHelperUtil;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.datastru.LinkDB;
import com.boliao.sunshine.main.SpiderLauncher;
import com.boliao.sunshine.parsers.ALIbabaHRParser2;
import com.boliao.sunshine.parsers.BaiduHRParser2;
import com.boliao.sunshine.parsers.BaseParser;
import com.boliao.sunshine.parsers.TencentHRParser;

/**
 * 网页url抓取器
 * 
 * @author liaobo
 * 
 */
public class URLFetcher {

	// 日志记录器
	private static Logger logger = Logger.getLogger(URLFetcher.class);

	// 系统异常，日志记录器
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	// 类名
	private final String name = URLFetcher.class.getSimpleName();

	// 抓取网页url的实例
	private static URLFetcher urlFetcher = new URLFetcher();

	// 不同网站内容的parser
	private final Map<String, BaseParser> parserMap = new ConcurrentHashMap<String, BaseParser>();

	// 协调执行任务的线程池
	private ThreadPoolExecutor executor = null;

	// 线程池里线程个数
	private final int THREADE_POOL_SIZE = 10;

	// 是否存储内容url
	private boolean isSaveConUrlFlag = true;

	// 工作线程的对象池
	List<URLFetchWorker> workers = new ArrayList<URLFetchWorker>(THREADE_POOL_SIZE);

	// 计数器
	AtomicInteger caculator = new AtomicInteger(0);

	// 锁存器
	CountDownLatch counter = null;

	// 锁
	Object lock = new Object();

	// 抓取失败的锁
	Object errorLock = new Object();

	// 空闲线程设置变量的锁
	Object freeWorkerLock = new Object();

	// 抓取结果的队列
	ArrayBlockingQueue<JobDemandArt> resultQueue = null;

	// 是否是恢复模式启动，默认为false
	boolean isRecoveryMode = false;

	// url抓取工作是否完成
	boolean isURLFetchDone = false;

	// 主启动类
	SpiderLauncher spiderLauncher;

	// 不同host对应的记录文件
	Map<String, File> urlFileMap = new HashMap<String, File>();

	/**
	 * 构造方法，初始化抓取种子
	 */
	private URLFetcher() {
		parserMap.put(CommonConstants.TENCENTHR, TencentHRParser.getInstance());
		parserMap.put(CommonConstants.BAIDUHR, BaiduHRParser2.getInstance());
		parserMap.put(CommonConstants.TAOBAOHR, ALIbabaHRParser2.getInstance());
		executor = new ThreadPoolExecutor(THREADE_POOL_SIZE, THREADE_POOL_SIZE, 10L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
		for (int i = 0; i < THREADE_POOL_SIZE; i++) {
			URLFetchWorker worker = new URLFetchWorker();
			workers.add(worker);
		}
		for (String url : parserMap.keySet()) {
			String filePath = spiderLauncher.baseDir + File.separator + CommonConstants.SEEDS_DIR + url + File.separator
					+ CommonConstants.filefmt.format(new Date()) + ".txt";
			if (this.isRecoveryMode) {
				filePath = spiderLauncher.baseDir + File.separator + CommonConstants.RECOVERY_SEEDS_DIR + url
						+ File.separator + CommonConstants.filefmt.format(new Date()) + ".txt";
			}
			File file = new File(filePath);
			try {
				FileUtils.createFile(file.getAbsolutePath());
			} catch (IOException e) {
				LogUtil.error(errorLogger, "创建抓取结果文件时失败，出错", e);
				throw new RuntimeException(e);
			}
			urlFileMap.put(url, file);
		}
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
	 * 获取所有，可以获得内容url的，队列
	 * 
	 * @param seeds
	 *            抓取内容url的种子连接
	 * @return 可以抓取内容的url队列
	 * @throws Exception
	 */
	public BlockingQueue<JobDemandArt> spideUrls(String[] seeds, String dateRecordFile) throws Exception {
		// 初始化种子链表
		initCrawlerWithSeeds(seeds);
		ArrayBlockingQueue<JobDemandArt> tmpResultQueue = new ArrayBlockingQueue<JobDemandArt>(1000);
		new SaveConUrlWorker(tmpResultQueue, resultQueue).start();
		// 循环条件：待抓取的链接不空且抓取的网页不多于 1000
		while (isMatchCondition() || (!isMatchCondition() && isJobOver())) {
			// 队头 URL 出队
			String visitUrl = LinkDB.unVisitedUrlDeQueue();
			if (visitUrl == null)
				continue;
			LogUtil.info(logger, name + "：start to fetch this url : " + visitUrl);
			// 准备抓取时加1
			caculator.incrementAndGet();
			Thread worker = findOutFreeWorker(tmpResultQueue, visitUrl);
			executor.execute(worker);
		}
		if (!this.isRecoveryMode) {
			executor.shutdown();
			recordLastDate(seeds, dateRecordFile);
			// 唤醒，抓取主启动器
			spiderLauncher.increateOrNotify();
		}
		tmpResultQueue.add(new JobDemandArtEnd());
		isURLFetchDone = true;
		return resultQueue;
	}

	/**
	 * 将中间结果，保存在文件中
	 * 
	 * @param tmpResultQueue
	 * @param resultQueue
	 *            最终结果
	 * @throws Exception
	 */
	private void saveContentUrlToFile(ArrayBlockingQueue<JobDemandArt> tmpResultQueue,
			ArrayBlockingQueue<JobDemandArt> resultQueue) throws Exception {
		JobDemandArt jobDemandArt = tmpResultQueue.take();
		if (jobDemandArt instanceof JobDemandArtEnd) {
			isSaveConUrlFlag = false;
			if (!this.isRecoveryMode) {
				resultQueue.add(jobDemandArt);
			}
			return;
		}
		String urlStr = jobDemandArt.getSource();
		URL url = new URL(urlStr);
		StringBuilder sb = new StringBuilder();
		String link = StringHelperUtil.transformUrl(urlStr);
		sb.append(link).append("\n");
		// 存储要抓取内容的网页链接地址
		FileUtils.saveToLocal(sb.toString().getBytes(), urlFileMap.get(url.getHost()).getAbsolutePath(), true);

		resultQueue.add(jobDemandArt);
	}

	// 内容抓取器主线程
	public class MainThread extends Thread {
		private final String[] seeds;
		private final String dateRecordFile;

		public MainThread(String[] seeds, String dateRecordFile) {
			this.seeds = seeds;
			this.dateRecordFile = dateRecordFile;
		}

		@Override
		public void run() {
			try {
				spideUrls(seeds, dateRecordFile);
			} catch (Exception e) {
				e.printStackTrace();
				LogUtil.error(errorLogger, "抓取url失败", e);
			}
		}
	}

	/**
	 * url抓取器启动方法
	 * 
	 * @param seeds
	 * @param dateRecordFile
	 */
	public BlockingQueue<JobDemandArt> run(String[] seeds, String dateRecordFile) {
		MainThread mainThread = new MainThread(seeds, dateRecordFile);
		mainThread.start();
		return resultQueue;
	}

	/**
	 * 是否满足条件
	 * 
	 * @return
	 */
	private boolean isMatchCondition() {
		if (!LinkDB.unVisitedUrlsEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * 判断工作是否完成
	 * 
	 * @return
	 */
	private boolean isJobOver() {
		try {
			counter = new CountDownLatch(caculator.get());
			counter.await();
			// 如果url工作线程，工作完后，未访问队列里依然是空的，就返回false；
			if (isMatchCondition()) {
				synchronized (this) {
					counter = null;
				}
				return true;
			}
			return false;
		} catch (InterruptedException e) {
			LogUtil.error(errorLogger, "获取招聘内容的url时，系统异常", e);
		}
		return false;
	}

	/**
	 * 计数器逐减
	 */
	private void counterDown() {
		synchronized (this) {
			if (counter != null) {
				counter.countDown();
				LogUtil.info(logger, "the counter number is : " + counter.getCount());
			}
		}
	}

	/**
	 * 从工作线程的对象池中，获取一个合适的工作线程
	 * 
	 * @param resultQueue
	 *            结果队列
	 * @param visitUrl
	 *            访问的url
	 * @return 空闲的工作线程
	 */
	private Thread findOutFreeWorker(ArrayBlockingQueue<JobDemandArt> resultQueue, String visitUrl) {
		URLFetchWorker freeWorker = null;
		while (freeWorker == null) {

			for (URLFetchWorker worker : workers) {
				synchronized (freeWorkerLock) {
					if (!worker.isBusy()) {
						worker.setResultQueue(resultQueue);
						worker.setVisitUrl(visitUrl);
						worker.setBusy();
						freeWorker = worker;
						return freeWorker;
					}
				}
			}
			try {
				synchronized (lock) {
					lock.wait(2000);
				}
			} catch (InterruptedException e) {
				LogUtil.error(errorLogger, "抓取招聘内容页的时候，报了系统异常", e);
			}
		}
		return freeWorker;
	}

	/**
	 * 将所有种子url要抓取的最新日期，给记录到指定的文件中
	 * 
	 * @param seeds
	 *            所有要抓取的种子url
	 * @param dateRecordFile
	 *            指定记录最新日期的文件
	 */
	private void recordLastDate(String[] seeds, String dateRecordFile) {
		try {
			for (String seed : seeds) {
				BaseParser baseParser = queryParser(seed);
				ConfigService.getInstance().flushRecentRecord(baseParser.getSite() + CommonConstants.LAST_RECORD_DATE,
						baseParser.getMaxDateStr());
				ConfigService.getInstance().storeRecentDate(dateRecordFile);
			}
		} catch (Exception e) {
			LogUtil.error(errorLogger, "记录最后更新日期出错，seeds：" + seeds, e);
		}

	}

	/**
	 * 根据种子抓取相应的url
	 * 
	 * @param url
	 * @param resultQueue
	 * @throws Exception
	 */
	private void fetchUrl(String visitUrl, ArrayBlockingQueue<JobDemandArt> resultQueue) throws Exception {

		BaseParser parser = queryParser(visitUrl);

		// 抓取到的网页内容
		String htmlContent = null;
		if (CommonConstants.OBTAIN_HTML_PARSERS.contains(parser.getClass())) {
			htmlContent = HttpUtil.getHtmlContent(visitUrl);
		} else {
			htmlContent = HttpUtil.doPost(visitUrl, null, "utf-8");
		}

		// 如果没有抓取到网页内容则跳过
		if (StringUtils.isBlank(htmlContent)) {
			throw new RuntimeException("抓取到的网页内容，空字符 串");
		}

		URL url = new URL(visitUrl);
		// 必须先抓取内容的url，再抓page的url
		List<JobDemandArt> contentLinks = parser.getLinks(htmlContent, this.isRecoveryMode);
		int tryTime = 3;
		while ((contentLinks == null || contentLinks.isEmpty()) && (tryTime-- > 0)) {
			// 抓取到的网页内容
			htmlContent = HttpUtil.getHtmlContent(visitUrl);
			parser = queryParser(visitUrl);
			// 必须先抓取内容的url，再抓page的url
			contentLinks = parser.getLinks(htmlContent, this.isRecoveryMode);
		}
		if ((contentLinks == null || contentLinks.isEmpty()) && !this.isRecoveryMode) {
			logger.info("the fetched contentLinks are null...");
			throw new RuntimeException("the contentLinks is null");
		}

		for (JobDemandArt jobDemandArt : contentLinks) {
			String link = jobDemandArt.getSource();
			link = StringHelperUtil.transformUrl(link);
			jobDemandArt.setSource(link);
			if (!link.startsWith("http")) {
				jobDemandArt.setSource(fillUpHead(url, link));
			}
			try {
				resultQueue.add(jobDemandArt);
			} catch (IllegalStateException e) {
				if (StringUtils.equals(e.getMessage(), "Queue full")) {
					while (resultQueue.size() > 999) {
						LogUtil.info(logger, "resultqueue is full wait 3 seconds");
						Thread.sleep(3000);
					}
					resultQueue.add(jobDemandArt);
				}
			}

		}

		List<String> pageLinks = parser.getPageLinks(htmlContent);
		// 新的未访问的 URL 入队
		for (String link : pageLinks) {
			link = StringHelperUtil.transformUrl(link);
			if (link.startsWith("http")) {
				LinkDB.addUnvisitedUrl(link);
			} else {
				LinkDB.addUnvisitedUrl(fillUpHead(url, link));
			}

		}
		LogUtil.info(logger, name + "：the url: " + visitUrl + " is fetched done.");
	}

	/**
	 * 补全抓取到的url的头部分
	 * 
	 * @param url
	 * @param link
	 * @return
	 */
	private String fillUpHead(URL url, String link) {
		StringBuilder sb = new StringBuilder();
		sb.append(url.getProtocol()).append("://").append(url.getHost());
		if (link.startsWith("/")) {
			sb.append(link);
		} else {
			sb.append("/").append(link);
		}
		return sb.toString();
	}

	/**
	 * 获取相应的解析器
	 * 
	 * @param visitUrl
	 * @return
	 * @throws MalformedURLException
	 */
	private BaseParser queryParser(String visitUrl) throws MalformedURLException {
		URL url = new URL(visitUrl);
		String host = url.getHost();
		BaseParser parser = parserMap.get(host);
		return parser;
	}

	/**
	 * 记录抓取失败的url和当时最新的日期
	 * 
	 * @param urlErrFile
	 *            记录抓取url发生错误的文件
	 * @param dtErrFile
	 *            记录抓取错误的上次日期
	 */
	private void recordFetchFaildUrl(String urlErrFile, String dtErrFile, String url) {
		synchronized (errorLock) {
			try {
				// 万一文件不存在，所以先创建文件
				FileUtils.createFile(urlErrFile);
				FileUtils.createFile(dtErrFile);
				// 进行错误文件记录
				FileWriter fw = new FileWriter(urlErrFile, true);
				fw.write(CommonConstants.ENTER_STR + url);
				fw.flush();
				fw.close();
				fw = new FileWriter(dtErrFile);
				BaseParser baseParser = this.queryParser(url);
				fw.write(StringUtils.defaultIfEmpty(
						baseParser.getSite() + CommonConstants.LAST_RECORD_DATE + "=" + baseParser.getLastDateStr(),
						StringUtils.EMPTY));
				fw.flush();
				fw.close();
			} catch (IOException e) {
				LogUtil.error(errorLogger, "记录错误文件出错：" + url, e);
			}
		}
	}

	/**
	 * 保存数据的线程
	 * 
	 * @author Liaobo
	 * 
	 */
	private class SaveConUrlWorker extends Thread {
		// 结果队列
		private ArrayBlockingQueue<JobDemandArt> tmpResultQueue = null;
		private ArrayBlockingQueue<JobDemandArt> resultQueue = null;

		public SaveConUrlWorker(ArrayBlockingQueue<JobDemandArt> tmpResultQueue,
				ArrayBlockingQueue<JobDemandArt> resultQueue) {
			this.tmpResultQueue = tmpResultQueue;
			this.resultQueue = resultQueue;
		}

		@Override
		public void run() {
			try {
				while (isSaveConUrlFlag) {
					saveContentUrlToFile(tmpResultQueue, resultQueue);
				}
			} catch (Exception e) {
				LogUtil.error(errorLogger, name + "：存储抓取内容的url，出错", e);
			}
		}
	}

	/**
	 * 真正执行抓取任务的工作者
	 * 
	 * @author Liaobo
	 * 
	 */
	private class URLFetchWorker extends Thread {

		// 结果队列
		private ArrayBlockingQueue<JobDemandArt> resultQueue = null;
		// 抓取的url
		private String visitUrl = null;
		// 是否正在工作
		private final AtomicBoolean isBusy = new AtomicBoolean(false);

		// 构造方法
		public URLFetchWorker(ArrayBlockingQueue<JobDemandArt> resultQueue, String visitUrl) {
			this.resultQueue = resultQueue;
			this.visitUrl = visitUrl;
		}

		// 默认构造方法
		public URLFetchWorker() {
		}

		@Override
		public void run() {
			try {
				fetchUrl(visitUrl, resultQueue);
			} catch (Exception e) {
				LogUtil.error(errorLogger, "抓取url出错，请详查，url：" + visitUrl, e);
				String urlErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.URL_FETCH_ERROR
						+ File.separator + CommonConstants.filefmt.format(new Date()) + File.separator
						+ CommonConstants.ERROR_URL;
				String dtErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.URL_FETCH_ERROR
						+ File.separator + CommonConstants.filefmt.format(new Date()) + File.separator
						+ CommonConstants.ERROR_DATE;
				recordFetchFaildUrl(urlErrFile, dtErrFile, visitUrl);
			} finally {
				// 唤醒可能等待的线程
				synchronized (lock) {
					lock.notifyAll();
				}
				// 抓取完减一
				LogUtil.info(logger, "the caculator is : " + caculator.decrementAndGet());
				if (counter != null) {
					counterDown();
				}
				// 设置本工作线程空闲
				isBusy.set(false);
			}
		}

		/**
		 * 设置本工人繁忙
		 */
		public void setBusy() {
			isBusy.set(true);
		}

		/**
		 * 是否繁忙
		 * 
		 * @return
		 */
		private boolean isBusy() {
			return isBusy.get();
		}

		/**
		 * @param resultQueue
		 *            the resultQueue to set
		 */
		public void setResultQueue(ArrayBlockingQueue<JobDemandArt> resultQueue) {
			this.resultQueue = resultQueue;
		}

		/**
		 * @param visitUrl
		 *            the visitUrl to set
		 */
		public void setVisitUrl(String visitUrl) {
			this.visitUrl = visitUrl;
		}

	}

	/**
	 * @param resultQueue
	 *            the resultQueue to set
	 */
	public void setResultQueue(ArrayBlockingQueue<JobDemandArt> resultQueue) {
		this.resultQueue = resultQueue;
	}

	/**
	 * @param isRecoveryMode
	 *            the isRecoveryMode to set
	 */
	public void setRecoveryMode(boolean isRecoveryMode) {
		this.isRecoveryMode = isRecoveryMode;
	}

	/**
	 * @return the isURLFetchDone
	 */
	public boolean isURLFetchDone() {
		return isURLFetchDone;
	}

	/**
	 * @param spiderLauncher
	 *            the spiderLauncher to set
	 */
	public void setSpiderLauncher(SpiderLauncher spiderLauncher) {
		this.spiderLauncher = spiderLauncher;
	}

	/**
	 * @return the executor
	 */
	public ThreadPoolExecutor getExecutor() {
		return executor;
	}

	public static void main(String[] args) {
		String homeDir = System.getProperty("user.dir");
		System.out.println(homeDir);
	}

}
