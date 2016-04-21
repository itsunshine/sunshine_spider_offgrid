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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.constants.TypeConstants;
import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.biz.model.JobDemandArtEnd;
import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.context.SpiderContext;
import com.boliao.sunshine.main.SpiderLauncher;
import com.boliao.sunshine.spider.BaseContentExtraction;

import net.sf.json.JSONObject;

/**
 * 内容抓取器
 * 
 * @author liaobo.lb
 * 
 */
public class ContentFetcher {
	// 日志记录器
	private final static Logger logger = Logger.getLogger(ContentFetcher.class);
	// 错误日志记录器
	private final static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// 标记内容页的抓取已经完成
	private final static String END = "end";
	// 抓取网页内容的实例
	private static ContentFetcher contentFetcher = new ContentFetcher();
	// 抓取内容的类型
	Map<String, String> configMap = new HashMap<String, String>();
	// 工作要求数据模型的队列
	BlockingQueue<JobDemandArt> queryQueue = null;
	// 协调执行任务的线程池
	private ThreadPoolExecutor executor = null;
	// 线程池里线程个数
	private final int THREADE_POOL_SIZE = 100;
	// 计数器
	AtomicInteger caculator = new AtomicInteger(0);
	// 工作线程的对象池
	List<ConFetchWorker> workers = new ArrayList<ConFetchWorker>(THREADE_POOL_SIZE);
	// 锁
	Object lock = new Object();

	// 空闲线程设置变量的锁
	Object freeWorkerLock = new Object();
	// 锁存器
	CountDownLatch counter = null;
	// 存储硬盘内容的线程
	SaveConUrlWorker saveConUrlWorker = null;

	// 是否是恢复模式启动，默认为false
	boolean isRecoveryMode = false;

	// 抓取失败的锁
	Object errorLock = new Object();

	// 内容抓取工作是否已经完成
	private boolean isContentFetchDone = false;

	// 主启动类
	SpiderLauncher spiderLauncher;

