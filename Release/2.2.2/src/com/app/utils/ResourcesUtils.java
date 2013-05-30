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

import java.io.IOException;
import java.io.InputStream;


public class ResourcesUtils {
	

	public static InputStream getResourceAsStream(final String resourceName) {
		Class<ResourcesUtils> clazz = ResourcesUtils.class;
		InputStream is = clazz.getResourceAsStream(resourceName);
		clazz = null;
		return is;
	}

	public static java.util.Properties loadPropertiesFile(final String resourceName) throws IOException {
		java.util.Properties properties = new java.util.Properties();
		InputStream ins = getResourceAsStream(resourceName);
		if (null == ins) return null;
		try {
			properties.load(ins);
			return properties;
		} finally {
			if (null != ins) {
				ins.close();
				ins = null;
			}
		}
	}

	public static String loadResourceFileAsString(final String resourceName) throws IOException {
		InputStream ins = getResourceAsStream(resourceName);
		return FileManager.read(ins);
	}

	

}
