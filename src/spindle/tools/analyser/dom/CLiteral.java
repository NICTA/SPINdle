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
package spindle.tools.analyser.dom;

import java.text.DecimalFormat;

import spindle.core.dom.Literal;
import spindle.sys.AppConst;

/**
 * DOM used to represent literals when searching for strongly connected literals in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public class CLiteral extends Literal {

	private static final long serialVersionUID = 1L;
	private static final DecimalFormat formatter = new DecimalFormat("000");

	private int pre = Integer.MIN_VALUE;
	private int groupId = Integer.MIN_VALUE;

	public CLiteral(Literal literal) {
		super(literal);
		setPre(Integer.MIN_VALUE);
		setGroupId(Integer.MIN_VALUE);
	}

	public int getPre() {
		return pre;
	}

	public void setPre(final int pre) {
		this.pre = pre;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(final int groupId) {
		this.groupId = groupId;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());

		if (!AppConst.isDeploy) {
			sb.append("[").append((pre == Integer.MIN_VALUE) ? "-1" : formatter.format(pre)).append(":");
			sb.append((groupId == Integer.MIN_VALUE) ? "-1" : formatter.format(groupId)).append("]");
		}
		return sb.toString();
	}

}