	private ContentFetcher() {
		configMap.put(CommonConstants.IBM, TypeConstants.ARTICLE);
		configMap.put(CommonConstants.ITEYE, TypeConstants.QUESTION);
		configMap.put(CommonConstants.TENCENTHR, TypeConstants.JOBDEMANDART);
		configMap.put(CommonConstants.BAIDUHR, TypeConstants.JOBDEMANDART);
		executor = new ThreadPoolExecutor(THREADE_POOL_SIZE, THREADE_POOL_SIZE, 10L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	// 获得实例对象
	public static ContentFetcher getInstance() {
		if (contentFetcher == null) {
			synchronized (ContentFetcher.class) {
				if (contentFetcher == null) {
					contentFetcher = new ContentFetcher();
				}
			}
		}
		return contentFetcher;
	}

	// 搜集内容的方法
	public void collect(JobDemandArt jobDemandArt) {
		BaseContentExtraction baseContentExtraction = BaseContentExtraction.getInstance(jobDemandArt.getSource());
		try {
			URL u = new URL(jobDemandArt.getSource());
			String host = u.getHost();
			String type = configMap.get(host);
			List<Object> articles = baseContentExtraction.collect(jobDemandArt);
			if (articles != null) {
				for (Object a : articles) {
					if (StringUtils.equals(type, TypeConstants.JOBDEMANDART)) {
						JSONObject obj = JSONObject.fromObject(a);
						saveConUrlWorker.addStoredContent(obj.toString());
					}
				}
			}
		} catch (MalformedURLException e) {
			LogUtil.error(errorLogger, "抓取招聘页面内容时出错，抓取的url为：" + jobDemandArt.getSource(), e);
		} catch (Exception e) {
			LogUtil.error(errorLogger, "抓取招聘页面内容时出错，抓取的url为：" + jobDemandArt.getSource(), e);
			String conErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.CON_FETCH_ERROR
					+ File.separator + CommonConstants.filefmt.format(new Date());
			recordFetchFaildUrl(conErrFile, jobDemandArt);
		}

	}

	/**
	 * 记录抓取失败的url和当时最新的日期
	 * 
	 * @param urlErrFile
	 *            记录抓取url发生错误的文件
	 * @param dtErrFile
	 *            记录抓取错误的上次日期
	 */
	private void recordFetchFaildUrl(String conErrFile, JobDemandArt jobDemandArt) {
		synchronized (errorLock) {
			try {
				// 万一文件不存在，所以先创建文件
				FileUtils.createFile(conErrFile);
				// 进行错误文件记录
				FileWriter fw = new FileWriter(conErrFile, true);
				JSONObject jobj = JSONObject.fromObject(jobDemandArt);
				fw.write(CommonConstants.ENTER_STR + jobj.toString());
				fw.flush();
				fw.close();
			} catch (IOException e) {
				LogUtil.error(errorLogger, "记录错误文件出错：" + jobDemandArt.getSource(), e);
			}
		}
	}

	// 内容抓取器主线程
	public class MainThread extends Thread {
		@Override
		public void run() {
			spide();
		}
	}

	/**
	 * 内容抓取器启动方法
	 */
	public void run() {
		new MainThread().start();
	}

	/**
	 * 开始抓取招聘页面的内容
	 */
	public void spide() {
		try {
			initWorkers();
			while (true) {
				JobDemandArt jobDemandArt = queryQueue.take();
				if (jobDemandArt instanceof JobDemandArtEnd) {
					waitJobOver();
					saveConUrlWorker.addStoredContent(END);
					break;
				}
				// 准备抓取时加1
				caculator.incrementAndGet();
				ConFetchWorker conFetchWorker = (ConFetchWorker) findOutFreeWorker(jobDemandArt);
				executor.execute(conFetchWorker);
			}
			executor.shutdown();
			isContentFetchDone = true;

			if (!this.isRecoveryMode) {
				// 唤醒，抓取主启动器
				spiderLauncher.increateOrNotify();
			}
		} catch (InterruptedException e) {
			LogUtil.error(errorLogger, "获取招聘对象参数时，出错", e);
		}

	}

	/**
	 * 初始化线程对象池
	 */
	private void initWorkers() {
		for (int i = 0; i < THREADE_POOL_SIZE; i++) {
			ConFetchWorker worker = new ConFetchWorker();
			workers.add(worker);
		}

		String filePath = spiderLauncher.baseDir + File.separator + "spider" + File.separator
				+ TypeConstants.JOBDEMANDART + File.separator + CommonConstants.filefmt.format(new Date()) + "_"
				+ TypeConstants.JOBDEMANDART;
		if (this.isRecoveryMode) {
			filePath = spiderLauncher.baseDir + File.separator + "recovery" + File.separator + "spider" + File.separator
					+ TypeConstants.JOBDEMANDART + File.separator + CommonConstants.filefmt.format(new Date()) + "_"
					+ TypeConstants.JOBDEMANDART;
		}
		SpiderContext.setJobDemandFile(filePath);
		try {
			FileUtils.createFile(filePath);
		} catch (IOException e) {
			LogUtil.error(errorLogger, "创建抓取结果文件时失败，出错", e);
			throw new RuntimeException(e);
		}

		saveConUrlWorker = new SaveConUrlWorker();
		saveConUrlWorker.start();
	}

	/**
	 * 计数器逐减
	 */
	private void counterDown() {
		synchronized (this) {
			counter.countDown();
		}
	}

	/**
	 * 判断工作是否完成
	 * 
	 * @return
	 */
	private void waitJobOver() {
		try {
			counter = new CountDownLatch(caculator.get());
			counter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
	private Thread findOutFreeWorker(JobDemandArt jobDemandArt) {
		ConFetchWorker freeWorker = null;
		while (freeWorker == null) {
			for (ConFetchWorker worker : workers) {
				// 这里必须保持空闲线程设置的原子性
				synchronized (freeWorkerLock) {
					if (!worker.isBusy()) {
						worker.setBusy();
						worker.setJobDemandArt(jobDemandArt);
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
	 * 创建存储json对象的文件
	 * 
	 * @param jsonStr
	 * @return
	 */
	private void writeToObjFile(String jsonStr, String type) {
		try {
			String filePath = SpiderContext.getJobDemandFile();
			FileUtils.createFile(filePath);
			jsonStr = jsonStr + "\n";
			FileUtils.saveToLocal(jsonStr.getBytes(), filePath, true);
		} catch (Exception e) {
			LogUtil.error(errorLogger, "将招聘要求内容，落地时，出错：" + jsonStr, e);
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
		private final ArrayBlockingQueue<String> tmpResultQueue = new ArrayBlockingQueue<String>(500);

		public SaveConUrlWorker() {

		}

		@Override
		public void run() {
			try {
				String content = tmpResultQueue.take();
				while (!StringUtils.equals(content, END)) {
					writeToObjFile(content, TypeConstants.JOBDEMANDART);
					content = tmpResultQueue.take();
				}
			} catch (Exception e) {
				LogUtil.error(errorLogger, "存储抓取到内容的content时，出错", e);
			}
		}

		/**
		 * 将要持久化的字符串，添加进临时队列
		 * 
		 * @param content
		 *            要被持久化队列
		 */
		public void addStoredContent(String content) {
			tmpResultQueue.add(content);
		}
	}

	/**
	 * @param urlQueue
	 *            the urlQueue to set
	 */
	public void setQueryQueue(BlockingQueue<JobDemandArt> urlQueue) {
		this.queryQueue = urlQueue;
	}

	/**
	 * 真正执行抓取任务的工作者
	 * 
	 * @author Liaobo
	 * 
	 */
	private class ConFetchWorker extends Thread {

		// 结果队列
		private JobDemandArt jobDemandArt = null;
		// 是否正在工作
		private final AtomicBoolean isBusy = new AtomicBoolean(false);

		// 构造方法
		public ConFetchWorker(JobDemandArt jobDemandArt) {
			this.jobDemandArt = jobDemandArt;
		}

		// 默认构造方法
		public ConFetchWorker() {
		}

		@Override
		public void run() {
			try {
				// 如果没有内容，就去抓取内容
				if (StringUtils.isBlank(jobDemandArt.getContent())) {
					collect(jobDemandArt);
				}
				// 如果想阿里网页一样，已经有了内容，就直接存入文件
				else {
					JSONObject obj = JSONObject.fromObject(jobDemandArt);
					saveConUrlWorker.addStoredContent(obj.toString());
				}

			} catch (Exception e) {
				LogUtil.error(errorLogger, "抓取url出错，请详查，url：" + jobDemandArt.getSource(), e);
			} finally {
				// 唤醒可能等待的线程
				synchronized (lock) {
					lock.notifyAll();
				}
				// 抓取完减一
				caculator.decrementAndGet();
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
		 * @param jobDemandArt
		 *            the jobDemandArt to set
		 */
		public void setJobDemandArt(JobDemandArt jobDemandArt) {
			this.jobDemandArt = jobDemandArt;
		}

		/**
		 * 是否繁忙
		 * 
		 * @return
		 */
		private boolean isBusy() {
			return isBusy.get();
		}

	}

	/**
	 * @return the isContentFetchDone
	 */
	public boolean isContentFetchDone() {
		return isContentFetchDone;
	}

	/**
	 * @param spiderLauncher
	 *            the spiderLauncher to set
	 */
	public void setSpiderLauncher(SpiderLauncher spiderLauncher) {
		this.spiderLauncher = spiderLauncher;
	}

	/**
	 * @param isRecoveryMode
	 *            the isRecoveryMode to set
	 */
	public void setRecoveryMode(boolean isRecoveryMode) {
		this.isRecoveryMode = isRecoveryMode;
	}

}
