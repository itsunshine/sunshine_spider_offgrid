/**
 * 
 */
package com.boliao.sunshine.seedsfilled;

/**
 * @author Liaobo
 * 
 */
public interface SeedsFillService {

	/**
	 * 根据传入的种子数组，进行种子数据补全
	 * 
	 * @param seeds
	 *            种子数组
	 */
	public String[] fillSeeds(String[] seeds);
}
