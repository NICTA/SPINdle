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
package spindle.engine;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.app.utils.Utilities.ProcessStatus;

import spindle.core.MessageType;
import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleException;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.sys.AppModuleBase;
import spindle.sys.AppModuleListener;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * Base class for Theory normalizer.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.1.0
 */
public abstract class TheoryNormalizer extends AppModuleBase {
	private static final String DEFEASIBLE_RULE_POSTFIX_A = ".a";
	private static final String DEFEASIBLE_RULE_POSTFIX_C = ".c";
	private static final String DEFEATER_POSTFIX_A = ".a";
	private static final String DEFEATER_POSTFIX_C = ".c";
	private static final String SUPERIORITY_RULE_PREFIX = "s.";
	private static final String SUPERIORITY_RULE_POSTFIX_PLUS = "+";
	private static final String SUPERIORITY_RULE_POSTFIX_MINUS = "-";
	private static final String SYMBOL_INF = "inf";

	protected static final String TRANSFORM_POSTFIX = "'";
	protected static final String TRANSFORM_POSTFIX2 = "*";
	protected static final String FACT_RULE_TRANSFORM_PREFIX = "fact.";

	protected Theory theory = null;

	protected Map<String, Rule> factsAndRules = null;

	public TheoryNormalizer() {
		theory = null;
		factsAndRules = null;
	}

	public Theory getTheory() {
		return theory;
	}

	public void setTheory(final Theory theory) {
		this.theory = theory;
		factsAndRules = this.theory.getFactsAndAllRules();
	}

