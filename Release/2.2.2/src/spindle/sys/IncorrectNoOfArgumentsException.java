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
package spindle.sys;

import spindle.sys.message.ErrorMessage;

/**
 * Signals that an incorrect number of arguments have received to the current command, or application constants.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class IncorrectNoOfArgumentsException extends ConfigurationException {

	private static final long serialVersionUID = 1L;

	public IncorrectNoOfArgumentsException(int noOfArguments) {
		this(null, new Object[] { noOfArguments });
	}

	public IncorrectNoOfArgumentsException(String noOfArguments) {
		this(null, new Object[] { noOfArguments });
	}

	public IncorrectNoOfArgumentsException(String commandName, int noOfArguments) {
		this(commandName, new Object[] { noOfArguments });
	}

	public IncorrectNoOfArgumentsException(String commandName, String noOfArguments) {
		this(commandName, new Object[] { noOfArguments });
	}

	protected IncorrectNoOfArgumentsException(String commandName, Object... arguments) {
		super( ErrorMessage.INCORRECT_NO_OF_ARGUMENTS, null, null, rearrangeArguments(commandName, arguments));
	}

}
