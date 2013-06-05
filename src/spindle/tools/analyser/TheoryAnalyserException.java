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
package spindle.tools.analyser;

import spindle.tools.ToolsException;

/**
 * Signals an exception of some sort has occurred in analysing the theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public class TheoryAnalyserException extends ToolsException {

	private static final long serialVersionUID = 1L;

	public TheoryAnalyserException(String errorTag) {
		this(errorTag, null, null);
	}

	public TheoryAnalyserException(Throwable cause) {
		this(null, null, cause);
	}

	protected TheoryAnalyserException(String errorTag, String message, Throwable cause, Object... arguments) {
		super(errorTag, message, cause, arguments);
	}

}