	/**
	 * transform theory to regular form and convert all modal operators in all rules and literals for MDL theory
	 * 
	 * @return ProcessStatus.SUCCESS if completed successfully
	 * @throws TheoryNormalizerException
	 */
	public ProcessStatus transformTheoryToRegularForm() throws TheoryNormalizerException {
		if (factsAndRules == null) throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_NULL_THEORY);
		transformTheoryToRegularFormImpl();
		return ProcessStatus.SUCCESS;
	}

	public ProcessStatus removeDefeater() throws TheoryNormalizerException {
		if (theory == null) throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_NULL_THEORY);
		removeDefeaterImpl();
		return ProcessStatus.SUCCESS;
	}

	public ProcessStatus removeSuperiority() throws TheoryNormalizerException {
		if (theory == null) throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_NULL_THEORY);
		removeSuperiorityImpl();
		return ProcessStatus.SUCCESS;
	}

	/**
	 * add literals to the body of a rule
	 * 
	 * <pre>
	 * assumed that the literal is a place holder (dummy literal) if postfix is defined
	 * </pre>
	 * 
	 * @param rule
	 * @param literals
	 * @param postfix
	 * @throws RuleException
	 */
	protected void addBodyLiterals(Rule rule, List<Literal> literals, String postfix) throws RuleException {
		try {
			if (literals == null || literals.size() == 0) {
			} else {
				if ("".equals(postfix)) {
					for (Literal literal : literals) {
						rule.addBodyLiteral(literal);
					}
				} else {
					for (Literal literal : literals) {
						Literal newLiteral = DomUtilities.getLiteral( //
								literal.getName() + postfix, literal.isNegation(), //
								literal.getMode(), //
								literal.getTemporal(), //
								literal.getPredicates(), //
								true);
						rule.addBodyLiteral(newLiteral);
					}
				}
			}
		} catch (Exception e) {
			throw new RuleException(e);
		}
	}

	/**
	 * add literals to the head of a rule
	 * 
	 * <pre>
	 * assumed that the literal is a place holder (dummy literal) if postfix is defined
	 * </pre>
	 * 
	 * @param rule
	 * @param literals
	 * @param postfix
	 * @throws RuleException
	 */
	protected void addHeadLiterals(Rule rule, List<Literal> literals, String postfix) throws RuleException {
		try {
			if (literals == null || literals.size() == 0) return;
			if ("".equals(postfix)) {
				for (Literal literal : literals) {
					rule.addHeadLiteral(literal);
				}
			} else {
				for (Literal literal : literals) {
					Literal newLiteral = DomUtilities.getLiteral( //
							literal.getName() + postfix, literal.isNegation(), //
							literal.getMode(),//
							literal.getTemporal(), //
							literal.getPredicates(),//
							true);
					rule.addHeadLiteral(newLiteral);
				}
			}
		} catch (RuleException e) {
			throw e;
		} catch (Exception e) {
			throw new RuleException(e);
		}
	}

	// ========================
	// implementation
	//
	private void removeSuperiorityImpl() throws TheoryNormalizerException {
		List<Superiority> superiority = theory.getAllSuperiority();

		List<Rule> rulesToAdd = new ArrayList<Rule>();
		Set<String> rulesToDelete = new TreeSet<String>();

		removeSuperiority_ruleModification(rulesToAdd, rulesToDelete);

		// superiority transform
		Rule newRule1 = null;
		Rule newRule2 = null;
		try {
			for (Superiority s : superiority) {
				newRule1 = null;
				newRule2 = null;

				String superiorRuleId = s.getSuperior();
				String inferiorRuleId = s.getInferior();

				Rule superiorRule = factsAndRules.get(superiorRuleId);
				Rule inferiorRule = factsAndRules.get(inferiorRuleId);

				if (null == superiorRule) throw new TheoryNormalizerException(getClass(),
						ErrorMessage.THEORY_SUPERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { superiorRuleId });
				if (null == inferiorRule) throw new TheoryNormalizerException(getClass(),
						ErrorMessage.THEORY_INFERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { inferiorRuleId });

				Mode superiorRuleMode = superiorRule.getMode();
				Mode inferiorRuleMode = inferiorRule.getMode();

				Literal supRuleHead = superiorRule.getHeadLiterals().get(0);
				Literal infRuleHead = inferiorRule.getHeadLiterals().get(0);

				if (supRuleHead.isComplementTo(infRuleHead)) {
					String newSupRuleLabel = theory.getUniqueRuleLabel(SUPERIORITY_RULE_PREFIX);

					Literal bodyLiteral = DomUtilities.getLiteral(SYMBOL_INF + "(" + superiorRuleId + ")+", true, //
							superiorRuleMode,null, null, true);
					Literal headLiteralPlus = DomUtilities.getLiteral(SYMBOL_INF + "(" + inferiorRuleId + ")+", false,//
							inferiorRuleMode,null, null, true);
					Literal headLiteralMinus = DomUtilities.getLiteral(SYMBOL_INF + "(" + inferiorRuleId + ")-", false, //
							inferiorRuleMode,null, null, true);

					newRule1 = DomUtilities.getRule(newSupRuleLabel + SUPERIORITY_RULE_POSTFIX_PLUS,
							RuleType.DEFEASIBLE);
newRule1.setOriginalLabel(superiorRuleId);				
					newRule1.setMode(superiorRuleMode);

					newRule1.addBodyLiteral(bodyLiteral);
					newRule1.addHeadLiteral(headLiteralPlus);

					newRule2 = DomUtilities.getRule(newSupRuleLabel + SUPERIORITY_RULE_POSTFIX_MINUS,
							RuleType.DEFEASIBLE);
newRule2.setOriginalLabel(inferiorRuleId);					
					newRule2.setMode(superiorRuleMode);

					newRule2.addBodyLiteral(bodyLiteral);
					newRule2.addHeadLiteral(headLiteralMinus);

					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
				} else {
					fireTheoryNormalizerMessage(MessageType.WARNING, "The heads of [" + superiorRuleId + "] and ["
							+ inferiorRuleId + "] are not complement to each other!  Superiority relation ignored!");
				}
			}

			theory.updateTheory(rulesToAdd, rulesToDelete, null);
			theory.clearSuperiority();
		} catch (TheoryException e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.TRANSFORMATION_SUPERIORITY_REMOVAL_ERROR,
					"Exception throw while updating theory", e);
		} catch (Exception e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.TRANSFORMATION_SUPERIORITY_REMOVAL_ERROR,
					"Exception throw whlie removing superiority", e);
		}
	}

	private ProcessStatus removeSuperiority_ruleModification(final List<Rule> rulesToAdd, //
			Set<String> rulesToDelete) throws TheoryNormalizerException {

		Rule newRule1 = null;
		Rule newRule2 = null;
		try {
			for (Rule rule : factsAndRules.values()) {
				newRule1 = null;
				newRule2 = null;

				String ruleLabel = rule.getLabel();
				String originalRuleLabel=rule.getOriginalLabel();
				Mode ruleMode = rule.getMode();
				switch (rule.getRuleType()) {
				case DEFEASIBLE:
					Literal literalDef = DomUtilities.getLiteral("inf(" + ruleLabel + ")+", true, //
							ruleMode, null,null, true);

					newRule1 = DomUtilities.getRule(ruleLabel + DEFEASIBLE_RULE_POSTFIX_A, RuleType.DEFEASIBLE);
newRule1.setOriginalLabel(originalRuleLabel);
					newRule1.setMode(ruleMode);
					addBodyLiterals(newRule1, rule.getBodyLiterals(), "");
					newRule1.addHeadLiteral(literalDef);

					newRule2 = DomUtilities.getRule(ruleLabel + DEFEASIBLE_RULE_POSTFIX_C, RuleType.DEFEASIBLE);
newRule2.setOriginalLabel(originalRuleLabel);
					newRule2.setMode(ruleMode);
					newRule2.addBodyLiteral(literalDef);
					addHeadLiterals(newRule2, rule.getHeadLiterals(), "");

					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
					rulesToDelete.add(ruleLabel);

					break;
				case DEFEATER:
					Literal literalDft = DomUtilities.getLiteral("inf(" + ruleLabel + ")-", true, //
							ruleMode,null, null, true);

					newRule1 = DomUtilities.getRule(ruleLabel + DEFEATER_POSTFIX_A, RuleType.DEFEASIBLE);
newRule1.setOriginalLabel(originalRuleLabel);
					newRule1.setMode(ruleMode);
					addBodyLiterals(newRule1, rule.getBodyLiterals(), "");
					newRule1.addHeadLiteral(literalDft);

					newRule2 = DomUtilities.getRule(ruleLabel + DEFEATER_POSTFIX_C, RuleType.DEFEATER);
newRule2.setOriginalLabel(originalRuleLabel);
					newRule2.setMode(ruleMode);
					newRule2.addBodyLiteral(literalDft);
					addHeadLiterals(newRule2, rule.getHeadLiterals(), "");

					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
					rulesToDelete.add(ruleLabel);

					break;
				default:
				}
			}
		} catch (Exception e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.TRANSFORMATION_DEFEATER_REMOVAL_ERROR, e);
		}
		return ProcessStatus.SUCCESS;
	}

	private void removeDefeaterImpl() throws TheoryNormalizerException {
		List<Rule> rulesToAdd = new ArrayList<Rule>();
		Set<String> rulesToDelete = new TreeSet<String>();

		Map<String, List<Rule>> oldNewRuleMapping = new Hashtable<String, List<Rule>>();
		List<Rule> newRules;
		List<Rule> newRuleMapping = null;

		for (Rule rule : factsAndRules.values()) {
			newRuleMapping = null;
			switch (rule.getRuleType()) {
			case STRICT:
				newRules = removeDefeater_transformRule(rule);

				rulesToAdd.addAll(newRules);
				rulesToDelete.add(rule.getLabel());
				newRuleMapping = newRules;
				break;
			case DEFEASIBLE:
				if (rule.getHeadLiterals().size() > 1) throw new TheoryNormalizerException(getClass(),
						Messages.getErrorMessage(ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_MULTIPLE_HEADS));

				newRules = removeDefeater_transformRule(rule);

				rulesToDelete.add(rule.getLabel());
				rulesToAdd.addAll(newRules);
				newRuleMapping = newRules;
				break;
			case DEFEATER:
				Rule r = removeDefeater_transformDefeater(rule);
				rulesToAdd.add(r);
				rulesToDelete.add(rule.getLabel());
				break;
			default:
			}
			if (newRuleMapping != null) oldNewRuleMapping.put(rule.getLabel(), newRuleMapping);
		}

		try {
			theory.updateTheory(rulesToAdd, rulesToDelete, oldNewRuleMapping);
		} catch (TheoryException e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.TRANSFORMATION_DEFEATER_REMOVAL_ERROR,
					"Defeater removal exception!", e);
		}
	}

	private Rule removeDefeater_transformDefeater(Rule origRule) throws TheoryNormalizerException {
		if (origRule.getRuleType() != RuleType.DEFEATER) return null;

		List<Literal> origHeadLiterals = origRule.getHeadLiterals();

		if (origHeadLiterals.size() > 1) throw new TheoryNormalizerException(getClass(),
				"rule head contains more than 1 literal");
		Literal origHeadLiteral = origHeadLiterals.get(0);
		Mode dummyLiteralMode = origHeadLiteral.getMode();
		Mode ruleMode = origRule.getMode();
		if ("".equals(dummyLiteralMode.getName())) dummyLiteralMode = ruleMode;
		String sign = (origHeadLiteral.isNegation()) ? "+" : "-";

		Rule newRule = DomUtilities.getRule(origRule.getLabel(), RuleType.DEFEASIBLE);
newRule.setOriginalLabel(origRule.getOriginalLabel());
		newRule.setMode(ruleMode);
		try {
			addBodyLiterals(newRule, origRule.getBodyLiterals(), "");
			newRule.addHeadLiteral(DomUtilities.getLiteral(origHeadLiteral.getName() + sign, true, dummyLiteralMode,null,
					(String[]) null, true));
		} catch (Exception e) {
		}

		return newRule;
	}

	private List<Rule> removeDefeater_transformRule(Rule origRule) throws TheoryNormalizerException {
		List<Literal> origBodyLiterals = origRule.getBodyLiterals();
		List<Literal> origHeadLiterals = origRule.getHeadLiterals();

		if (origHeadLiterals.size() > 1) throw new TheoryNormalizerException(getClass(),
				"rule head contains more than 1 literal");
		List<Rule> newRules = new ArrayList<Rule>();
		Literal origHeadLiteral = origHeadLiterals.get(0);
		Mode dummyLiteralMode = origHeadLiteral.getMode();
		Mode ruleMode = origRule.getMode();
		if ("".equals(dummyLiteralMode.getName())) dummyLiteralMode = ruleMode;

		Literal newHeadLiteral = null;
		String[] sign = new String[2];
		if (origHeadLiteral.isNegation()) {
			sign[0] = "-";
			sign[1] = "+";
			newHeadLiteral = origHeadLiteral.getComplementClone();
		} else {
			sign[0] = "+";
			sign[1] = "-";
			newHeadLiteral = origHeadLiteral.clone();
		}

		String originalRuleLabel=origRule.getOriginalLabel();
		Literal dummyLiteral1 = DomUtilities.getLiteral(newHeadLiteral.getName() + sign[0], false, dummyLiteralMode,null,
				null, true);
		Literal dummyLiteral2 = DomUtilities.getLiteral(newHeadLiteral.getName() + sign[1], true, dummyLiteralMode,null,
				null, true);

		Rule newRule1 = DomUtilities.getRule(origRule.getLabel() + sign[0], origRule.getRuleType());
newRule1.setOriginalLabel(originalRuleLabel);
		newRule1.setMode(ruleMode);
		try {
			addBodyLiterals(newRule1, origBodyLiterals, "");
			newRule1.addHeadLiteral(dummyLiteral1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Rule newRule2 = DomUtilities.getRule(origRule.getLabel() + sign[1], origRule.getRuleType());
newRule2.setOriginalLabel(originalRuleLabel);
		newRule2.setMode(ruleMode);
		try {
			addBodyLiterals(newRule2, origBodyLiterals, "");
			newRule2.addHeadLiteral(dummyLiteral2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Rule newRule3 = DomUtilities.getRule(origRule.getLabel(), origRule.getRuleType());
newRule3.setOriginalLabel(originalRuleLabel);
		newRule3.setMode(ruleMode);
		try {
			newRule3.addBodyLiteral(dummyLiteral1);
			newRule3.addHeadLiteral(origHeadLiteral.clone());
		} catch (Exception e) {
			e.printStackTrace();
		}

		newRules.add(newRule1);
		newRules.add(newRule2);
		newRules.add(newRule3);

		return newRules;
	}

	public void addTheoryNormalizerListener(TheoryNormalizerListener listener) {
		addAppModuleListener(listener);
	}

	public void removeTheoryNormalizerListener(TheoryNormalizerListener listener) {
		removeAppModuleListener(listener);
	}

	protected void fireTheoryNormalizerMessage(MessageType messageType, String message) {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof TheoryNormalizerListener) {
				((TheoryNormalizerListener) listener).onTheoryNormalizerMessage(messageType, message);
			}
		}
	}

	protected abstract void transformTheoryToRegularFormImpl() throws TheoryNormalizerException;

}
