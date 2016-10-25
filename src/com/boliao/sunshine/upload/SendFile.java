package com.boliao.sunshine.upload;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * 上传文件类
 * 
 * @author liaobo
 * 
 */
public class SendFile {

	public static void main(String[] args) throws ClientProtocolException, IOException {
		// uploadFile("http://www.itsunshine.net:8080/views/upload.do",
		// "D:\\workspace\\sunshine\\corp.jobCorp");
		// uploadFile("http://www.itsunshine.net/views/upload.do",
		// "D:\\workspace\\sunshine\\upload.docs");
		// uploadFile("http://localhost:8989/sunshine_new/views/upload.do",
		// "D:\\workspace\\sunshine\\upload.ebooks");
		uploadFile("http://www.itsunshine.net/views/upload.do", "/Users/liaobo/workspace_j2ee/sunshine/upload.ebooks");
		// uploadFile("http://www.itsunshine.net/views/upload.do",
		// "/Users/liaobo/zip/data/spider/jobDemandArt/20160602_jobDemandArt");
	}

	/**
	 * 上传文件的工具方法
	 * 
	 * @param url
	 * @param filePath
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static boolean uploadFile(String url, String filePath) throws ClientProtocolException, IOException {

		HttpClient httpclient = new DefaultHttpClient();
		// 请求处理页面
		// HttpPost httppost = new
		// HttpPost("http://localhost:8989/sunshine04/process/spider/fileUpload");
		HttpPost httppost = new HttpPost(url);
		File uploadFile = new File(filePath);
		if (uploadFile.isDirectory())
			return false;
		// 创建待处理的文件
		FileBody file = new FileBody(uploadFile);
		// 创建待处理的表单域内容文本
		StringBody descript = new StringBody("IBM data");

		// 对请求的表单域进行填充
		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("file", file);
		reqEntity.addPart("descript", descript);
		// 设置请求
		httppost.setEntity(reqEntity);
		// 执行
		HttpResponse response = httpclient.execute(httppost);
		// HttpEntity resEntity = response.getEntity();
		// System.out.println(response.getStatusLine());
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			HttpEntity entity = response.getEntity();
			// 显示内容
			if (entity != null) {
				System.out.println(EntityUtils.toString(entity));
			}
			if (entity != null) {
				entity.consumeContent();
			}
			return true;
		}
		return false;

	}

}
