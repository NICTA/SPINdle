/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2012 NICTA Ltd.
 *
 * This file is part of SPINdle project.
 * 
 * SPINdle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SPINdle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPINdle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory 
 */
package com.app.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.app.exception.InvalidArgumentException;
import com.app.utils.TextUtilities.CharType;

public class Converter {
	private static DecimalFormat fileSizeFormatter = new DecimalFormat("###,###,##0.00");

	private static final Map<String, Integer> TIME_VALUES = new LinkedHashMap<String, Integer>();
	private static final List<String> TIME_VALUES_KEY = new ArrayList<String>();

	private static final String INVALID_STRING = "invalid string [{0}]";

	static {
		TIME_VALUES.put("y", 1);
		TIME_VALUES.put("d", 365);
		TIME_VALUES.put("h", 24);
		TIME_VALUES.put("m", 60);
		TIME_VALUES.put("s", 60);
		TIME_VALUES.put("ms", 1000);

		TIME_VALUES_KEY.addAll(TIME_VALUES.keySet());
	}

	/**
	 * Convert the file size of a file from long to a string.
	 * 
	 * @param filesize file size as long.
	 * @return <code>String</code> file size as string.
	 */
	public static String long2FileSize(final long filesize) {
		if (filesize <= 0) return "-";
		double d = 1.0 * filesize / 1024;
		if (d < 512) return fileSizeFormatter.format(d) + " KB";
		d /= 1024;
		return fileSizeFormatter.format(d) + " MB";
	}

	/**
	 * Convert a duration value from long to a string.
	 * 
	 * @param time duration as long.
	 * @return <code>String</code> duration as string.
	 */
	public static String long2TimeString(final long time) {
		long ms = time % 1000;
		long sec = (time / 1000);
		long min = (sec / 60);
		sec = sec % 60;
		long hr = min / 60;
		min = min % 60;

		StringBuilder sbTime = new StringBuilder();
		if (hr > 0) sbTime.append(hr).append(" hr ");
		if (min == 0) {
			if (sbTime.length() > 0) sbTime.append("0 min ");
		} else {
			sbTime.append(min).append(" min ");
		}
		DecimalFormat nf = null;
		if (sec == 0 && sbTime.length() == 0) {
			nf = new DecimalFormat("##0 ms");
			sbTime.append(nf.format(ms));
		} else {
			nf = new DecimalFormat("0.000 sec");
			sbTime.append(nf.format(sec + 1.0 * ms / 1000));
		}
		return sbTime.toString();
	}

	public static String long2TimeString2(final long time) throws InvalidArgumentException {
		long[] v = new long[TIME_VALUES.size()];
		long t = time;
		for (int i = TIME_VALUES_KEY.size() - 1; i >= 0; i--) {
			String key = TIME_VALUES_KEY.get(i);
			int value = TIME_VALUES.get(key);
			v[i] = t % value;
			t /= value;
		}
		v[0] = t;

		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (java.util.Map.Entry<String, Integer> entry : TIME_VALUES.entrySet()) {
			if (v[i] > 0) sb.append(v[i]).append(entry.getKey());
			i++;
		}

		assert timeString2long(sb.toString()) == time;

		return sb.toString();
	}

	public static long timeString2long(final String timeStr) throws InvalidArgumentException {
		List<NameValuePair<CharType, String>> tokens = TextUtilities.extractStringTokens(timeStr, true);
		if (tokens.size() % 2 != 0 || tokens.size() > TIME_VALUES.size() * 2) throw new IllegalArgumentException(TextUtilities.formatArguments(INVALID_STRING,
				1, new String[] { timeStr }));

		Map<String, Integer> values = new HashMap<String, Integer>();
		try {
			for (int i = 0; i < tokens.size(); i += 2) {
				if (tokens.get(i).getKey() != CharType.NUMERIC) throw new IllegalArgumentException(TextUtilities.formatArguments(INVALID_STRING, 1,
						new String[] { timeStr }));
				NameValuePair<CharType, String> entry = tokens.get(i + 1);
				if (entry.getKey() != CharType.CHARACTER || !TIME_VALUES.containsKey(entry.getValue())) throw new IllegalArgumentException(
						TextUtilities.formatArguments(INVALID_STRING, 1, new String[] { timeStr }));
				if (values.containsKey(entry.getValue())) throw new IllegalArgumentException(TextUtilities.formatArguments(INVALID_STRING, 1,
						new String[] { timeStr }));
				values.put(entry.getValue(), Integer.parseInt(tokens.get(i).getValue()));
			}

			long time = 0;
			for (java.util.Map.Entry<String, Integer> entry : TIME_VALUES.entrySet()) {
				int v = (values.containsKey(entry.getKey())) ? values.get(entry.getKey()) : 0;
				time = (time * entry.getValue() + v);
			}

			return time;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Indicates whether the input string is a time string following the "yymmddhhmmss" pattern.
	 * 
	 * @param timeStr time string to verify.
	 * @return true if the input string is a patterned time string; <br/>
	 *         false otherwise.
	 * @throws InvalidArgumentException throws when the input string is not a valid time string.
	 * @see #long2TimeString2(long)
	 * @see #timeString2long(String)
	 */
	public static boolean isTimeString(final String timeStr) throws InvalidArgumentException {
		List<NameValuePair<CharType, String>> tokens = TextUtilities.extractStringTokens(timeStr, true);
		if (tokens.size() % 2 != 0 || tokens.size() > TIME_VALUES.size() * 2) throw new IllegalArgumentException(TextUtilities.formatArguments(INVALID_STRING,
				1, new String[] { timeStr }));

		for (int i = 0; i < tokens.size(); i += 2) {
			if (tokens.get(i).getKey() != CharType.NUMERIC) throw new InvalidArgumentException(TextUtilities.formatArguments(INVALID_STRING, 1,
					new String[] { timeStr }));

			NameValuePair<CharType, String> entry = tokens.get(i + 1);
			if (entry.getKey() != CharType.CHARACTER || !TIME_VALUES.containsKey(entry.getValue())) throw new IllegalArgumentException(
					TextUtilities.formatArguments(INVALID_STRING, 1, new String[] { timeStr }));
		}

		return true;
	}

}
