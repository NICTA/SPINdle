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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class CsvFileReader {
	private static final String DELIMITER = ",";
	private LineNumberReader rdr = null;

	public CsvFileReader(File filename) throws IOException {
		this(new FileInputStream(filename));
	}

	public CsvFileReader(InputStream in) throws IOException {
		try {
			rdr = new LineNumberReader(new InputStreamReader(in));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public int getLineNumber() throws IOException {
		if (null == rdr) throw new IOException("read is null");
		return rdr.getLineNumber();
	}

	public List<String> getLine() throws IOException {
		if (rdr == null) throw new IOException("filename is null");

		List<String> data = null;
		String theLine = null;

		do {
			theLine = rdr.readLine();
			if (theLine != null) {
				theLine = theLine.trim();
				if (theLine.startsWith("#")) {
					theLine = "";
				}
			}
		} while ("".equals(theLine));

		if (null == theLine || "".equals(theLine)) return null;

		int currLoc = 0;
		int l1 = -1;
		String token = null;
		data = new ArrayList<String>();
		try {
			do {
				currLoc = 0;
				String sep = DELIMITER;
				if (theLine.startsWith("\"")) {
					sep = "\"";
				} else if (theLine.startsWith("'")) {
					sep = "'";
				}
				if (!DELIMITER.equals(sep)) theLine = theLine.substring(1);

				l1 = theLine.indexOf(sep, currLoc);
				if (!DELIMITER.equals(sep)) l1 = theLine.indexOf(DELIMITER, l1 + 1);

				if (l1 < 0) l1 = theLine.length();

				token = theLine.substring(currLoc, l1).trim();

				if (sep.equals(DELIMITER)) {
					data.add(token);
				} else {
					int el = token.endsWith(sep) ? 1 : 0;
					data.add(token.substring(currLoc, l1 - el));

				}
				if (l1 < theLine.length()) theLine = theLine.substring(l1 + 1).trim();
				else theLine = "";
			} while (theLine.length() > 0);
		} catch (Exception e) {
			throw new IOException("Exception throw in line " + rdr.getLineNumber(), e);
		}
		return data;
	}
}
