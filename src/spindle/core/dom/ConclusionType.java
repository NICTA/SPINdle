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
import spindle.sys.message.ErrorMessage;

/**
 * Enumerate on the types of conclusion.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 1.0.0
 */
public enum ConclusionType {
	DEFINITE_PROVABLE("Definitely provable", "+D", "DEFINITE_PROVABLE"), //
	DEFINITE_NOT_PROVABLE("NOT Definitely provable", "-D", "NOT_DEFINITE_PROVABLE"), //
	DEFEASIBLY_PROVABLE("Defeasibly provable", "+d", "DEFEASIBLE_PROVABLE"), //
	DEFEASIBLY_NOT_PROVABLE("NOT Defeasibly provable", "-d", "NOT_DEFEASIBLE_PROVABLE"), //
	TENTATIVELY_PROVABLE("Tentatively provable", "+tt", "TENTATIVELY_PROVABLE"), //
	TENTATIVELY_NOT_PROVABLE("NOT Tentatively provable", "-tt", "NOT_TENTATIVELY_PROVABLE"), //
	POSITIVELY_SUPPORT("Positively support", "+z", "POSITIVELY_SUPPORT"), //
	NEGATIVELY_SUPPORT("Negatively support", "-z", "NEGATIVELY_SUPPORT"), //
	AMBIGUITY_DEFEATED("Ambiguity defeated", "-ad", "AMBIGUITY_DEFEATED");

	private final String label;
	private final String symbol;
	private final String textTag;

	ConclusionType(String _label, String _symbol, String _textTag) {
		label = _label;
		symbol = _symbol;
		textTag = _textTag;
	}

	public String getLabel() {
		return label;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getTextTag() {
		return textTag;
	}

	public static ConclusionType getConclusionType(String str) throws ParserException {
		for (ConclusionType conclusionType : ConclusionType.values()) {
			if (str.indexOf(conclusionType.getSymbol()) >= 0) return conclusionType;
		}
		throw new ParserException(ErrorMessage.CONCLUSION_UNKNOWN_CONCLUSION_TYPE, str);
	}
}
