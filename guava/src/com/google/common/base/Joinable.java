package com.google.common.base;

public interface Joinable {

	/**
	   * Returns a string containing the string representation of each of {@code parts}, using the
	   * previously configured separator between each.
	   */
	String join(Object[] parts);

}