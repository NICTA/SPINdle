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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import spindle.sys.AppFeatureConst;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing a literal in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.01
 */
public class Literal implements Comparable<Object>, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	private static final char LITERAL_NEGATION_SIGN = DomConst.Literal.LITERAL_NEGATION_SIGN;
	private static final char LITERAL_SEPARATOR = DomConst.Literal.LITERAL_SEPARATOR;
	private static final char PREDICATE_START = DomConst.Literal.PREDICATE_START;
	private static final char PREDICATE_END = DomConst.Literal.PREDICATE_END;
	private static final String DEFAULT_PREDICATE_VALUE = DomConst.Literal.DEFAULT_PREDICATE_VALUE;
	private static final Comparator<Literal> DEFAULT_LITERAL_COMPARATOR = new LiteralComparator(true);

	protected String name;
	protected boolean isNegation;
	protected Mode mode;

	protected String[] predicates = null;
	protected Literal[] predicates2 = null;

	protected boolean[] isPredicatesGrounded = null;
	protected boolean isPlaceHolder;
	protected Temporal temporal = null;

	public Literal(final String name) {
		this(name, false, null, null, (String[]) null, false);
	}

	public Literal(final String name, final boolean isNegation) {
		this(name, isNegation, null, null, (String[]) null, false);
	}

	public Literal(final String name, final boolean isNegation, final Mode mode) {
		this(name, isNegation, mode, null, (String[]) null, false);
	}

	public Literal(final String name, final boolean isNegation, final Mode mode, final Temporal temporal, final String[] predicates,
			final boolean isPlaceHolder) {
		setName(name);
		setNegation(isNegation);
		setMode(mode);
		setTemporal(temporal);
		setPredicates(predicates);
		setPlaceHolder(isPlaceHolder);
	}

	public Literal(Literal literal) {
		this(literal, true);
	}

	protected Literal(Literal literal, boolean withTemporal) {
		if (null == literal) throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.LITERAL_NULL_LITERAL));
		setName(literal.name);
		setNegation(literal.isNegation);
		setMode(literal.mode);
		if (withTemporal) setTemporal(literal.temporal);
		setPredicates(literal.predicates);
		setPlaceHolder(literal.isPlaceHolder);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		if (null == name || "".equals(name.trim()))
			throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.LITERAL_NAME_MISSING));
		this.name = name.trim();
	}

	public boolean isNegation() {
		return isNegation;
	}

	public void setNegation(final boolean isNegation) {
		this.isNegation = isNegation;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(final Mode mode) {
		this.mode = (mode == null) ? new Mode("", false) : mode.clone();
	}

	public String[] getPredicates() {
		return predicates;
	}

	public void setPredicates(final String[] predicates) {
		if (null == predicates || predicates.length < 1) {
			this.predicates = new String[] { DEFAULT_PREDICATE_VALUE };
			this.isPredicatesGrounded = new boolean[] { false };
		} else {
			this.predicates = new String[predicates.length];
			this.isPredicatesGrounded = new boolean[predicates.length];
			for (int i = 0; i < predicates.length; i++) {
				setPredicate(i, predicates[i]);
			}

			// List<Literal>predicatesL=new ArrayList<Literal>();
			// try{
			// for (int i=0;i<predicates.length;i++){
			// System.out.println(getName());
			// predicatesL.add(DflTheoryParser2.extractLiteral(predicates[i]));
			// }
			// predicates2=new Literal[predicates.length];
			// predicatesL.toArray(predicates2);
			// }catch (Exception e){
			// e.printStackTrace();
			// }
		}
	}

	public void setPredicate(final int loc, final String predicate) {
		if (loc >= predicates.length) throw new IllegalArgumentException("index is out of boundary");
		String tPredicate = (null == predicate) ? "" : predicate.trim();
		if ("".equals(tPredicate)) {
			predicates[loc] = DEFAULT_PREDICATE_VALUE;
			isPredicatesGrounded[loc] = false;
		} else {
			if (Character.isUpperCase(tPredicate.charAt(0))) {
				predicates[loc] = tPredicate.toUpperCase();
				isPredicatesGrounded[loc] = false;
			} else {
				predicates[loc] = tPredicate;
				isPredicatesGrounded[loc] = true;
			}
		}
	}

	public String getPredicate(final int loc) {
		if (loc >= predicates.length) throw new IllegalArgumentException("index is out of boundary");
		return predicates[loc];
	}

	public int getPredicatesSize() {
		return predicates.length;
	}

	public boolean isPredicateGrounded(final int loc) {
		if (loc >= predicates.length) throw new IllegalArgumentException("index is out of boundary");
		return isPredicatesGrounded[loc];
	}

	public void updatePredicatesValues(Map<String, String> predicateValues) {
		for (int i = 0; i < predicates.length; i++) {
			if (!isPredicatesGrounded[i]) {
				String value = predicateValues.get(predicates[i]);
				if (null != value) setPredicate(i, value);
			}
		}
	}

	public Literal cloneWithUpdatePredicatesValues(Map<String, String> predicateValues) {
		String[] newPredicates = new String[predicates.length];
		for (int i = 0; i < predicates.length; i++) {
			if (isPredicatesGrounded[i]) {
				newPredicates[i] = predicates[i];
			} else {
				String value = predicateValues.get(predicates[i]);
				newPredicates[i] = (null == value) ? predicates[i] : value;
			}
		}
		return new Literal(name, isNegation, mode, temporal, newPredicates, isPlaceHolder);
	}

	public Temporal getTemporal() {
		return temporal;
	}

	public void setTemporal(Temporal temporal) {
		if (null == temporal || !temporal.hasTemporalInfo()) this.temporal = null;
		else this.temporal = temporal.clone();
	}

	/**
	 * check if the literal contains any temporal information.
	 * 
	 * @return true if there is no temporal information; and false otherwise.
	 */
	public boolean hasTemporalInfo() {
		return null == temporal ? false : temporal.hasTemporalInfo();

	}

	public boolean contains(Literal literal) {
		if (!equals(literal, false)) return false;
		if (hasTemporalInfo()) {
			if (literal.hasTemporalInfo()) return temporal.contains(literal.getTemporal());
			else return false;
		} else {
			// the two literals are of the same
			// and this literal does not contains any temporal information,
			// if (literal.hasTemporalInfo())return true;
			return true;
		}
		// if (hasTemporalInfo() && literal.hasTemporalInfo()) {
		// return temporal.includes(literal.temporal);
		// } else return equals(literal);// return false;
	}

	public boolean isPlaceHolder() {
		return isPlaceHolder;
	}

	public void setPlaceHolder(final boolean isPlaceHolder) {
		this.isPlaceHolder = isPlaceHolder;
	}

	public Literal clone() {
		return new Literal(this);
	}

	/**
	 * check if the literal is a complement of the input literal.
	 * 
	 * @return true if the literal is a complement of the input literal; false otherwise.
	 */
	public boolean isComplementTo(Literal literal) {
		if (!this.name.equals(literal.name)) return false;
		if (!this.mode.getName().equals(literal.mode.getName())) return false;


		Temporal literalTemporal = literal.temporal;
		if (null == temporal) {
			if (null != literalTemporal) {
				if (AppFeatureConst.checkConflictLiteralsWithTemporalStartOnly) {
					if (Long.MIN_VALUE!=literalTemporal.getStartTime())return false;
				}
				//else return false;
			}
		} else {
			if (null == literalTemporal) {
				if (AppFeatureConst.checkConflictLiteralsWithTemporalStartOnly){
					if (Long.MIN_VALUE!=temporal.getStartTime())return false;
				}
				//else return false;
			} else{
			if (AppFeatureConst.checkConflictLiteralsWithTemporalStartOnly) {
				if (!temporal.sameStart(literalTemporal)) return false;
			} else {
				if (!temporal.overlap(literalTemporal)) return false;
			}
			}
		}
		
		
		if (predicates.length != literal.predicates.length) return false;
		for (int i = 0; i < predicates.length; i++) {
			if (!isPredicatesGrounded[i] && !literal.isPredicatesGrounded[i]) {
			} else if (isPredicatesGrounded[i] && literal.isPredicatesGrounded[i]) {
				if (!predicates[i].equals(literal.predicates[i])) return false;
			} else {
				return false;
			}
		}
		
		// if (null == temporal && null != literalTemporal) return false;
		// if (null != temporal) {
		// if (null == literalTemporal) return false;
		// // if (!temporal.sameStartTime(literal.temporal)) return false;
		//
		// if (AppFeatureConst.checkConflictLiteralsWithTemporalStartOnly) {
		// if (!temporal.sameStartTime(literalTemporal)) return false;
		// } else {
		// if (!temporal.overlap(literalTemporal)) return false;
		// }
		// // if (!temporal.equals(literal.temporal)) return false;
		// }
		if (!"".equals(mode.getName()) && this.isNegation == literal.isNegation) { return mode.isComplementTo(literal.getMode()); }
		return this.isNegation != literal.isNegation;
	}

	/**
	 * create a complement clone of this literal.
	 * 
	 * @return a complemented clone of this literal.
	 */
	public Literal getComplementClone() {
		Literal l = new Literal(this);
		l.setNegation(!isNegation);
		return l;
	}

	public Literal getComplementCloneWithNoTemporal() {
		Literal l = new Literal(this, false);
		l.setNegation(!isNegation);
		return l;
	}

	/**
	 * Return a copy of literal without temporal information.
	 * 
	 * @return A copy of the literal without temporal information.
	 */
	public Literal cloneWithNoTemporal() {
		return new Literal(this, false);
	}

	public String getPredicateString() {
		StringBuilder sb = new StringBuilder();
		sb.append(PREDICATE_START);
		if (predicates.length > 0) {
			for (int i = 0; i < predicates.length; i++) {
				if (i > 0) sb.append(LITERAL_SEPARATOR);
				sb.append(predicates[i]);
			}
		} else {
			sb.append(DEFAULT_PREDICATE_VALUE);
		}
		sb.append(PREDICATE_END);
		return sb.toString();
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Literal)) return getClass().getName().compareTo(o.getClass().getName());

		return DEFAULT_LITERAL_COMPARATOR.compare(this, (Literal) o);

		// Literal literal = (Literal) o;
		// int c = name.compareTo(literal.name);
		// if (c != 0) return c;
		// if (isNegation != literal.isNegation) return (this.isNegation) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		//
		// // same name, negation sign
		// // check mode and temporal
		// c = mode.compareTo(literal.mode);
		// if (c != 0) return c;
		//
		// if (null == temporal) {
		// if (null != literal.temporal) return Integer.MIN_VALUE;
		// } else {
		// if (null == literal.temporal)
		// return Integer.MAX_VALUE;
		// else {
		// c = temporal.compareTo(literal.temporal);
		// if (c != 0) return c;
		// }
		// }
		//
		// // check predicates
		// c=predicates.length-literal.predicates.length;
		// if (c!=0)return c;
		// // if (predicates.length != literal.predicates.length) return predicates.length - literal.predicates.length;
		// for (int i = 0; i < predicates.length; i++) {
		// if (!isPredicatesGrounded[i] && !literal.isPredicatesGrounded[i]) {
		// } else if (isPredicatesGrounded[i] && literal.isPredicatesGrounded[i]) {
		// c = predicates[i].compareTo(literal.predicates[i]);
		// if (c != 0) return c;
		// // if (!predicates[i].equals(literal.predicates[i])) return
		// // predicates[i].compareTo(literal.predicates[i]);
		// } else {
		// return (isPredicatesGrounded[i]) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		// }
		// }
		// return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isNegation ? 1231 : 1237);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(predicates);
		result = prime * result + ((temporal == null) ? 0 : temporal.hashCode());
		return result;
	}

	private boolean equals(Literal literal, boolean checkTemporal) {
		if (!name.equals(literal.name)) return false;
		if (isNegation != literal.isNegation) return false;
		if (!mode.equals(literal.mode)) return false;
		if (checkTemporal) {
			if (null == temporal) {
				if (null != literal.temporal) return false;
			} else if (!temporal.equals(literal.temporal)) return false;
		}
		if (predicates.length != literal.predicates.length) return false;
		for (int i = 0; i < predicates.length; i++) {
			if (!isPredicatesGrounded[i] && !literal.isPredicatesGrounded[i]) {
			} else if (isPredicatesGrounded[i] && literal.isPredicatesGrounded[i]) {
				if (!predicates[i].equals(literal.predicates[i])) return false;
			} else {
				return false;
			}
		}
		return true;
	}

	public boolean equalsWithNoTemporal(Literal literal) {
		return equals(literal, false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof Literal)) return false;
		return equals((Literal) o, true);

		// Literal literal = (Literal) o;
		// if (!name.equals(literal.name)) return false;
		// if (isNegation != literal.isNegation) return false;
		// if (!mode.equals(literal.mode)) return false;
		// if (predicates.length != literal.predicates.length) return false;
		// for (int i = 0; i < predicates.length; i++) {
		// if (!isPredicatesGrounded[i] && !literal.isPredicatesGrounded[i]) {
		// } else if (isPredicatesGrounded[i] && literal.isPredicatesGrounded[i]) {
		// if (!predicates[i].equals(literal.predicates[i])) return false;
		// } else {
		// return false;
		// }
		// }
		// if (null == temporal) {
		// if (null != literal.temporal) return false;
		// } else if (!temporal.equals(literal.temporal)) return false;
		// return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!"".equals(mode.getName())) sb.append(mode.toString());
		if (isNegation) sb.append(LITERAL_NEGATION_SIGN);
		sb.append(name);
		sb.append(getPredicateString());

		if (null != temporal) sb.append(temporal.toString());
		if (isPlaceHolder) {
			if (DomConst.Literal.IS_SHOW_PLACE_HOLDER) sb.append("[PlaceHolder]");
		}

		return sb.toString();
	}
}
