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
package spindle.io;

import spindle.core.SpindleException;

/**
 * Signals that an attempt of parsing the deafeasible theory or conclusions saved has failed.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2011.07.27
 */
public class ParserException extends SpindleException {

	private static final long serialVersionUID = 1L;

	public ParserException(String errorTag, Object... arguments) {
		this(errorTag, null, arguments);
	}

	public ParserException(String errorTag, Throwable cause) {
		this(errorTag, cause, (Object[]) null);
	}

	public ParserException(Throwable cause) {
		this(null, cause, (Object[]) null);
	}

	public ParserException(String errorTag, Throwable cause, Object... arguments) {
		super(null, errorTag, null, cause, arguments);
	}
}
