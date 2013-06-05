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

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	private static final String INFO_TEXT = "===";
	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	@Override
	public String format(LogRecord record) {
		return LINE_SEPARATOR + formatMessage(record);
	}

	// This method is called just after the handler using this
	// formatter is created
	public String getHead(Handler h) {
		return getBlock("Logging started at " + ((new Date())).toString());
	}

	// This method is called just after the handler using this
	// formatter is closed
	public String getTail(Handler h) {
		return LINE_SEPARATOR + getBlock("Logging ended at " + ((new Date())).toString()) //
				+ LINE_SEPARATOR + LINE_SEPARATOR;
	}

	private String getBlock(String text) {
		int l = text.length();

		StringBuilder sbBar = new StringBuilder();
		for (int i = 0; i < l; i++)
			sbBar.append("=");

		StringBuilder sb = new StringBuilder();
		sb.append(INFO_TEXT).append(" ").append(sbBar.toString()).append(" ").append(INFO_TEXT);
		sb.append(LINE_SEPARATOR);
		sb.append(INFO_TEXT).append(" ").append(text).append(" ").append(INFO_TEXT);
		sb.append(LINE_SEPARATOR);
		sb.append(INFO_TEXT).append(" ").append(sbBar.toString()).append(" ").append(INFO_TEXT);
		return sb.toString();
	}
}
