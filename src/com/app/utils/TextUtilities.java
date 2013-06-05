/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2013 NICTA Ltd.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.app.exception.InvalidArgumentException;

public class TextUtilities {
	public static enum CharType {
		NUMERIC, CHARACTER, SYMBOL, MATH_OPERATOR, BRACKET, COMPARTOR, SPACER, PUNCTUATION, MISC
	};

	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	private static final String SYMBOLS = "@#$%^&_`~|\\";
	private static final String MATH_OPERATORS = "+-*/";
	private static final String BRACKETS = "{}[]()";
	private static final String COMPARATORS = "><=";
	private static final String SPACER = " ";
	private static final String PUNCTUATION = "!?:\"'.,;";

	private static final String ESCAPE_CHARACTERS = "[\t\n\\x0B\f\r]";

	private static final Map<Character, CharType> characterSet = new TreeMap<Character, CharType>();

	static {
		for (char c : SYMBOLS.toCharArray()) {
			characterSet.put(c, CharType.SYMBOL);
		}
		for (char c : MATH_OPERATORS.toCharArray()) {
			characterSet.put(c, CharType.MATH_OPERATOR);
		}
		for (char c : BRACKETS.toCharArray()) {
			characterSet.put(c, CharType.BRACKET);
		}
		for (char c : COMPARATORS.toCharArray()) {
			characterSet.put(c, CharType.COMPARTOR);
		}
		for (char c : SPACER.toCharArray()) {
			characterSet.put(c, CharType.SPACER);
		}
		for (char c : PUNCTUATION.toCharArray()) {
			characterSet.put(c, CharType.PUNCTUATION);
		}
	}

	public static String getExceptionMessage(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		t.printStackTrace(ps);
		return baos.toString();
	}

	public static String generateHighLightedMessage(String msg) {
		if ("".equals(msg.trim())) return "";
		String[] messages = msg.split("\n");
		int maxLength = 0;
		for (int i = 0; i < messages.length; i++) {
			messages[i] = messages[i].replaceAll("\t", "    ").trim();
			if (messages[i].length() > maxLength) maxLength = messages[i].length();
		}
		String border = repeatStringPatternWithLength("*", maxLength + 4);
		StringBuilder sb = new StringBuilder(border);
		for (int i = 0; i < messages.length; i++) {
			sb.append(LINE_SEPARATOR).append("* ");
			sb.append(messages[i]).append(repeatStringPatternWithLength(" ", maxLength - messages[i].length()));
			sb.append(" *");
		}
		sb.append(LINE_SEPARATOR).append(border);
		return sb.toString();
	}

	public static String repeatStringPatternWithLength(String pattern, int length) {
		if (length <= 0) return "";
		StringBuilder sb = new StringBuilder();
		int l = pattern.length();
		int t = length / l + 1;
		for (int i = 0; i < t; i++)
			sb.append(pattern);
		sb.append(pattern);
		return sb.toString().substring(0, length);
	}

