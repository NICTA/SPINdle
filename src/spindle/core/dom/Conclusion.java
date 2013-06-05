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

import java.io.Serializable;
import java.util.Collection;

/**
 * DOM for representing a conclusion in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class Conclusion implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private ConclusionType conclusionType;
	private Literal literal;

	public Conclusion(final ConclusionType conclusionType, final Literal literal) {
		setConclusionType(conclusionType);
		setLiteral(literal);
	}

	public Conclusion(Conclusion conclusion) {
		setConclusionType(conclusion.conclusionType);
		setLiteral(conclusion.literal.clone());
	}

	public Literal getLiteral() {
		return literal;
	}

	public void setLiteral(Literal literal) {
		this.literal = literal;
	}

	public ConclusionType getConclusionType() {
		return conclusionType;
	}

	public void setConclusionType(ConclusionType conclusionType) {
		this.conclusionType = conclusionType;
	}

	public boolean hasTemporalInfo() {
		return literal.hasTemporalInfo();
	}

	public Temporal getTemporal() {
		return literal.getTemporal();
	}

	public ProvabilityLevel getProvabilityLevel() {
		return conclusionType.getProvabilityLevel();
	}

	/**
	 * two conclusions are conflict if <br/>
	 * <ul>
	 * <li>they have the same literal, but one with +D while the other with -D (+D q and -D q)</li>
	 * <li>they have conflict literal, and can be prove definitely (+D q and +D ~q)</li>
	 * </ul>
	 */
	public boolean isConflictWith(Conclusion conclusion) {
		if (this.literal.equals(conclusion.literal)) {
			return (this.conclusionType.isConflictWith(conclusion.conclusionType));
//			if (this.conclusionType == ConclusionType.DEFINITE_PROVABLE
//					&& conclusion.conclusionType == ConclusionType.DEFINITE_NOT_PROVABLE) return true;
//			if (this.conclusionType == ConclusionType.DEFINITE_NOT_PROVABLE
//					&& conclusion.conclusionType == ConclusionType.DEFINITE_PROVABLE) return true;
//
//			if (this.conclusionType == ConclusionType.DEFEASIBLY_PROVABLE
//					&& conclusion.conclusionType == ConclusionType.DEFEASIBLY_NOT_PROVABLE) return true;
//			if (this.conclusionType == ConclusionType.DEFEASIBLY_NOT_PROVABLE
//					&& conclusion.conclusionType == ConclusionType.DEFEASIBLY_PROVABLE) return true;
		} else if (this.literal.getComplementClone().equals(conclusion.literal)) {
			if (this.conclusionType == conclusion.conclusionType) {
				if (this.conclusionType == ConclusionType.DEFINITE_PROVABLE) return true;
				if (this.conclusionType == ConclusionType.DEFEASIBLY_PROVABLE) return true;
			}
		}
		return false;
	}

	public boolean isConflictWith(Collection<Conclusion> conclusionList) {
		for (Conclusion conclusion : conclusionList) {
			if (isConflictWith(conclusion)) return true;
		}
		return false;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Conclusion)) return getClass().getName().compareTo(o.getClass().getName());

		Conclusion conclusion = (Conclusion) o;

		int c = conclusionType.compareTo(conclusion.conclusionType);
		if (c != 0) return c;

		return literal.compareTo(conclusion.literal);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (getClass() != o.getClass()) return false;

		Conclusion c = (Conclusion) o;
		if (conclusionType != c.conclusionType) return false;
		return literal.equals(c.literal);
	}

	@Override
	public Conclusion clone() {
		return new Conclusion(this);
	}

	public String toString() {
		return conclusionType.getSymbol() + " " + literal.toString();
	}

}
