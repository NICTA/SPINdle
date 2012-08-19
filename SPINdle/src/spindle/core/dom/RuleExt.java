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

import spindle.sys.AppConst;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing a rule in theory with number of stronger rules and number of weaker rules included (for
 * reasoner ver. 2.0.0 or above).
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 */
public class RuleExt extends spindle.core.dom.Rule {

	private static final long serialVersionUID = 1L;

	private int strongerRulesCount = 0;
	private int weakerRulesCount = 0;

	public RuleExt(String label, RuleType ruleType) {
		super(label, ruleType);
	}

	public RuleExt(RuleExt rule) {
		super(rule);
		strongerRulesCount = rule.strongerRulesCount;
		weakerRulesCount = rule.weakerRulesCount;
	}

	public Rule clone() {
		return new RuleExt(this);
	}

	public boolean isActive() {
		return (body.size() == 0 && strongerRulesCount == 0);
	}

	public int getStrongerRulesCount() {
		return strongerRulesCount;
	}

	public void setStrongerRulesCount(final int strongerRulesCount) {
		this.strongerRulesCount = strongerRulesCount;
	}

	public void strongerRulesCountIncrement() {
		strongerRulesCount++;
	}

	public void strongerRulesCountDecrement() throws RuleException {
		if (strongerRulesCount > 0) {
			strongerRulesCount--;
		} else throw new RuleException(ErrorMessage.RULE_NO_STRONGER_RULE_EXISTS, new Object[] { label });
	}

	public int getWeakerRulesCount() {
		return weakerRulesCount;
	}

	public void setWeakerRulesCount(final int weakerRulesCount) {
		this.weakerRulesCount = weakerRulesCount;
	}

	public void weakerRulesCountIncrement() {
		weakerRulesCount++;
	}

	public void weakerRulesCountDecrement() throws RuleException {
		if (weakerRulesCount > 0) {
			weakerRulesCount--;
		} else throw new RuleException(ErrorMessage.RULE_NO_WEAKER_RULE_EXISTS, new Object[] { label });
	}

	public void resetRuleSuperiorityRelationCounter() {
		strongerRulesCount = 0;
		weakerRulesCount = 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(label);
		if (!"".equals(mode.getName())) sb.append(mode);
		if (!AppConst.isDeploy) {
			sb.append(" (").append(strongerRulesCount).append(",").append(weakerRulesCount)//
					.append(",").append(isActive()).append(")");
		}
		sb.append("]\t").append(getRuleAsString());
		return sb.toString();
	}

}
