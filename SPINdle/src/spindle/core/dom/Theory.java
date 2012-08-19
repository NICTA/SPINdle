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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import com.app.utils.Utilities.ProcessStatus;

import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing a defeasible theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.08
 */
public class Theory extends TheoryCore implements Cloneable {

	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_RULE_LABEL_PREFIX = "Rule_";
	public static final DecimalFormat formatter = new DecimalFormat("00000");
	private Map<String, Integer> rulesCounter = null;

	private final Map<String, Set<String>> strongerModeSet = new TreeMap<String, Set<String>>();
	private final Map<String, Set<String>> allModeConflictRulesResolved = new TreeMap<String, Set<String>>();

	protected Map<Literal, List<Literal>> conflictLiteralsStore;
	private Map<String, Set<String>> ruleLabelMapping;

	public Theory() {
		this(null);
	}

	public Theory(Theory theory) {
		super(theory);
		rulesCounter = new HashMap<String, Integer>();
		rulesCounter.put(DEFAULT_RULE_LABEL_PREFIX, 0);
		conflictLiteralsStore = new TreeMap<Literal, List<Literal>>();
	}

	/**
	 * Return a duplicated copy of the theory.
	 * 
	 * @return cloned theory.
	 */
	public Theory clone() {
		return new Theory(this);
	}

	/**
	 * Return an unique new rule label.
	 * 
	 * @return An unique rule label.
	 * @see #getUniqueRuleLabel(String)
	 */
	public String getUniqueRuleLabel() {
		return getUniqueRuleLabel(DEFAULT_RULE_LABEL_PREFIX);
	}

	/**
	 * Return an unique new rule label with the specified prefix.
	 * 
	 * @return An unique rule label with the specified prefix.
	 * @see #getUniqueRuleLabel()
	 */
	public String getUniqueRuleLabel(final String prefix) {
		String id;
		int ruleCounter = (rulesCounter.containsKey(prefix) ? rulesCounter.get(prefix) : 0);
		do {
			id = DEFAULT_RULE_LABEL_PREFIX + formatter.format(ruleCounter++);
		} while (factsAndAllRules.containsKey(id));
		rulesCounter.put(prefix, ruleCounter);
		return id;
	}

	public void updateRuleLabelMapping(Map<String, List<Rule>> ruleLabelMapping) {
		if (null == ruleLabelMapping || ruleLabelMapping.size() == 0) return;
		if (this.ruleLabelMapping == null) this.ruleLabelMapping = new TreeMap<String, Set<String>>();
		for (Entry<String, List<Rule>> entry : ruleLabelMapping.entrySet()) {
			Set<String> ruleLabels = new TreeSet<String>();
			for (Rule rule : entry.getValue()) {
				ruleLabels.add(rule.getLabel());
			}
			this.ruleLabelMapping.put(entry.getKey(), ruleLabels);
		}
	}

	/**
	 * Update the theory by adding new rules, removing old rules, and updating the superiority relations with the
	 * old-new rule mapping.
	 * 
	 * @param rulesToAdd New rules to be added to the theory.
	 * @param rulesToDelete Old rules to be removed from the theory
	 * @throws TheoryException
	 */
	public void updateTheory(final List<Rule> rulesToAdd, //
			final Set<String> rulesToDelete, //
			Map<String, List<Rule>> oldNewRuleMapping) throws TheoryException {
		if (null != rulesToDelete) {
			for (String ruleLabel : rulesToDelete) {
				removeRule(ruleLabel);
			}
		}
		if (null != rulesToAdd) {
			for (Rule rule : rulesToAdd) {
				addRule(rule);
			}
		}
		if (null != oldNewRuleMapping && oldNewRuleMapping.size() > 0) {
			System.out.println(oldNewRuleMapping.toString());
			try {
				updateSuperiorityMapping(oldNewRuleMapping);
			} catch (TheoryException e) {
				throw new TheoryException("Old-New rule mapping exception appeared while updating the theory!", e);
			}
		}
	}

	
	/**
	 * Update the superior rules and inference rules counters of each rules.
	 * @throws TheoryException
	 */
	public void updateRuleSuperiorityRelationCounter() throws TheoryException {
		if (superiors.size() == 0) return;
		switch (Conf.getReasonerVersion()) {
		case 2:
			for (Entry<String, Set<Superiority>> entry : superiors.entrySet()) {
				RuleExt superiorRule = (RuleExt) factsAndAllRules.get(entry.getKey());
				if (null == superiorRule) throw new RuleException(ErrorMessage.SUPERIORITY_SUPERIOR_RULE_NOT_DEFINED,
						new Object[] { entry.getKey() });
				superiorRule.resetRuleSuperiorityRelationCounter();
				for (Superiority sup : entry.getValue()) {
					RuleExt inferiorRule = (RuleExt) factsAndAllRules.get(sup.getInferior());
					if (null == inferiorRule) throw new RuleException(
							ErrorMessage.SUPERIORITY_INFERIOR_RULE_NOT_DEFINED, new Object[] { sup.getInferior() });
					inferiorRule.resetRuleSuperiorityRelationCounter();
				}
			}
			for (Entry<String, Set<Superiority>> superiorityEntry : superiors.entrySet()) {
				RuleExt superiorRule = (RuleExt) factsAndAllRules.get(superiorityEntry.getKey());
				superiorRule.setWeakerRulesCount(superiorityEntry.getValue().size());
				for (Superiority superiority : superiorityEntry.getValue()) {
					RuleExt inferiorRule = (RuleExt) factsAndAllRules.get(superiority.getInferior());
					inferiorRule.strongerRulesCountIncrement();
				}
			}
			break;
		default:
		}
	}

