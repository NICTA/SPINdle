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

import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomConst;
import spindle.core.dom.Literal;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;

/**
 * Application constant for representing a numerical value.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public class Val extends AppConstantBase {
	public static final String LABEL = DomConst.Literal.LITERAL_VARIABLE_PREFIX + "VAL";
	public static final String DESCRIPTION = "numerical value";
	public static final String USAGE = LABEL + DflTheoryConst.PREDICATE_START + "n" + DflTheoryConst.PREDICATE_END;
	public static final int NO_OF_ARGUMENTS = 1;
	protected static final String CODE_BASE = "";

	public Val() {
		super(LABEL, DESCRIPTION, USAGE, NO_OF_ARGUMENTS, CODE_BASE);
	}

	@Override
	protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
		try {
			Double.parseDouble(args[0]);
		} catch (Exception e) {
			throw new InvalidArgumentException(e);
		}
	}

	@Override
	protected String generateCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		try {
			return "(" + (isNegation ? "-" : "") + Double.parseDouble(args[0]) + ")";
		} catch (Exception e) {
			throw new AppConstantException(e);
		}
	}

	@Override
	public String getCodeBase(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		try {
			return "(" + (isNegation ? "-" : "") + Double.parseDouble(args[0].getName()) + ")";
		} catch (Exception e) {
			throw new AppConstantException(e);
		}
	}
}
