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

import com.app.utils.DateTime;

import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;
import spindle.sys.message.ErrorMessage;

/**
 * Application constant for represent the current date.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public class Date extends AppConstantBase {
	public static final String LABEL = DomConst.Literal.LITERAL_VARIABLE_PREFIX + "DATE";
	public static final String DESCRIPTION = "set a date for logic comparison";
	public static final String USAGE = LABEL + DomConst.Literal.PREDICATE_START + "yyyy,mm,dd" + DomConst.Literal.PREDICATE_END //
			+ " or " + LABEL + DomConst.Literal.PREDICATE_START + "yyyy,mm,dd,hh,mm,ss" + DomConst.Literal.PREDICATE_START;
	public static final int NO_OF_ARGUMENTS_1 = 3;
	public static final int NO_OF_ARGUMENTS_2 = 6;
	protected static final String CODE_BASE = "com.app.utils.DateTime.getDate({0},{1},{2},{3},{4},{5}).getTimeInMillis()";

	public Date() {
		super(LABEL, DESCRIPTION, USAGE, NO_OF_ARGUMENTS_1, CODE_BASE);
	}

	@Override
	protected int getNoOfArgumentsToExtract(Object[] args) throws IncorrectNoOfArgumentsException {
		if (null == args) throw new IncorrectNoOfArgumentsException(NO_OF_ARGUMENTS_1);
		if (args.length == NO_OF_ARGUMENTS_2) return NO_OF_ARGUMENTS_2;
		if (args.length == NO_OF_ARGUMENTS_1) return NO_OF_ARGUMENTS_1;
		throw new IncorrectNoOfArgumentsException(NO_OF_ARGUMENTS_1 + " or " + NO_OF_ARGUMENTS_2);
	}

	@Override
	protected void verifyArguments(boolean isNegation, String[] args) throws InvalidArgumentException {
		if (isNegation) {
			throw new InvalidArgumentException(ErrorMessage.LITERAL_VARIABLE_CANNOT_BE_NEGATED, new Object[] { LABEL });
		}
		try {
			DateTime.verifyDateTimeArguments(args);
		} catch (Exception e) {
			throw new InvalidArgumentException(e);
		}
	}

	@Override
	protected String generateCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (isNegation) throw new AppConstantException(ErrorMessage.LITERAL_VARIABLE_CANNOT_BE_NEGATED, new Object[] { LABEL });
		try {
			String[] d = DateTime.verifyDateTimeArguments(args);
			// update the month value since it starts from 0 to 11 in java.util.Date
			int t[] = new int[d.length];
			for (int i = 0; i < d.length; i++) {
				t[i] = Integer.parseInt(d[i]);
			}
			// update the month value since it starts from 0 to 11 in java.util.Date
			t[1] = t[1] - 1;
			if (d.length == 3) return "(" + DateTime.getDate(t[0], t[1], t[2]).getTimeInMillis() + ")";
			else return "(" + DateTime.getDate(t[0], t[1], t[2], t[3], t[4], t[5]).getTimeInMillis() + ")";
		} catch (Exception e) {
			throw new AppConstantException(e);
		}
	}
}
