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

import spindle.io.outputter.DflTheoryConst;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing a literal variable in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 2.0.0
 * @since 2011.07.27
 */
public class LiteralVariable extends Literal {
	private static final long serialVersionUID = 1L;

	protected Literal[] literalPredicates = null;

	public LiteralVariable(String name, boolean isNegation) {
		this(name, isNegation, null, null, null, true);
	}

	public LiteralVariable(String name, boolean isNegation, Mode mode) {
		this(name, isNegation, mode, null, null, true);
	}

	public LiteralVariable(String name, boolean isNegation, Mode mode, String[] predicates) {
		this(name, isNegation, mode, predicates, null, true);
	}

	public LiteralVariable(String name, boolean isNegation, Mode mode, Literal[] literalPredicates) {
		this(name, isNegation, mode, null, literalPredicates, true);
	}

	public LiteralVariable(String name, boolean isNegation, Mode mode, String[] predicates, Literal[] literalPredicates) {
		this(name, isNegation, mode, predicates, literalPredicates, true);
	}

	protected LiteralVariable(String name, boolean isNegation, Mode mode, String[] predicates,
			Literal[] literalPredicates, boolean isValidateName) {
		super(name, isNegation, mode, null,predicates, true);
		setupLiteralVariable(literalPredicates, isValidateName);
	}

	public LiteralVariable(Literal literal) {
		this(literal, true);
	}

	protected LiteralVariable(Literal literal, boolean isValidateName) {
		super(literal);
		Literal[] literalPredicates = literal instanceof LiteralVariable ? ((LiteralVariable) literal).literalPredicates
				: null;
		setupLiteralVariable(literalPredicates, isValidateName);
	}

	protected void setupLiteralVariable(Literal[] literalPredicates, boolean isValidateName) {
		setPlaceHolder(true);
		setLiteralPredicates(literalPredicates);
		if (isValidateName) validateName();
	}

	private void validateName() {
		if (isLiteralVariable()) {
		} else if (isLiteralBooleanFunction()) {
//			try {
				setName(name);
	//		} catch (Exception e) {
		//		throw new IllegalArgumentException(e);
			//}
		} else throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.LITERAL_VARIABLE_PREFIX_ERROR,
				new Object[] { name }));
	}

	@Override
	public LiteralVariable clone() {
		return new LiteralVariable(this, false);
	}

	@Override
	public LiteralVariable getComplementClone() {
		LiteralVariable lv = new LiteralVariable(this, false);
		lv.setNegation(!isNegation);
		return lv;
	}

	public boolean isLiteralVariable() {
		return name.length() > 1 && name.charAt(0) == DomConst.Literal.LITERAL_VARIABLE_PREFIX;
	}

	public boolean isLiteralBooleanFunction() {
		return name.length() > 2 && name.charAt(0) == DomConst.Literal.LITERAL_BOOLEAN_FUNCTION_PREFIX
				&& name.charAt(name.length() - 1) == DomConst.Literal.LITERAL_BOOLEAN_FUNCTION_POSTFIX;
	}

	@Override
	public String[] getPredicates() {
		if (null != literalPredicates) setPredicates(null);
		return super.getPredicates();
	}

	@Override
	public int getPredicatesSize() {
		if (null != literalPredicates) setPredicates(null);
		return super.getPredicatesSize();
	}

	@Override
	public String getPredicateString() {
		if (null != literalPredicates) setPredicates(null);
		return isLiteralBooleanFunction() ? "" : super.getPredicateString();
	}

	public Literal[] getLiteralPredicates() {
		return literalPredicates;
	}

	public void setLiteralPredicates(Literal[] literalPredicates) {
		if (null == literalPredicates || literalPredicates.length < 1) {
			this.literalPredicates = null;
		} else {
			this.literalPredicates = new Literal[literalPredicates.length];
			for (int i = 0; i < literalPredicates.length; i++) {
				setLiteralPredicate(i, literalPredicates[i]);
			}
		}
	}

	public void setLiteralPredicate(int loc, Literal literalPredicate) {
		if (loc >= literalPredicates.length) throw new IllegalArgumentException("index is out of boundary");
		if (null == literalPredicate) {
			literalPredicates[loc] = null;
		} else {
			literalPredicates[loc] = literalPredicate.clone();
		}
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		int c = super.compareTo(o);
		if (c != 0) return c;
		if (!(o instanceof LiteralVariable)) return getClass().getName().compareTo(o.getClass().getName());
		// if (o instanceof LiteralVariable) {
		LiteralVariable lv = (LiteralVariable) o;

		if (null == literalPredicates) {
			if (null == lv.literalPredicates) return 0;
			if (null != lv.literalPredicates) return Integer.MIN_VALUE;
		}
		if (null == lv.literalPredicates) return Integer.MAX_VALUE;
		c = literalPredicates.length - lv.literalPredicates.length;
		if (c != 0) return c;
		for (int i = 0; i < literalPredicates.length; i++) {
			c = literalPredicates[i].compareTo(lv.literalPredicates[i]);
			if (c != 0) return c;
		}
		// }
		// return toString().compareTo(o.toString());
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!super.equals(o)) return false;
		if (!(o instanceof LiteralVariable)) return false;

		LiteralVariable lv = (LiteralVariable) o;

		if (null == literalPredicates) {
			if (null == lv.literalPredicates) return true;
			if (null != lv.literalPredicates) return false;
		}
		if (null == lv.literalPredicates) return false;
		if (literalPredicates.length != lv.literalPredicates.length) return false;

		for (int i = 0; i < literalPredicates.length; i++) {
			if (!literalPredicates[i].equals(lv.literalPredicates[i])) return false;
		}
		return true;
		// }
		// return false;
		// return toString().equals(o.toString());
	}

	public String toString() {
		if (null != literalPredicates) setPredicates(null);
		StringBuilder sb = new StringBuilder();
		if (!"".equals(mode.getName())) sb.append(mode.toString());
		// if (!"".equals(mode.getName()))
		// sb.append(DomConst.Literal.MODE_START).append(mode.toString()).append(DomConst.Literal.MODE_END);
		if (isNegation) sb.append(DomConst.Literal.LITERAL_NEGATION_SIGN);
		sb.append(name);
		if (predicates.length > 1 || isPredicatesGrounded[0]) sb.append(getPredicateString());
		if (null != literalPredicates) {
			StringBuilder sbP = new StringBuilder();
			for (int i = 0; i < literalPredicates.length; i++) {
				if (i > 0) sbP.append(",");
				if (null == literalPredicates[i]) sbP.append("null");
				else sbP.append(literalPredicates[i].toString());
			}
			if (sb.length() > 0) {
				sb.append(DflTheoryConst.PREDICATE_START).append(sbP.toString()).append(DflTheoryConst.PREDICATE_END);
			}
		}
		if (isPlaceHolder) {
			if (DomConst.Literal.IS_SHOW_PLACE_HOLDER) sb.append("[PlaceHolder]");
		}
		return sb.toString();
	}

}
