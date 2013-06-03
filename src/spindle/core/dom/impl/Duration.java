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
package spindle.core.dom.impl;

import com.app.utils.Converter;

import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;
import spindle.sys.message.ErrorMessage;

/**
 * Application constant for representing a duration of time.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public class Duration extends AppConstantBase {
	public static final String LABEL = DomConst.Literal.LITERAL_VARIABLE_PREFIX + "DURATION";
	public static final String DESCRIPTION = "time value";
	public static final String USAGE = LABEL;
	public static final int NO_OF_ARGUMENTS = 1;
	protected static final String CODE_BASE = "";

	public Duration() {
		super(LABEL, DESCRIPTION, USAGE, NO_OF_ARGUMENTS, CODE_BASE);
	}

	@Override
	protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
		if (args.length > 1) throw new InvalidArgumentException(ErrorMessage.INCORRECT_NO_OF_ARGUMENTS);
		try {
			Converter.timeString2long(args[0]);
		} catch (Exception e) {
			throw new InvalidArgumentException(e + ":pred=" + args[0]);
		}
	}

	@Override
	protected String generateCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (null == args || args.length == 0) throw new IncorrectNoOfArgumentsException(minNoOfArguments);

		try {
			return "(" + (isNegation ? "-" : "") + Converter.timeString2long(args[0]) + ")";
		} catch (com.app.exception.InvalidArgumentException e) {
			throw new InvalidArgumentException(e);
		}
	}

}
