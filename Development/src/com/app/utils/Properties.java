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

import java.util.Map.Entry;

public class Properties extends java.util.Properties {

	private static final long serialVersionUID = 1L;

	public Properties() {
		super();
	}

	public Properties(java.util.Properties properties) {
		super(properties);
		for (Entry<Object, Object> e : properties.entrySet()) {
			setProperty(e.getKey().toString(), e.getValue().toString());
		}
	}

	public String getProperty(final String name, final String defaultValue) {
		return containsKey(name) ? getProperty(name) : defaultValue;
	}

	public Integer getPropertyAsInteger(final String name) {
		return getPropertyAsInteger(name, null);
	}

	public Integer getPropertyAsInteger(final String name, final Integer defaultValue) {
		return containsKey(name) ? Integer.parseInt(getProperty(name)) : defaultValue;
	}

	public Long getPropertyAsLong(final String name) {
		return getPropertyAsLong(name, null);
	}

	public Long getPropertyAsLong(final String name, final Long defaultValue) {
		return containsKey(name) ? Long.parseLong(getProperty(name)) : defaultValue;
	}

	public Double getPropertyAsDouble(final String name) {
		return getPropertyAsDouble(name, null);
	}

	public Double getPropertyAsDouble(final String name, final Double defaultValue) {
		return containsKey(name) ? Double.parseDouble(getProperty(name)) : defaultValue;
	}

	public Boolean getPropertyAsBoolean(final String name) {
		return getPropertyAsBoolean(name, null);
	}

	public Boolean getPropertyAsBoolean(final String name, final Boolean defaultValue) {
		return containsKey(name) ? Boolean.parseBoolean(getProperty(name)) : defaultValue;
	}
}
