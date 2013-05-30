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
package spindle.engine.mdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.TheoryException;
import spindle.engine.ReasoningEngineException;
import spindle.sys.AppConst;
import spindle.tools.explanation.RuleInferenceStatus;

import com.app.utils.Utilities.ProcessStatus;

/**
 * MDL Reasoning Engine with ambiguous propagation.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.20
 */
public class MdlReasoningEngineAP extends MdlReasoningEngine {

	public MdlReasoningEngineAP() {
		super();
	}

	@Override
	protected ProcessStatus generateInitialPendingConclusions() throws ReasoningEngineException, TheoryException {
		// literals appeared in strict rule with empty body are definitely provable
		// literals appeared in defeasible rule with empty body are tentatively provable
		logMessage(Level.FINE, 0, "=== SdlReasoningEngine.generateInitialPendingConclusions - start ===");

		Set<Literal> unprovedStrictRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<Literal> unprovedDefeasibleRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<String> rulesToDelete = new TreeSet<String>();

		logMessage(Level.FINER, 1, "=== +ve set - start ===");
		Set<String> rulesToRemove = new TreeSet<String>();

		Map<Conclusion, Set<String>> tempPosDefiniteConclusionSet = new TreeMap<Conclusion, Set<String>>();
		Map<Conclusion, Set<String>> tempPosDefeasibleConclusionSet = new TreeMap<Conclusion, Set<String>>();
		Set<String> ruleSet = null;

		for (Rule rule : theory.getFactsAndAllRules().values()) {
			Literal literal = rule.getHeadLiterals().get(0);
			switch (rule.getRuleType()) {
			case STRICT:
				if (rule.isEmptyBody()) {
					Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal);
					addRecord(conclusion);

					ruleSet = tempPosDefiniteConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosDefiniteConclusionSet.put(conclusion, ruleSet);
					}
					ruleSet.add(rule.getOriginalLabel());

					rulesToRemove.add(rule.getLabel());
				}
				unprovedStrictRuleLiterals.remove(literal);
				break;
			case DEFEASIBLE:
				if (rule.isEmptyBody()) {
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal);
					addRecord(conclusion);

					ruleSet = tempPosDefeasibleConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosDefeasibleConclusionSet.put(conclusion, ruleSet);
					}
					ruleSet.add(rule.getOriginalLabel());

