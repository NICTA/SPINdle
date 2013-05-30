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
package spindle.core.dom;

import spindle.io.ParserException;
import spindle.sys.message.ErrorMessage;

/**
 * Enumerate on the types of conclusion.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2012.09.18
 * @version 2011.07.27
 * @since version 1.0.0
 */
public enum ConclusionType {

	DEFINITE_PROVABLE("Definitely provable", "+D", "DEFINITE_PROVABLE", ProvabilityLevel.DEFINITE, true, false), //
	DEFINITE_NOT_PROVABLE("NOT Definitely provable", "-D", "NOT_DEFINITE_PROVABLE", ProvabilityLevel.DEFINITE, false, true), //
	DEFEASIBLY_PROVABLE("Defeasibly provable", "+d", "DEFEASIBLE_PROVABLE", ProvabilityLevel.DEFEASIBLE, true, false), //
	DEFEASIBLY_NOT_PROVABLE("NOT Defeasibly provable", "-d", "NOT_DEFEASIBLE_PROVABLE", ProvabilityLevel.DEFEASIBLE, false, true), //
	TENTATIVELY_PROVABLE("Tentatively provable", "+tt", "TENTATIVELY_PROVABLE", ProvabilityLevel.NONE, true, false), //
	TENTATIVELY_NOT_PROVABLE("NOT Tentatively provable", "-tt", "NOT_TENTATIVELY_PROVABLE", ProvabilityLevel.NONE, false, true), //
	POSITIVELY_SUPPORT("Positively support", "+z", "POSITIVELY_SUPPORT", ProvabilityLevel.NONE, true, false), //
	NEGATIVELY_SUPPORT("Negatively support", "-z", "NEGATIVELY_SUPPORT", ProvabilityLevel.NONE, false, true), //
	AMBIGUITY_DEFEATED("Ambiguity defeated", "-ad", "AMBIGUITY_DEFEATED", ProvabilityLevel.NONE, false, true);

	private final String label;
	private final String symbol;
	private final String textTag;
	private final ProvabilityLevel provabilityLevel;
	private final boolean pos;
	private final boolean neg;

	ConclusionType(String _label, String _symbol, String _textTag, ProvabilityLevel _provabilityLevel, boolean _pos, boolean _neg) {
		label = _label;
		symbol = _symbol;
		textTag = _textTag;
		provabilityLevel = _provabilityLevel;
		pos = _pos;
		neg = _neg;
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

	public ProvabilityLevel getProvabilityLevel() {
		return provabilityLevel;
	}

	public boolean isPositiveConclusion() {
		return pos;
	}

	public boolean isNegativeConclusion() {
		return neg;
	}

	public boolean isConflictWith(ConclusionType conclusionType) {
		return isConflict(this, conclusionType);
	}

	public static ConclusionType getConclusionType(String str) throws ParserException {
		for (ConclusionType conclusionType : ConclusionType.values()) {
			if (str.indexOf(conclusionType.getSymbol()) >= 0) return conclusionType;
		}
		throw new ParserException(ErrorMessage.CONCLUSION_UNKNOWN_CONCLUSION_TYPE, new Object[] { str });
	}

	public static boolean isConflict(ConclusionType ct1, ConclusionType ct2) {
		if (DEFINITE_PROVABLE.equals(ct1) && DEFINITE_NOT_PROVABLE.equals(ct2)) return true;
		if (DEFINITE_NOT_PROVABLE.equals(ct1) && DEFINITE_PROVABLE.equals(ct2)) return true;
		if (DEFEASIBLY_PROVABLE.equals(ct1) && DEFEASIBLY_NOT_PROVABLE.equals(ct2)) return true;
		if (DEFEASIBLY_NOT_PROVABLE.equals(ct1) && DEFEASIBLY_PROVABLE.equals(ct2)) return true;
		return false;
	}

}