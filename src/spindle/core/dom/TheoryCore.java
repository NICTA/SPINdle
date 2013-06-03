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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.app.utils.FileManager;
import com.app.utils.Utilities;

import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Base class for defeasible theory (data getter/setter functions).
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public abstract class TheoryCore implements Serializable {

	private static final long serialVersionUID = 1L;
	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	// =====
	// basic theory data
	// - literal variables
	protected Map<LiteralVariable, LiteralVariable> literalVariables = null;
	protected Map<LiteralVariable, LiteralVariable> literalBooleanFunctions = null;
	protected Set<LiteralVariable> literalVariablesInRules = null;
	protected Set<LiteralVariable> literalBooleanFunctionsInRules = null;

	// - rules, superiority relations and their association list
	protected Map<String, Rule> factsAndAllRules = null;
	protected Map<String, Set<Superiority>> superiors = null;
	protected Map<String, Set<Superiority>> inferiors = null;

	protected int superiorityCount = 0;

	protected TheoryType theoryType = TheoryType.SDL;
	protected Map<Literal, Map<String, Rule>> literalRuleAssoList = null;
	protected Map<RuleType, Map<String, Rule>> ruleTypeAssoList = null;

	// - mode conversation and mode conflict rules
	protected Map<String, Set<String>> modeConversionRules = null;
	protected Map<String, Set<String>> modeConflictRules = null;
	private boolean conversionRulesModified = false;
	private boolean conflictRulesModified = false;

	public TheoryCore() {
		literalVariables = new TreeMap<LiteralVariable, LiteralVariable>();
		literalBooleanFunctions = new TreeMap<LiteralVariable, LiteralVariable>();
		literalVariablesInRules = new TreeSet<LiteralVariable>();
		literalBooleanFunctionsInRules = new TreeSet<LiteralVariable>();

		factsAndAllRules = new TreeMap<String, Rule>();

		superiors = new TreeMap<String, Set<Superiority>>();
		inferiors = new TreeMap<String, Set<Superiority>>();

		superiorityCount = 0;

		literalRuleAssoList = new TreeMap<Literal, Map<String, Rule>>();
		ruleTypeAssoList = new TreeMap<RuleType, Map<String, Rule>>();

		modeConversionRules = new TreeMap<String, Set<String>>();
		modeConflictRules = new TreeMap<String, Set<String>>();

		resetConversionRulesModified();
		resetConflictRulesModified();
	}

	public TheoryCore(TheoryCore theory) {
		this();
		if (null == theory) return;
		try {
			for (Entry<LiteralVariable, LiteralVariable> entry : theory.literalVariables.entrySet()) {
				addLiteralVariable(entry.getKey(), entry.getValue());
			}
			for (Entry<LiteralVariable, LiteralVariable> entry : theory.literalBooleanFunctions.entrySet()) {
				addLiteralVariable(entry.getKey(), entry.getValue());
			}
			for (Rule rule : theory.factsAndAllRules.values()) {
				addRule(rule.clone());
			}
			for (Superiority superiority : theory.getAllSuperiority()) {
				add(superiority.clone());
			}
			for (Entry<String, Set<String>> entry : theory.modeConversionRules.entrySet()) {
				Set<String> convertModesSet = entry.getValue();
				String[] convertModes = convertModesSet.toArray(new String[convertModesSet.size()]);
				addModeConversionRules(entry.getKey(), convertModes);
			}
			for (Entry<String, Set<String>> entry : theory.modeConflictRules.entrySet()) {
				Set<String> conflictModesSet = entry.getValue();
				String[] conflictModes = conflictModesSet.toArray(new String[conflictModesSet.size()]);
				addModeConflictRules(entry.getKey(), conflictModes);
			}
		} catch (TheoryException e) {
			e.printStackTrace();
		}
	}

	public void addLiteralVariable(final LiteralVariable varName, final LiteralVariable value) throws TheoryException {
		if (varName.getName().charAt(0) != DomConst.Literal.LITERAL_VARIABLE_PREFIX) throw new TheoryException(
				ErrorMessage.LITERAL_VARIABLE_PREFIX_ERROR, new String[] { varName.toString() });

		if (literalVariables.containsKey(varName)) {
			throw new TheoryException(ErrorMessage.LITERAL_VARIABLE_EXISTS, new Object[] { varName.getName() });
		} else if (literalBooleanFunctions.containsKey(varName)) { throw new TheoryException(
				ErrorMessage.LITERAL_BOOLEAN_FUNCTION_EXISTS, new Object[] { varName.getName() }); }

		if (value.getName().charAt(0) == DomConst.Literal.LITERAL_VARIABLE_PREFIX) {
			literalVariables.put(varName.clone(), value.clone());
		} else if (value.getName().charAt(0) == DomConst.Literal.LITERAL_BOOLEAN_FUNCTION_PREFIX) {
			literalBooleanFunctions.put(varName.clone(), value.clone());
		} else {
			throw new TheoryException(ErrorMessage.LITERAL_VARIABLE_PREFIX_ERROR);
		}
	}

	public void removeLiteralVariable(final LiteralVariable varName) throws TheoryException {
		if (varName.getName().charAt(0) == DomConst.Literal.LITERAL_VARIABLE_PREFIX) {
			literalVariables.remove(varName);
		} else if (varName.getName().charAt(0) == DomConst.Literal.LITERAL_BOOLEAN_FUNCTION_PREFIX) {
			literalBooleanFunctions.remove(varName);
		} else {
			throw new TheoryException(ErrorMessage.LITERAL_VARIABLE_PREFIX_ERROR);
		}
	}

	public Map<LiteralVariable, LiteralVariable> getLiteralVariables() {
		return literalVariables;
	}

	public int getLiteralVariableCount() {
		return literalVariables.size();
	}

	public void clearLiteralVariables() {
		literalVariables.clear();
	}

	public Map<LiteralVariable, LiteralVariable> getLiteralBooleanFunctions() {
		return literalBooleanFunctions;
	}

	public int getLiteralBooleanFunctionCount() {
		return literalBooleanFunctions.size();
	}

	public void clearLiteralBooleanFunctions() {
		literalBooleanFunctions.clear();
	}

	public Set<LiteralVariable> getLiteralVariablesInRules() {
		return literalVariablesInRules;
	}

	public int getLiteralVariablesInRulesCount() {
		return literalVariablesInRules.size();
	}

	public Set<LiteralVariable> getLiteralBooleanFunctionsInRules() {
		return literalBooleanFunctionsInRules;
	}

	public int getLiteralBooleanFunctionsInRulesCount() {
		return literalBooleanFunctionsInRules.size();
	}

	/**
	 * add mode conversions rule to theory
	 * 
	 * @param modeName Name of modal operator.
	 * @param convertModes List of converting modalities.
	 */
	public void addModeConversionRules(final String modeName, final String[] convertModes) {
		if (null == convertModes || convertModes.length == 0) return;
		String o = modeName.trim().toUpperCase();
		Set<String> modeList = modeConversionRules.get(o);
		if (null == modeList) {
			modeList = new TreeSet<String>();
			modeConversionRules.put(o, modeList);
		}
		for (String convertMode : convertModes) {
			String mode = convertMode.trim().toUpperCase();
			if (!"".equals(mode)) {
				if (modeList.add(mode)) conversionRulesModified = true;
			}
		}
		if (modeList.size() == 0) modeConversionRules.remove(o);
	}

	/**
	 * remove mode conversion rule from theory
	 * 
	 * @param modeName
	 * @param convertMode
	 */
	public void removeModeConversionRule(final String modeName, final String convertMode) {
		String modeNameUpper = modeName.trim().toUpperCase();
		String covertModeUpper = convertMode.trim().toUpperCase();
		Set<String> modeList = modeConversionRules.get(modeNameUpper);
		if (null == modeList) return;
		if (modeList.remove(covertModeUpper)) {
			if (modeList.size() == 0) modeConversionRules.remove(modeNameUpper);
			conversionRulesModified = true;
		}
	}

	public Set<String> getModeConversionRules(final String mode) {
		return modeConversionRules.get(mode.trim().toUpperCase());
	}

	public Map<String, Set<String>> getAllModeConversionRules() {
		return modeConversionRules;
	}

	/**
	 * check if there exist any mode conversion rules
	 * 
	 * @return true if there exist any mode conversion rules in theory
	 */
	public boolean isConversionRulesModified() {
		return conversionRulesModified;
	}

	/**
	 * reset all mode conversion rules
	 */
	public void resetConversionRulesModified() {
		conversionRulesModified = false;
	}

	/**
	 * add mode conflict rule to theory
	 * 
	 * @param modeName Name of modal operator.
	 * @param conflictModes List of conflicting modalities.
	 */
	public void addModeConflictRules(final String modeName, final String[] conflictModes) {
		if (null == conflictModes || conflictModes.length == 0) return;
		String o = modeName.trim().toUpperCase();
		Set<String> modeList = modeConflictRules.get(o);
		if (null == modeList) {
			modeList = new TreeSet<String>();
			modeConflictRules.put(o, modeList);
		}
		for (String conflictMode : conflictModes) {
			String mode = conflictMode.trim().toUpperCase();
			if (!"".equals(mode)) {
				if (modeList.add(mode)) conflictRulesModified = true;
			}
		}
		if (modeList.size() == 0) modeConversionRules.remove(o);
	}

	/**
	 * remove a mode conflict rule from theory
	 * 
	 * @param modeName
	 * @param conflictMode
	 */
	public void removeModeConflictRule(final String modeName, final String conflictMode) {
		String modeNameUpper = modeName.trim().toUpperCase();
		String conflictModeUpper = conflictMode.trim().toUpperCase();
		Set<String> modeList = modeConflictRules.get(modeNameUpper);
		if (null == modeList) return;
		if (modeList.remove(conflictModeUpper)) {
			if (modeList.size() == 0) modeConflictRules.remove(modeNameUpper);
			conflictRulesModified = true;
		}
	}

	/**
	 * return the set of mode conversion rules with the original mode specified
	 * 
	 * @param mode
	 * @return set of mode conversion rules
	 */
	public Set<String> getModeConflictRules(final String mode) {
		return modeConflictRules.get(mode.trim().toUpperCase());
	}

	/**
	 * get all mode conflict rules
	 * 
	 * @return all mode conflict rules
	 */
	public Map<String, Set<String>> getAllModeConflictRules() {
		return modeConflictRules;
	}

	/**
	 * check if there exist any mode conflict rules in theory
	 * 
	 * @return true if theory contains any mode conflict rules
	 */
	public boolean isConflictRulesModified() {
		return conflictRulesModified;
	}

	/**
	 * reset all mode conflict rules
	 */
	public void resetConflictRulesModified() {
		conflictRulesModified = false;
	}

	/**
	 * add new fact to theory
	 * 
	 * @param fact
	 * @throws TheoryException
	 */
	public void addFact(Rule fact) throws TheoryException {
		addRule(fact);
	}

	/**
	 * update the rule with new content
	 * 
	 * @param rule
	 * @throws TheoryException
	 */
	public synchronized void updateRule(Rule rule) throws TheoryException {
		if (factsAndAllRules.containsKey(rule.getLabel())) removeRule(rule.getLabel());
		addRule(rule);
	}

	/**
	 * add new rule to theory
	 * 
	 * @param newRule
	 * @throws TheoryException
	 */
	public synchronized void addRule(Rule newRule) throws TheoryException {
		if (newRule == null) throw new TheoryException(ErrorMessage.RULE_NULL_RULE);

		// throw exception if rule contains no head literals
		if (newRule.getHeadLiterals().size() == 0) throw new RuleException(ErrorMessage.RULE_NO_HEAD_LITERAL,
				new Object[] { newRule.getLabel() });

		// add rule to the theory if it does not already exist
		if (factsAndAllRules.containsKey(newRule.getLabel())) throw new TheoryException(
				ErrorMessage.RULE_ALREADY_EXISTS, new Object[] { newRule.getLabel() });
		switch (newRule.getRuleType()) {
		case FACT:
			factsAndAllRules.put(newRule.getLabel(), newRule);
			break;
		case STRICT:
			factsAndAllRules.put(newRule.getLabel(), newRule);
			break;
		case DEFEASIBLE:
			factsAndAllRules.put(newRule.getLabel(), newRule);
			break;
		case DEFEATER:
			factsAndAllRules.put(newRule.getLabel(), newRule);
			break;
		default:
			throw new TheoryException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE, new Object[] { newRule.getRuleType() });
		}
		// update literal-rule type association list
		updateRuleTypeAssociationList_addRule(newRule);

		// update literal-rule association list
		updateLiteralRuleAssociationList_addRule(newRule);
	}

	/**
	 * remove the rule from the rule set and update the literal-rule association
	 * list
	 * 
	 * @param ruleLabel
	 *            label of the rule to be removed
	 * @throws TheoryException
	 */
	public void removeRule(final String ruleLabel) throws TheoryException {
		if (!factsAndAllRules.containsKey(ruleLabel)) throw new TheoryException(ErrorMessage.RULE_UNRECOGNIZED_RULE_ID,
				new Object[] { ruleLabel });

		Rule rule = factsAndAllRules.get(ruleLabel);
		if (rule == null) throw new TheoryException(ErrorMessage.RULE_NULL_RULE);

		updateRuleTypeAssociationList_removeRule(rule);

		// update literal-rule association list
		updateLiteralRuleAssociationList_removeRule(rule);

		// delete the rule from the rule set
		factsAndAllRules.remove(ruleLabel);
	}

	/**
	 * remove the literal from rule body
	 * 
	 * @param literal
	 *            literal to be removed from body
	 * @return list of rules removed from the literal association list
	 * @throws TheoryException
	 */
	public Set<Rule> removeBodyLiteralFromRules(final Literal literal, final RuleType ruleType) throws TheoryException {
		try {
			Set<Rule> rulesModified = new HashSet<Rule>();

			Map<String, Rule> ruleList = literalRuleAssoList.get(literal);
			if (null == ruleList || ruleList.size() == 0) return rulesModified;

			if (ruleList != null) {
				for (Rule rule : ruleList.values()) {
					if (ruleType == null) {
						if (rule.isBodyLiteral(literal)) {
							rule.removeBodyLiteral(literal);
							rulesModified.add(rule);
						}
					} else {
						if (rule.getRuleType() == ruleType && rule.isBodyLiteral(literal)) {
							rule.removeBodyLiteral(literal);
							rulesModified.add(rule);
						}
					}
				}
				for (Rule rule : rulesModified) {
					if (!rule.isHeadLiteral(literal)) ruleList.remove(rule.getLabel());
				}
				if (ruleList.size() == 0) literalRuleAssoList.remove(literal);
			}
			return rulesModified;
		} catch (Exception e) {
			throw new TheoryException("exception throw, litreal=" + literal + ", ruleType=" + ruleType, e);
		}
	}

	/**
	 * add a superiority relation to theory
	 * 
	 * @param sup
	 */
	public void add(final Superiority sup) {
		Set<Superiority> list = superiors.get(sup.getSuperior());
		if (null == list) {
			list = new TreeSet<Superiority>();
			superiors.put(sup.getSuperior(), list);
		}
		if (!list.contains(sup)) {
			list.add(sup);
			superiorityCount++;
		}

		list = inferiors.get(sup.getInferior());
		if (null == list) {
			list = new TreeSet<Superiority>();
			inferiors.put(sup.getInferior(), list);
		}
		if (!list.contains(sup)) list.add(sup);
	}

	/**
	 * remove the superiority relation specified
	 * 
	 * @param sup
	 */
	public void remove(final Superiority sup) {
		Set<Superiority> list = superiors.get(sup.getSuperior());
		if (null != list) {
			if (list.remove(sup)) superiorityCount--;
			if (list.size() == 0) superiors.remove(sup.getSuperior());
		}

		list = inferiors.get(sup.getInferior());
		if (null != list) {
			list.remove(sup);
			if (list.size() == 0) inferiors.remove(sup.getInferior());
		}
	}

	/**
	 * add all rules and superiority relations of other theory to this theory
	 * 
	 * @param theoryPrefix
	 * @param theory
	 * @throws TheoryException
	 */
	public void add(String theoryPrefix, TheoryCore theory) throws TheoryException {
		Collection<Rule> newRules = theory.factsAndAllRules.values();
		Map<String, Set<Superiority>> newSuperiority = theory.superiors;

		if (null == theoryPrefix) theoryPrefix = Utilities.getRandomString(5);
		if (!"".equals(theoryPrefix) && !theoryPrefix.endsWith("_")) theoryPrefix += "_";

		Set<String> rulesAdded = new HashSet<String>();

		try {
			for (Entry<LiteralVariable, LiteralVariable> entry : theory.literalVariables.entrySet()) {
				addLiteralVariable(entry.getKey(), entry.getValue());
			}
			for (Entry<LiteralVariable, LiteralVariable> entry : theory.literalBooleanFunctions.entrySet()) {
				addLiteralVariable(entry.getKey(), entry.getValue());
			}
			if ("".equals(theoryPrefix)) {
				for (Rule rule : newRules) {
					Rule newRule = rule.clone();
					addRule(newRule);
					rulesAdded.add(newRule.getLabel());
				}
				for (Entry<String, Set<Superiority>> entry : newSuperiority.entrySet()) {
					for (Superiority s : entry.getValue()) {
						add(new Superiority(s.getSuperior(), s.getInferior()));
					}
				}
			} else {
				for (Rule rule : newRules) {
					Rule newRule = rule.clone();
					String newRuleLabel = theoryPrefix + rule.getLabel();
					newRule.setLabel(newRuleLabel);
					addRule(newRule);
					rulesAdded.add(newRule.getLabel());
				}
				for (Entry<String, Set<Superiority>> entry : newSuperiority.entrySet()) {
					for (Superiority s : entry.getValue()) {
						add(new Superiority(theoryPrefix + s.getSuperior(), theoryPrefix + s.getInferior()));
					}
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.print("restore theory...");
			for (String ruleAdded : rulesAdded) {
				this.removeRule(ruleAdded);
			}
			System.err.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS));
			throw new TheoryException(e);
		}
	}

	private void updateLiteralRuleAssociationList_addRule(final Rule newRule) throws TheoryException {
		Set<Literal> literalList = newRule.getLiteralList();
		Map<String, Rule> ruleList = null;
		if (newRule.containsTemporalInfo()) theoryType = TheoryType.TDL;
		else if (!"".equals(newRule.getMode().getName()) && theoryType == TheoryType.SDL) theoryType = TheoryType.MDL;
		for (Literal literal : literalList) {
			// theory type checking
			// if literal contains mode value, theory type = MDL
			if (literal.containsTemporalInfo()) theoryType = TheoryType.TDL;
			else if (!"".equals(literal.getMode().getName()) && theoryType == TheoryType.SDL) theoryType = TheoryType.MDL;

			ruleList = literalRuleAssoList.get(literal);
			if (null == ruleList) {
				ruleList = new TreeMap<String, Rule>();
				literalRuleAssoList.put(literal, ruleList);
			}
			if (!ruleList.containsKey(newRule.getLabel())) ruleList.put(newRule.getLabel(), newRule);

			// literal variable and boolean operation handling
			if (literal instanceof LiteralVariable) {
				LiteralVariable lv = (LiteralVariable) literal;
				if (lv.isLiteralVariable()) literalVariablesInRules.add(lv);
				else if (lv.isLiteralBooleanFunction()) literalBooleanFunctionsInRules.add(lv);
			}
		}
	}

	private void updateLiteralRuleAssociationList_removeRule(final Rule rule) throws TheoryException {
		try {
			for (Literal literal : rule.getLiteralList()) {
				Map<String, Rule> ruleList = literalRuleAssoList.get(literal);
				if (null == ruleList) throw new TheoryException(
						"updateLiteralRuleAssociationList_removeRule: literal [" + literal + "] contains in no rules!!");
				Rule retn = ruleList.remove(rule.getLabel());
				if (null == retn) throw new TheoryException(ErrorMessage.RULE_UNRECOGNIZED_RULE_ID,
						"updateLiteralRuleAssociationList_removeRule", new Object[] { rule.getLabel() });

				if (ruleList.size() == 0) {
					literalRuleAssoList.remove(literal);

					// literal variable and boolean operation handling
					if (literal instanceof LiteralVariable) {
						LiteralVariable lv = (LiteralVariable) literal;
						if (lv.isLiteralVariable()) literalVariablesInRules.remove(lv);
						else if (lv.isLiteralBooleanFunction()) literalBooleanFunctionsInRules.remove(lv);
					}
				}
			}
		} catch (Exception e) {
			throw new TheoryException("exception throw while updating literal-rule association list", e);
		}
	}

	private void updateRuleTypeAssociationList_addRule(final Rule newRule) throws TheoryException {
		Map<String, Rule> ruleSet = ruleTypeAssoList.get(newRule.getRuleType());
		if (ruleSet == null) {
			ruleSet = new TreeMap<String, Rule>();
			ruleTypeAssoList.put(newRule.getRuleType(), ruleSet);
		}
		ruleSet.put(newRule.getLabel(), newRule);
	}

	private void updateRuleTypeAssociationList_removeRule(final Rule rule) throws TheoryException {
		try {
			Map<String, Rule> ruleSet = ruleTypeAssoList.get(rule.getRuleType());
			if (null == ruleSet) throw new TheoryException("Rule type not exist in the theory set");

			Rule retn = ruleSet.remove(rule.getLabel());
			if (null == retn) throw new TheoryException(ErrorMessage.RULE_UNRECOGNIZED_RULE_ID_IN_TYPE, new Object[] {
					rule.getLabel(), rule.getRuleType() });

			if (ruleSet.size() == 0) ruleTypeAssoList.remove(rule.getRuleType());
		} catch (Exception e) {
			throw new TheoryException("exception throw while updating literal-rule association list ", e);
		}
	}

	public Map<String, Rule> getFactsAndAllRules() {
		return factsAndAllRules;
	}

	public Map<String, Rule> getFacts() {
		return getRules(RuleType.FACT);
	}

	/**
	 * return the set of rules with specified rule type
	 * 
	 * @param ruleType
	 * @return list of rules with specified rule type
	 */
	public Map<String, Rule> getRules(final RuleType ruleType) {
		Map<String, Rule> ruleSet = new TreeMap<String, Rule>();
		Map<String, Rule> rules = ruleTypeAssoList.get(ruleType);
		if (null != rules) ruleSet.putAll(rules);
		return ruleSet;
	}

	/**
	 * Retrieve rule with the rule label specified.
	 * 
	 * @param ruleLabel Rule label.
	 * @return Rule with specified rule label; or null otherwise.
	 */
	public Rule getRule(String ruleLabel) {
		return factsAndAllRules.get(ruleLabel);
	}

	/**
	 * Retrieve the set of rules containing the literal specified.
	 * 
	 * @param literal
	 * @return list of rules contain the specified literal
	 */
	public Map<String, Rule> getRules(Literal literal) {
		Map<String, Rule> ruleSet = new TreeMap<String, Rule>();
		Map<String, Rule> rules = literalRuleAssoList.get(literal);
		if (null != rules) ruleSet.putAll(rules);
		return ruleSet;
	}

	/**
	 * Return the set of literals that appear in the theory.
	 * 
	 * @return The set of literals that appear in the theory.
	 */
	public Set<Literal> getAllLiteralsInRules() {
		return literalRuleAssoList.keySet();
	}

	/**
	 * get the number of mode conversion rules
	 * 
	 * @return no. of mode conversion rules
	 */
	public int getModeConversionRulesCount() {
		int c = 0;
		for (Collection<String> modeLst : modeConversionRules.values()) {
			c += modeLst.size();
		}
		return c;
	}

	/**
	 * get the number of mode conflict rules
	 * 
	 * @return no. of mode conflict rules
	 */
	public int getModeConflictRulesCount() {
		int c = 0;
		for (Collection<String> modeLst : modeConflictRules.values()) {
			c += modeLst.size();
		}
		return c;
	}

	/**
	 * get the number of facts
	 * 
	 * @return number of facts
	 */
	public int getFactsCount() {
		Map<String, Rule> ruleSet = ruleTypeAssoList.get(RuleType.FACT);
		return (ruleSet == null) ? 0 : ruleSet.size();
	}

	/**
	 * get the number of strict rules
	 * 
	 * @return number of strict rules
	 */
	public int getStrictRulesCount() {
		Map<String, Rule> ruleSet = ruleTypeAssoList.get(RuleType.STRICT);
		return (ruleSet == null) ? 0 : ruleSet.size();
	}

	/**
	 * get the number of defeaters
	 * 
	 * @return no. of defeaters
	 */
	public int getDefeasibleRulesCount() {
		Map<String, Rule> ruleSet = ruleTypeAssoList.get(RuleType.DEFEASIBLE);
		return (ruleSet == null) ? 0 : ruleSet.size();
	}

	/**
	 * get the number of superiority relation
	 * 
	 * @return no. of superiority relations
	 */
	public int getSuperiorityCount() {
		return superiors.size();
	}

	/**
	 * get the number of defeaters
	 * 
	 * @return number of defeaters
	 */
	public int getDefeatersCount() {
		Map<String, Rule> ruleSet = ruleTypeAssoList.get(RuleType.DEFEATER);
		return (ruleSet == null) ? 0 : ruleSet.size();
	}

	/**
	 * check if the theory contains any rules
	 * 
	 * @return true if there is no rule in the theory
	 */
	public boolean isEmpty() {
		if (literalVariables.size() > 0) return false;
		if (literalBooleanFunctions.size() > 0) return false;
		if (factsAndAllRules.size() > 0) return false;
		return true;
	}

	/**
	 * check whether a literal appear in theory
	 * 
	 * @param literal
	 * @return true if the theory contains the literal specified
	 */
	public boolean contains(final Literal literal) {
		return literalRuleAssoList.containsKey(literal);
	}

	public boolean containsRuleLabel(final String ruleLabel) throws RuleException {
		if (ruleLabel == null || "".equals(ruleLabel.trim())) throw new RuleException(ErrorMessage.RULE_NULL_RULE);
		// if (ruleLabel == null || "".equals(ruleLabel.trim())) throw new RuleException("rule label is null");
		return factsAndAllRules.containsKey(ruleLabel.trim());
	}

	public boolean contains(final Superiority sup) {
		if (sup == null) return false;
		Set<Superiority> superioritiesSet = superiors.get(sup.getSuperior());
		if (null != superioritiesSet) return superioritiesSet.contains(sup);
		return false;
	}

	public boolean containsRuleModeConversionRules() {
		return modeConversionRules.size() > 0;
	}

	public boolean containsRuleModeConflictRules() {
		return modeConflictRules.size() > 0;
	}

	/**
	 * get all superiority relations in theory
	 * 
	 * @return all superiority relations in theory
	 */
	public List<Superiority> getAllSuperiority() {
		List<Superiority> supList = new ArrayList<Superiority>();
		for (String s : superiors.keySet()) {
			supList.addAll(superiors.get(s));
		}
		return supList;
	}

	/**
	 * clear all superiority relations
	 */
	public void clearSuperiority() {
		superiors.clear();
		inferiors.clear();
	}

	public Map<String, Set<Superiority>> getAllSuperiors() {
		return superiors;
	}

	public Set<Superiority> getSuperior(final String ruleLabel) {
		return superiors.get(ruleLabel);
	}

	public Map<String, Set<Superiority>> getAllInferiors() {
		return inferiors;
	}

	public Set<Superiority> getInferior(final String ruleLabel) {
		return inferiors.get(ruleLabel);
	}

	/**
	 * get the theory type
	 * 
	 * @return theory type
	 * @see TheoryType
	 */
	public TheoryType getTheoryType() {
		return theoryType;
	}

	public void clearAllRules() {
		factsAndAllRules.clear();
		literalRuleAssoList.clear();
		ruleTypeAssoList.clear();

		superiors.clear();
		inferiors.clear();
		superiorityCount = 0;
	}

	/**
	 * clear the theory
	 */
	public void clear() {
		clearAllRules();
		clearModeConversionRules();
		clearModeConflictRules();
	}

	public void clearModeConversionRules() {
		modeConversionRules.clear();
	}

	public void clearModeConflictRules() {
		modeConflictRules.clear();
	}

	public String toString() {
		if (isEmpty()) return "** Theory is EMPTY **";
		StringBuilder sb = new StringBuilder();
		int v = literalVariables.size() + literalBooleanFunctions.size();
		String NEW_LINE = LINE_SEPARATOR + DomConst.IDENTATOR;

		if (v > 0) {
			sb.append(LINE_SEPARATOR).append(RuleType.LITERAL_VARIABLE_SET.getLabel()).append(" (").append(v)
					.append("):");
			for (Entry<LiteralVariable, LiteralVariable> entry : literalVariables.entrySet()) {
				sb.append(NEW_LINE).append(RuleType.LITERAL_VARIABLE_SET.getSymbol()).append(" ")
						.append(entry.getKey()).append(DomConst.Literal.THEORY_EQUAL_SIGN).append(entry.getValue());
			}
			for (Entry<LiteralVariable, LiteralVariable> entry : literalBooleanFunctions.entrySet()) {
				sb.append(NEW_LINE).append(RuleType.LITERAL_VARIABLE_SET.getSymbol()).append(" ")
						.append(entry.getKey()).append(DomConst.Literal.THEORY_EQUAL_SIGN).append(entry.getValue());
			}
		}

		if (modeConversionRules.size() > 0) {
			sb.append(LINE_SEPARATOR).append(RuleType.MODE_CONVERSION.getLabel()).append(" (")
					.append(modeConversionRules.size()).append("):");
			for (Entry<String, Set<String>> entry : modeConversionRules.entrySet()) {
				sb.append(NEW_LINE).append(entry.getKey()).append(": ");
				sb.append(entry.getValue());
			}
		}

		if (modeConflictRules.size() > 0) {
			sb.append(LINE_SEPARATOR).append(RuleType.MODE_CONFLICT.getLabel()).append(" (")
					.append(modeConflictRules.size()).append("):");
			for (Entry<String, Set<String>> entry : modeConflictRules.entrySet()) {
				sb.append(NEW_LINE).append(entry.getKey()).append(": ");
				sb.append(entry.getValue());
			}
		}

		Map<String, Rule> ruleSet = null;
		for (RuleType ruleType : RuleType.values()) {
			ruleSet = getRules(ruleType);
			if (ruleSet.size() > 0) {
				sb.append(LINE_SEPARATOR).append(ruleType.getLabel()).append(" (").append(ruleSet.size()).append("):");
				for (Rule rule : ruleSet.values()) {
					sb.append(NEW_LINE).append(rule.toString());
				}
			}
		}

		if (superiors.size() > 0) {
			if (sb.length() > 0) sb.append(LINE_SEPARATOR);
			sb.append(RuleType.SUPERIORITY.getLabel()).append(" (").append(superiors.size()).append("):");
			for (Entry<String, Set<Superiority>> entry : superiors.entrySet()) {
				sb.append(LINE_SEPARATOR + "  ").append(entry.getKey());
				for (Superiority sup : entry.getValue()) {
					sb.append(NEW_LINE).append(sup.toString());
				}
			}
		}
		return sb.toString().substring(LINE_SEPARATOR.length());
	}
}
