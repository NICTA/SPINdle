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
package spindle.console;

import spindle.core.SpindleException;

/**
 * Signals that an exception of some sort has occurred in the console.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 */
public class ConsoleException extends SpindleException {

	private static final long serialVersionUID = 1L;

	public ConsoleException(String errorTag) {
		this(null, errorTag, null, null);
	}

	public ConsoleException(String commandName, String errorTag) {
		this(commandName, errorTag, null, null);
	}

	public ConsoleException(String commandName, String errorTag, String message) {
		this(commandName, errorTag, message, null);
	}

	public ConsoleException(String commandName, String errorTag, Object... arguments) {
		this(commandName, errorTag, null, null, arguments);
	}

	public ConsoleException(String commandName, String errorTag, Throwable cause) {
		this(commandName, errorTag, null, cause);
	}

	protected ConsoleException(String commandName, String errorTag, String message, Throwable cause,
			Object... arguments) {
		super(null, errorTag, message, cause, rearrangeArguments(commandName, arguments));
	}
}