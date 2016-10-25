/**
 * 
 */
package com.boliao.sunshine.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.boliao.sunshine.biz.model.JobDemandArt;
import com.boliao.sunshine.biz.model.JobDemandArtEnd;
import com.boliao.sunshine.biz.utils.FileUtils;
import com.boliao.sunshine.biz.utils.LogUtil;
import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.context.SpiderContext;
import com.boliao.sunshine.fetchers.ContentFetcher;
import com.boliao.sunshine.fetchers.URLFetcher;
import com.boliao.sunshine.upload.SendFile;

import net.sf.json.JSONObject;

/**
 * ץȡ��ҳ����
 * 
 * @author liaobo
 * 
 */
public class SpiderLauncher {

	// �ռǼ�¼����
	private static Logger logger = Logger.getLogger(SpiderLauncher.class);

	// ϵͳ�쳣����־��¼��
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	// ��ҳ����ץȡ��
	URLFetcher uRLFetcher;

	// ����ץȡ��
	ContentFetcher contentFetcher;

	// �ָ�ģʽ�£��Ƿ�������urlfetcherץȡ��
	boolean isUrlFetcherLauch = false;

	// ��Ҫ��������ץȡ����Ŀ��
	private final int NEED_COMP_NUM = 2;

	// ץȡ��ҳ���ݺ�Ļ���Ŀ¼
	public static String baseDir = "";

	// �����ж��Ƿ�����һ���
	AtomicInteger needCompNum = new AtomicInteger(0);

	public SpiderLauncher() {
		uRLFetcher = URLFetcher.getInstance();
		contentFetcher = ContentFetcher.getInstance();
	}

