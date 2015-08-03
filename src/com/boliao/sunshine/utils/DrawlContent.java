package com.boliao.sunshine.utils;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TitleTag;

import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.filters.LinkFilter;

public class DrawlContent {

	/**
	 * 取出相应网站导航页面的内容
	 * 
	 * @param htmlContent
	 * @return
	 */
	public static String getContent(String htmlContent, String start,
			String end, ConfigService configService) {
		TitleTag title = ParseUtils.parseTag(htmlContent, TitleTag.class);
		String titleName = title.getStringText();
		Set<String> webSites = configService.getWebSit();
		String contentStr = null;
		for (String site : webSites) {
			if (titleName.contains(site)) {
				String[] conStart = configService.getString(site + start, null)
						.split(CommonConstants.SEPARATOR);
				String[] conEnd = configService.getString(site + end, null)
						.split(CommonConstants.SEPARATOR);

				for (int i = 0; i < conStart.length; i++) {
					contentStr = StringUtils.substringBetween(htmlContent,
							conStart[i], conEnd[i]);
				}
				return contentStr;
			}
		}
		return contentStr;
	}

	/**
	 * 创建filter
	 * 
	 * @param htmlContent
	 * @param start
	 * @param configService
	 * @return
	 */
	public static LinkFilter getFilter(final String htmlContent,
			final String start, final ConfigService configService) {
		TitleTag title = ParseUtils.parseTag(htmlContent, TitleTag.class);
		String titleName = title.getStringText();
		Set<String> webSites = configService.getWebSit();
		LinkFilter filter = null;
		for (String site : webSites) {
			if (titleName.contains(site)) {
				final String[] urlStrs = configService.getString(site + start,
						null).split(CommonConstants.SEPARATOR);
				filter = new LinkFilter() {
					// 提取以 http://www.twt.edu.cn 开头的链接
					public boolean accept(String url) {
						for (String u : urlStrs) {
							if (url.startsWith(u)) {
								return true;
							}
						}
						return false;
					}
				};
			}
		}

		return filter;
	}
}
