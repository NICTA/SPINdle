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
package spindle.core.dom.impl;

import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomConst;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;
import spindle.sys.message.ErrorMessage;

/**
 * Application constant for set inclusion <i>(Under development)</i>.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2012.07.18
 * @since version 2.1.2
 */
public class Contains extends AppConstantBase {
	public static final String LABEL = DomConst.Literal.LITERAL_VARIABLE_PREFIX + "CONTAINS";
	public static final String DESCRIPTION = "check whether a literal is contained in a set";
	public static final String USAGE = LABEL + DflTheoryConst.PREDICATE_START + Set.USAGE + ", literal"
			+ DflTheoryConst.PREDICATE_END;
	public static final int NO_OF_ARGUMENTS = 0;
	protected static final String CODE_BASE = "";

	public Contains() {
		super(LABEL, DESCRIPTION, USAGE, NO_OF_ARGUMENTS, CODE_BASE);
	}

	@Override
	protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
	}

	@Override
	protected String generateCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		throw new AppConstantException(ErrorMessage.NOT_YET_IMPLEMENTED);
	}

}
