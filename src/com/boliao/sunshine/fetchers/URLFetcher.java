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
 * ��ҳurlץȡ��
 * 
 * @author liaobo
 * 
 */
public class URLFetcher {

	// ��־��¼��
	private static Logger logger = Logger.getLogger(URLFetcher.class);

	// ϵͳ�쳣����־��¼��
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	// ����
	private final String name = URLFetcher.class.getSimpleName();

	// ץȡ��ҳurl��ʵ��
	private static URLFetcher urlFetcher = new URLFetcher();

	// ��ͬ��վ���ݵ�parser
	private final Map<String, BaseParser> parserMap = new ConcurrentHashMap<String, BaseParser>();

	// Э��ִ��������̳߳�
	private ThreadPoolExecutor executor = null;

	// �̳߳����̸߳���
	private final int THREADE_POOL_SIZE = 10;

	// �Ƿ�洢����url
	private boolean isSaveConUrlFlag = true;

	// �����̵߳Ķ����
	List<URLFetchWorker> workers = new ArrayList<URLFetchWorker>(THREADE_POOL_SIZE);

	// ������
	AtomicInteger caculator = new AtomicInteger(0);

	// ������
	CountDownLatch counter = null;

	// ��
	Object lock = new Object();

	// ץȡʧ�ܵ���
	Object errorLock = new Object();

	// �����߳����ñ�������
	Object freeWorkerLock = new Object();

	// ץȡ����Ķ���
	ArrayBlockingQueue<JobDemandArt> resultQueue = null;

	// �Ƿ��ǻָ�ģʽ������Ĭ��Ϊfalse
	boolean isRecoveryMode = false;

	// urlץȡ�����Ƿ����
	boolean isURLFetchDone = false;

	// ��������
	SpiderLauncher spiderLauncher;

	// ��ͬhost��Ӧ�ļ�¼�ļ�
	Map<String, File> urlFileMap = new HashMap<String, File>();

	/**
	 * ���췽������ʼ��ץȡ����
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
				LogUtil.error(errorLogger, "����ץȡ����ļ�ʱʧ�ܣ�����", e);
				throw new RuntimeException(e);
			}
			urlFileMap.put(url, file);
		}
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
	 * ��ȡ���У����Ի������url�ģ�����
	 * 
	 * @param seeds
	 *            ץȡ����url����������
	 * @return ����ץȡ���ݵ�url����
	 * @throws Exception
	 */
	public BlockingQueue<JobDemandArt> spideUrls(String[] seeds, String dateRecordFile) throws Exception {
		// ��ʼ����������
		initCrawlerWithSeeds(seeds);
		ArrayBlockingQueue<JobDemandArt> tmpResultQueue = new ArrayBlockingQueue<JobDemandArt>(1000);
		new SaveConUrlWorker(tmpResultQueue, resultQueue).start();
		// ѭ����������ץȡ�����Ӳ�����ץȡ����ҳ������ 1000
		while (isMatchCondition() || (!isMatchCondition() && isJobOver())) {
			// ��ͷ URL ����
			String visitUrl = LinkDB.unVisitedUrlDeQueue();
			if (visitUrl == null)
				continue;
			LogUtil.info(logger, name + "��start to fetch this url : " + visitUrl);
			// ׼��ץȡʱ��1
			caculator.incrementAndGet();
			Thread worker = findOutFreeWorker(tmpResultQueue, visitUrl);
			executor.execute(worker);
		}
		if (!this.isRecoveryMode) {
			executor.shutdown();
			recordLastDate(seeds, dateRecordFile);
			// ���ѣ�ץȡ��������
			spiderLauncher.increateOrNotify();
		}
		tmpResultQueue.add(new JobDemandArtEnd());
		isURLFetchDone = true;
		return resultQueue;
	}

	/**
	 * ���м������������ļ���
	 * 
	 * @param tmpResultQueue
	 * @param resultQueue
	 *            ���ս��
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
		// �洢Ҫץȡ���ݵ���ҳ���ӵ�ַ
		FileUtils.saveToLocal(sb.toString().getBytes(), urlFileMap.get(url.getHost()).getAbsolutePath(), true);

		resultQueue.add(jobDemandArt);
	}

	// ����ץȡ�����߳�
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
				LogUtil.error(errorLogger, "ץȡurlʧ��", e);
			}
		}
	}

	/**
	 * urlץȡ����������
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
	 * �Ƿ���������
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
	 * �жϹ����Ƿ����
	 * 
	 * @return
	 */
	private boolean isJobOver() {
		try {
			counter = new CountDownLatch(caculator.get());
			counter.await();
			// ���url�����̣߳��������δ���ʶ�������Ȼ�ǿյģ��ͷ���false��
			if (isMatchCondition()) {
				synchronized (this) {
					counter = null;
				}
				return true;
			}
			return false;
		} catch (InterruptedException e) {
			LogUtil.error(errorLogger, "��ȡ��Ƹ���ݵ�urlʱ��ϵͳ�쳣", e);
		}
		return false;
	}