	public static String repeatStringPattern(String pattern, int noOfTimes) {
		if (noOfTimes < 1) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < noOfTimes; i++) {
			sb.append(pattern);
		}
		return sb.toString();
	}

	private static String trimLineTextWithWidth(String text, int width) {
		String tText = text.trim();
		if ("".equals(tText)) return "";
		if (text.length() <= width) return text;
		StringBuilder sb = new StringBuilder();
		int currLoc = 0, nextLoc = 0;

		do {
			currLoc = 0;
			nextLoc = tText.lastIndexOf(" ", width);
			if (nextLoc < currLoc) {
				nextLoc = tText.indexOf(" ", currLoc);
				if (nextLoc < 0) nextLoc = tText.length();
			}

			sb.append(tText.substring(currLoc, nextLoc));
			tText = (tText.length() > nextLoc + 1) ? tText.substring(nextLoc + 1).trim() : "";
			if (tText.length() > 0) {
				sb.append(LINE_SEPARATOR);
				if (tText.length() < width + 1) {
					sb.append(tText);
					tText = "";
				}
			}
		} while (tText.length() > 0);
		return sb.toString();
	}

	public static String trimTextWithWidth(String text, int width) {
		if (null == text || "".equals(text.trim()) || width <= 0) return "";
		StringBuilder sb = new StringBuilder();
		String[] textList = text.replace("\r", "").split("\n");
		int c = 0;
		for (String t : textList) {
			String trimedText = trimLineTextWithWidth(t, width);
			if (!"".equals(trimedText)) {
				if (c++ > 0) sb.append(LINE_SEPARATOR);
				sb.append(trimedText);
			}
		}
		return sb.toString();
	}

	public static String generateColumnText(final String[] header, final String[][] text, //
			final int[] columnWidth, final int[] sep, //
			final String horizontalSep, final String verticalSep) {
		if (header.length > columnWidth.length + 1) throw new IllegalArgumentException("incorrect no. of column width");
		if (header.length > sep.length + 1) throw new IllegalArgumentException("incorrect no. of width");
		if (text.length == 0) return "";
		for (int i = 0; i < text.length; i++) {
			if (header.length > text[i].length) throw new IllegalArgumentException("incorrect no. of width at record [" + i + "]");
		}

		StringBuilder sb = new StringBuilder(), sbT = new StringBuilder();
		int[] colWidth = new int[header.length];

		List<List<String>> textList = new ArrayList<List<String>>();

		// calculate column width
		for (int i = 0; i < columnWidth.length; i++)
			colWidth[i] = 0;
		for (int i = 0; i < header.length; i++) {
			if (columnWidth[i] == 0) {
				for (int j = 0; i < text.length; j++) {
					text[j][i] = text[j][i].trim();
					if (text[j][i].length() > colWidth[i]) colWidth[i] = text[j][i].length();
				}
			} else {
				colWidth[i] = columnWidth[i];
			}
		}
		for (int i = 0; i < text.length; i++) {
			String[][] t = new String[header.length][];
			int maxLen = 0;
			List<List<String>> tempTextList = null;
			boolean widthChanged = false;
			do {
				widthChanged = false;
				tempTextList = new ArrayList<List<String>>();
				for (int j = 0; j < header.length; j++) {
					t[j] = trimTextWithWidth(text[i][j], colWidth[j]).split(LINE_SEPARATOR);
					if (t[j].length > maxLen) maxLen = t[j].length;
				}
				for (int j = 0; j < maxLen; j++) {
					List<String> tList = new ArrayList<String>();
					for (int k = 0; k < header.length; k++) {
						if (t[k].length > j) {
							tList.add(t[k][j]);
							if (t[k][j].length() > colWidth[k]) {
								colWidth[k] = t[k][j].length();
								widthChanged = true;
							}
						} else {
							tList.add("");
						}
					}
					if (widthChanged) break;
					else tempTextList.add(tList);
				}
			} while (widthChanged);
			textList.addAll(tempTextList);
		}

		// calculate column width required
		String[] hSep = new String[header.length];
		String[] vSep = new String[header.length];
		if ("".equals(horizontalSep)) {
			for (int i = 0; i < header.length - 1; i++) {
				hSep[i] = repeatStringPatternWithLength(" ", sep[i]);
			}
		} else {
			for (int i = 0; i < header.length - 1; i++) {
				int c = sep[i] / 2;
				hSep[i] = repeatStringPatternWithLength(" ", c) + horizontalSep + repeatStringPatternWithLength(" ", sep[i] - c - horizontalSep.length());
				System.out.println("h[" + i + "]=" + hSep[i]);
			}
		}
		if ("".equals(verticalSep)) {
			for (int i = 0; i < header.length; i++) {
				vSep[i] = "";
			}
		} else {
			for (int i = 0; i < header.length - 1; i++) {
				vSep[i] = repeatStringPatternWithLength(verticalSep, columnWidth[i]) + horizontalSep;
			}
		}

		boolean hasHeaderContent = false;
		for (int i = 0; i < header.length; i++) {
			String t = header[i].trim();
			if (t.length() > 0) hasHeaderContent = true;
			sb.append(t).append(repeatStringPatternWithLength(" ", colWidth[i] - t.length()));
			sbT.append(repeatStringPatternWithLength("-", t.length())).append(repeatStringPatternWithLength(" ", colWidth[i] - t.length()));
			if (i < header.length - 1) {
				sb.append(hSep[i]);
				sbT.append(hSep[i]);
			}
		}
		if (hasHeaderContent) sb.append(LINE_SEPARATOR).append(sbT.toString());
		else sb = new StringBuilder();

		for (List<String> rowText : textList) {
			if (sb.length() > 0) sb.append(LINE_SEPARATOR);
			for (int i = 0; i < rowText.size(); i++) {
				String t = rowText.get(i);
				sb.append(t).append(repeatStringPatternWithLength(" ", colWidth[i] - t.length()));
				if (i < header.length - 1) sb.append(hSep[i]);
			}
		}

		return sb.toString();
	}

	public static List<String> trimTextArray(String args, String sep) {
		List<String> v = new ArrayList<String>();
		if (null == args || "".equals(args.trim())) return v;

		return trimTextArray(args.split(sep));
	}

	public static List<String> trimTextArray(String[] args) {
		List<String> v = new ArrayList<String>();
		if (null == args) return v;
		for (int i = 0; i < args.length; i++) {
			String s = args[i].trim();
			if (null != args[i] && !"".equals(s)) v.add(s);
		}
		return v;
	}

	public static List<NameValuePair<CharType, String>> extractStringTokens(final String str, boolean removeWhiteSpace) {
		List<NameValuePair<CharType, String>> tokens = new ArrayList<NameValuePair<CharType, String>>();

		if (null == str || "".equals(str.trim())) return tokens;
		String s = (removeWhiteSpace) ? str.replaceAll("\\s", "") : str.replaceAll(ESCAPE_CHARACTERS, "").trim();
		if ("".equals(s)) return tokens;

		CharType charType = null;
		CharType lastCharType = null;
		String token = "";

		for (int i = 0; i < s.length(); i++) {
			lastCharType = charType;

			char c = s.charAt(i);

			charType = characterSet.get(c);
			if (null == charType) {
				if (c >= '0' && c <= '9') {
					charType = CharType.NUMERIC;
				} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
					charType = CharType.CHARACTER;
				} else {
					charType = CharType.MISC;
				}
			}

			if (null == lastCharType) lastCharType = charType;

			if (charType == lastCharType) {
				token += c;
			} else {
				tokens.add(new NameValuePair<CharType, String>(lastCharType, token));
				token = "" + c;
			}
		}

		if (!"".equals(token)) tokens.add(new NameValuePair<CharType, String>(charType, token));

		return tokens;
	}

	public static String formatArguments(String str, Object[] args) throws InvalidArgumentException {
		return formatArguments(str, args.length, args);
	}

	public static String formatArguments(String str, int noOfArguments, Object[] args) throws InvalidArgumentException {
		if (null == args) throw new InvalidArgumentException("invalid argument found, argument=" + args);
		String newStr = str;
		for (int i = 0; i < args.length; i++) {
			String arg = (null == args[i]) ? "" : args[i].toString().trim();
			newStr = newStr.replaceAll("\\{" + i + "\\}", arg);
		}
		if (noOfArguments > args.length) {
			for (int i = args.length; i < noOfArguments; i++) {
				newStr = newStr.replaceAll("\\{" + i + "\\}", "");
			}
		}
		return newStr;
	}

}
