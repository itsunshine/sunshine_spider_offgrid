/**
 * 
 */
package com.boliao.sunshine.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import com.boliao.sunshine.biz.utils.StringHelperUtil;

/**
 * @author Liaobo
 * 
 */
public class VelUtil {

	private static final String JOB_CONTENT = "jobContent.vm";

	private static Properties properties = new Properties();

	static {
		// 设置velocity资源加载方式为file
		properties.setProperty("resource.loader", "file");
		// 设置velocity资源加载方式为file时的处理类
		properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, "templates/");
	}

	/**
	 * 传入公司名和工作职责要求、工作内容描述，返回统一格式字符串
	 * 
	 * @param cpName
	 *            公司名
	 * @param rpList
	 *            职责描述
	 * @param cdList
	 *            工作要求描述
	 * @return
	 * @throws IOException
	 */
	public static String cstJobCd(String title, List<String> rpList, List<String> cdList) throws IOException {
		// 实例化一个VelocityEngine对象
		VelocityEngine velocityEngine = new VelocityEngine(properties);

		VelocityContext context = new VelocityContext();
		context.put("title", title);
		context.put("cdList", cdList);
		context.put("rpList", rpList);
		// 实例化一个StringWriter
		StringWriter writer = new StringWriter();
		velocityEngine.mergeTemplate(JOB_CONTENT, "utf-8", context, writer);
		String jobCon = writer.toString();
		/*
		 * flush and cleanup
		 */
		writer.flush();
		writer.close();
		jobCon = StringHelperUtil.removeBlankWord(jobCon);
		return jobCon;

	}

	public VelUtil(String templateFile) {
		try {
			// 初始化参数
			Properties properties = new Properties();
			// 设置velocity资源加载方式为file
			properties.setProperty("resource.loader", "file");
			// File userDir = new File("templates/");
			//
			// System.out.println("================================" +
			// userDir.getAbsolutePath());
			// 设置velocity资源加载方式为file时的处理类
			properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, "templates/");
			// 实例化一个VelocityEngine对象
			VelocityEngine velocityEngine = new VelocityEngine(properties);

			VelocityContext context = new VelocityContext();
			context.put("cdList", getNames());

			// 实例化一个StringWriter
			StringWriter writer = new StringWriter();
			velocityEngine.mergeTemplate(templateFile, "utf-8", context, writer);
			System.out.println(writer.toString());
			/*
			 * flush and cleanup
			 */

			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public ArrayList getNames() {
		ArrayList list = new ArrayList();

		list.add("ArrayList element 1");
		list.add("ArrayList element 2");
		list.add("ArrayList element 3");
		list.add("ArrayList element 4");

		return list;
	}

	public static void main(String[] args) {
		// VelUtil t = new
		// VelUtil("D:/workspace/spiderServiceUtil/spider/jobContent.vm");
		// VelUtil v = new VelUtil("example.vm");
		VelUtil v = new VelUtil("jobContent.vm");
	}
}