	/**
	 * ���������
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
	 * �ӹ����̵߳Ķ�����У���ȡһ�����ʵĹ����߳�
	 * 
	 * @param resultQueue
	 *            �������
	 * @param visitUrl
	 *            ���ʵ�url
	 * @return ���еĹ����߳�
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
				LogUtil.error(errorLogger, "ץȡ��Ƹ����ҳ��ʱ�򣬱���ϵͳ�쳣", e);
			}
		}
		return freeWorker;
	}

	/**
	 * ����������urlҪץȡ���������ڣ�����¼��ָ�����ļ���
	 * 
	 * @param seeds
	 *            ����Ҫץȡ������url
	 * @param dateRecordFile
	 *            ָ����¼�������ڵ��ļ�
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
			LogUtil.error(errorLogger, "��¼���������ڳ���seeds��" + seeds, e);
		}

	}

	/**
	 * ��������ץȡ��Ӧ��url
	 * 
	 * @param url
	 * @param resultQueue
	 * @throws Exception
	 */
	private void fetchUrl(String visitUrl, ArrayBlockingQueue<JobDemandArt> resultQueue) throws Exception {

		BaseParser parser = queryParser(visitUrl);

		// ץȡ������ҳ����
		String htmlContent = null;
		if (CommonConstants.OBTAIN_HTML_PARSERS.contains(parser.getClass())) {
			htmlContent = HttpUtil.getHtmlContent(visitUrl);
		} else {
			htmlContent = HttpUtil.doPost(visitUrl, null, "utf-8");
		}

		// ���û��ץȡ����ҳ����������
		if (StringUtils.isBlank(htmlContent)) {
			throw new RuntimeException("ץȡ������ҳ���ݣ����ַ� ��");
		}

		URL url = new URL(visitUrl);
		// ������ץȡ���ݵ�url����ץpage��url
		List<JobDemandArt> contentLinks = parser.getLinks(htmlContent, this.isRecoveryMode);
		int tryTime = 3;
		while ((contentLinks == null || contentLinks.isEmpty()) && (tryTime-- > 0)) {
			// ץȡ������ҳ����
			htmlContent = HttpUtil.getHtmlContent(visitUrl);
			parser = queryParser(visitUrl);
			// ������ץȡ���ݵ�url����ץpage��url
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
		// �µ�δ���ʵ� URL ���
		for (String link : pageLinks) {
			link = StringHelperUtil.transformUrl(link);
			if (link.startsWith("http")) {
				LinkDB.addUnvisitedUrl(link);
			} else {
				LinkDB.addUnvisitedUrl(fillUpHead(url, link));
			}

		}
		LogUtil.info(logger, name + "��the url: " + visitUrl + " is fetched done.");
	}

	/**
	 * ��ȫץȡ����url��ͷ����
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
	 * ��ȡ��Ӧ�Ľ�����
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
	 * ��¼ץȡʧ�ܵ�url�͵�ʱ���µ�����
	 * 
	 * @param urlErrFile
	 *            ��¼ץȡurl����������ļ�
	 * @param dtErrFile
	 *            ��¼ץȡ������ϴ�����
	 */
	private void recordFetchFaildUrl(String urlErrFile, String dtErrFile, String url) {
		synchronized (errorLock) {
			try {
				// ��һ�ļ������ڣ������ȴ����ļ�
				FileUtils.createFile(urlErrFile);
				FileUtils.createFile(dtErrFile);
				// ���д����ļ���¼
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
				LogUtil.error(errorLogger, "��¼�����ļ�����" + url, e);
			}
		}
	}

	/**
	 * �������ݵ��߳�
	 * 
	 * @author Liaobo
	 * 
	 */
	private class SaveConUrlWorker extends Thread {
		// �������
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
				LogUtil.error(errorLogger, name + "���洢ץȡ���ݵ�url������", e);
			}
		}
	}

	/**
	 * ����ִ��ץȡ����Ĺ�����
	 * 
	 * @author Liaobo
	 * 
	 */
	private class URLFetchWorker extends Thread {

		// �������
		private ArrayBlockingQueue<JobDemandArt> resultQueue = null;
		// ץȡ��url
		private String visitUrl = null;
		// �Ƿ����ڹ���
		private final AtomicBoolean isBusy = new AtomicBoolean(false);

		// ���췽��
		public URLFetchWorker(ArrayBlockingQueue<JobDemandArt> resultQueue, String visitUrl) {
			this.resultQueue = resultQueue;
			this.visitUrl = visitUrl;
		}

		// Ĭ�Ϲ��췽��
		public URLFetchWorker() {
		}

		@Override
		public void run() {
			try {
				fetchUrl(visitUrl, resultQueue);
			} catch (Exception e) {
				LogUtil.error(errorLogger, "ץȡurl��������飬url��" + visitUrl, e);
				String urlErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.URL_FETCH_ERROR
						+ File.separator + CommonConstants.filefmt.format(new Date()) + File.separator
						+ CommonConstants.ERROR_URL;
				String dtErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.URL_FETCH_ERROR
						+ File.separator + CommonConstants.filefmt.format(new Date()) + File.separator
						+ CommonConstants.ERROR_DATE;
				recordFetchFaildUrl(urlErrFile, dtErrFile, visitUrl);
			} finally {
				// ���ѿ��ܵȴ����߳�
				synchronized (lock) {
					lock.notifyAll();
				}
				// ץȡ���һ
				LogUtil.info(logger, "the caculator is : " + caculator.decrementAndGet());
				if (counter != null) {
					counterDown();
				}
				// ���ñ������߳̿���
				isBusy.set(false);
			}
		}

		/**
		 * ���ñ����˷�æ
		 */
		public void setBusy() {
			isBusy.set(true);
		}

		/**
		 * �Ƿ�æ
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