					rulesToRemove.add(rule.getLabel());
				}
				unprovedDefeasibleRuleLiterals.remove(literal);
				break;
			default:
			}
		}

		removeRules(rulesToRemove);
		if (!AppConst.isDeploy) {printPendingConclusionSet(unprovedStrictRuleLiterals, unprovedDefeasibleRuleLiterals,
				tempPosDefiniteConclusionSet.keySet(),tempPosDefeasibleConclusionSet.keySet());
		}

		TreeSet<Literal> tempPosDefiniteConclusions = extractLiteralsFromConclusions(tempPosDefiniteConclusionSet.keySet());
		TreeSet<Literal> tempPosDefeasibleConclusions = extractLiteralsFromConclusions(tempPosDefeasibleConclusionSet.keySet());

		for (Entry<Conclusion, Set<String>> entry : tempPosDefiniteConclusionSet.entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			boolean ambiguousExist = false, pos = true;
			Literal literal = conclusion.getLiteral();
			Set<Literal> conflictLiterals = getConflictLiterals(literal);

		//	switch (conclusion.getConclusionType()) {
	//		case DEFINITE_PROVABLE:
				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosDefiniteConclusions);//,ConclusionType.DEFINITE_PROVABLE);
				if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT);
				if (ambiguousExist) {
					logMessage(Level.FINER, 1, "==> generateInitialPendingConclusions: ==> add (+D Ambiguous)", literal);
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
//				break;
			}
				for (Entry<Conclusion, Set<String>> entry : tempPosDefeasibleConclusionSet.entrySet()) {
					Conclusion conclusion = entry.getKey();
					Set<String> ruleLabels = entry.getValue();

					boolean ambiguousExist = false, pos = true;
					Literal literal = conclusion.getLiteral();
					Set<Literal> conflictLiterals = getConflictLiterals(literal);
//			case DEFEASIBLY_PROVABLE:
				// -- for AP - start
				addRecord(new Conclusion(ConclusionType.POSITIVELY_SUPPORT, conclusion.getLiteral()));
				// -- for AP - end

				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosDefeasibleConclusions);//,ConclusionType.DEFEASIBLY_PROVABLE);
				if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals,
						RuleType.DEFEASIBLE);
				if (ambiguousExist) {
					logMessage(Level.FINER, 1, "==> generateInitialPendingConclusions: ==> add (+d Ambiguous)", literal);
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
				//break;
			//default:
		//	}
		}
		logMessage(Level.FINER, 1, "=== +ve set - end ===");

		// literals cannot be resolved using all rules are defeasibly not provable
		logMessage(Level.FINER, 1, "=== -df set - start ===");
		for (Literal literal : unprovedDefeasibleRuleLiterals) {
			logMessage(Level.FINEST, 2, "rule.getLiteralList()(NOT_DEFEASIBLE)=", literal);
			Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
			if (!unresolveableConclusionsSet.contains(conclusion)) {
				addPendingConclusion(conclusion);
				addRecord(conclusion);
				// -- for AP - start
				addRecord(new Conclusion(ConclusionType.NEGATIVELY_SUPPORT, literal));
				// -- for AP - end
				newLiteralFind_defeasiblyNotProvable(literal, false);
				addInapplicableLiteralsBeforeInference(literal, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
			}
		}
		logMessage(Level.FINER, 1, "=== -df set -  end  ===");

		// literals that do not exist in strict rule or
		// literals cannot be resolved using only strict rules are definite not provable
		logMessage(Level.FINER, 1, "=== -Df set - start ===");
		for (Literal literal : unprovedStrictRuleLiterals) {
			newLiteralFind_definiteNotProvable(literal, false);
			addInapplicableLiteralsBeforeInference(literal, ConclusionType.DEFINITE_NOT_PROVABLE);
		}
		logMessage(Level.FINER, 1, "=== -Df set -  end  ===");
		logMessage(Level.FINE, 0, "=== SdlReasoningEngine.generateInitialPendingConclusions -  end  ===");

		removeRules(rulesToDelete);

		return ProcessStatus.SUCCESS;
	}

	// remove ambiguity caused by complementary literal
	@Override
	protected void removeComplementLiteralAmbiguity(int i) {
		logMessage(Level.FINE, 1, "=== removeComplementLiteralAmbiguity - start ===");
		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();
		List<Conclusion> recordsToRemove = new ArrayList<Conclusion>();

		for (Entry<Conclusion, Set<String>> entry : ambiguousConclusions[i].entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			Literal literal = conclusion.getLiteral();
			Set<Literal> conflictLiterals = getConflictLiteralListWithoutOperatorChange(literal);
			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE: // same as ambiguity blocking
				logMessage(Level.FINER, 2, "removeComplementLiteralAmbiguity, check literal (definite): ", literal);
				if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
					if (isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) {
						ambiguousConclusionToRemove.add(conclusion);
						recordsToRemove.add(new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal));
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
								RuleType.STRICT, ConclusionType.DEFINITE_NOT_PROVABLE, literal,
								RuleInferenceStatus.DEFEATED);
						newLiteralFind_definiteNotProvable(literal, true);
					}
				}
				break;
			case DEFEASIBLY_PROVABLE:
				logMessage(Level.FINER, 2, "removeComplementLiteralAmbiguity, check literal (defeasible): ", literal);
				if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
					boolean dchk4 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					// ambiguity propagation - start
					boolean dchk5 = isRecordExist(conflictLiterals, ConclusionType.POSITIVELY_SUPPORT);
					boolean dchk6 = isRecordExist(literal, ConclusionType.AMBIGUITY_DEFEATED);
					boolean dchk7 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					if (!AppConst.isDeploy) {
						logMessage(Level.INFO, 0, "*** dchk4=" + dchk4);
						logMessage(Level.INFO, 0, "*** dchk5=" + dchk5);
						logMessage(Level.INFO, 0, "*** dchk6=" + dchk6);
						logMessage(Level.INFO, 0, "*** dchk7=" + dchk7);
					}
					if (dchk7) {
						ambiguousConclusionToRemove.add(conclusion);
						recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
								RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal,
								RuleInferenceStatus.DEFEATED);
						newLiteralFind_defeasiblyNotProvable(literal, true);
						if (!literal.isPlaceHolder()) {
							addRecord(new Conclusion(ConclusionType.AMBIGUITY_DEFEATED, literal));
							for (Literal cl : conflictLiterals) {
								addRecord(new Conclusion(ConclusionType.AMBIGUITY_DEFEATED, cl));
							}
						}
					} else {
						if ((dchk5 && dchk4) || dchk6) {
							ambiguousConclusionToRemove.add(conclusion);
							addRecord(new Conclusion(ConclusionType.AMBIGUITY_DEFEATED, literal));
							recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal,
									RuleInferenceStatus.DEFEATED);
							newLiteralFind_defeasiblyNotProvable(literal, true);
						}
					}
					// ambiguity propagation - end
				}
				break;
			default:
			}
		}

		for (Conclusion conclusion : ambiguousConclusionToRemove) {
			ambiguousConclusions[i].remove(conclusion);
		}

		for (Conclusion record : recordsToRemove) {
			removeRecord(record);
		}
		logMessage(Level.FINE, 1, "=== removeComplementLiteralAmbiguity -  end  ===");
	}

	// === ========================================= ===
	// === extract from SdlReasoningEngineAP - start ===
	// === ========================================= ===
	@Override
	protected ProcessStatus generateConclusions_definiteProvable(final Literal literal)
			throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINE, 0, "=== generate inference: definite provable: ", literal);

		Set<String> rulesToDelete = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, null);
		for (Rule rule : rulesModified) {
			logMessage(Level.FINER, 1, null, literal, ": rule=", rule, ", is empty body=", rule.isEmptyBody());
			if (rule.isEmptyBody()) {
				Literal headLiteral = rule.getHeadLiterals().get(0);
				Set<Literal> conflictLiterals = null;
				rulesToDelete.add(rule.getLabel());
				switch (rule.getRuleType()) {
				case STRICT:
					logMessage(Level.FINEST, 1, "==> (strict) ", literal);
					conflictLiterals = getConflictLiterals(headLiteral);

					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
						logMessage(Level.FINEST, 2, "==> generateConclusions_definiteProvable: ==> add ambiguous (+D)",
								headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
						addRecord(conclusion);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
						if (chk1 || chk2) {
							logMessage(Level.FINEST, 2,
									"==> generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
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
					// }
					break;
				case DEFEASIBLE:
					// ambiguity propagation - start
					addRecord(new Conclusion(ConclusionType.POSITIVELY_SUPPORT, headLiteral));
					// ambiguity propagation - end

					logMessage(Level.FINER, 1, "==> (defeasible) ", literal);
					// same as 'generateConclusions_defeasiblyProvable(Literal literal)'
					// duplicated here for efficiency
					conflictLiterals = getConflictLiterals(headLiteral);
					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
						logMessage(Level.FINEST, 2, "==> generateConclusions_definiteProvable: ==> add ambiguous (+d)",
								headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						addRecord(conclusion);
					} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
						// do nothing
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
						boolean chk3 = isRecordExist(conflictLiterals, ConclusionType.AMBIGUITY_DEFEATED);
						if (chk1 || chk2) {
							logMessage(Level.FINEST, 2,
									"==> generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
							addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
									rule.getOriginalLabel());
							// -- for AP - start
						} else if (chk3) {
							removeRecord(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
									rule.getOriginalLabel(), RuleType.DEFEASIBLE,
									ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DEFEATED);
							newLiteralFind_defeasiblyNotProvable(literal, true);
							// -- for AP - end
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
										ConclusionType.DEFEASIBLY_PROVABLE, headLiteral, RuleInferenceStatus.APPICABLE);
								newLiteralFind_defeasiblyProvable(headLiteral, true);
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
		logMessage(Level.FINE, 0, "generate inference: defeasibly provable: ", literal);

		Set<String> rulesToRemove = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, RuleType.DEFEASIBLE);
		for (Rule rule : rulesModified) {
			logMessage(Level.FINER, 1, null, literal, ": rule=", rule, ", is empty body=", rule.isEmptyBody());
			if (rule.isEmptyBody()) {
				Literal headLiteral = rule.getHeadLiterals().get(0);
				// ambiguity propagation - start
				addRecord(new Conclusion(ConclusionType.POSITIVELY_SUPPORT, headLiteral));
				// ambiguity propagation - end
				rulesToRemove.add(rule.getLabel());
				Set<Literal> conflictLiterals = getConflictLiterals(headLiteral);
				if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
					logMessage(Level.FINEST, 2, "==> generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)",
							headLiteral);
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
					addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					addRecord(conclusion);
				} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
				} else {
					boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
					boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					boolean chk3 = isRecordExist(conflictLiterals, ConclusionType.AMBIGUITY_DEFEATED);
					if (chk1 || chk2) {
						logMessage(Level.FINEST, 2,
								"==> generateConclusions_defeasiblyProvable: ==> add ambiguous (+D)", headLiteral);
						addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
								rule.getOriginalLabel());
						// -- for AP - start
					} else if (chk3) {
						removeRecord(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(
								rule.getOriginalLabel(), RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE,
								headLiteral, RuleInferenceStatus.DEFEATED);
						newLiteralFind_defeasiblyNotProvable(literal, true);
						// -- for AP - end
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
		removeRules(rulesToRemove);
		return ProcessStatus.SUCCESS;
	}

	@Override
	protected ProcessStatus generateConclusions_defeasiblyNotProvable(final Literal literal)
			throws ReasoningEngineException {
		logMessage(Level.FINE, 0, "generate inference: defeasibly not provable: ", literal);

		Collection<Rule> rules = theory.getRules(literal).values();
		Set<String> rulesToRemove = new TreeSet<String>();
		List<Literal> inapplicableLiterals = new ArrayList<Literal>();

		if (rules == null) return ProcessStatus.SUCCESS;
		boolean ambiguityDefeated = isRecordExist(literal, ConclusionType.AMBIGUITY_DEFEATED);
		ConclusionType literalConclusionType = isRecordExist(literal, ConclusionType.POSITIVELY_SUPPORT) ? ConclusionType.POSITIVELY_SUPPORT
				: ConclusionType.NEGATIVELY_SUPPORT;
		for (Rule rule : rules) {
			Literal headLiteral = rule.getHeadLiterals().get(0);
			// ambiguity propagation - start
			addRecord(new Conclusion(literalConclusionType, headLiteral));
			if (ambiguityDefeated) {
				addRecord(new Conclusion(ConclusionType.AMBIGUITY_DEFEATED, headLiteral));
				for (Literal cl : getConflictLiterals(headLiteral)) {
					addRecord(new Conclusion(ConclusionType.AMBIGUITY_DEFEATED, cl));
				}
			}
			// ambiguity propagation - end
			if (rule.getRuleType() == RuleType.DEFEASIBLE && rule.isBodyLiteral(literal)) {
				rulesToRemove.add(rule.getLabel());
				inapplicableLiterals.add(headLiteral);

				if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
						RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral,
						RuleInferenceStatus.DISCARDED);
			}
		}

		removeRules(rulesToRemove);

		// check whether the literal can be proved by other strict rules
		// if not, add -d to the pending conclusion list
		for (Literal inapplicableLiteral : inapplicableLiterals) {
			logMessage(Level.FINER, 1, "checking literal for other rules", inapplicableLiteral);
			if (theory.containsInRuleHead(inapplicableLiteral, RuleType.DEFEASIBLE)) {
				// literal still provable by other strict rules, so do nothing
				logMessage(Level.FINEST, 2, "literal is provable by other defeasible rules", inapplicableLiteral);
			} else {
				Set<Literal> conflictLiterals = getConflictLiterals(inapplicableLiteral);
				boolean acChk1 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
				boolean acChk2 = isAmbiguousConclusionExist(inapplicableLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
				boolean recChk1 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
				boolean recChk2 = isRecordExist(inapplicableLiteral, ConclusionType.DEFEASIBLY_PROVABLE);

				if (acChk1 || acChk2 || !(recChk1 || recChk2)) {
					// add to new conclusion list
					newLiteralFind_defeasiblyNotProvable(inapplicableLiteral, true);
				}
			}
		}

		return ProcessStatus.SUCCESS;
	}
	// === ======================================= ===
	// === extract from SdlReasoningEngineAP - end ===
	// === ======================================= ===

}
