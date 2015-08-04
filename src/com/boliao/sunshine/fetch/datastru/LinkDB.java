package com.boliao.sunshine.fetch.datastru;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

/**
 * ���������Ѿ����ʹ� Url �ʹ����ʵ� Url ����
 */
public class LinkDB {

	// �ѷ��ʵ� url ����
	private static BlockingQueue<String> visitedUrl = new ArrayBlockingQueue<String>(1000);
	// �����ʵ� url ����
	private static BlockingQueue<String> unVisitedUrl = new ArrayBlockingQueue<String>(300);

	public static BlockingQueue<String> getUnVisitedUrl() {
		return unVisitedUrl;
	}

	public static void addVisitedUrl(String url) {
		visitedUrl.add(url);
	}

	public static void removeVisitedUrl(String url) {
		visitedUrl.remove(url);
	}

	public static String unVisitedUrlDeQueue() {
		try {
			return unVisitedUrl.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ��֤ÿ�� url ֻ������һ��
	public static void addUnvisitedUrl(String url) {
		if (StringUtils.isNotBlank(url) && !visitedUrl.contains(url) && !unVisitedUrl.contains(url)) {
			unVisitedUrl.add(url);
		}

	}

	public static int getVisitedUrlNum() {
		return visitedUrl.size();
	}

	// ����δ���������Ƿ�Ϊ�գ�����δ��������Ĵ�С���ж�
	public static boolean unVisitedUrlsEmpty() {
		return (unVisitedUrl.size() == 0);
	}

	public static void clean() {
		visitedUrl.clear();
		unVisitedUrl.clear();
	}
}