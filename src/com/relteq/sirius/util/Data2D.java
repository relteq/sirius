package com.relteq.sirius.util;

import java.math.BigDecimal;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class Data2D {
	private BigDecimal[][] data = null;

	private static Logger logger = Logger.getLogger(Data2D.class);

	/**
	 * @param str a serialized matrix
	 * @param delim delimiters
	 */
	public Data2D(String str, String[] delim) {
		if (null == delim) logger.error("no delimiters");
		else if (2 != delim.length) logger.error("delim.length != 2");
		else if (null == str) logger.error("str == null");
		else {
			str = str.replaceAll("\\s", "");
			StringTokenizer st1 = new StringTokenizer(str, delim[0]);
			data = new BigDecimal[st1.countTokens()][];
			for (int i = 0; st1.hasMoreTokens(); ++i) {
				StringTokenizer st2 = new StringTokenizer(st1.nextToken(), delim[1]);
				data[i] = new BigDecimal[st2.countTokens()];
				for (int j = 0; st2.hasMoreTokens(); ++j)
					data[i][j] = new BigDecimal(st2.nextToken());
			}
		}
	}

	public boolean isEmpty() {
		return null == data;
	}

	public BigDecimal[][] getData() {
		return data;
	}
}
