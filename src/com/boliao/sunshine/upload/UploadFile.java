package com.boliao.sunshine.upload;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

/**
 * 
 * @author liaobo.lb
 *
 */
public class UploadFile {

    public void uploadFile(File file, String url) {
        if (!file.exists()) {
            return;
        }
        PostMethod postMethod = new PostMethod(url);
        try {
            //FilePart�������ϴ��ļ�����
        FilePart fp = new FilePart("filedata", file);
            Part[] parts = { fp };

            //����MIME���͵�����httpclient����ȫ��MulitPartRequestEntity���а�װ
            MultipartRequestEntity mre = new MultipartRequestEntity(parts, postMethod.getParams());
            postMethod.setRequestEntity(mre);
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(50000);// ��������ʱ��
            int status = client.executeMethod(postMethod);
            if (status == HttpStatus.SC_OK) {
                System.out.println(postMethod.getResponseBodyAsString());
            } else {
                System.out.println("fail");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //�ͷ�����
            postMethod.releaseConnection();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        UploadFile test = new UploadFile();
        test.uploadFile(new File("D:\\workspaceBoliao\\spider\\spider\\20140221"),
            "http://localhost:8989/sunshine04/process/spider/fileUpload");
    }

}

