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
package spindle.engine.sdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import com.app.utils.Utilities.ProcessStatus;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleExt;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.TheoryException;
import spindle.engine.ReasoningEngineException;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;
import spindle.tools.explanation.RuleInferenceStatus;

/**
 * SDL reasoning engine (version 2).
 * <p>
 * Derive the conclusions of a defeasible theory based on the algorithm presented in:
 * <ul>
 * <li>H.-P. Lam and G. Governatori (2011) What are the Necessity Rules in Defeasible Logic, <i> In Proceedings of the
 * 11th International Conference on Logic Programming and Nonmonotonic Reasoning (LPNMR-2011)</i>, 16-19 May 2011,
 * Vancouver, BC, Canada</li>
 * </ul>
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @version Last modified 2012.07.21
 */
public class SdlReasoningEngine2 extends spindle.engine.sdl.SdlReasoningEngine {

	public SdlReasoningEngine2() {
		super();
	}

	/**
	 * a rule is removed if
	 * <ul>
	 * <li>there exists no rules weaker than it, and</li>
	 * <li>there exists a superior rule with empty body.</li>
	 * </ul>
	 * 
	 * @param ruleLabel rule label
	 * @return true if the rule can remove from theory
	 * @throws TheoryException
	 */
	protected Set<String> getDefeatedRulesInTheory(String ruleLabel) throws TheoryException {
		RuleExt rule = (RuleExt) theory.getRule(ruleLabel);

		Set<String> defeatedRules = new TreeSet<String>();
		if (!rule.isEmptyBody()) return defeatedRules;

		Set<Superiority> superiors = theory.getSuperior(ruleLabel);

		// remove all inferior rules with no weaker rule
		if (null != superiors) {
			for (Superiority superiority : superiors) {
				RuleExt inferiorRule = (RuleExt) theory.getRule(superiority.getInferior());
				if (inferiorRule.getHeadLiterals().size() > 1) throw new TheoryException(
						ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_MULTIPLE_HEADS);
				if (inferiorRule.getWeakerRulesCount() == 0) {
					defeatedRules.add(superiority.getInferior());
				}
			}
		}
		return defeatedRules;
	}

	protected void removeDefeatedRulesInTheory() throws ReasoningEngineException, TheoryException {
		if (theory.getSuperiorityCount() == 0) return;
		logMessage(Level.FINE, 1, "removeDefeatedRuleInTheory - start");
		Map<String, Set<Superiority>> superiorities = theory.getAllSuperiors();
		Set<String> defeatedRules = new TreeSet<String>();
		logMessage(Level.FINER, 2, "superiorities=", superiorities);
		try {
			do {
				defeatedRules.clear();
				for (String superiorRuleLabel : superiorities.keySet()) {
					defeatedRules.addAll(getDefeatedRulesInTheory(superiorRuleLabel));
				}
				if (defeatedRules.size() > 0) {
					removeDefeatedRulesWithInference(defeatedRules, false);
				}
			} while (defeatedRules.size() > 0);
			if (!AppConst.isDeploy) logMessage(Level.INFO, 0, "======\ntheory\n======\n", theory);
		} catch (TheoryException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		} finally {
			logMessage(Level.FINE, 1, "removeDefeatedRuleInTheory - end");
		}
	}

	protected void verifyWeakestRuleInTheory(Set<RuleExt> rulesToVerify, Set<String> rulesToRemove)
			throws ReasoningEngineException {
		for (RuleExt rule : rulesToVerify) {
			boolean isBlockedBySccLiteral = false;
			Set<Superiority> superiorities = theory.getInferior(rule.getLabel());
			boolean isAllSuperiorRuleBlockedBySccLiteral = true;
			for (Superiority sup : superiorities) {
				RuleExt supRule = (RuleExt) theory.getRule(sup.getSuperior());
				isBlockedBySccLiteral = false;
				for (Literal literal : supRule.getBodyLiterals()) {
					if (isBlockedBySccLiteral(literal)) isBlockedBySccLiteral = true;
				}
				if (!isBlockedBySccLiteral) isAllSuperiorRuleBlockedBySccLiteral = false;
			}
			if (isAllSuperiorRuleBlockedBySccLiteral) {
				rulesToRemove.add(rule.getLabel());
			}
		}
	}

