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
 * ����ץȡ��
 * 
 * @author liaobo.lb
 * 
 */
public class ContentFetcher {
	// ��־��¼��
	private final static Logger logger = Logger.getLogger(ContentFetcher.class);
	// ������־��¼��
	private final static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);
	// �������ҳ��ץȡ�Ѿ����
	private final static String END = "end";
	// ץȡ��ҳ���ݵ�ʵ��
	private static ContentFetcher contentFetcher = new ContentFetcher();
	// ץȡ���ݵ�����
	Map<String, String> configMap = new HashMap<String, String>();
	// ����Ҫ������ģ�͵Ķ���
	BlockingQueue<JobDemandArt> queryQueue = null;
	// Э��ִ��������̳߳�
	private ThreadPoolExecutor executor = null;
	// �̳߳����̸߳���
	private final int THREADE_POOL_SIZE = 100;
	// ������
	AtomicInteger caculator = new AtomicInteger(0);
	// �����̵߳Ķ����
	List<ConFetchWorker> workers = new ArrayList<ConFetchWorker>(THREADE_POOL_SIZE);
	// ��
	Object lock = new Object();

	// �����߳����ñ�������
	Object freeWorkerLock = new Object();
	// ������
	CountDownLatch counter = null;
	// �洢Ӳ�����ݵ��߳�
	SaveConUrlWorker saveConUrlWorker = null;

	// �Ƿ��ǻָ�ģʽ������Ĭ��Ϊfalse
	boolean isRecoveryMode = false;

	// ץȡʧ�ܵ���
	Object errorLock = new Object();

	// ����ץȡ�����Ƿ��Ѿ����
	private boolean isContentFetchDone = false;

	// ��������
	SpiderLauncher spiderLauncher;

	private ContentFetcher() {
		configMap.put(CommonConstants.IBM, TypeConstants.ARTICLE);
		configMap.put(CommonConstants.ITEYE, TypeConstants.QUESTION);
		configMap.put(CommonConstants.TENCENTHR, TypeConstants.JOBDEMANDART);
		configMap.put(CommonConstants.BAIDUHR, TypeConstants.JOBDEMANDART);
		executor = new ThreadPoolExecutor(THREADE_POOL_SIZE, THREADE_POOL_SIZE, 10L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	// ���ʵ������
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

	// �Ѽ����ݵķ���
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
			LogUtil.error(errorLogger, "ץȡ��Ƹҳ������ʱ����ץȡ��urlΪ��" + jobDemandArt.getSource(), e);
		} catch (Exception e) {
			LogUtil.error(errorLogger, "ץȡ��Ƹҳ������ʱ����ץȡ��urlΪ��" + jobDemandArt.getSource(), e);
			String conErrFile = spiderLauncher.baseDir + File.separator + CommonConstants.CON_FETCH_ERROR
					+ File.separator + CommonConstants.filefmt.format(new Date());
			recordFetchFaildUrl(conErrFile, jobDemandArt);
		}

	}

	/**
	 * ��¼ץȡʧ�ܵ�url�͵�ʱ���µ�����
	 * 
	 * @param urlErrFile
	 *            ��¼ץȡurl����������ļ�
	 * @param dtErrFile
	 *            ��¼ץȡ������ϴ�����
	 */
	private void recordFetchFaildUrl(String conErrFile, JobDemandArt jobDemandArt) {
		synchronized (errorLock) {
			try {
				// ��һ�ļ������ڣ������ȴ����ļ�
				FileUtils.createFile(conErrFile);
				// ���д����ļ���¼
				FileWriter fw = new FileWriter(conErrFile, true);
				JSONObject jobj = JSONObject.fromObject(jobDemandArt);
				fw.write(CommonConstants.ENTER_STR + jobj.toString());
				fw.flush();
				fw.close();
			} catch (IOException e) {
				LogUtil.error(errorLogger, "��¼�����ļ�����" + jobDemandArt.getSource(), e);
			}
		}
	}

	// ����ץȡ�����߳�
	public class MainThread extends Thread {
		@Override
		public void run() {
			spide();
		}
	}

	/**
	 * ����ץȡ����������
	 */
	public void run() {
		new MainThread().start();
	}

	/**
	 * ��ʼץȡ��Ƹҳ�������
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
				// ׼��ץȡʱ��1
				caculator.incrementAndGet();
				ConFetchWorker conFetchWorker = (ConFetchWorker) findOutFreeWorker(jobDemandArt);
				executor.execute(conFetchWorker);
			}
			executor.shutdown();
			isContentFetchDone = true;

			if (!this.isRecoveryMode) {
				// ���ѣ�ץȡ��������
				spiderLauncher.increateOrNotify();
			}
		} catch (InterruptedException e) {
			LogUtil.error(errorLogger, "��ȡ��Ƹ�������ʱ������", e);
		}

	}

	/**
	 * ��ʼ���̶߳����
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
			LogUtil.error(errorLogger, "����ץȡ����ļ�ʱʧ�ܣ�����", e);
			throw new RuntimeException(e);
		}

		saveConUrlWorker = new SaveConUrlWorker();
		saveConUrlWorker.start();
	}

	/**
	 * ���������
	 */
	private void counterDown() {
		synchronized (this) {
			counter.countDown();
		}
	}

	/**
	 * �жϹ����Ƿ����
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
	 * �ӹ����̵߳Ķ�����У���ȡһ�����ʵĹ����߳�
	 * 
	 * @param resultQueue
	 *            �������
	 * @param visitUrl
	 *            ���ʵ�url
	 * @return ���еĹ����߳�
	 */
	private Thread findOutFreeWorker(JobDemandArt jobDemandArt) {
		ConFetchWorker freeWorker = null;
		while (freeWorker == null) {
			for (ConFetchWorker worker : workers) {
				// ������뱣�ֿ����߳����õ�ԭ����
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
				LogUtil.error(errorLogger, "ץȡ��Ƹ����ҳ��ʱ�򣬱���ϵͳ�쳣", e);
			}
		}
		return freeWorker;
	}

	/**
	 * �����洢json������ļ�
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
			LogUtil.error(errorLogger, "����ƸҪ�����ݣ����ʱ������" + jsonStr, e);
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
				LogUtil.error(errorLogger, "�洢ץȡ�����ݵ�contentʱ������", e);
			}
		}

		/**
		 * ��Ҫ�־û����ַ�������ӽ���ʱ����
		 * 
		 * @param content
		 *            Ҫ���־û�����
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
	 * ����ִ��ץȡ����Ĺ�����
	 * 
	 * @author Liaobo
	 * 
	 */
	private class ConFetchWorker extends Thread {

		// �������
		private JobDemandArt jobDemandArt = null;
		// �Ƿ����ڹ���
		private final AtomicBoolean isBusy = new AtomicBoolean(false);

		// ���췽��
		public ConFetchWorker(JobDemandArt jobDemandArt) {
			this.jobDemandArt = jobDemandArt;
		}

		// Ĭ�Ϲ��췽��
		public ConFetchWorker() {
		}

		@Override
		public void run() {
			try {
				// ���û�����ݣ���ȥץȡ����
				if (StringUtils.isBlank(jobDemandArt.getContent())) {
					collect(jobDemandArt);
				}
				// ����밢����ҳһ�����Ѿ��������ݣ���ֱ�Ӵ����ļ�
				else {
					JSONObject obj = JSONObject.fromObject(jobDemandArt);
					saveConUrlWorker.addStoredContent(obj.toString());
				}

			} catch (Exception e) {
				LogUtil.error(errorLogger, "ץȡurl��������飬url��" + jobDemandArt.getSource(), e);
			} finally {
				// ���ѿ��ܵȴ����߳�
				synchronized (lock) {
					lock.notifyAll();
				}
				// ץȡ���һ
				caculator.decrementAndGet();
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
		 * @param jobDemandArt
		 *            the jobDemandArt to set
		 */
		public void setJobDemandArt(JobDemandArt jobDemandArt) {
			this.jobDemandArt = jobDemandArt;
		}

		/**
		 * �Ƿ�æ
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
