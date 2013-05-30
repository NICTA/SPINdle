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
package spindle.tools.explanation;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.RuleType;
import spindle.sys.AppConst;

public class InferenceLogItem implements Comparable<Object> {
	private String ruleLabel;

	private Map<RuleType, Set<Conclusion>> conclusions;
	private Map<RuleType, RuleInferenceStatus> ruleInferenceStatuses;
	private Set<Literal> literals;

	public InferenceLogItem(String ruleLabel) {
		setRuleLabel(ruleLabel);
		conclusions = new TreeMap<RuleType, Set<Conclusion>>();
		ruleInferenceStatuses = new TreeMap<RuleType, RuleInferenceStatus>();

		literals = new TreeSet<Literal>();
	}

	public String getRuleLabel() {
		return ruleLabel;
	}

	public void setRuleLabel(String ruleLabel) {
		this.ruleLabel = ruleLabel;
	}

	private RuleType getRuleType(ConclusionType conclusionType) {
		switch (conclusionType) {
		case DEFINITE_PROVABLE:
		case DEFINITE_NOT_PROVABLE:
			return RuleType.STRICT;
		case DEFEASIBLY_PROVABLE:
		case DEFEASIBLY_NOT_PROVABLE:
			return RuleType.DEFEASIBLE;
		default:
			throw new UnsupportedOperationException("Rule type not supported");
		}
	}

	public void addRuleInfernceItem(Conclusion conclusion, RuleInferenceStatus ruleInferenceStatus) {
		RuleType ruleType = getRuleType(conclusion.getConclusionType());
		addConclusion(ruleType, conclusion);
		updateRuleInferenceStatus(conclusion, ruleType, ruleInferenceStatus);
	}

	public void addRuleInferenceItem(RuleType ruleType, ConclusionType conclusionType, Literal literal,
			RuleInferenceStatus ruleInferenceStatus) {
		Conclusion conclusion = new Conclusion(conclusionType, literal);
		addConclusion(ruleType, conclusion);
		updateRuleInferenceStatus(conclusion, ruleType, ruleInferenceStatus);
	}

	private void addConclusion(RuleType ruleType, Conclusion conclusion) {
		Set<Conclusion> conclusionSet = conclusions.get(ruleType);
		if (null == conclusionSet) {
			conclusionSet = new TreeSet<Conclusion>();
			conclusions.put(ruleType, conclusionSet);
		}
		conclusionSet.add(conclusion);

		literals.add(conclusion.getLiteral());
	}

	public Map<RuleType, RuleInferenceStatus> getRuleInferenceStatuses() {
		return ruleInferenceStatuses;
	}

	public RuleInferenceStatus getRuleInferenceStatus(ConclusionType conclusionType) {
		return getRuleInferenceStatus(getRuleType(conclusionType));
	}

	public RuleInferenceStatus getRuleInferenceStatus(RuleType ruleType) {
		return ruleInferenceStatuses.get(ruleType);
	}

	private void updateRuleInferenceStatus(Conclusion conclusion, RuleType ruleType,
			RuleInferenceStatus ruleInferenceStatus) {
		if (!ruleInferenceStatuses.containsKey(ruleType)) {
			ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
		} else {
			RuleInferenceStatus origInferenceStatus = ruleInferenceStatuses.get(ruleType);

			if (conclusion.getLiteral().isPlaceHolder()) {
				switch (conclusion.getConclusionType()) {
				case DEFEASIBLY_NOT_PROVABLE:
				case DEFINITE_NOT_PROVABLE:
					if (origInferenceStatus.compareTo(ruleInferenceStatus) < 0) {
						if (!origInferenceStatus.equals(RuleInferenceStatus.APPICABLE)) {
							ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
						}
					}
					break;
				default:
					if (origInferenceStatus.compareTo(ruleInferenceStatus) < 0) {
						if (!origInferenceStatus.equals(RuleInferenceStatus.APPICABLE)) {
							ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
						}
					}
				}
			} else {
				switch (conclusion.getConclusionType()) {
				case DEFEASIBLY_NOT_PROVABLE:
				case DEFINITE_NOT_PROVABLE:
					if (origInferenceStatus.compareTo(ruleInferenceStatus) > 0) {
						ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
					}
					break;
				default:
					if (origInferenceStatus.compareTo(ruleInferenceStatus) < 0) {
						ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
					}
					// ruleInferenceStatuses.put(ruleType, ruleInferenceStatus);
				}
			}
		}
	}