	@Override
	protected ProcessStatus generateInitialPendingConclusions() throws ReasoningEngineException, TheoryException {
		try {
			theory.updateRuleSuperiorityRelationCounter();
			generatePendingConclusions(false);
		} catch (TheoryException e) {
			throw e;
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
		return ProcessStatus.SUCCESS;
	}

	protected void generatePendingConclusions(boolean isDefeasibleRuleOnly) throws ReasoningEngineException,
			TheoryException {
		logMessage(Level.FINE, 0, "=== SdlReasoningEngine.generatePendingConclusions - start ===");
		logMessage(Level.FINE, 1, "isDefeasibleRuleOnly=", isDefeasibleRuleOnly);

		Set<Literal> unprovedStrictRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<Literal> unprovedDefeasibleRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());

		logMessage(Level.FINER, 1, "=== +ve set - start ===");

		removeDefeatedRulesInTheory();

		Set<String> rulesToDelete = new TreeSet<String>();
		Map<Conclusion, Set<String>> tempPosConclusionSet = new TreeMap<Conclusion, Set<String>>();
		Set<String> ruleSet = null;

		for (Rule r : theory.getFactsAndAllRules().values()) {
			if (r.getHeadLiterals().size() > 1) throw new ReasoningEngineException(getClass(),
					"theory is not in regular form, rule [" + r.getLabel() + "] contains more than 1 literals");
			RuleExt rule = (RuleExt) r;
			Literal literal = rule.getHeadLiterals().get(0);
			switch (rule.getRuleType()) {
			case STRICT:
				if (!isDefeasibleRuleOnly) {
					if (rule.isEmptyBody()) {
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal);
						addRecord(conclusion);

						ruleSet = tempPosConclusionSet.get(conclusion);
						if (null == ruleSet) {
							ruleSet = new TreeSet<String>();
							tempPosConclusionSet.put(conclusion, ruleSet);
						}
						ruleSet.add(rule.getOriginalLabel());

						rulesToDelete.add(rule.getLabel());
					}
					unprovedStrictRuleLiterals.remove(literal);
				}
				break;
			case DEFEASIBLE:
				if (rule.isEmptyBody() && rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0) {
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal);
					addRecord(conclusion);

					ruleSet = tempPosConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosConclusionSet.put(conclusion, ruleSet);
					}
					ruleSet.add(rule.getOriginalLabel());

					rulesToDelete.add(rule.getLabel());
				}
				unprovedDefeasibleRuleLiterals.remove(literal);
				break;
			default:
			}
		}

		removeRules(rulesToDelete);

		if (!AppConst.isDeploy) {
			logMessage(Level.INFO, 1, "tempPosConclusionSet=", tempPosConclusionSet);
			logMessage(Level.INFO, 1, "unprovedDefeasibleRuleLiterals=", unprovedDefeasibleRuleLiterals);
			printPendingConclusionSet(unprovedStrictRuleLiterals, unprovedDefeasibleRuleLiterals,
					tempPosConclusionSet.keySet());
		}

		Set<Conclusion> tempPosConclusions = tempPosConclusionSet.keySet();
		for (Entry<Conclusion, Set<String>> entry : tempPosConclusionSet.entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			boolean ambiguousExist = false, pos = true;
			Literal literal = conclusion.getLiteral();
			List<Literal> conflictLiterals = getConflictLiterals(literal);

			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE:
				if (!isDefeasibleRuleOnly) {
					ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosConclusions,
							ConclusionType.DEFINITE_PROVABLE);
					if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals,
							RuleType.STRICT);
					if (ambiguousExist) {
						logMessage(Level.FINEST, 1, "==> generatePendingConclusions: ==> add (+D Ambiguous)", literal);
						addAmbiguousConclusion(conclusion, ruleLabels);
					} else {
						if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) pos = false;
						if (isRecordExist(literal, ConclusionType.DEFINITE_NOT_PROVABLE)) pos = false;
						logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
						if (pos) {
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.APPICABLE);
							newLiteralFind_definiteProvable(literal, false);
						} else {
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.DISCARDED);
							newLiteralFind_definiteNotProvable(literal, false);
						}
					}
				}
				break;
			case DEFEASIBLY_PROVABLE:
				if (!pendingConclusions[1].contains(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal)) //
						|| !isRecordExist(literal, ConclusionType.DEFEASIBLY_PROVABLE)) {
					ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosConclusions,
							ConclusionType.DEFEASIBLY_PROVABLE);
					if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals,
							RuleType.DEFEASIBLE);
					if (ambiguousExist) {
						logMessage(Level.FINEST, 1, "==> generatePendingConclusions: ==> add (+d Ambiguous)", literal);
						addAmbiguousConclusion(conclusion, ruleLabels);
					} else {
						if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) pos = false;
						if (isRecordExist(literal, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) pos = false;
						logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
						if (pos) {
							addPendingConclusion(conclusion);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.APPICABLE);
							newLiteralFind_defeasiblyProvable(literal, false);
						} else {
							Conclusion c = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
							addPendingConclusion(c);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.DISCARDED);
							newLiteralFind_defeasiblyNotProvable(literal, false);
						}
					}
				}
				break;
			default:
			}
		}
		logMessage(Level.FINER, 1, "=== +ve set - end ===");

		logMessage(Level.FINER, 1, "=== -df set - start ===");
		for (Literal literal : unprovedDefeasibleRuleLiterals) {
			if (!isAmbiguousConclusionExist(literal, ConclusionType.DEFEASIBLY_PROVABLE)) {
				Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
				if (!unresolveableConclusionsSet.contains(conclusion)) {
					addPendingConclusion(conclusion);
					addRecord(conclusion);
					newLiteralFind_defeasiblyNotProvable(literal, false);
					addInapplicableLiteralsBeforeInference(literal, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
				}
			}
		}
		logMessage(Level.FINER, 1, "=== -df set -  end  ===");

		// literals that do not exist in strict rule or
		// literals cannot be resolved using only strict rules are definite not provable
		if (!isDefeasibleRuleOnly) {
			logMessage(Level.FINEST, 1, "=== -Df set - start ===");
			for (Literal literal : unprovedStrictRuleLiterals) {
				newLiteralFind_definiteNotProvable(literal, false);
				addInapplicableLiteralsBeforeInference(literal, ConclusionType.DEFINITE_NOT_PROVABLE);
			}
			logMessage(Level.FINEST, 1, "=== -Df set -  end  ===");
		}
		logMessage(Level.FINE, 0, "=== SdlReasoningEngine.generatePendingConclusions -  end  ===");
	}

	@Override
	protected ProcessStatus generateConclusions_definiteProvable(final Literal literal)
			throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINE, 0, "=== generate inference: definite provable: ", literal);

		Set<String> rulesToDelete = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, null);

		// modified for new algorithm - start
		provableRulesSuperiorityUpdate(rulesModified);
		// modified for new algorithm - end

		if (rulesModified.size() == 0) return ProcessStatus.SUCCESS;

		for (Rule r : rulesModified) {
			RuleExt rule = (RuleExt) r;
			logMessage(Level.FINER, 1, null, literal, ": rule=", rule, ", is empty body=", rule.isEmptyBody());
			if (rule.isEmptyBody()) {
				logMessage(Level.FINER, 2, "remove rule:", rule.getLabel());

				Literal headLiteral = rule.getHeadLiterals().get(0);
				List<Literal> conflictLiterals = null;
				switch (rule.getRuleType()) {
				case STRICT:
					logMessage(Level.FINEST, 1, "==> (strict) ", literal);
					rulesToDelete.add(rule.getLabel());
					conflictLiterals = getConflictLiterals(headLiteral);
					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
						logMessage(Level.FINEST, 2,
								"==>1.1 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
						addRecord(conclusion);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
						if (chk1 || chk2) {
							logMessage(Level.FINEST, 2,
									"==>1.2 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
							Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
							addRecord(conclusion);
							addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						} else {
							boolean hasConflictRecord = false;
							if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) hasConflictRecord = true;
							if (isRecordExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE)) hasConflictRecord = true;
							if (hasConflictRecord) {
								if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
										rule.getOriginalLabel(), RuleType.STRICT, ConclusionType.DEFINITE_NOT_PROVABLE,
										headLiteral, RuleInferenceStatus.DISCARDED);
								newLiteralFind_definiteNotProvable(headLiteral, true);
							} else {
								if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
										rule.getOriginalLabel(), RuleType.STRICT, ConclusionType.DEFINITE_PROVABLE,
										headLiteral, RuleInferenceStatus.APPICABLE);
								newLiteralFind_definiteProvable(headLiteral, true);
							}
						}
					}
					break;
				case DEFEASIBLE:
					if (rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0) {
						logMessage(Level.FINER, 1, "==> (defeasible) ", literal);
						rulesToDelete.add(rule.getLabel());
						// same as 'generateConclusions_defeasiblyProvable(Literal literal)'
						// duplicated here for efficiency
						conflictLiterals = getConflictLiterals(headLiteral);
						if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
							logMessage(Level.FINEST, 2,
									"==>1.5 generateConclusions_definiteProvable: ==> add ambiguous (+d)", headLiteral);
							Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
							addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
							addRecord(conclusion);
						} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
							// do nothing
						} else {
							boolean chk1 = isAmbiguousConclusionExist(headLiteral,
									ConclusionType.DEFEASIBLY_NOT_PROVABLE);
							boolean chk2 = isAmbiguousConclusionExist(conflictLiterals,
									ConclusionType.DEFEASIBLY_PROVABLE);
							if (chk1 || chk2) {
								logMessage(Level.FINEST, 2,
										"==>1.6 generateConclusions_definiteProvable: ==> add ambiguous (+D)",
										headLiteral);
								addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
										rule.getOriginalLabel());
							} else {
								boolean hasConflictRecord = false;
								if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) hasConflictRecord = true;
								if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) hasConflictRecord = true;
								if (hasConflictRecord) {
									if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
											rule.getOriginalLabel(), RuleType.DEFEASIBLE,
											ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral,
											RuleInferenceStatus.DISCARDED);
									newLiteralFind_defeasiblyNotProvable(headLiteral, true);
								} else {
									if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
											rule.getOriginalLabel(), RuleType.DEFEASIBLE,
											ConclusionType.DEFEASIBLY_PROVABLE, headLiteral,
											RuleInferenceStatus.APPICABLE);
									newLiteralFind_defeasiblyProvable(headLiteral, true);
								}
							}
						}
					}
					break;
				default:
				}
			}
		}
		removeRules(rulesToDelete);

		return ProcessStatus.SUCCESS;
	}

	@Override
	protected ProcessStatus generateConclusions_defeasiblyProvable(final Literal literal)
			throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINE, 1, "generate inference: defeasibly provable: ", literal);

		Set<String> rulesToRemove = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, RuleType.DEFEASIBLE);

		// modified for new algorithm - start
		provableRulesSuperiorityUpdate(rulesModified);
		// modified for new algorithm - end

		if (rulesModified.size() == 0) return ProcessStatus.SUCCESS;

		for (Rule r : rulesModified) {
			RuleExt rule = (RuleExt) r;
			logMessage(Level.FINER, 2, null, literal, ": rule=", rule, ", is empty body=", rule.isEmptyBody());
			if (rule.isEmptyBody() && rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0) {
				Literal headLiteral = rule.getHeadLiterals().get(0);
				rulesToRemove.add(rule.getLabel());
				List<Literal> conflictLiterals = getConflictLiterals(headLiteral);

				boolean containsUnprovedRuleInTheory = containsUnprovedRuleInTheory(conflictLiterals,
						RuleType.DEFEASIBLE);
				logMessage(Level.FINEST, 2, "==>2.0 conflictLiterals: ", conflictLiterals, ",unproved rule in theory="
						+ containsUnprovedRuleInTheory);
				if (containsUnprovedRuleInTheory) {
					logMessage(Level.FINEST, 2,
							"==>2.1 generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)", headLiteral);
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
					addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					addRecord(conclusion);
				} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
				} else {
					boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
					boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					if (chk1 || chk2) {
						logMessage(Level.FINEST, 2,
								"==>2.2 generateConclusions_defeasiblyProvable: ==> add ambiguous (+D)", headLiteral);
						addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
								rule.getOriginalLabel());
					} else {
						boolean hasConflictRecord = false;
						if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) hasConflictRecord = true;
						if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) hasConflictRecord = true;
						if (hasConflictRecord) {
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
									rule.getOriginalLabel(), RuleType.DEFEASIBLE,
									ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
							newLiteralFind_defeasiblyNotProvable(headLiteral, true);
						} else {
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
									rule.getOriginalLabel(), RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_PROVABLE,
									headLiteral, RuleInferenceStatus.APPICABLE);
							newLiteralFind_defeasiblyProvable(headLiteral, true);
						}
					}
				}
			}
		}
		logMessage(Level.FINE, 2, "generateConclusions_defeasiblyProvable.removeRules=", rulesToRemove);
		removeRules(rulesToRemove);
		return ProcessStatus.SUCCESS;
	}

	protected void provableRulesSuperiorityUpdate(Set<Rule> rulesModified) throws ReasoningEngineException,
			TheoryException {
		if (rulesModified.size() == 0) return;

		Set<String> defeatedRulesStr = new TreeSet<String>();
		for (Rule rule : rulesModified) {
			Set<String> inferiorlyDefeatedRulesSet = getDefeatedRulesInTheory(rule.getLabel());
			defeatedRulesStr.addAll(inferiorlyDefeatedRulesSet);
		}

		if (defeatedRulesStr.size() == 0) return;
		logMessage(Level.FINE, 2, "defeatedRulesStr=", defeatedRulesStr);

		for (String ruleLabel : defeatedRulesStr) {
			Rule rule = theory.getRule(ruleLabel);
			rulesModified.remove(rule);
		}
		removeDefeatedRulesWithInference(defeatedRulesStr, true);
	}

	protected void removeDefeatedRulesWithInference(Set<String> defeatedRulesStr, boolean checkInference)
			throws ReasoningEngineException {
		try {
			List<Rule> defeatedRules = new ArrayList<Rule>();
			for (String ruleLabel : defeatedRulesStr) {
				Rule rule = theory.getRule(ruleLabel);
				defeatedRules.add(rule);
				if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
						RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, rule.getHeadLiterals().get(0),
						RuleInferenceStatus.DEFEATED);
			}
			removeRules(defeatedRulesStr);
			for (Rule rule : defeatedRules) {
				Literal literal = rule.getHeadLiterals().get(0);
				if (!theory.containsUnprovedRule(literal, RuleType.DEFEASIBLE)) {
					newLiteralFind_defeasiblyNotProvable(literal, checkInference);
				}
			}
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	@Override
	protected Conclusion getNextPendingConclusion() throws ReasoningEngineException {
		Conclusion pendingConclusion = null;
		int totalPendingConclusionsCount = 0;

		for (int i = 0; i < pendingConclusions.length && null == pendingConclusion; i++) {
			totalPendingConclusionsCount += pendingConclusions[i].size();

			if (totalPendingConclusionsCount == 0) {
				// flag used to indicate the last update of the scc literals groups
				int sccLiteralUpdated = -1;

				// modification start
				// try generating the conclusion using superiority relations
				if (i == 1 && pendingConclusions[i].size() == 0) {
					if (!Conf.isReasoningWithWellFoundedSemantics() && ambiguousConclusions[i].size() == 0) {
						logMessage(Level.FINEST, 1, "check for inferiorly defeated rules");
						int theorySize = theory.getFactsAndAllRules().size();
						do {
							theorySize = theory.getFactsAndAllRules().size();
							Set<RuleExt> weakestRules = new TreeSet<RuleExt>();
							for (Superiority sup : theory.getAllSuperiority()) {
								RuleExt infRule = (RuleExt) theory.getRule(sup.getInferior());
								if (infRule.getWeakerRulesCount() == 0 && infRule.getStrongerRulesCount() > 0) weakestRules
										.add(infRule);
							}
							if (weakestRules.size() > 0) {
								Set<RuleExt> rulesToVerify = new TreeSet<RuleExt>();
								Set<String> rulesToRemove = new TreeSet<String>();
								for (RuleExt rule : weakestRules) {
									rulesToVerify.add(rule);
								}
								getSccLiteralsGroupInTheory();
								sccLiteralUpdated = 1;
								verifyWeakestRuleInTheory(weakestRules, rulesToRemove);
								if (rulesToRemove.size() > 0) {
									if (isLogInferenceProcess) {
										for (String ruleLabel : rulesToRemove) {
											Rule rule = theory.getRule(ruleLabel);
											getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
													RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE,
													rule.getHeadLiterals().get(0), RuleInferenceStatus.DEFEATED);
										}
									}
									removeRules(rulesToRemove);
								}
							}
						} while (theorySize > theory.getFactsAndAllRules().size());
					}
					try {
						generatePendingConclusions(true);
					} catch (Exception e) {
						throw new ReasoningEngineException(getClass(), e);
					}
				}
				// modification end

				if (pendingConclusions[i].size() == 0 && ambiguousConclusions[i].size() > 0) {
					// update ambiguous conclusions found
					if (!AppConst.isDeploy) logMessage(Level.FINEST, 1, "search for ambiguous conclusions, lvl=1, i=",
							i);

					if (sccLiteralUpdated != 1) getSccLiteralsGroupInTheory();
					sccLiteralUpdated = 2;
					updateAmbiguousConclusions(i);
					totalPendingConclusionsCount += pendingConclusions[i].size();
				}
				if (pendingConclusions[i].size() == 0 && ambiguousConclusions[i].size() > 0) {
					updateSccAmbiguousConclusions(i, Conf.isReasoningWithWellFoundedSemantics());
				}

				// well-found semantics
				// - check for strongly connected literals if no new pending conclusions found after ambiguity check
				if (Conf.isReasoningWithWellFoundedSemantics() //
						&& i == pendingConclusions.length - 1 //
						&& totalPendingConclusionsCount == 0 //
						&& theory.getFactsAndAllRules().size() > 0) {
					if (!AppConst.isDeploy) logMessage(Level.FINEST, 1,
							"Analyse theory for strongly connected literals, i=", i);

					try {
						if (updateStronglyConnectedComponents(sccLiteralUpdated != 2) > 0) {
							int jj = 0;
							while (jj < pendingConclusions.length && pendingConclusions[jj].size() == 0) {
								jj++;
							}
							if (i > jj) i = jj;
						}
					} catch (ReasoningEngineException e) {
						throw e;
					} catch (Exception e) {
						throw new ReasoningEngineException(getClass(),
								"Exception throw while generating strongly connected components", e);
					}
				}
			}
			if (pendingConclusions[i].size() > 0) {
				return pendingConclusions[i].removeFirst();
				// Iterator<Conclusion> it = pendingConclusions[i].iterator();
				// pendingConclusion = it.next();
				// it.remove();
			} else {
				// some loop in the theory may occur
			}
		}
		return pendingConclusion;
	}

}
