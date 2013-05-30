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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;

import com.app.exception.InvalidArgumentException;

public class Utilities {

	private static final String CHARACTORS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private static Random RANDOM_GENERATOR = new Random(System.currentTimeMillis());

	public static enum ProcessStatus {
		SUCCESS, FAILED;
	}

	public static String getRandomString(final int len) {
		StringBuilder sb = new StringBuilder();
		int max = CHARACTORS.length();
		for (int i = 0; i < len; i++) {
			sb.append(CHARACTORS.charAt(RANDOM_GENERATOR.nextInt(max)));
		}
		return sb.toString();
	}

	public static int getRandomInt(final int max) {
		int v = RANDOM_GENERATOR.nextInt(max);
		return (v < 0) ? v + max : v;
	}

	public static long getRandomLong(final long max) {
		long v = RANDOM_GENERATOR.nextLong() % max;
		return (v < 0) ? v + max : v;
	}

	public static Level getLogLevel(final String logLevelStr) {
		Level logLevel = null;
		try {
			logLevel = Level.parse(logLevelStr.toUpperCase());
		} catch (Exception e) {
			logLevel = null;
		}
		return logLevel;
	}

	public static URL getUrl(String urlStr) throws URISyntaxException, IOException {
		URI uri = new URI(urlStr.trim());
		if (null == uri.getScheme()) {
			File f = new File(urlStr);
			return new URL("file", "", f.getCanonicalPath());
		} else {
			return uri.toURL();
		}
	}

	public static <T> T getInstance(String classname, Class<T> clazz) throws ClassNotFoundException, ClassCastException,
			InstantiationException, IllegalAccessException {
		if (null == classname || "".equals(classname.trim())) throw new IllegalArgumentException("classname is null");
		Class<?> cl = Class.forName(classname.trim());
		Class<? extends T> c = cl.asSubclass(clazz);
		return c.newInstance();
	}

	public static void extractArguments(String[] args, String argumentPrefix, Map<String, String> _arguments, List<String> _nonArguments)
			throws InvalidArgumentException {
		if (null == args || args.length == 0) return;

		Map<String, String> arguments = new TreeMap<String, String>();
		List<String> nonArguments = new ArrayList<String>();
		int prefLen = argumentPrefix.length();
		for (String arg : args) {
			String s = arg.trim();
			if (s.startsWith(argumentPrefix)) {
				s = s.substring(prefLen).trim();
				if (s.length() > 0) {
					int l = s.indexOf("=");
					String name = "";
					String value = "";
					if (l > 0) {
						name = s.substring(0, l);
						value = s.substring(l + 1);
					} else if (l == 0) {
						throw new InvalidArgumentException("invalid argument [" + arg.trim() + "] - argument name not found!");
					} else {
						name = s;
					}
					arguments.put(name, value);
				}
			} else {
				nonArguments.add(s);
			}
		}

		if (null != _arguments) {
			_arguments.clear();
			_arguments.putAll(arguments);
		}
		if (null != _nonArguments) {
			_nonArguments.clear();
			_nonArguments.addAll(nonArguments);
		}
	}
}