	/**
	 * Update rule mapping using the normalized new rule labels
	 * 
	 * @param oldNewRuleMapping Mapping between the old and new rules.
	 * @return
	 */
	private ProcessStatus updateSuperiorityMapping( //
			Map<String, List<Rule>> oldNewRuleMapping) throws TheoryException {
		List<Superiority> originalSuperiorityList = null;

		for (String originalLabel : oldNewRuleMapping.keySet()) {
			originalSuperiorityList = new ArrayList<Superiority>();
			if (superiors.containsKey(originalLabel)) originalSuperiorityList.addAll(superiors.get(originalLabel));
			if (inferiors.containsKey(originalLabel)) originalSuperiorityList.addAll(inferiors.get(originalLabel));

			if (originalSuperiorityList.size() > 0) {
				superiors.remove(originalLabel);
				inferiors.remove(originalLabel);

				List<Superiority> supToAdd = new ArrayList<Superiority>();
				List<Superiority> supToDelete = new ArrayList<Superiority>();

				for (Superiority origSuperiority : originalSuperiorityList) {
					String supLabel = origSuperiority.getSuperior();
					String infLabel = origSuperiority.getInferior();

					List<Rule> supRules = oldNewRuleMapping.get(supLabel);
					if (null == supRules) {
						supRules = new ArrayList<Rule>();
						Rule r = factsAndAllRules.get(supLabel);
						if (null == r) throw new TheoryException(ErrorMessage.SUPERIORITY_SUPERIOR_RULE_NOT_DEFINED,
								new Object[] { supLabel });
						supRules.add(r);
					} else {
						if (!supToDelete.contains(origSuperiority)) supToDelete.add(origSuperiority);
					}

					List<Rule> infRules = oldNewRuleMapping.get(infLabel);
					if (null == infRules) {
						infRules = new ArrayList<Rule>();
						Rule r = factsAndAllRules.get(infLabel);
						if (null == r) throw new TheoryException(ErrorMessage.SUPERIORITY_INFERIOR_RULE_NOT_DEFINED,
								new Object[] { infLabel });
						infRules.add(r);
					} else {
						if (!supToDelete.contains(origSuperiority)) supToDelete.add(origSuperiority);
					}

					for (Rule newSupRule : supRules) {
						System.out.println(newSupRule.toString());
						for (Rule inferiorRule : infRules) {
							System.out.println("  "+inferiorRule);
							System.out.println("    "+newSupRule.isConflictRule(inferiorRule));
							if (newSupRule.isConflictRule(inferiorRule)) {
								supToAdd.add(new Superiority(newSupRule.getLabel(), inferiorRule.getLabel()));
							}
						}
					}
				}

				for (Superiority s : supToDelete)
					remove(s);
				for (Superiority s : supToAdd)
					add(s);
			}
		}
		return ProcessStatus.SUCCESS;
	}

	public boolean containsUnprovedRule(final Literal literal, final RuleType ruleType) {
		Map<String, Rule> rules = getRules(literal);
		if (null == rules || rules.size() == 0) return false; // literal does not appear in theory

		if (null == ruleType) {
			for (Rule rule : rules.values()) {
				if (!rule.isEmptyBody() && rule.isHeadLiteral(literal)) return true;
			}
		} else {
			for (Rule rule : rules.values()) {
				if (ruleType == rule.getRuleType() && !rule.isEmptyBody() && rule.isHeadLiteral(literal)) return true;
			}
		}
		return false;
	}

