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

import java.io.Serializable;

/**
 * DOM for representing a rule/literal modal operator.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 */
public class Mode implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private static final char MODE_START = DomConst.Literal.MODE_START;
	private static final char MODE_END = DomConst.Literal.MODE_END;
	private static final char MODE_NEGATION_SIGN = DomConst.Literal.LITERAL_NEGATION_SIGN;

	private String name;
	private boolean isNegation;

	public Mode(final String name, final boolean isNegation) {
		setName(name);
		setNegation(isNegation);
	}

	public Mode(Mode mode) {
		setName(mode.name);
		setNegation(mode.isNegation);
	}

	public void setNegation(final boolean isNegation) {
		this.isNegation = isNegation;
	}

	public boolean isNegation() {
		return this.isNegation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = (null == name) ? "" : name.trim().toUpperCase();
	}

	public Mode clone() {
		return new Mode(this);
	}

	/**
	 * create a complement of this mode by:
	 * <ul>
	 * <li>duplicate the mode if the mode name is empty;</li>
	 * <li>or otherwise duplicate the mode with negation.</li>
	 * </ul>
	 * 
	 * @return <code>Mode</code> - a complemented mode
	 */
	public Mode getComplementClone() {
		return new Mode(name, (("".equals(name)) ? false : !isNegation));
	}

	/**
	 * check if the mode is a complement of the input mode.
	 * 
	 * @param mode mode to check.
	 * @return true if the mode is a complement of the input mode; false otherwise.
	 */
	public boolean isComplementTo(final Mode mode) {
		if (!name.equals(mode.name)) return false;
		if ("".equals(name)) return true;
		return isNegation != mode.isNegation;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Mode)) return getClass().getName().compareTo(o.getClass().getName());

		Mode m = (Mode) o;
		int c = name.compareTo(m.name);
		if (c != 0) return c;
		if ("".equals(name)) return 0;
		if (isNegation != m.isNegation()) return (isNegation) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if ("".equals(name)) {
			result = prime * result + 1237;
		} else {
			result = prime * result + (isNegation ? 1231 : 1237);
		}
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Mode other = (Mode) obj;
		if (isNegation != other.isNegation) return false;
		return name.equals(other.name);
	}

	public String toString() {
		if ("".equals(name)) return "";
		return MODE_START //
				+ (isNegation ? String.valueOf(MODE_NEGATION_SIGN) : "") //
				+ name //
				+ MODE_END;
	}

}