	public Set<Literal> getLiteralsIncluded() {
		return literals;
	}

	public boolean contains(Literal literal) {
		return literals.contains(literal);
	}

	public boolean contains(Conclusion conclusion) {
		RuleType ruleType = getRuleType(conclusion.getConclusionType());
		if (!conclusions.containsKey(ruleType)) return false;
		return conclusions.get(ruleType).contains(conclusion);
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;

		if (!(o instanceof InferenceLogItem)) getClass().getName().compareTo(o.getClass().getName());

		InferenceLogItem item = (InferenceLogItem) o;
		int c = ruleLabel.compareTo(item.ruleLabel);
		if (c != 0) return c;

		c = ruleInferenceStatuses.size() - item.ruleInferenceStatuses.size();
		if (c != 0) return c;
		if (ruleInferenceStatuses.size() == 0) return 0;

		if (ruleInferenceStatuses.containsKey(RuleType.STRICT)
				&& !item.ruleInferenceStatuses.containsKey(RuleType.STRICT)) return Integer.MIN_VALUE;
		if (ruleInferenceStatuses.containsKey(RuleType.DEFEASIBLE)
				&& !item.ruleInferenceStatuses.containsKey(RuleType.DEFEASIBLE)) return Integer.MAX_VALUE;

		for (Entry<RuleType, Set<Conclusion>> entry : conclusions.entrySet()) {
			RuleType ruleType = entry.getKey();

			c = ruleInferenceStatuses.get(ruleType).compareTo(item.ruleInferenceStatuses.get(ruleType));
			if (c != 0) return c;

			Set<Conclusion> conclusionSet = entry.getValue();
			Set<Conclusion> iconclusionSet = item.conclusions.get(ruleType);

			c = conclusionSet.size() - iconclusionSet.size();
			if (c != 0) return c;

			Iterator<Conclusion> it = conclusionSet.iterator();
			Iterator<Conclusion> iit = iconclusionSet.iterator();
			while (it.hasNext()) {
				Conclusion cc = it.next();
				Conclusion cci = iit.next();
				c = cc.compareTo(cci);
				if (c != 0) return c;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (null == o) return false;
		if (this == o) return true;
		if (getClass() != o.getClass()) return false;

		InferenceLogItem item = (InferenceLogItem) o;
		if (!ruleLabel.equals(item.ruleLabel)) return false;

		for (Entry<RuleType, Set<Conclusion>> entry : conclusions.entrySet()) {
			RuleType ruleType = entry.getKey();
			if (!item.ruleInferenceStatuses.containsKey(ruleType)) return false;
			if (!ruleInferenceStatuses.get(ruleType).equals(item.ruleInferenceStatuses.get(ruleType))) return false;

			Set<Conclusion> conclusionSet = entry.getValue();
			Set<Conclusion> iconclusionSet = item.conclusions.get(ruleType);
			if (conclusionSet.size() != iconclusionSet.size()) return false;
			for (Conclusion conclusion : conclusionSet) {
				if (!iconclusionSet.contains(conclusion)) return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ruleLabel);
		for (Entry<RuleType, Set<Conclusion>> entry : conclusions.entrySet()) {
			RuleType ruleType = entry.getKey();
			sb.append("\n").append(AppConst.LIST_SYMBOL).append("[").append(ruleType).append("] ")
					.append(ruleInferenceStatuses.get(ruleType).getName()) //
					.append(" :- ").append(entry.getValue());
		}
		return sb.toString();
	}

}
