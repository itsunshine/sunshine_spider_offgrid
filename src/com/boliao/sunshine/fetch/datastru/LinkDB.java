package com.boliao.sunshine.fetch.datastru;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

/**
 * 用来保存已经访问过 Url 和待访问的 Url 的类
 */
public class LinkDB {

	// 已访问的 url 集合
	private static BlockingQueue<String> visitedUrl = new ArrayBlockingQueue<String>(1000);
	// 待访问的 url 集合
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

	// 保证每个 url 只被访问一次
	public static void addUnvisitedUrl(String url) {
		if (StringUtils.isNotBlank(url) && !visitedUrl.contains(url) && !unVisitedUrl.contains(url)) {
			unVisitedUrl.add(url);
		}

	}

	public static int getVisitedUrlNum() {
		return visitedUrl.size();
	}

	// 返回未访问链表是否为空，根据未访问链表的大小来判断
	public static boolean unVisitedUrlsEmpty() {
		return (unVisitedUrl.size() == 0);
	}

	public static void clean() {
		visitedUrl.clear();
		unVisitedUrl.clear();
	}
}