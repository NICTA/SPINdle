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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import spindle.sys.AppConst;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing a rule in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2012.08.09
 */
public class Rule implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	protected String label;
	protected String originalLabel;
	protected RuleType ruleType;
	protected Mode mode;
	private long delay = -1;

	protected Set<Literal> body = null;
	protected List<Literal> head = null;

	public Rule(final String label, final RuleType ruleType) {
		this.label = label.trim();
		this.originalLabel = this.label;
		setRuleType(ruleType);

		body = new TreeSet<Literal>();
		head = new ArrayList<Literal>();
		setMode(null);
	}

	public Rule(Rule rule) {
		this(rule.label, rule.ruleType);
		setMode(rule.mode);
		try {
			for (spindle.core.dom.Literal literal : rule.getBodyLiterals()) {
				addBodyLiteral(literal.clone());
			}
			for (spindle.core.dom.Literal literal : rule.getHeadLiterals()) {
				addHeadLiteral(literal.clone());
			}
		} catch (Exception e) {
		}
	}

	public void setHeadLiteralMode(final Mode headLiteralMode) {
		for (Literal literal : head) {
			literal.setMode(headLiteralMode.clone());
		}
	}

	public void addHeadLiteral(final Literal literal) throws RuleException {
		if (null == literal || "".equals(literal.getName())) throw new RuleException(ErrorMessage.LITERAL_NAME_MISSING);

		if ((ruleType != RuleType.DEFEASIBLE) && head.size() > 0) {
			// only defeasible rule is allowed to have
			// more than one head literal
			throw new RuleException(ErrorMessage.RULE_NON_DEFEASIBLE_RULE_WITH_MULTIPLE_HEADS, new Object[] { label });
		} else if (literal instanceof LiteralVariable) {
			throw new RuleException(ErrorMessage.RULE_THEORY_VARIABLE_IN_HEAD, new Object[] { literal });
		} else {
			head.add(literal);
		}
	}

	public void removeHeadLiteral(final Literal literal) {
		for (int i = 0; i < head.size(); i++) {
			if (head.get(i).equals(literal)) {
				head.remove(i);
				return;
			}
		}
	}

	public boolean isHeadLiteral(final Literal literal) {
		return head.contains(literal);
	}

	public List<Literal> getHeadLiterals() {
		List<Literal> literalList = new ArrayList<Literal>();
		for (Literal literal : head) {
			literalList.add(literal.clone());
		}
		return literalList;
	}

	public void addBodyLiteral(final Literal literal) throws RuleException {
		if (null == literal || "".equals(literal.getName())) throw new RuleException(ErrorMessage.LITERAL_NAME_MISSING);
		body.add(literal);
	}

	public void removeBodyLiteral(final Literal literal) {
		body.remove(literal);
	}

	public boolean isBodyLiteral(final Literal literal) {
		return body.contains(literal);
	}

	public List<Literal> getBodyLiterals() {
		List<Literal> literalList = new ArrayList<Literal>();
		if (AppConst.isCloneRuleBodyLiterals) {
			for (Literal bodyLiteral : body) {
				literalList.add(bodyLiteral.clone());
			}
		} else {
			for (Literal bodyLiteral : body) {
				literalList.add(bodyLiteral);
			}
		}
		return literalList;
	}

	public boolean isEmptyBody() {
		return body.size() == 0;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label.trim();
	}

	public String getOriginalLabel() {
		return originalLabel;
	}

	public void setOriginalLabel(String originalLabel) {
		this.originalLabel = originalLabel.trim();
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(final Mode mode) {
		this.mode = (mode == null) ? new Mode("", false) : mode.clone();
	}

	public RuleType getRuleType() {
		return ruleType;
	}

	public void setRuleType(final RuleType ruleType) {
		this.ruleType = ruleType;
	}

	public void setDelay(long delay) {
		this.delay = (delay < 0) ? -1 : delay;
	}

	public long getDelay() {
		return delay;
	}

	public boolean containsTemporalInfo() {
		return delay > 0;
	}

	public boolean isConflictRule(Rule rule) {
		List<Literal> ruleHeadLiteral = rule.getHeadLiterals();
		for (Literal literal : head) {
			for (Literal literalX : ruleHeadLiteral) {
				if (literal.isComplementTo(literalX)) { return true; }
			}
		}
		return false;
	}

	public Set<Literal> getLiteralList() {
		Set<Literal> literalSet = new TreeSet<Literal>();
		literalSet.addAll(getBodyLiterals());
		literalSet.addAll(getHeadLiterals());
		return literalSet;
	}

	public Rule clone() {
		Rule r = DomUtilities.getRule(label, ruleType);
		r.setMode(mode.clone());
		try {
			for (Literal literal : body) {
				r.addBodyLiteral(literal.clone());
			}
			for (Literal literal : head) {
				r.addHeadLiteral(literal.clone());
			}
		} catch (Exception e) {
		}
		return r;
	}

	public List<Mode> getModeUsedInBody() {
		List<Mode> modeList = new ArrayList<Mode>();
		for (Literal literal : body) {
			if (!"".equals(literal.getMode().getName())) {
				if (!modeList.contains(literal.getMode())) modeList.add(literal.getMode().clone());
			}
		}
		return modeList;
	}

	public List<Mode> getModeUsedInHead() {
		List<Mode> modeList = new ArrayList<Mode>();
		for (Literal literal : head) {
			if (!"".equals(literal.getMode().getName())) {
				if (!modeList.contains(literal.getMode())) modeList.add(literal.getMode().clone());
			}
		}
		return modeList;
	}

	public Rule cloneWithModeChange(final Mode toMode) throws RuleException {
		if ("".equals(toMode)) throw new RuleException("invalid mode [" + toMode + "] to convert");

		boolean isModified = false;

		Rule newRule = clone();
		List<Mode> modeUsedInBody = getModeUsedInBody();
		if (modeUsedInBody.size() == 0) {
		} else {
			for (Literal literal : newRule.body) {
				if (!toMode.equals(literal.getMode())) throw new RuleModeConversionException(
						"inconsistent mode found in rule body");
			}
		}
		if (!toMode.equals(newRule.getMode())) {
			newRule.setMode(mode.clone());
			isModified = true;
		}
		for (Literal literal : newRule.head) {
			if (!"".equals(literal.getMode().toString()) && !toMode.equals(literal.getMode())) {
				literal.setMode(toMode.clone());
				isModified = true;
			}
		}

		if (!isModified) throw new RuleModeConversionException("no mode is modified");
		return newRule;
	}

	public String getRuleAsString() {
		char LITERAL_SEPARATOR = DomConst.Literal.LITERAL_SEPARATOR;

		int c = 0;
		StringBuilder sb = new StringBuilder();
		if (ruleType == RuleType.FACT) {
			// do nothing for the body part
		} else {
			if (body.size() > 0) {
				c = 0;
				for (Literal literal : body) {
					if (c > 0) sb.append(LITERAL_SEPARATOR);
					sb.append(literal.toString());
					c++;
				}
				sb.append(" ");
			}
		}
		sb.append(ruleType.getSymbol());
		if (delay < 0) {
			sb.append(" ");
		} else {
			sb.append("{").append(delay).append("} ");
		}
		if (head.size() > 0) {
			c = 0;
			for (Literal literal : head) {
				if (c > 0) sb.append(LITERAL_SEPARATOR);
				sb.append(literal.toString());
				c++;
			}
		}
		return sb.toString();
	}

	public void updateLiteralPredicatesValues(Map<String, String> predicateValues) {
		if (body.size() > 0) {
			for (Literal literal : body) {
				literal.updatePredicatesValues(predicateValues);
			}
		}
		if (head.size() > 0) {
			for (Literal literal : head) {
				literal.updatePredicatesValues(predicateValues);
			}
		}
	}

	public Rule cloneWithUpdatePredicatesValues(Map<String, String> predicateValues) {
		Rule r = DomUtilities.getRule(label, ruleType);
		r.setMode(mode.clone());
		try {
			for (Literal literal : body)
				r.body.add(literal.cloneWithUpdatePredicatesValues(predicateValues));
			for (Literal literal : head)
				r.addHeadLiteral(literal.cloneWithUpdatePredicatesValues(predicateValues));
		} catch (Exception e) {
		}
		return r;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(label);
		if (!"".equals(mode.getName())) sb.append(mode);
		sb.append("]\t").append(getRuleAsString());
		return sb.toString();
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Rule)) return getClass().getName().compareTo(o.getClass().getName());

		Rule r = (Rule) o;
		int c = mode.compareTo(r.mode);
		if (c != 0) return c;
		c = body.size() - r.body.size();
		if (c != 0) return c;
		Iterator<Literal> it = body.iterator();
		Iterator<Literal> itr = r.body.iterator();
		Literal l, lr;
		while (it.hasNext()) {
			l = it.next();
			lr = itr.next();
			c = l.compareTo(lr);
			if (c != 0) return c;
		}
		c = head.size() - r.head.size();
		if (c != 0) return c;
		for (int i = 0; i < head.size(); i++) {
			c = head.get(i).compareTo(r.head.get(i));
			if (c != 0) return c;
		}
		return 0;
	}

	@Override
	public int hashCode() {
		return label.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof Rule)) return false;

		Rule r = (Rule) o;
		if (!ruleType.equals(r.ruleType)) return false;
		if (!mode.equals(r.mode)) return false;
		if (body.size() != r.body.size()) return false;
		if (head.size() != r.head.size()) return false;
		if (body.size() > 0) {
			for (Literal literal : body) {
				if (!r.body.contains(literal)) return false;
			}
		}
		for (int i = 0; i < head.size(); i++) {
			if (!head.get(i).equals(r.head.get(i))) return false;
		}
		return true;
	}
}