	/**
	 * �������ļ��У�������е�����
	 * 
	 * @return ����url������
	 */
	public String[] obtainSeeds(String seedUrlFile) {
		List<String> seedList = new ArrayList<String>();
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(seedUrlFile);
			br = new BufferedReader(fr);
			String line = br.readLine();
			while (line != null) {
				if (!line.startsWith("#"))
					seedList.add(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return seedList.toArray(new String[0]);
	}

	/**
	 * ����ץȡ�����߳�
	 */
	public void notifyLauncher() {
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * launcher�ȴ���������ץȡ���������ץȡ����
	 * 
	 * @throws InterruptedException
	 */
	public void waitLauncher() throws InterruptedException {
		synchronized (this) {
			this.wait();
		}
	}

	/**
	 * ��Ҫ���ץȡ�ģ�ץȡ�����������ж��Ƿ�Ӧ�����һ���
	 */
	public void increateOrNotify() {
		int compNum = needCompNum.incrementAndGet();
		if (compNum == NEED_COMP_NUM) {
			this.notifyLauncher();
		}
	}

	/**
	 * spider ���������
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		inputArgsCheck(args);
		try {
			// ��ȡ��������Ĳ���
			String seedsFile = null;
			String recentDateRecordFile = null;
			boolean isRecovery = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-s")) {
					seedsFile = args[++i];
				} else if (args[i].equals("-d")) {
					recentDateRecordFile = args[++i];
				} else if (args[i].equals("-r")) {
					isRecovery = true;
				} else if (args[i].equals("-b")) {
					baseDir = args[++i];
				}
			}
			final SpiderLauncher spiderLauncher = new SpiderLauncher();
			// ���������У��
			if ((StringUtils.isBlank(seedsFile) || StringUtils.isBlank(recentDateRecordFile)
					|| StringUtils.isBlank(baseDir)) && !isRecovery) {
				printArgumentsAlerts(args);
			}
			// ץȡ���������������ļ�����
			ConfigService.getInstance().defaultLastDateRecordFile = recentDateRecordFile;
			// ��ʼ��url��ץȡ�����Լ����ݵ�ץȡ��������������ץȡ�߳�
			final ArrayBlockingQueue<JobDemandArt> resultQueue = new ArrayBlockingQueue<JobDemandArt>(1000);
			spiderLauncher.uRLFetcher.setResultQueue(resultQueue);
			spiderLauncher.contentFetcher.setQueryQueue(resultQueue);
			spiderLauncher.contentFetcher.run();

			// ��ץȡ��������URLץȡ������contentץȡ����
			spiderLauncher.uRLFetcher.setSpiderLauncher(spiderLauncher);
			spiderLauncher.contentFetcher.setSpiderLauncher(spiderLauncher);
			final ConfigService configService = ConfigService.getInstance();
			// ����ǻָ�������ģʽ������������ļ���ַ���������ڵ�ַ
			if (isRecovery) {
				spiderLauncher.uRLFetcher.setRecoveryMode(true);
				spiderLauncher.contentFetcher.setRecoveryMode(true);
				ConfigService.isLocalSpecifiedMode = true;

				// ����Ŀ¼�µ�urlץȡʧ�ܵ�Ŀ¼
				String urlRecoveries = baseDir + File.separator + CommonConstants.URL_FETCH_ERROR;
				File urlErrDirs = new File(urlRecoveries);
				// ���Ŀ¼�µģ�����ץȡʧ�ܵ������ļ���
				final File[] urlErrs = urlErrDirs.listFiles();
				if (urlErrs.length > 0) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								spiderLauncher.asyncLaunchUrlFetcher(urlErrs, configService, resultQueue);
							} catch (Exception e) {
								LogUtil.error(errorLogger, "itsunshine URLץȡ�����첽ץȡurl���ݴ���", e);
							}
						}

					}).start();
					spiderLauncher.isUrlFetcherLauch = true;
					spiderLauncher.waitLauncher();
				}

				String conRecoveries = baseDir + File.separator + CommonConstants.CON_FETCH_ERROR;
				File conErrDirs = new File(conRecoveries);
				final File[] conErrs = conErrDirs.listFiles();
				if (conErrs != null && conErrs.length > 0) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								spiderLauncher.asyncLaunchConFetcher(conErrs, resultQueue);
							} catch (Exception e) {
								LogUtil.error(errorLogger, "itsunshine CONץȡ�����첽ץȡurl���ݴ���", e);
							}
						}

					}).start();
					spiderLauncher.waitLauncher();
				}
				while (true) {
					// ���urlץȡ�����Ѿ���ɣ���ɾ����֮ǰץȡʧ�ܵ���־�ļ�;�������ץȡ�Ĺ����Ѿ���ɣ���ɾ��֮ǰץȡʧ�ܵ���־�ļ�
					if (spiderLauncher.uRLFetcher.isURLFetchDone()
							&& spiderLauncher.contentFetcher.isContentFetchDone()) {
						for (File file : urlErrs) {
							LogUtil.info(logger, "the deleted urlFile is : " + file.getAbsolutePath());
							FileUtils.deleteDir(file.getAbsolutePath());
						}
						for (File file : conErrs) {
							LogUtil.info(logger, "the deleted conFile is : " + file.getAbsolutePath());
							FileUtils.deleteDir(file.getAbsolutePath());
						}
						break;
					}
					Thread.sleep(1000);
				}

			} else {
				configService.init();
				String[] seeds = spiderLauncher.obtainSeeds(seedsFile);
				spiderLauncher.uRLFetcher.setResultQueue(resultQueue);
				spiderLauncher.contentFetcher.setQueryQueue(resultQueue);
				// ץȡurl����
				BlockingQueue<JobDemandArt> urlFetchResult = spiderLauncher.uRLFetcher.run(seeds, recentDateRecordFile);
				// ץȡ����
				spiderLauncher.waitLauncher();
			}
			// ѹ���ļ������ϴ�����������
			File jobDemandFile = new File(SpiderContext.getJobDemandFile());
			// File jobDemandFile = new
			// File("D:/workspace/spiderServiceUtil/recovery/spider/jobDemandArt/20150502_jobDemandArt");
			// KenZip.zip(jobDemandFile.getName(), jobDemandFile);
			boolean uploadResult = false;
			while (!uploadResult) {
				try {
					uploadResult = SendFile.uploadFile(CommonConstants.UPLOAD_URL, jobDemandFile.getAbsolutePath());
				} catch (Exception e) {
					LogUtil.error(errorLogger, "itsunshine��Ƹ��Ϣ���ݣ��ϴ�ʧ��", e);
					// ��������ֱ���˳�
					System.exit(0);
					// ���ץȡʧ�ܣ�������10���Ӻ��ٴ�ץȡ
					Thread.sleep(1000 * 60 * 10);
				}

			}
		} catch (Exception e) {
			LogUtil.error(errorLogger, "itsunshineץȡ���ݴ���", e);
		}
	}

	/**
	 * �첽�ָ�ץȡ��������־���url
	 * 
	 * @param urlErrs
	 * @param configService
	 * @throws Exception
	 */
	private void asyncLaunchConFetcher(final File[] conErrs, final ArrayBlockingQueue<JobDemandArt> resultQueue)
			throws Exception {
		// �ָ�����ץȡ����Ĺ�����Ϣ
		for (File file : conErrs) {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while (line != null) {
				if (!StringUtils.isBlank(line)) {
					JSONObject jObj = JSONObject.fromObject(line);
					JobDemandArt jobDemandArt = (JobDemandArt) JSONObject.toBean(jObj, JobDemandArt.class);
					resultQueue.add(jobDemandArt);
				}
				line = br.readLine();
			}
			if (!this.isUrlFetcherLauch) {
				resultQueue.add(new JobDemandArtEnd());
			}
			fr.close();
			br.close();
		}
		this.notifyLauncher();
	}

	/**
	 * �첽�ָ�ץȡ��������־���url
	 * 
	 * @param urlErrs
	 * @param configService
	 * @throws Exception
	 */
	private void asyncLaunchUrlFetcher(File[] urlErrs, ConfigService configService,
			ArrayBlockingQueue<JobDemandArt> resultQueue) throws Exception {
		for (File urlErr : urlErrs) {
			File[] files = urlErr.listFiles();
			String seedsFile = null;
			String recentDateRecordFile = null;
			for (File f : files) {
				// �������ļ����µ��ļ�����ƥ��
				if (f.getName().equals(CommonConstants.ERROR_URL)) {
					seedsFile = f.getAbsolutePath();
				}
				if (f.getName().equals(CommonConstants.ERROR_DATE)) {
					recentDateRecordFile = f.getAbsolutePath();
					configService.setLocalLastDateRecord(recentDateRecordFile);
					configService.clearProperties();
					configService.init();
				}
			}
			if (StringUtils.isNotBlank(seedsFile) && StringUtils.isNotBlank(recentDateRecordFile)) {
				String[] seeds = this.obtainSeeds(seedsFile);
				// ץȡurl����
				this.uRLFetcher.spideUrls(seeds, recentDateRecordFile); // ץȡ����
			}
		}
		this.uRLFetcher.getExecutor().shutdown();
		resultQueue.add(new JobDemandArtEnd());
		this.notifyLauncher();
	}

	/**
	 * �������У��
	 * 
	 * @param args
	 */
	private static void inputArgsCheck(String[] args) {
		if (args == null || args.length == 0 || args.length < 4) {
			// ����ǻָ�ģʽ�����ģ���ֱ�ӷ���
			if (args[0].startsWith("-r"))
				return;
			printArgumentsAlerts(args);
		}
	}

	/**
	 * ��ӡ���������������־
	 * 
	 * @param args
	 */
	private static void printArgumentsAlerts(String[] args) {
		LogUtil.warn(logger,
				"itsunshine--->spider module: the input args wrong. use -s input seeds file,use -b input basepath use -d input dateRecord file or -r set the run mode to recovery mode!");
		throw new RuntimeException("�����������args��" + args);
	}
}
