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
package spindle.core.dom;

import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.message.ErrorMessage;

/**
 * Enumerate on the types of rule.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 1.0.0
 */
public enum RuleType {
	LITERAL_VARIABLE_SET("Literal variable/boolean function", "set"), //
	FACT("Fact", ">>"), //
	STRICT("Strict rule", "->"), //
	DEFEASIBLE("Defeasible rule", "=>"), //
	DEFEATER("Defeater", "~>"), //
	SUPERIORITY("Superiority relation", ">"), //
	INFERIORITY("Inferiority relation", "<"), //
	MODE_CONVERSION("Mode conversion", "=="), //
	MODE_CONFLICT("Mode conflict", "!=");

	private final String label;
	private final String symbol;

	RuleType(String _label, String _symbol) {
		label = _label.trim();
		symbol = _symbol.trim();
	}

	public String getLabel() {
		return label;
	}

	public String getSymbol() {
		return symbol;
	}

	/**
	 * @param str rule string
	 * @return rule type associated with the rule string
	 * @throws ParserException If no rule type associated with the string can found
	 */
	public static RuleType getRuleType(String str) throws ParserException {
		for (RuleType ruleType : RuleType.values()) {
			switch (ruleType) {
			case LITERAL_VARIABLE_SET:
				if (str.startsWith(ruleType.symbol)) return ruleType;
				break;
			default:
				// skip all characters before the rule label
				int loc = str.indexOf(DflTheoryConst.RULE_LABEL_SEPARATOR);
				if (loc < 0) loc = 0;
				// return ruleType if the rule symbol appears in the rule body
				if (str.indexOf(ruleType.symbol, loc) >= 0) return ruleType;
			}
		}
		throw new ParserException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE,new Object[]{ str});
	}
}
