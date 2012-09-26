package com.relteq.sirius.util;

import java.math.BigDecimal;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class Data1D {
	private BigDecimal[] data = null;

	private static Logger logger = Logger.getLogger(Data1D.class);

	/**
	 * @param str a serialized vector
	 * @param delim a delimiter
	 */
	public Data1D(String str, String delim) {
		if (null == str) logger.error("str == null");
		else if (null == delim) logger.error("delim == null");
		else {
			StringTokenizer st = new StringTokenizer(str, delim);
			data = new BigDecimal[st.countTokens()];
			for (int i = 0; st.hasMoreTokens(); ++i)
				data[i] = new BigDecimal(st.nextToken());
		}
	}

	public boolean isEmpty() {
		return null == data;
	}

	public BigDecimal[] getData() {
		return data;
	}
}
