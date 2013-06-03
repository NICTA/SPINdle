/**
 * SPINdle (version 2.2.0)
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
package spindle.sys;

import spindle.core.SpindleException;

/**
 * Signals that an exception of some sort has occurred in configuring the system/reasoner environment.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class ConfigurationException extends SpindleException {

	private static final long serialVersionUID = 1L;

	public ConfigurationException(Throwable cause) {
		this(null, null, cause, (Object[]) null);
	}

	public ConfigurationException(String errorTag, Object... arguments) {
		this(null, errorTag, null, null, arguments);
	}

	public ConfigurationException(String errorTag, Throwable cause, Object... arguments) {
		this(errorTag, null, cause, arguments);
	}

	protected ConfigurationException(String errorTag, String message, Throwable cause, Object... arguments) {
		super(null, errorTag, message, cause, arguments);
	}
}
