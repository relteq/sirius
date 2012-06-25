package com.relteq.sirius.db.util;

public class UUID {
	/**
	 * Generates a universally unique identifier
	 * @return the generated UUID
	 */
	public static String generate() {
		return java.util.UUID.randomUUID().toString();
	}
}
