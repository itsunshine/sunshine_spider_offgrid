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
		// ����velocity��Դ���ط�ʽΪfile
		properties.setProperty("resource.loader", "file");
		// ����velocity��Դ���ط�ʽΪfileʱ�Ĵ�����
		properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, "templates/");
	}

	/**
	 * ���빫˾���͹���ְ��Ҫ�󡢹�����������������ͳһ��ʽ�ַ���
	 * 
	 * @param cpName
	 *            ��˾��
	 * @param rpList
	 *            ְ������
	 * @param cdList
	 *            ����Ҫ������
	 * @return
	 * @throws IOException
	 */
	public static String cstJobCd(String title, List<String> rpList, List<String> cdList) throws IOException {
		// ʵ����һ��VelocityEngine����
		VelocityEngine velocityEngine = new VelocityEngine(properties);

		VelocityContext context = new VelocityContext();
		context.put("title", title);
		context.put("cdList", cdList);
		context.put("rpList", rpList);
		// ʵ����һ��StringWriter
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
			// ��ʼ������
			Properties properties = new Properties();
			// ����velocity��Դ���ط�ʽΪfile
			properties.setProperty("resource.loader", "file");
			// File userDir = new File("templates/");
			//
			// System.out.println("================================" +
			// userDir.getAbsolutePath());
			// ����velocity��Դ���ط�ʽΪfileʱ�Ĵ�����
			properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, "templates/");
			// ʵ����һ��VelocityEngine����
			VelocityEngine velocityEngine = new VelocityEngine(properties);

			VelocityContext context = new VelocityContext();
			context.put("cdList", getNames());

			// ʵ����һ��StringWriter
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
