package com.boliao.sunshine.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ���ַ�����ת��
 * @author liaobo.lb
 *
 */
public class StringUtil {
	/**
	 * ������ת����utf8����
	 * @param link
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String transformUrl(String link) throws UnsupportedEncodingException{
		Pattern pattern = Pattern.compile("[\u0391-\uFFE5]+");
		Matcher match = pattern.matcher(link);
		StringBuffer sb = new StringBuffer();
		while(match.find()){
			String str = match.group();
			str = URLEncoder.encode(str, "utf-8");
			match.appendReplacement(sb, str);
		}
		match.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * ��ȡ�ַ�����ժҪ
	 * @param data
	 * @return
	 */
	public static String generateMD5Str(String data){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			return String.valueOf(md.digest(data.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
