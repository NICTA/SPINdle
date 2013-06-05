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
package spindle.core.dom;

import spindle.sys.AppConst;

/**
 * Constant values used in DOM.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 1.0.0
 */
public interface DomConst extends AppConst {
	/**
	 * Constants defined for rule.
	 */
	public interface Rule {
		int RULE_ID_LENGTH = 15;
	//	boolean IS_SHOW_RULE_TYPE = false;
	}

	/**
	 * Constants defined for literal.
	 */
	public interface Literal {
		int DUMMY_LITERAL_ID_LENGTH = 10;

		char LITERAL_NEGATION_SIGN = '-';

		char MODE_START = '[';
		char MODE_END = ']';

		char TIMESTAMP_START = '{';
		char TIMESTAMP_END = '}';

		char PREDICATE_START = '(';
		char PREDICATE_END = ')';

		String DEFAULT_PREDICATE_VALUE = "X";

		char LITERAL_SEPARATOR = ',';

		boolean IS_SHOW_PLACE_HOLDER = false;

		char LITERAL_VARIABLE_PREFIX = '@';

		char LITERAL_BOOLEAN_FUNCTION_PREFIX = '$';
		char LITERAL_BOOLEAN_FUNCTION_POSTFIX = '$';

		char THEORY_EQUAL_SIGN = '=';
		
		String TEMPORAL_POSITIVE_INFINITY="+inf";
		String TEMPORAL_NEGATIVE_INFINITY="-inf";
	}

}
