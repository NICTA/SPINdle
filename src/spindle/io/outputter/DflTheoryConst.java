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
package spindle.io.outputter;

import spindle.core.dom.RuleType;
import spindle.core.dom.DomConst.Literal;

/**
 * Constants used in DFL theory parser and outputter.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.05.28
 */
public interface DflTheoryConst {
	String COMMENT_SYMBOL = "#";

	char RULE_LABEL_SEPARATOR = ':';
	char LITERAL_SEPARATOR = Literal.LITERAL_SEPARATOR;
	char MODE_START = Literal.MODE_START;
	char MODE_END = Literal.MODE_END;

	char TIMESTAMP_START = Literal.TIMESTAMP_START;
	char TIMESTAMP_END = Literal.TIMESTAMP_END;

	char PREDICATE_START = Literal.PREDICATE_START;
	char PREDICATE_END = Literal.PREDICATE_END;

	String SYMBOL_MODE_CONVERSION = RuleType.MODE_CONVERSION.getSymbol();
	String SYMBOL_MODE_CONFLICT = RuleType.MODE_CONFLICT.getSymbol();
	String SYMBOL_MODE_EXCLUSION = RuleType.MODE_EXCLUSION.getSymbol();

	String SYMBOL_NEGATION = "-";

	char LITERAL_VARIABLE_PREFIX = Literal.LITERAL_VARIABLE_PREFIX;

	char LITERAL_BOOLEAN_FUNCTION_PREFIX = Literal.LITERAL_BOOLEAN_FUNCTION_PREFIX;
	char LITERAL_BOOLEAN_FUNCTION_POSTFIX = Literal.LITERAL_BOOLEAN_FUNCTION_POSTFIX;

	char THEORY_EQUAL_SIGN = Literal.THEORY_EQUAL_SIGN;
}
