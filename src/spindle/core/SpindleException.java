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
package spindle.core;

import spindle.sys.Messages;

/**
 * Base class for all SPINdle exception
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.30
 */
public abstract class SpindleException extends Exception {
	private static final long serialVersionUID = 1L;

	protected SpindleException(Class<?> caller, String errorTag, String message, Throwable cause, Object... arguments) {
		super(getMessage(caller, errorTag, message, arguments), cause);
	}

	protected final static String getMessage(Class<?> caller, String errorTag, String message, Object... arguments) {
		StringBuilder sb = new StringBuilder();
		if (null != caller) {
			String classname = caller.getName();
			sb.append(classname).append(": ");
		}
		if (null != errorTag) sb.append(Messages.getErrorMessage(errorTag, arguments));
		if (null != message) sb.append(sb.length() == 0 ? "" : ": ").append(message);
		return sb.toString();
	}

	protected final static Object[] rearrangeArguments(String str, Object... arguments) {
		if (null == str) return arguments;
		if (null == arguments) return new Object[] { str };
		Object[] newArgs = new Object[arguments.length + 1];
		newArgs[0] = str;
		System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
		return newArgs;
	}
}
