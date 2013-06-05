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
package spindle.core.dom.impl;

import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomConst;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;
import spindle.sys.message.ErrorMessage;

/**
 * Application constant for set representation <i>(Under development)</i>.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2012.07.18
 * @since version 2.1.2
 */
public class Set extends AppConstantBase {

	public static final String LABEL = DomConst.Literal.LITERAL_VARIABLE_PREFIX + "SET";
	public static final String DESCRIPTION = "literal set";
	public static final String USAGE = LABEL + DflTheoryConst.PREDICATE_START + "literal1,literal2,.." + DflTheoryConst.PREDICATE_END;
	public static final int NO_OF_ARGUMENTS = 0;
	protected static final String CODE_BASE = "";

	// private static TheoryParserUtilities parserUtilities = TheoryParserUtilities.getInstance();

	public Set() {
		super(LABEL, DESCRIPTION, USAGE, NO_OF_ARGUMENTS, CODE_BASE);
	}

	@Override
	protected int getNoOfArgumentsToExtract(Object[] args) throws IncorrectNoOfArgumentsException {
		if (null == args) return 0;
		return args.length;
	}

	@Override
	public LiteralVariable getLiteralVariable(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (null == args || args.length == 0) return getLiteralVariable(isNegation);

		throw new AppConstantException(ErrorMessage.NOT_YET_IMPLEMENTED);
	}

	@Override
	protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
	//	if (isNegation) throw new InvalidArgumentException(ErrorMessage.LITERAL_VARIABLE_CANNOT_BE_NEGATED, new Object[] { LABEL });
	}

	// @Override
	// protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
	// try {
	// for (String arg : args) {
	// parserUtilities.parseLiteralString(arg);
	// }
	// } catch (Exception e) {
	// throw new InvalidArgumentException(e);
	// }
	// }

	@Override
	protected String generateCodeBase(boolean isNegation, String[] args) throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		return null;
	}
}
