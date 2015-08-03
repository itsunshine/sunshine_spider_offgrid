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
 * �ϴ��ļ���
 * 
 * @author liaobo
 * 
 */
public class SendFile {

	public static void main(String[] args) throws ClientProtocolException,
			IOException {
		uploadFile(
				"http://localhost:8989/sunshine04/process/spider/fileUpload",
				"D:/workspaceBoliao/spider/spider/20140221");
	}

	/**
	 * �ϴ��ļ��Ĺ��߷���
	 * 
	 * @param url
	 * @param filePath
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static boolean uploadFile(String url, String filePath)
			throws ClientProtocolException, IOException {

		HttpClient httpclient = new DefaultHttpClient();
		// ������ҳ��
		// HttpPost httppost = new
		// HttpPost("http://localhost:8989/sunshine04/process/spider/fileUpload");
		HttpPost httppost = new HttpPost(url);
		File uploadFile = new File(filePath);
		if (uploadFile.isDirectory())
			return false;
		// ������������ļ�
		FileBody file = new FileBody(uploadFile);
		// ����������ı��������ı�
		StringBody descript = new StringBody("IBM data");

		// ������ı���������
		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("file", file);
		reqEntity.addPart("descript", descript);
		// ��������
		httppost.setEntity(reqEntity);
		// ִ��
		HttpResponse response = httpclient.execute(httppost);
		// HttpEntity resEntity = response.getEntity();
		// System.out.println(response.getStatusLine());
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			HttpEntity entity = response.getEntity();
			// ��ʾ����
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