	/**
	 * Return the set of rules contain the set of literals specified.
	 * Note that the literals specified may appear in the body or head of the rules returned.
	 * 
	 * @param literals literals to be contained in rules.
	 * @return A set of rule labels for the set of rules contain the set of literals specified.
	 * @see #getRulesWithHead(Literal)
	 * @see #getRulesWithBody(Set)
	 * @see #getRulesWithBody(Set, int)
	 */
	public Set<String> getRules(Set<Literal> literals) {
		Set<String> rules = new TreeSet<String>();
		if (null == literals || literals.size() == 0) return rules;
		for (Literal literal : literals) {
			rules.addAll(getRules(literal).keySet());
		}
		return rules;
	}

	/**
	 * Return the set of rules with the specified literals appear in the body and retrieve its set of dependency rules
	 * that appear in the theory.
	 * 
	 * @param literals Literals that appeared in the body of rules.
	 * @return A set of rules with the specified literals appeared in the body.
	 * @see #getRules(Literal)
	 * @see #getRulesWithHead(Literal)
	 * @see #getRulesWithBody(Set, int)
	 */
	public Set<String> getRulesWithBody(Set<Literal> literals) {
		return getRulesWithBody(literals, -1);
	}

	/**
	 * Return the set of rules with the specified literals appear in the body and retrieve its set of dependency rules
	 * that appear in the theory with the depth specified.
	 * 
	 * @param literals Literals that appeared in the body of rules.
	 * @param maxDepth Depth of inference to be traversed. If maxDepth is negative, then the rules will be traversed
	 *            until it is exhausted.
	 * @return A set of rules with the specified literals appeared in the body.
	 * @see #getRules(Literal)
	 * @see #getRulesWithHead(Literal)
	 * @see #getRulesWithBody(Set)
	 */
	public Set<String> getRulesWithBody(Set<Literal> literals, int maxDepth) {
		Set<String> rulesExtracted = new TreeSet<String>();
		if (null == literals || literals.size() == 0) return rulesExtracted;

		Set<Literal> literalsToCheck = new TreeSet<Literal>();
		Set<Literal> literalsChecked = new TreeSet<Literal>();
		Set<Literal> literalsToAdd = new TreeSet<Literal>();

		for (Literal literal : getLiteralsToCheck(literals)) {
			Map<String, Rule> rules = getRules(literal);
			for (Rule rule : rules.values()) {
				if (rule.isBodyLiteral(literal)) {
					rulesExtracted.add(rule.getLabel());
					literalsToCheck.addAll(getLiteralsToCheck(rule.getHeadLiterals()));
				}
			}
		}

		if (0 == maxDepth) return rulesExtracted;

		int currDepth = 0;
		do {
			literalsToCheck.addAll(literalsToAdd);
			literalsChecked.addAll(literalsToAdd);
			literalsToAdd.clear();
			for (Literal literal : literalsToCheck) {
				Map<String, Rule> rules = getRules(literal);
				for (Rule rule : rules.values()) {
					if (!rulesExtracted.contains(rule.getLabel())) {
						if (rule.isHeadLiteral(literal)) {
							rulesExtracted.add(rule.getLabel());
						} else {
							for (Literal hl : rule.getHeadLiterals()) {
								if (!literalsChecked.contains(hl)) {
									literalsToAdd.addAll(getLiteralsToCheck(hl));
								}
							}
						}
					}
				}
			}
		} while ((maxDepth < 0 || ++currDepth < maxDepth) && literalsToAdd.size() > 0);

		return rulesExtracted;
	}

	/**
	 * Return the set of rules with the specified literals appear in the head.
	 * 
	 * @param literal Literal that appeared in the head of rules.
	 * @return A set of rules with the specified literals appeared in the head.
	 * @see #getRules(Literal)
	 * @see #getRulesWithBody(Set)
	 * @see #getRulesWithBody(Set, int)
	 */
	public Set<Rule> getRulesWithHead(Literal literal) {
		Set<Rule> rules = new HashSet<Rule>();

		for (Rule rule : getRules(literal).values()) {
			if (rule.isHeadLiteral(literal)) rules.add(rule);
		}
		return rules;
	}

	public boolean containsInRuleHead(final Literal literal, final RuleType ruleType) {
		Collection<Rule> rules = getRules(literal).values();
		if (rules == null || rules.size() == 0) return false; // literal does not appear in theory

		if (null == ruleType) {
			for (Rule rule : rules) {
				if (rule.isHeadLiteral(literal)) return true;
			}
		} else {
			for (Rule rule : rules) {
				if (ruleType == rule.getRuleType() && rule.isHeadLiteral(literal)) return true;
			}
		}
		return false;
	}

