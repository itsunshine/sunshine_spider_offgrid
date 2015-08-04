package com.boliao.sunshine.utils;

import org.apache.commons.lang.StringUtils;

import com.boliao.sunshine.config.ConfigService;
import com.boliao.sunshine.constants.CommonConstants;
import com.boliao.sunshine.fetch.filters.LinkFilter;

/**
 * ץȡ��ҳ�ĸ���������
 * 
 * @author Liaobo
 * 
 */
public class SpideContentUtil {

	/**
	 * ȡ����Ӧ��վ����ҳ�������
	 * 
	 * @param htmlContent
	 * @return
	 */
	public static String getContent(String htmlContent, String prefix, String suffix, String site, ConfigService configService) {
		String contentStr = null;
		String[] conStart = configService.getFlagString(site + prefix, null).split(CommonConstants.SEPARATOR);
		String[] conEnd = configService.getFlagString(site + suffix, null).split(CommonConstants.SEPARATOR);

		for (int i = 0; i < conStart.length; i++) {
			String conBegin = conStart[i].substring(0, conStart[i].length() - 1);
			String conLast = conEnd[i].substring(0, conEnd[i].length() - 1);
			int start = htmlContent.indexOf(conBegin);
			int end = htmlContent.indexOf(conLast);
			if (isInclude(conStart[i]) && isInclude(conEnd[i])) {
				contentStr = StringUtils.substring(htmlContent, start, (end + conLast.length()));
			} else if (!isInclude(conStart[i]) && isInclude(conEnd[i])) {
				contentStr = StringUtils.substring(htmlContent, start + conBegin.length(), (end + conLast.length()));
			} else if (isInclude(conStart[i]) && !isInclude(conEnd[i])) {
				contentStr = StringUtils.substring(htmlContent, start, end);
			} else {
				contentStr = StringUtils.substringBetween(htmlContent, conBegin, conLast);
			}
		}
		return contentStr;
	}

	/**
	 * �ж��Ƿ�Ӧ�ð����ý�ȡ�ı�ʾ�ַ���
	 * 
	 * @param paramStr
	 * @return
	 */
	private static boolean isInclude(String paramStr) {
		if (paramStr.endsWith("+") || !paramStr.endsWith("-")) {
			return true;
		}
		return false;
	}

	/**
	 * ����filter
	 * 
	 * @param htmlContent
	 * @param suffix
	 * @param configService
	 * @return
	 */
	public static LinkFilter getFilter(final String suffix, final ConfigService configService, final String site) {
		LinkFilter filter = null;
		final String[] urlStrs = configService.getFlagString(site + suffix, null).split(CommonConstants.SEPARATOR);
		filter = new LinkFilter() {
			// ��ȡ�� http://www.twt.edu.cn ��ͷ������
			public boolean accept(String url) {
				for (String u : urlStrs) {
					if (url.startsWith(u)) {
						return true;
					}
				}
				return false;
			}
		};

		return filter;
	}
}
