/**
 * 
 */
package com.boliao.sunshine.test;

import java.net.URL;

/**
 * @author Liaobo
 * 
 */
public class TestMain {

	public static void main(String[] args) throws Exception {
		String requestUrl = "http://hr.tencent.com/position.php?keywords=&tid=87&lid=2218";
		URL url = new URL(requestUrl);
		String host = url.getHost();
		System.out.println(host);
	}

}