	public List<Rule> duplicateRulesToType(Collection<Rule> rules, RuleType ruleType, final String rulePostfix) {
		List<Rule> newRules = new ArrayList<Rule>();
		for (Rule rule : rules) {
			Rule newRule = rule.clone();
			newRule.setLabel(rule.getLabel() + rulePostfix);
			newRule.setOriginalLabel(rule.getOriginalLabel());
			newRule.setRuleType(ruleType);
			newRules.add(newRule);
		}
		return newRules;
	}

	public List<Literal> getConflictLiterals(Literal literal) {
		if (isConflictRulesModified() || isConversionRulesModified()) {
			updateModeConversionConflictRules();
		}

		List<Literal> conflictLiterals = conflictLiteralsStore.get(literal);
		if (null != conflictLiterals) return conflictLiterals;

		conflictLiterals = new Vector<Literal>();

		Literal literalComplement = literal.getComplementClone();
		conflictLiterals.add(literalComplement);

		Mode literalMode = literal.getMode();
		if (!"".equals(literalMode.getName())) {
			Set<String> modeList = allModeConflictRulesResolved.get(literalMode.getName());
			if (null != modeList) {
				for (String modeStr : modeList) {
					Literal conflictLiteral = literalComplement.clone();
					conflictLiteral.setMode(new Mode(modeStr, literalMode.isNegation()));
					conflictLiterals.add(conflictLiteral);
					
					Literal conflictLiteral2=literal.clone();
					conflictLiteral2.setMode(new Mode(modeStr,!literalMode.isNegation()));
					conflictLiterals.add(conflictLiteral2);
				}
			}
			Literal conflictLiteral = literal.clone();
			conflictLiteral.setMode(literalMode.getComplementClone());
			conflictLiterals.add(conflictLiteral);
		}
		conflictLiteralsStore.put(literal, conflictLiterals);
		return conflictLiterals;
	}

	public Map<String, Set<String>> getStrongModeSet() {
		if (isConflictRulesModified() || isConversionRulesModified()) {
			updateModeConversionConflictRules();
		}
		return strongerModeSet;
	}

	private void updateModeConversionConflictRules() {
		synchronized (this) {
			generateStrongModeSet();
			updateConflictModeRules();
			resetConversionRulesModified();
			resetConflictRulesModified();
			conflictLiteralsStore.clear();
		}
	}

	private void generateStrongModeSet() {
		strongerModeSet.clear();
		strongerModeSet.putAll(getAllModeConflictRules());

		boolean isModified = false;
		List<String> modeToAdd = null;
		do {
			isModified = false;
			for (Entry<String, Set<String>> entry : strongerModeSet.entrySet()) {
				Set<String> mList = entry.getValue();
				modeToAdd = new Vector<String>();
				for (String weakerMode : mList) {
					if (strongerModeSet.containsKey(weakerMode)) {
						Set<String> weakerModeList = strongerModeSet.get(weakerMode);
						for (String m : weakerModeList) {
							if (!mList.contains(m)) modeToAdd.add(m);
						}
					}
				}
				if (modeToAdd.size() > 0) {
					mList.addAll(modeToAdd);
					isModified = true;
				}
			}
		} while (isModified);
	}

	private void updateConflictModeRules() {
		allModeConflictRulesResolved.clear();
		Map<String, Set<String>> modeConflictRules = strongerModeSet;
		for (Entry<String, Set<String>> entry : modeConflictRules.entrySet()) {
			String mode = entry.getKey();
			for (String conflictMode : entry.getValue()) {
				addResolvedConflictRule(mode, conflictMode);
				addResolvedConflictRule(conflictMode, mode);
			}
		}
	}

	private void addResolvedConflictRule(final String mode, final String conflictMode) {
		Set<String> conflictModeList = allModeConflictRulesResolved.get(mode);
		if (null == conflictModeList) {
			conflictModeList = new TreeSet<String>();
			allModeConflictRulesResolved.put(mode, conflictModeList);
		}
		if (!conflictModeList.contains(conflictMode)) conflictModeList.add(conflictMode);
	}

	private Set<Literal> getLiteralsToCheck(Collection<Literal> literals) {
		Set<Literal> literalsToCheck = new TreeSet<Literal>();
		for (Literal literal : literals) {
			literalsToCheck.addAll(getLiteralsToCheck(literal));
		}
		return literalsToCheck;
	}

