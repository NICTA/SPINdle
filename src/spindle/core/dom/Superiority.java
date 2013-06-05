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

/**
 * DOM for representing a superiority relation in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.09
 */
public class Superiority implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private static final String RULE_TYPE_SYMBOL = " " + RuleType.SUPERIORITY.getSymbol() + " ";

	private String superior;
	private String inferior;

	public Superiority(final String superior, final String inferior) {
		setSuperior(superior);
		setInferior(inferior);
	}

	public Superiority(Superiority superiority) {
		this(superiority.superior, superiority.inferior);
	}

	public void setSuperior(String superior) {
		this.superior = superior;
	}

	public String getSuperior() {
		return superior;
	}

	public void setInferior(String inferior) {
		this.inferior = inferior;
	}

	public String getInferior() {
		return inferior;
	}

	public Superiority clone() {
		return new Superiority(this);
	}

	public String toString() {
		return superior + RULE_TYPE_SYMBOL + inferior;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Superiority)) return getClass().getName().compareTo(o.getClass().getName());

		Superiority s = (Superiority) o;
		int c = superior.compareTo(s.superior);
		if (c != 0) return c;
		return inferior.compareTo(s.inferior);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (null == o) return false;
		if (getClass() != o.getClass()) return false;

		Superiority s = (Superiority) o;
		return superior.equals(s.superior) && inferior.equals(s.inferior);
	}
}
