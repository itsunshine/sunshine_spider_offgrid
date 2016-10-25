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
 * 抓取网页内容
 * 
 * @author liaobo
 * 
 */
public class SpiderLauncher {

	// 日记记录工具
	private static Logger logger = Logger.getLogger(SpiderLauncher.class);

	// 系统异常，日志记录器
	private static Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	// 网页链接抓取器
	URLFetcher uRLFetcher;

	// 内容抓取器
	ContentFetcher contentFetcher;

	// 恢复模式下，是否启动了urlfetcher抓取器
	boolean isUrlFetcherLauch = false;

	// 需要完成任务的抓取器数目；
	private final int NEED_COMP_NUM = 2;

	// 抓取网页内容后的基础目录
	public static String baseDir = "";

	// 用于判断是否该自我唤醒
	AtomicInteger needCompNum = new AtomicInteger(0);

	public SpiderLauncher() {
		uRLFetcher = URLFetcher.getInstance();
		contentFetcher = ContentFetcher.getInstance();
	}

	/**
	 * 从种子文件中，获得所有的种子
	 * 
	 * @return 种子url的数组
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
	 * 唤醒抓取启动线程
	 */
	public void notifyLauncher() {
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * launcher等待，其它的抓取器完成所有抓取任务
	 * 
	 * @throws InterruptedException
	 */
	public void waitLauncher() throws InterruptedException {
		synchronized (this) {
			this.wait();
		}
	}

	/**
	 * 需要完成抓取的，抓取数自增，并判断是否应该自我唤醒
	 */
	public void increateOrNotify() {
		int compNum = needCompNum.incrementAndGet();
		if (compNum == NEED_COMP_NUM) {
			this.notifyLauncher();
		}
	}

	/**
	 * spider 主函数入口
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		inputArgsCheck(args);
		try {
			// 获取所有输入的参数
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
			// 输入参数，校验
			if ((StringUtils.isBlank(seedsFile) || StringUtils.isBlank(recentDateRecordFile)
					|| StringUtils.isBlank(baseDir)) && !isRecovery) {
				printArgumentsAlerts(args);
			}
			// 抓取的最新日期配置文件进行
			ConfigService.getInstance().defaultLastDateRecordFile = recentDateRecordFile;
			// 初始化url的抓取器，以及内容的抓取器，并启动内容抓取线程
			final ArrayBlockingQueue<JobDemandArt> resultQueue = new ArrayBlockingQueue<JobDemandArt>(1000);
			spiderLauncher.uRLFetcher.setResultQueue(resultQueue);
			spiderLauncher.contentFetcher.setQueryQueue(resultQueue);
			spiderLauncher.contentFetcher.run();

			// 将抓取器，存入URL抓取器，和content抓取器中
			spiderLauncher.uRLFetcher.setSpiderLauncher(spiderLauncher);
			spiderLauncher.contentFetcher.setSpiderLauncher(spiderLauncher);
			final ConfigService configService = ConfigService.getInstance();
			// 如果是恢复型运行模式，则计算种子文件地址和最新日期地址
			if (isRecovery) {
				spiderLauncher.uRLFetcher.setRecoveryMode(true);
				spiderLauncher.contentFetcher.setRecoveryMode(true);
				ConfigService.isLocalSpecifiedMode = true;

				// 运行目录下的url抓取失败的目录
				String urlRecoveries = baseDir + File.separator + CommonConstants.URL_FETCH_ERROR;
				File urlErrDirs = new File(urlRecoveries);
				// 获得目录下的，所有抓取失败的日期文件夹
				final File[] urlErrs = urlErrDirs.listFiles();
				if (urlErrs.length > 0) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								spiderLauncher.asyncLaunchUrlFetcher(urlErrs, configService, resultQueue);
							} catch (Exception e) {
								LogUtil.error(errorLogger, "itsunshine URL抓取器，异步抓取url数据错误", e);
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
								LogUtil.error(errorLogger, "itsunshine CON抓取器，异步抓取url数据错误", e);
							}
						}

					}).start();
					spiderLauncher.waitLauncher();
				}
				while (true) {
					// 如果url抓取工作已经完成，就删除掉之前抓取失败的日志文件;如果内容抓取的工作已经完成，就删除之前抓取失败的日志文件
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
				// 抓取url种子
				BlockingQueue<JobDemandArt> urlFetchResult = spiderLauncher.uRLFetcher.run(seeds, recentDateRecordFile);
				// 抓取内容
				spiderLauncher.waitLauncher();
			}
			// 压缩文件，并上传到服务器上
			File jobDemandFile = new File(SpiderContext.getJobDemandFile());
			// File jobDemandFile = new
			// File("D:/workspace/spiderServiceUtil/recovery/spider/jobDemandArt/20150502_jobDemandArt");
			// KenZip.zip(jobDemandFile.getName(), jobDemandFile);
			boolean uploadResult = false;
			while (!uploadResult) {
				try {
					uploadResult = SendFile.uploadFile(CommonConstants.UPLOAD_URL, jobDemandFile.getAbsolutePath());
				} catch (Exception e) {
					LogUtil.error(errorLogger, "itsunshine招聘信息数据，上传失败", e);
					// 发生错误直接退出
					System.exit(0);
					// 如果抓取失败，就休眠10分钟后，再次抓取
					Thread.sleep(1000 * 60 * 10);
				}

			}
		} catch (Exception e) {
			LogUtil.error(errorLogger, "itsunshine抓取数据错误", e);
		}
	}

	/**
	 * 异步恢复抓取，错误日志里的url
	 * 
	 * @param urlErrs
	 * @param configService
	 * @throws Exception
	 */
	private void asyncLaunchConFetcher(final File[] conErrs, final ArrayBlockingQueue<JobDemandArt> resultQueue)
			throws Exception {
		// 恢复内容抓取错误的工作信息
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
	 * 异步恢复抓取，错误日志里的url
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
				// 对日期文件夹下的文件进行匹配
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
				// 抓取url种子
				this.uRLFetcher.spideUrls(seeds, recentDateRecordFile); // 抓取内容
			}
		}
		this.uRLFetcher.getExecutor().shutdown();
		resultQueue.add(new JobDemandArtEnd());
		this.notifyLauncher();
	}

	/**
	 * 输入参数校验
	 * 
	 * @param args
	 */
	private static void inputArgsCheck(String[] args) {
		if (args == null || args.length == 0 || args.length < 4) {
			// 如果是恢复模式启动的，则直接返回
			if (args[0].startsWith("-r"))
				return;
			printArgumentsAlerts(args);
		}
	}

	/**
	 * 打印输入参数，错误日志
	 * 
	 * @param args
	 */
	private static void printArgumentsAlerts(String[] args) {
		LogUtil.warn(logger,
				"itsunshine--->spider module: the input args wrong. use -s input seeds file,use -b input basepath use -d input dateRecord file or -r set the run mode to recovery mode!");
		throw new RuntimeException("输入参数不对args：" + args);
	}
}