	private Set<Literal> getLiteralsToCheck(Literal literal) {
		String literalModeName = literal.getMode().getName();

		Set<Literal> literalsToCheck = new TreeSet<Literal>();

		literalsToCheck.add(literal.clone());

		if ("".equals(literalModeName)) {
			literalsToCheck.add(literal.getComplementClone());
		} else {
			Set<String> conversionRules = getModeConversionRules(literalModeName);
			if (null == conversionRules) {
				literalsToCheck.addAll(getConflictLiterals(literal));
			} else {
				for (String modeName : conversionRules) {
					Literal nl = literal.clone();
					Mode mode = nl.getMode().clone();
					mode.setName(modeName);
					nl.setMode(mode);

					literalsToCheck.add(nl);
					literalsToCheck.addAll(getConflictLiterals(nl));
				}
			}
		}

		return literalsToCheck;
	}

	/**
	 * Return the set of rules that are used to derive the literals specified.
	 * 
	 * @param literals literals to check
	 * @return A set of rule labels that indicating the set of rules that are required to derive the set of literals
	 *         specified.
	 */
	public Set<String> getRulesToDerive(Set<Literal> literals) {
		Set<Literal> literalsChecked = new TreeSet<Literal>();
		Set<Literal> literalsToCheck = new TreeSet<Literal>();
		Set<Literal> literalsToAdd = new TreeSet<Literal>();
		Set<String> rulesExtracted = new TreeSet<String>();

		literalsToAdd.addAll(getLiteralsToCheck(literals));

		do {
			literalsChecked.addAll(literalsToAdd);
			literalsToCheck.addAll(literalsToAdd);
			literalsToAdd.clear();
			for (Literal literalToCheck : literalsToCheck) {
				for (Rule rule : getRulesWithHead(literalToCheck)) {
					if (!rulesExtracted.contains(rule.getLabel())) {
						rulesExtracted.add(rule.getLabel());
						for (Literal bodyLiteral : rule.getBodyLiterals()) {
							if (!literalsToAdd.contains(bodyLiteral)) {
								literalsToAdd.addAll(getLiteralsToCheck(bodyLiteral));
							}
						}
					}
				}
			}
		} while (literalsToAdd.size() > 0);

		return rulesExtracted;
	}

	/**
	 * Return the set of rules after excluding the rule set specified from the theory.
	 * 
	 * @param excludedRules The set of rules to be excluded.
	 * @return A set of rule labels after excluding the rules specified.
	 */
	public Set<String> getRulesExclude(Set<String> excludedRules) {
		Set<String> ruleLabels = new TreeSet<String>(factsAndAllRules.keySet());
		ruleLabels.removeAll(excludedRules);
		return ruleLabels;
	}

	/**
	 * Create a new theory using the rule specified.
	 * 
	 * @param rules The set of rules to be used.
	 * @return A new theory that contains the set of rules (including facts, all types of rules, superiorities, etc)
	 *         that are specified.
	 * @see #getRulesToDerive(Set literals)
	 */
	public Theory createNewTheoryWithRules(Set<String> rules) throws TheoryException {
		Theory newTheory = new Theory();

		// add rules to theory
		for (String ruleLabel : rules) {
			Rule rule = getRule(ruleLabel);
			newTheory.addRule(rule.clone());

			Set<Superiority> superiorities = getSuperior(ruleLabel);
			if (null != superiorities) {
				for (Superiority sup : superiorities) {
					if (rules.contains(sup.getInferior())) newTheory.add(sup.clone());
				}
			}

			Set<Superiority> inferiorities = getInferior(ruleLabel);
			if (null != inferiorities) {
				for (Superiority sup : inferiorities) {
					if (rules.contains(sup.getSuperior())) newTheory.add(sup.clone());
				}
			}
		}

		// add all literal variables to theory
		for (Entry<LiteralVariable, LiteralVariable> entry : literalVariables.entrySet()) {
			newTheory.addLiteralVariable(entry.getKey().clone(), entry.getValue().clone());
		}

		// add all literal boolean functions to theory
		for (Entry<LiteralVariable, LiteralVariable> entry : literalBooleanFunctions.entrySet()) {
			newTheory.addLiteralVariable(entry.getKey().clone(), entry.getValue().clone());
		}

		// add mode conversion rules to theory
		for (Entry<String, Set<String>> entry : modeConversionRules.entrySet()) {
			String[] modeRules = new String[entry.getValue().size()];
			entry.getValue().toArray(modeRules);
			newTheory.addModeConversionRules(entry.getKey(), modeRules);
		}

		// add mode conflict rules to theory
		for (Entry<String, Set<String>> entry : modeConflictRules.entrySet()) {
			String[] modeRules = new String[entry.getValue().size()];
			entry.getValue().toArray(modeRules);
			newTheory.addModeConflictRules(entry.getKey(), modeRules);
		}

		return newTheory;
	}
}
