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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import com.app.utils.TextUtilities;
import com.app.utils.Utilities.ProcessStatus;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.TheoryException;
import spindle.engine.ReasoningEngineBase;
import spindle.engine.ReasoningEngineException;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;
import spindle.tools.analyser.TheoryAnalyser;
import spindle.tools.explanation.RuleInferenceStatus;

/**
 * SDL Reasoning Engine.
 * <p>
 * This reasoning engine is based on the algorithm presented in:
 * <ul>
 * <li>Maher, M.J. (2001) Propositional Defeasible Logic has Linear Complexity, <i>Theory and Practice of Logic
 * Programming</i>, 1:6:691-711, Cambridge University Press</li>
 * </ul>
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @see SdlReasoningEngine2
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public class SdlReasoningEngine extends ReasoningEngineBase {
	public static final String STRICT_TO_DEFEASIBLE_POSTFIX = "_s2d";

	/**
	 * pending conclusions set
	 */
	protected Deque<Conclusion>[] pendingConclusions = null;

	/**
	 * Used to hold possibly conflicting conclusions in the theory temporary.<br>
	 * That is in the condition that when [+D q] is concluded from one rule,<br>
	 * another rule containing [+D -q] is still pending to process. <br>
	 * Conclusion will be added to the conclusions set only when all its related
	 * consequence are computed
	 */
	protected Map<Conclusion, Set<String>>[] ambiguousConclusions = null;
	protected Map<Literal, Map<ConclusionType, Conclusion>> conclusions = null;
	protected List<Set<Literal>> sccLiteralsGroups = null;
	protected Set<Conclusion> unresolveableConclusionsSet = null;
	protected boolean theoryWithLoops = true;

	public SdlReasoningEngine() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ReasoningEngineException {
		pendingConclusions = new ArrayDeque[2];
		ambiguousConclusions = new TreeMap[2];
		unresolveableConclusionsSet = new TreeSet<Conclusion>();
		sccLiteralsGroups = null;

		for (int i = 0; i < 2; i++) {
			pendingConclusions[i] = new ArrayDeque<Conclusion>();
			ambiguousConclusions[i] = new TreeMap<Conclusion, Set<String>>();
		}
		conclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();

		duplicateStrictRulesToDefeasibleRules();
		strictRules = theory.getRules(RuleType.STRICT);
		defeasibleRules = theory.getRules(RuleType.DEFEASIBLE);

		theoryWithLoops = true;

		if (!AppConst.isDeploy) {
			logMessage(Level.INFO, 0, "=== SdlReasoningEngine.initialize - theory@start - start ===");
			logMessage(Level.INFO, 0, null, theory);
			logMessage(Level.INFO, 0, "=== SdlReasoningEngine.initialize - theory@start -  end  ===");
		}
		try {
			generateInitialPendingConclusions();
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	@Override
	protected void generateConclusions() throws ReasoningEngineException {
		Conclusion conclusion = null;
		if (!AppConst.isDeploy) printEngineStatus("generateConclusions - start");

		try {
			while ((conclusion = getNextPendingConclusion()) != null) {
				if (isConclusionExist(conclusion)) continue;
				logMessage(Level.FINE, 0, "generate conclusion for ", conclusion);

				addConclusion(conclusion);
				switch (conclusion.getConclusionType()) {
				case DEFINITE_PROVABLE:
					generateConclusions_definiteProvable(conclusion.getLiteral());
					break;
				case DEFINITE_NOT_PROVABLE:
					generateConclusions_definiteNotProvable(conclusion.getLiteral());
					break;
				case DEFEASIBLY_PROVABLE:
					generateConclusions_defeasiblyProvable(conclusion.getLiteral());
					break;
				case DEFEASIBLY_NOT_PROVABLE:
					generateConclusions_defeasiblyNotProvable(conclusion.getLiteral());
					break;
				default:
				}
				if (!AppConst.isDeploy)
					printEngineStatus("generateConclusions-after inflencing with [" + conclusion + "]");
			}
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	@Override
	protected void terminate() throws ReasoningEngineException {
		if (!AppConst.isDeploy) printEngineStatus("terminate");

		conclusions = verifyConclusions(conclusions);
		setConclusion(conclusions);
	}

	protected ProcessStatus generateInitialPendingConclusions() throws ReasoningEngineException, TheoryException {
		// literals appeared in strict rule with empty body are definitely provable
		// literals appeared in defeasible rule with empty body are tentatively provable
		logMessage(Level.FINE, 0, "=== SdlReasoningEngine.generateInitialPendingConclusions - start ===");

		Set<Literal> unprovedStrictRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<Literal> unprovedDefeasibleRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<String> rulesToDelete = new TreeSet<String>();

		logMessage(Level.FINER, 1, "=== +ve set - start ===");
		Set<String> rulesToRemove = new TreeSet<String>();
		Map<Conclusion, Set<String>> tempPosConclusionSet = new TreeMap<Conclusion, Set<String>>();

		Set<String> ruleSet = null;

		for (Rule rule : theory.getFactsAndAllRules().values()) {
			Literal literal = rule.getHeadLiterals().get(0);
			switch (rule.getRuleType()) {
			case STRICT:
				if (rule.isEmptyBody()) {
					Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal);
					addRecord(conclusion);

					ruleSet = tempPosConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosConclusionSet.put(conclusion, ruleSet);
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
					ruleSet = tempPosConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosConclusionSet.put(conclusion, ruleSet);
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

		if (!AppConst.isDeploy) {
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
				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosConclusions,
						ConclusionType.DEFINITE_PROVABLE);

				if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT);
				if (ambiguousExist) {
					logMessage(Level.FINER, 1, "==> generateInitialPendingConclusions: ==> add (+D Ambiguous)", literal);
					addAmbiguousConclusion(conclusion, ruleLabels);
				} else {
					if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) pos = false;
					if (isRecordExist(literal, ConclusionType.DEFINITE_NOT_PROVABLE)) pos = false;
					logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
					if (pos) {
						newLiteralFind_definiteProvable(literal, false);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.APPICABLE);
					} else {
						newLiteralFind_definiteNotProvable(literal, false);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.DISCARDED);
					}
				}
				break;
			case DEFEASIBLY_PROVABLE:
				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosConclusions,
						ConclusionType.DEFEASIBLY_PROVABLE);

				if (!ambiguousExist)
					ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE);
				if (ambiguousExist) {
					logMessage(Level.FINER, 1, "==> generateInitialPendingConclusions: ==> add (+d Ambiguous)", literal);
					addAmbiguousConclusion(conclusion, ruleLabels);
				} else {
					if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) pos = false;
					if (isRecordExist(literal, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) pos = false;
					logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
					if (pos) {
						addPendingConclusion(conclusion);
						newLiteralFind_defeasiblyProvable(literal, false);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.APPICABLE);
					} else {
						Conclusion c = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
						addPendingConclusion(c);
						newLiteralFind_defeasiblyNotProvable(literal, false);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.DISCARDED);
					}
				}
				break;
			default:
			}
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

	protected boolean containsUnprovedRuleInTheory(final List<Literal> literals, final RuleType ruleType) {
		for (Literal literal : literals) {
			if (theory.containsUnprovedRule(literal, ruleType, Conf.getReasonerVersion() == 1)) return true;
		}
		return false;
	}

	protected ProcessStatus generateConclusions_definiteProvable(final Literal literal)
			throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINER, 0, "=== generate inference: definite provable: ", literal);

		Set<String> rulesToDelete = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, null);

		if (rulesModified.size() == 0) return ProcessStatus.SUCCESS;

		for (Rule rule : rulesModified) {
			logMessage(Level.FINER, 1, null, literal, ": rule=", rule, ", is empty body=" + rule.isEmptyBody());
			if (rule.isEmptyBody()) {
				logMessage(Level.FINEST, 2, "remove rule:", rule.getLabel());
				if (!AppConst.isDeploy) logMessage(Level.FINER, 2, null, theory);
				rulesToDelete.add(rule.getLabel());

				Literal headLiteral = rule.getHeadLiterals().get(0);
				List<Literal> conflictLiterals = getConflictLiterals(headLiteral);
				switch (rule.getRuleType()) {
				case STRICT:
					logMessage(Level.FINER, 1, "==> ", literal, " (strict)");
					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
						logMessage(Level.FINER, 2,
								"==>1.1 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
						addRecord(conclusion);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
						if (chk1 || chk2) {
							logMessage(Level.FINER, 2,
									"==>1.2 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
							Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
							addRecord(conclusion);
							addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						} else {
							boolean hasConflictRecord = false;
							if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE))
								hasConflictRecord = true;
							if (isRecordExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE))
								hasConflictRecord = true;
							if (hasConflictRecord) {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
											RuleType.STRICT, ConclusionType.DEFINITE_NOT_PROVABLE, headLiteral,
											RuleInferenceStatus.DISCARDED);
								newLiteralFind_definiteNotProvable(headLiteral, true);
							} else {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
											RuleType.STRICT, ConclusionType.DEFINITE_PROVABLE, headLiteral,
											RuleInferenceStatus.APPICABLE);
								newLiteralFind_definiteProvable(headLiteral, true);
							}
						}
					}
					break;
				case DEFEASIBLE:
					logMessage(Level.FINEST, 1, "==> ", literal, " (defeasible)");
					// same as 'generateConclusions_defeasiblyProvable(Literal literal)'
					// duplicated here for efficiency
					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
						logMessage(Level.FINER, 2,
								"==>1.5 generateConclusions_definiteProvable: ==> add ambiguous (+d)", headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						addRecord(conclusion);
					} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
						// do nothing
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
						if (chk1 || chk2) {
							logMessage(Level.FINER, 2,
									"==>1.6 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
							addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
									rule.getOriginalLabel());
						} else {
							boolean hasConflictRecord = false;
							if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE))
								hasConflictRecord = true;
							if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE))
								hasConflictRecord = true;
							if (hasConflictRecord) {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
											RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral,
											RuleInferenceStatus.DISCARDED);
								newLiteralFind_defeasiblyNotProvable(headLiteral, true);
							} else {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
											RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_PROVABLE, headLiteral,
											RuleInferenceStatus.APPICABLE);
								newLiteralFind_defeasiblyProvable(headLiteral, true);
							}
						}
					}
					break;
				default:
				}
			}
		}
		if (rulesToDelete.size() > 0) removeRules(rulesToDelete);

		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus generateConclusions_definiteNotProvable(final Literal literal)
			throws ReasoningEngineException {
		logMessage(Level.FINER, 0, "=== generate inference: definite not provable: ", literal);

		Collection<Rule> rules = theory.getRules(literal).values();
		Set<String> rulesToRemove = new TreeSet<String>();
		List<Literal> inapplicableLiterals = new ArrayList<Literal>();

		for (Rule rule : rules) {
			if (rule.getRuleType() == RuleType.STRICT && rule.isBodyLiteral(literal)) {
				rulesToRemove.add(rule.getLabel());
				Literal headLiteral = rule.getHeadLiterals().get(0);
				logMessage(Level.FINER, 1, "literals added=", headLiteral);
				inapplicableLiterals.add(headLiteral);
				if (isLogInferenceProcess)
					getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.STRICT,
							ConclusionType.DEFINITE_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
			}
		}

		if (rulesToRemove.size() > 0) removeRules(rulesToRemove);

		// check whether the literal can be proved by other strict rules
		// if not, add -D to the pending conclusion list
		for (Literal inapplicableLiteral : inapplicableLiterals) {
			logMessage(Level.FINEST, 1, "checking inapplicable literals for other rules=", inapplicableLiteral);
			if (theory.containsInRuleHead(inapplicableLiteral, RuleType.STRICT)) {
				// literal still provable by other strict rules, so do nothing
				logMessage(Level.FINEST, 2, "literal [", inapplicableLiteral, "] is provable by other strict rules");
			} else {
				List<Literal> conflictLiterals = getConflictLiterals(inapplicableLiteral);
				boolean acChk1 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
				boolean acChk2 = isAmbiguousConclusionExist(inapplicableLiteral, ConclusionType.DEFINITE_NOT_PROVABLE);
				boolean recChk1 = isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
				boolean recChk2 = isRecordExist(inapplicableLiteral, ConclusionType.DEFINITE_PROVABLE);
				if (acChk1 || acChk2 || !(recChk1 || recChk2)) {
					// add to new conclusion list
					newLiteralFind_definiteNotProvable(inapplicableLiteral, true);
				}
			}
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus generateConclusions_defeasiblyProvable(final Literal literal)
			throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINER, 0, "generate inference: defeasibly provable: ", literal);

		Set<String> rulesToRemove = new TreeSet<String>();
		Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, RuleType.DEFEASIBLE);

		if (rulesModified.size() == 0) return ProcessStatus.SUCCESS;

		for (Rule rule : rulesModified) {
			logMessage(Level.FINER, 1, null, literal, ": rule=", rule, ", is empty body=" + rule.isEmptyBody());
			if (rule.isEmptyBody()) {
				Literal headLiteral = rule.getHeadLiterals().get(0);
				rulesToRemove.add(rule.getLabel());
				List<Literal> conflictLiterals = getConflictLiterals(headLiteral);

				boolean containsUnprovableRuleInTheory = containsUnprovedRuleInTheory(conflictLiterals,
						RuleType.DEFEASIBLE);
				logMessage(Level.FINEST, 2, "==>2.0 conflictLiterals: ", conflictLiterals, ",unproved rules="
						+ containsUnprovableRuleInTheory);
				if (containsUnprovableRuleInTheory) {
					logMessage(Level.FINER, 2, "==>2.1 generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)",
							headLiteral);
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
					addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					addRecord(conclusion);
				} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
				} else {
					boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
					boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					if (chk1 || chk2) {
						logMessage(Level.FINER, 2,
								"==>2.2 generateConclusions_defeasiblyProvable: ==> add ambiguous (+D)", headLiteral);
						addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
								rule.getOriginalLabel());
					} else {
						boolean hasConflictRecord = false;
						if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE))
							hasConflictRecord = true;
						if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE))
							hasConflictRecord = true;
						if (hasConflictRecord) {
							if (isLogInferenceProcess)
								getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
										RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral,
										RuleInferenceStatus.DISCARDED);
							newLiteralFind_defeasiblyNotProvable(headLiteral, true);
						} else {
							if (isLogInferenceProcess)
								getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(),
										RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_PROVABLE, headLiteral,
										RuleInferenceStatus.APPICABLE);
							newLiteralFind_defeasiblyProvable(headLiteral, true);
						}
					}
				}
			}
		}
		logMessage(Level.FINER, 1, "generateConclusions_defeasiblyProvable.removeRules=", rulesToRemove);
		if (rulesToRemove.size() > 0) removeRules(rulesToRemove);
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus generateConclusions_defeasiblyNotProvable(final Literal literal)
			throws ReasoningEngineException {
		logMessage(Level.FINER, 0, "generate inference: defeasibly not provable: ", literal);

		Collection<Rule> rules = theory.getRules(literal).values();
		Set<String> rulesToRemove = new TreeSet<String>();
		List<Literal> inapplicableLiterals = new ArrayList<Literal>();

		if (rules == null) return ProcessStatus.SUCCESS;

		for (Rule rule : rules) {
			if (rule.getRuleType() == RuleType.DEFEASIBLE && rule.isBodyLiteral(literal)) {
				rulesToRemove.add(rule.getLabel());
				Literal headLiteral = rule.getHeadLiterals().get(0);
				logMessage(Level.FINEST, 1, "literals added=", headLiteral);
				inapplicableLiterals.add(headLiteral);

				if (isLogInferenceProcess)
					getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
							ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
			}
		}

		if (rulesToRemove.size() > 0) removeRules(rulesToRemove);

		// check whether the literal can be proved by other strict rules
		// if not, add -d to the pending conclusion list
		for (Literal inapplicableLiteral : inapplicableLiterals) {
			logMessage(Level.FINEST, 1, "checking inapplicable literals for other rules: ", inapplicableLiteral);
			if (theory.containsInRuleHead(inapplicableLiteral, RuleType.DEFEASIBLE)) {
				// literal still provable by other strict rules, so do nothing
				logMessage(Level.FINEST, 2, "literal [", inapplicableLiteral, "] is provable by other defesible rules");
			} else {
				List<Literal> conflictLiterals = getConflictLiterals(inapplicableLiteral);
				boolean acChk1 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
				boolean acChk2 = isAmbiguousConclusionExist(inapplicableLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
				boolean recChk1 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
				boolean recChk2 = isRecordExist(inapplicableLiteral, ConclusionType.DEFEASIBLY_PROVABLE);

				if (acChk1 || acChk2 || !(recChk1 || recChk2)) {
					newLiteralFind_defeasiblyNotProvable(inapplicableLiteral, true);
				}
			}
		}

		return ProcessStatus.SUCCESS;
	}

	@Override
	protected ProcessStatus addPendingConclusion(Conclusion conclusion) {
		if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;
		logMessage(Level.FINE, 3, "pending conclusion added: ", conclusion);

		switch (conclusion.getConclusionType()) {
		case DEFINITE_PROVABLE:
		case DEFINITE_NOT_PROVABLE:
			pendingConclusions[0].add(conclusion);
			break;
		case DEFEASIBLY_PROVABLE:
		case DEFEASIBLY_NOT_PROVABLE:
			pendingConclusions[1].add(conclusion);
			break;
		default:
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus addAmbiguousConclusion(Conclusion conclusion, Set<String> ruleLabels)
			throws ReasoningEngineException {
		for (String ruleLabel : ruleLabels) {
			addAmbiguousConclusion(conclusion, ruleLabel);
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus addAmbiguousConclusion(Conclusion conclusion, String ruleLabel)
			throws ReasoningEngineException {
		if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;
		logMessage(Level.FINE, 3, "ambiguous conclusion added: ", conclusion);

		int ind = -1;

		switch (conclusion.getConclusionType()) {
		case DEFINITE_PROVABLE:
		case DEFINITE_NOT_PROVABLE:
			ind = 0;
			break;
		case DEFEASIBLY_PROVABLE:
		case DEFEASIBLY_NOT_PROVABLE:
			ind = 1;
			break;
		default:
			throw new ReasoningEngineException(getClass(), ErrorMessage.REASONING_ENGINE_UNSUPPORTED_CONCLUSION_TYPE,
					conclusion.getConclusionType());
		}
		Set<String> ruleSet = ambiguousConclusions[ind].get(conclusion);
		if (null == ruleSet) {
			ruleSet = new TreeSet<String>();
			ambiguousConclusions[ind].put(conclusion, ruleSet);
		}
		ruleSet.add(ruleLabel);
		addRecord(conclusion);
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus removeAmbiguousConclusion(Conclusion conclusion) {
		if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;
		logMessage(Level.FINER, 3, "ambiguous conclusion to remove: ", conclusion);

		switch (conclusion.getConclusionType()) {
		case DEFINITE_PROVABLE:
		case DEFINITE_NOT_PROVABLE:
			ambiguousConclusions[0].remove(conclusion);
			break;
		case DEFEASIBLY_PROVABLE:
		case DEFEASIBLY_NOT_PROVABLE:
			ambiguousConclusions[1].remove(conclusion);
			break;
		default:
		}
		removeRecord(conclusion);
		return ProcessStatus.SUCCESS;
	}

	protected void updateAmbiguousConclusions(int i) {
		if (ambiguousConclusions[i].size() == 0) return;
		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-before");

		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();
		List<Conclusion> recordsToRemove = new ArrayList<Conclusion>();

		for (Entry<Conclusion, Set<String>> entry : ambiguousConclusions[i].entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			Literal literal = conclusion.getLiteral();
			List<Literal> conflictLiterals = getConflictLiterals(literal);
			boolean conflictLiteralInSccGroup = false;
			for (Literal conflictLiteral : conflictLiterals) {
				if (getSccGroup(conflictLiteral) != null) conflictLiteralInSccGroup = true;
			}

			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE:
				logMessage(Level.FINER, 1, "updateAmbiguousConclusion, check literal (definite): ", literal);
				if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
					ambiguousConclusionToRemove.add(conclusion);
					boolean chk1 = isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
					if (chk1) {
						recordsToRemove.add(new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal));
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, RuleType.STRICT,
									ConclusionType.DEFINITE_NOT_PROVABLE, literal, RuleInferenceStatus.DEFEATED);
						newLiteralFind_definiteNotProvable(literal, true);
					} else {
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.APPICABLE);
						newLiteralFind_definiteProvable(literal, true);
					}
				}
				break;
			case DEFEASIBLY_PROVABLE:
				logMessage(Level.FINER, 1, "updateAmbiguousConclusion, check literal (defeasible): ", literal);
				Set<Rule> rulesWithLiteralAsHead = theory.getRulesWithHead(literal);
				boolean keepLiteralInAmbiguousSet = false;
				for (Rule r : rulesWithLiteralAsHead) {
					for (Literal bodyLiteral : r.getBodyLiterals()) {
						if (bodyLiteral.isPlaceHolder()) keepLiteralInAmbiguousSet = true;
					}
				}
				boolean dchk4 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
				if (keepLiteralInAmbiguousSet) {
					// defer the removal of literal until all rules generated
					// by the superiority removal process have been evaluated
					logMessage(Level.FINER, 2, "keep literal in ambiguous set temporary");
				} else if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
					ambiguousConclusionToRemove.add(conclusion);
					logMessage(Level.FINER, 0, "*** dchk4=", dchk4);
					if (dchk4) {
						recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, RuleType.DEFEASIBLE,
									ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal, RuleInferenceStatus.DEFEATED);
						newLiteralFind_defeasiblyNotProvable(literal, true);
					} else {
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion,
									RuleInferenceStatus.APPICABLE);
						newLiteralFind_defeasiblyProvable(literal, true);
					}
				} else if (conflictLiteralInSccGroup && dchk4) {
					ambiguousConclusionToRemove.add(conclusion);
					recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
					if (isLogInferenceProcess)
						getInferenceLogger().updateRuleInferenceStatus(ruleLabels, RuleType.DEFEASIBLE,
								ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal, RuleInferenceStatus.DISCARDED);
					newLiteralFind_defeasiblyNotProvable(literal, true);
				}
				break;
			default:
			}
		}

		logMessage(Level.FINER, 1, "ambiguousConclusionToRemove=", ambiguousConclusionToRemove);
		for (Conclusion conclusion : ambiguousConclusionToRemove) {
			ambiguousConclusions[i].remove(conclusion);
		}
		for (Conclusion record : recordsToRemove) {
			removeRecord(record);
		}
		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-after");
	}

	/**
	 * return the next pending conclusion in the list and remove it from the
	 * pending conclusions list
	 * <p>
	 * this function will return all the definite (not) provable conclusions first and then the defeasible (not)
	 * provable conclusions
	 * </p>
	 * 
	 * @return next pending conclusion in the list
	 * @throws ReasoningEngineException
	 */
	protected Conclusion getNextPendingConclusion() throws ReasoningEngineException {
		Conclusion pendingConclusion = null;
		int totalPendingConclusionsCount = 0;

		for (int i = 0; i < pendingConclusions.length && null == pendingConclusion; i++) {
			totalPendingConclusionsCount += pendingConclusions[i].size();
			if (totalPendingConclusionsCount == 0 && ambiguousConclusions[i].size() == 0) {
				if (i == 0 && theory.getStrictRulesCount() == 0)
					continue;
				else if (i == 1 && theory.getDefeasibleRulesCount() == 0) continue;
			}

			if (pendingConclusions[i].size() == 0 && ambiguousConclusions[i].size() > 0) {
				// update ambiguous conclusions found
				if (theoryWithLoops) getSccLiteralsGroupInTheory();
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
				try {
					if (updateStronglyConnectedComponents(true) > 0) {
						int jj = 0;
						while (jj < pendingConclusions.length && pendingConclusions[jj].size() == 0) {
							jj++;
						}
						if (i > jj) i = jj;
						if (!AppConst.isDeploy) logMessage(Level.FINEST, 2, "XXX, i=", i, ", jj=", jj);
					}
				} catch (ReasoningEngineException e) {
					throw e;
				} catch (Exception e) {
					throw new ReasoningEngineException(getClass(),
							"Exception throw while generating strongly connected components", e);
				}
			}

			if (pendingConclusions[i].size() > 0) {
				return pendingConclusions[i].removeFirst();
				// Iterator<Conclusion> it = pendingConclusions[i].iterator();
				// pendingConclusion = it.next();
				// it.remove();
			} else {
				// some loop in the theory may occur
				if (ambiguousConclusions[i].size() > 0) {
					logMessage(Level.FINER, 0, "*** ambiguousConclusions[", i,
							"].size()=" + ambiguousConclusions[i].size());
				}
			}
		}
		return pendingConclusion;
	}

	// ======================================
	// well-found semantics - start
	//
	protected boolean isBlockedBySccLiteral(Literal literal) {
		Set<Rule> rules = theory.getRulesWithHead(literal);
		Set<Rule> newRules = new TreeSet<Rule>();
		Set<Rule> allRules = new TreeSet<Rule>();
		int lastAllCheckedRulesCount = 0;
		do {
			lastAllCheckedRulesCount = allRules.size();
			rules.addAll(newRules);
			newRules.clear();
			for (Rule rule : rules) {
				for (Literal bodyLiteral : rule.getBodyLiterals()) {
					Set<Literal> sccGroup = getSccGroup(bodyLiteral);
					if (sccGroup != null) {
						Set<String> rr = getRulesWithBodyLiteralOutOfSccGroup(bodyLiteral, sccGroup);
						if (rr.size() == 0) return true;
					}
					newRules.addAll(theory.getRulesWithHead(bodyLiteral));
				}
			}
			if (newRules.size() > 0) allRules.addAll(newRules);
		} while (newRules.size() > 0 && lastAllCheckedRulesCount != allRules.size());
		return false;
	}

	protected Set<Literal> getSccGroup(Literal literal) {
		for (Set<Literal> sccGroup : sccLiteralsGroups) {
			if (sccGroup.contains(literal)) return sccGroup;
		}
		return null;
	}

	protected void updateSccAmbiguousConclusions(int i, boolean isWellfounded) throws ReasoningEngineException {
		List<Conclusion> unprovableConclusion = new ArrayList<Conclusion>();
		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();

		logMessage(Level.FINE, 0, "updateSccAmbiguousConclusions - start");
		Set<String> rulesToRemove = new TreeSet<String>();
		if (!isWellfounded) {
			for (Set<Literal> sccLiteralsGroup : sccLiteralsGroups) {
				for (Literal sccLiteral : sccLiteralsGroup) {
					logMessage(Level.FINER, 1, "checking sccLiteral: ", sccLiteral);
					boolean sccLiteralToRemove = false;
					boolean conflictInSameSccGroup = isConflictLiteralInSameSccGroup(sccLiteral);
					if (conflictInSameSccGroup) {
						sccLiteralToRemove = true;
						List<Literal> conflictLiterals = getConflictLiterals(sccLiteral);
						for (Literal conflictLiteral : conflictLiterals) {
							if (isAmbiguousConclusionExist(conflictLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
								ambiguousConclusionToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE,
										conflictLiteral));
								break;
							}
						}
					} else {
						List<Literal> conflictLiterals = getConflictLiterals(sccLiteral);
						for (Literal conflictLiteral : conflictLiterals) {
							if (isAmbiguousConclusionExist(conflictLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
								sccLiteralToRemove = true;
								break;
							}
						}
					}
					if (sccLiteralToRemove) {
						for (Rule r : theory.getRules(sccLiteral).values()) {
							// if (r.isHeadLiteral(sccLiteral)) {
							// rulesToRemove.add(r.getLabel());
							// }
							ConclusionType t = (r.getRuleType() == RuleType.STRICT ? ConclusionType.DEFINITE_NOT_PROVABLE
									: ConclusionType.DEFEASIBLY_NOT_PROVABLE);
							Conclusion conclusion = new Conclusion(t, sccLiteral);
							unprovableConclusion.add(conclusion);
							// unprovableConclusion.add(new Conclusion(t, sccLiteral));
							if (r.isHeadLiteral(sccLiteral)) {
								rulesToRemove.add(r.getLabel());
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(r.getOriginalLabel(), conclusion,
											RuleInferenceStatus.DISCARDED);
							}
						}
					}
				}
			}
		}
		logMessage(Level.FINER, 1, "==> unprovableConclusion.size=" + unprovableConclusion.size());

		if (unprovableConclusion.size() == 0) {
			for (Conclusion conclusion : ambiguousConclusions[i].keySet()) {
				Literal ambiguousLiteral = conclusion.getLiteral();
				logMessage(Level.FINER, 1, "ambiguousLiteral=", ambiguousLiteral);
				List<Literal> conclictLiterals = getConflictLiterals(ambiguousLiteral);
				logMessage(Level.FINEST, 1, "conclictLiterals=", conclictLiterals);
				for (Literal conflictLiteral : conclictLiterals) {
					if (isBlockedBySccLiteral(conflictLiteral)) {
						boolean isConflictLiteralInSccGroup = (getSccGroup(conflictLiteral) != null);
						logMessage(Level.FINEST, 2, "isBlockedBySccLiteral=true, isConflictLiteralInSccGroup=",
								isConflictLiteralInSccGroup);

						Set<Rule> conflictHeadRules = theory.getRulesWithHead(conflictLiteral);
						for (Rule conflictHeadRule : conflictHeadRules) {
							boolean allBodyLiteralsAreAmbiguous = false;
							boolean allrealLiterals = true;
							ConclusionType conclusionType = (conflictHeadRule.getRuleType() == RuleType.STRICT ? ConclusionType.DEFINITE_PROVABLE
									: ConclusionType.DEFEASIBLY_PROVABLE);
							for (Literal bodyLiteral : conflictHeadRule.getBodyLiterals()) {
								if (!isAmbiguousConclusionExist(bodyLiteral.getComplementClone(), conclusionType))
									allBodyLiteralsAreAmbiguous = false;
								else if (bodyLiteral.isPlaceHolder()) allrealLiterals = false;
							}
							logMessage(Level.FINEST, 3, "rule [", conflictHeadRule.getLabel(),
									"], allBodyLiteralsAreAmbiguous=" + allBodyLiteralsAreAmbiguous
											+ ", allrealLiterals=" + allrealLiterals);
							if (allBodyLiteralsAreAmbiguous || allrealLiterals) {
								// rulesToRemove.add(conflictHeadRule.getLabel());
								ConclusionType t = (conflictHeadRule.getRuleType() == RuleType.STRICT ? ConclusionType.DEFINITE_NOT_PROVABLE
										: ConclusionType.DEFEASIBLY_NOT_PROVABLE);
								Conclusion tempConclusion = new Conclusion(t, conflictLiteral);
								unprovableConclusion.add(tempConclusion);
								// unprovableConclusion.add(new Conclusion(t, conflictLiteral));
								rulesToRemove.add(conflictHeadRule.getLabel());
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(conflictHeadRule.getOriginalLabel(),
											tempConclusion, RuleInferenceStatus.DISCARDED);
							}
							if (!isConflictLiteralInSccGroup && !isWellfounded) {
								if (allrealLiterals) ambiguousConclusionToRemove.add(conclusion);
								unresolveableConclusionsSet.add(new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE,
										conclusion.getLiteral()));
							}
						}
					} else {
						Set<Rule> conflictHeadRules = theory.getRulesWithHead(conflictLiteral);
						for (Rule conflictHeadRule : conflictHeadRules) {
							boolean allBodyLiteralsAreAmbiguous = true;
							boolean allRealLiterals = true;
							ConclusionType conclusionType = (conflictHeadRule.getRuleType() == RuleType.STRICT ? ConclusionType.DEFINITE_PROVABLE
									: ConclusionType.DEFEASIBLY_PROVABLE);
							for (Literal bodyLiteral : conflictHeadRule.getBodyLiterals()) {
								if (!isAmbiguousConclusionExist(bodyLiteral.getComplementClone(), conclusionType))
									allBodyLiteralsAreAmbiguous = false;
								if (bodyLiteral.isPlaceHolder()) allRealLiterals = false;
							}
							logMessage(Level.FINEST, 3, "rule [", conflictHeadRule.getLabel(),
									"], allBodyLiteralsAreAmbiguous=" + allBodyLiteralsAreAmbiguous
											+ ", allRealLiterals=" + allRealLiterals);
							if (allBodyLiteralsAreAmbiguous && !allRealLiterals) {
								// rulesToRemove.add(conflictHeadRule.getLabel());
								ConclusionType t = (conflictHeadRule.getRuleType() == RuleType.STRICT ? ConclusionType.DEFINITE_NOT_PROVABLE
										: ConclusionType.DEFEASIBLY_NOT_PROVABLE);
								Conclusion tempConclusion = new Conclusion(t, conflictLiteral);
								unprovableConclusion.add(tempConclusion);
								// unprovableConclusion.add(new Conclusion(t, conflictLiteral));
								rulesToRemove.add(conflictHeadRule.getLabel());
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(conflictHeadRule.getOriginalLabel(),
											tempConclusion, RuleInferenceStatus.DISCARDED);
							}
						}
					}
				}
			}
		}
		if (!AppConst.isDeploy) {
			logMessage(Level.INFO, 0, "rulesToRemove=" + rulesToRemove);
			logMessage(Level.INFO, 0, "unprovableConclusion=" + unprovableConclusion);
			logMessage(Level.INFO, 0, "ambiguousConclusionToRemove=" + ambiguousConclusionToRemove);
			logMessage(Level.INFO, 0, "unresolveableConclusionsSet=" + unresolveableConclusionsSet);
		}

		for (Conclusion c : ambiguousConclusionToRemove) {
			removeAmbiguousConclusion(c);
		}
		removeRules(rulesToRemove);
		for (Conclusion c : unprovableConclusion) {
			if (ConclusionType.DEFINITE_NOT_PROVABLE == c.getConclusionType()) {
				newLiteralFind_definiteNotProvable(c.getLiteral(), true);
			} else {
				newLiteralFind_defeasiblyNotProvable(c.getLiteral(), true);
			}
		}
		updateAmbiguousConclusions(i);

		if (!AppConst.isDeploy) printEngineStatus("updateSccAmbiguousConclusions");
		logMessage(Level.FINE, 0, "updateSccAmbiguousConclusions - end");
	}

	protected boolean isConflictLiteralInSameSccGroup(Literal literal) {
		Set<Literal> sccGroup = getSccGroup(literal);
		if (null == sccGroup) return false;
		boolean inSameSccGroup = sccGroup.contains(literal.getComplementClone());
		return inSameSccGroup;
	}

	protected boolean isSelfLoop(Literal literal) {
		Map<String, Rule> rules = theory.getRules(literal);
		for (Rule rule : rules.values()) {
			if (rule.isBodyLiteral(literal) && rule.isHeadLiteral(literal)) return true;
		}
		return false;
	}

	protected void getSccLiteralsGroupInTheory() throws ReasoningEngineException {
		TheoryAnalyser theoryAnalyser;
		try {
			theoryAnalyser = getTheoryAnalyser();
			sccLiteralsGroups = theoryAnalyser.getStronglyConnectedLiterals();
			// remove redundant group
			for (int i = sccLiteralsGroups.size() - 1; i >= 0; i--) {
				Set<Literal> scc = sccLiteralsGroups.get(i);

				if (scc.size() == 1 && !isSelfLoop(scc.iterator().next())) {
					logMessage(Level.FINER, 0, "*** scc (removed) =", scc);
					sccLiteralsGroups.remove(i);
				} else {
					logMessage(Level.FINER, 0, "*** scc literal group=", scc);
				}
			}
			if (sccLiteralsGroups.size() == 0) {
				theoryWithLoops = false;
			}
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	protected Set<String> getRulesWithBodyLiteralOutOfSccGroup(Literal literal, Set<Literal> sccGroup) {
		Set<Rule> rules = theory.getRulesWithHead(literal);
		Set<String> nonSccLiteralRules = new TreeSet<String>();
		for (Rule rule : rules) {
			if (rule.getBodyLiterals().size() > 0) {
				boolean containsSccLiteralInBody = false;
				for (Literal bodyLiteral : rule.getBodyLiterals()) {
					if (sccGroup.contains(bodyLiteral)) containsSccLiteralInBody = true;
				}
				if (!containsSccLiteralInBody) nonSccLiteralRules.add(rule.getLabel());
			}
		}
		return nonSccLiteralRules;
	}

	protected int updateStronglyConnectedComponents(boolean updateSccLiteralsGroup) throws ReasoningEngineException {
		logMessage(Level.FINER, 1, "updateStronglyConnectedComponents - start");
		int literalCount = 0;
		Set<String> rulesToRemove = new TreeSet<String>();
		try {
			if (updateSccLiteralsGroup) getSccLiteralsGroupInTheory();
			if (null == sccLiteralsGroups || sccLiteralsGroups.size() == 0) return literalCount;

			for (Set<Literal> literalsGroup : sccLiteralsGroups) {
				logMessage(Level.FINEST, 2, "** literalsGroup - [", literalsGroup.size(), "]-", literalsGroup);
				if (literalsGroup.size() == 1) {
					Literal literal = literalsGroup.iterator().next();
					if (isSelfLoop(literal)) {
						// check to see if there are any applicable rules with
						// body literal out of SCC group
						Set<String> applicableRules = getRulesWithBodyLiteralOutOfSccGroup(literal, literalsGroup);
						if (applicableRules.size() > 0) {
							Set<Rule> rules = theory.getRulesWithHead(literal);
							for (Rule r : rules) {
								if (!applicableRules.contains(r.getLabel())) rulesToRemove.add(r.getLabel());
							}
						} else {
							logMessage(Level.FINEST, 3, "self looping literal added to conclusion set [", literal, "]");
							updateStronglyConnectedComponents_addConclusions(literal);
							literalCount++;
						}
					} else {
						// literal is not self looping
					}
				} else {
					for (Literal literal : literalsGroup) {
						// check to see if there are any applicable rules with
						// body literal out of SCC group
						Set<String> applicableRules = getRulesWithBodyLiteralOutOfSccGroup(literal, literalsGroup);
						if (applicableRules.size() > 0) {
							Set<Rule> rules = theory.getRulesWithHead(literal);
							for (Rule r : rules) {
								if (!applicableRules.contains(r.getLabel())) rulesToRemove.add(r.getLabel());
							}
						} else {
							updateStronglyConnectedComponents_addConclusions(literal);
							literalCount++;
						}
					}
				}
			}

			if (rulesToRemove.size() > 0) removeRules(rulesToRemove);

			return literalCount;
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			logMessage(Level.SEVERE, 2, "*** " + TextUtilities.getExceptionMessage(e));
			throw new ReasoningEngineException(getClass(), e);
		} finally {
			logMessage(Level.FINER, 1, "updateStronglyConnectedComponents - end");
		}
	}

	protected void updateStronglyConnectedComponents_addConclusions(Literal literal) {
		Set<Rule> rules = theory.getRulesWithHead(literal);
		for (Rule rule : rules) {
			switch (rule.getRuleType()) {
			case STRICT:
				if (isLogInferenceProcess)
					getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.STRICT,
							ConclusionType.DEFINITE_NOT_PROVABLE, literal, RuleInferenceStatus.DISCARDED);
				newLiteralFind_definiteNotProvable(literal, true);
				break;
			case DEFEASIBLE:
				if (isLogInferenceProcess)
					getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
							ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal, RuleInferenceStatus.DISCARDED);
				newLiteralFind_defeasiblyNotProvable(literal, true);
				break;
			default:
			}
		}
	}

	//
	// well-found semantics - end
	// ======================================

	protected ProcessStatus addConclusion(Conclusion conclusion) {
		logMessage(Level.FINE, 1, "conclusion added: ", conclusion);

		Literal literal = conclusion.getLiteral();
		Map<ConclusionType, Conclusion> conclusionList = conclusions.get(literal);
		if (null == conclusionList) {
			conclusionList = new TreeMap<ConclusionType, Conclusion>();
			conclusions.put(literal, conclusionList);
		}
		conclusionList.put(conclusion.getConclusionType(), conclusion);
		return ProcessStatus.SUCCESS;
	}

	protected boolean isConclusionExist(Conclusion conclusion) {
		if (conclusion == null) return false;
		Map<ConclusionType, Conclusion> conclusionList = conclusions.get(conclusion.getLiteral());
		if (null == conclusionList) return false;
		if (conclusionList.containsKey(conclusion.getConclusionType())) return true;
		return false;
	}

	protected boolean isAmbiguousConclusionExist(Literal literal, ConclusionType conclusionType) {
		Conclusion conclusion = new Conclusion(conclusionType, literal);
		int i = 0;
		switch (conclusionType) {
		case DEFINITE_PROVABLE:
		case DEFINITE_NOT_PROVABLE:
			i = 0;
			break;
		case DEFEASIBLY_PROVABLE:
		case DEFEASIBLY_NOT_PROVABLE:
			i = 1;
			break;
		default:
		}
		boolean isExist = ambiguousConclusions[i].containsKey(conclusion);
		logMessage(Level.FINEST, 3, "isAmbiguousConclusionExist", literal, conclusionType, "=", isExist);
		return isExist;
	}

	protected boolean isAmbiguousConclusionExist(List<Literal> literals, ConclusionType conclusionType) {
		for (Literal literal : literals) {
			if (isAmbiguousConclusionExist(literal, conclusionType)) return true;
		}
		return false;
	}

	protected boolean isTempConclusionExist(List<Literal> literalsList, Set<Conclusion> tempConclusionSet,
			ConclusionType conclusionType) {
		for (Literal literal : literalsList) {
			Conclusion c = new Conclusion(conclusionType, literal);
			if (tempConclusionSet.contains(c)) return true;
		}
		return false;
	}

	protected ProcessStatus duplicateStrictRulesToDefeasibleRules() throws ReasoningEngineException {
		List<Rule> newDefeasibleRules = theory.duplicateRulesToType(strictRules.values(), RuleType.DEFEASIBLE,
				STRICT_TO_DEFEASIBLE_POSTFIX);
		for (Rule rule : newDefeasibleRules) {
			try {
				theory.addRule(rule);
			} catch (TheoryException e) {
				throw new ReasoningEngineException(getClass(), "Exception throw while duplicating strict rules, rule="
						+ rule.toString(), e);
			}
		}
		return ProcessStatus.SUCCESS;
	}

	/**
	 * check inference for a literal q,
	 * <p>
	 * for defeasible provable, check
	 * 
	 * <pre>
	 *  +D q OR
	 *  +tt q AND -D &tilde;q AND -tt &tilde;q
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * for not defeasible provable, check
	 * 
	 * <pre>
	 * -D q AND 
	 * -tt q OR +D &tilde;q OR +tt &tilde;q
	 * </pre>
	 * 
	 * where +tt and -tt are tentatively provable and not tentatively provable respectively
	 * 
	 * @param conclusion
	 * @return Process status - ProcessStatus.SUCCESS, if process completed
	 *         successfully, exception throw otherwise
	 */
	@Override
	protected ProcessStatus checkInference(Conclusion conclusion) {
		if (conclusion == null) return ProcessStatus.SUCCESS;

		Literal literal = conclusion.getLiteral();
		List<Literal> conflictLiteralList = getConflictLiterals(literal);

		boolean hasDefPosConflict = false;
		boolean hasDefNegConflict = false;
		for (int i = 0; i < conflictLiteralList.size() && !(hasDefPosConflict && hasDefNegConflict); i++) {
			Literal conflictLiteral = conflictLiteralList.get(i);
			logMessage(Level.FINER, 2, "checkInference: literal=", literal, "conflictLiteral=", conflictLiteral);
			boolean hasLiteralComplement = theory.contains(conflictLiteral);

			// for defeasibly provable
			boolean isDefiniteProvable = isRecordExist(literal, ConclusionType.DEFINITE_PROVABLE);
			boolean isTentativeProvable = isRecordExist(literal, ConclusionType.TENTATIVELY_PROVABLE);
			boolean isComplementDefiniteNotProvable = isRecordExist(conflictLiteral,
					ConclusionType.DEFINITE_NOT_PROVABLE);
			boolean isComplementNotTentativeProvable = isRecordExist(conflictLiteral,
					ConclusionType.TENTATIVELY_NOT_PROVABLE);

			// for NOT defeasibly provable
			boolean isDefiniteNotProvable = isRecordExist(literal, ConclusionType.DEFINITE_NOT_PROVABLE);
			boolean isTentativeNotProvable = isRecordExist(literal, ConclusionType.TENTATIVELY_NOT_PROVABLE);
			boolean isComplementDefiniteProvable = isRecordExist(conflictLiteral, ConclusionType.DEFINITE_PROVABLE);
			boolean isComplementTentativeProvable = isRecordExist(conflictLiteral, ConclusionType.TENTATIVELY_PROVABLE);

			logMessage(Level.FINEST, 3, "check inference for literal=", conclusion);
			logMessage(Level.FINEST, 4, "complement literal check=", conflictLiteral);
			logMessage(Level.FINEST, 5, "hasLiteralComplement=" + hasLiteralComplement);
			logMessage(Level.FINEST, 5, "[" + isDefiniteProvable + "]", "[" + isTentativeProvable,
					isComplementDefiniteNotProvable, isComplementNotTentativeProvable + "]");
			logMessage(Level.FINEST, 5, "[" + isDefiniteNotProvable + "]", "[" + isTentativeNotProvable,
					isComplementDefiniteProvable, isComplementTentativeProvable + "]");

			// for defeasibly provable
			boolean defeasibleProvableCase1 = isDefiniteProvable;
			boolean defeasibleProvableCase2 = isTentativeProvable
					&& (!hasLiteralComplement || (isComplementDefiniteNotProvable && isComplementNotTentativeProvable));
			if (defeasibleProvableCase1 || defeasibleProvableCase2) {
			} else {
				hasDefPosConflict = true;
				logMessage(Level.FINEST, 4, "==> hasDefPosConflict=" + hasDefPosConflict);
			}

			// for defeasibly NOT provable
			// check inference if the literal complement exist
			// i.e. for the literal ~q
			// defeasible provable, check
			// +D ~q OR
			// +tt ~q AND -D q AND -tt q
			// not defeasible provable, check
			// -D ~q AND
			// -tt ~q OR +D q OR +tt q
			boolean defeasibleNotProvableCase1 = isDefiniteNotProvable;
			boolean defeasibleNotProvableCase2 = isTentativeNotProvable || isComplementDefiniteProvable
					|| isComplementTentativeProvable;
			logMessage(Level.FINEST, 5, "[" + defeasibleNotProvableCase2 + "]", "[" + isTentativeNotProvable,
					isComplementDefiniteProvable, isComplementTentativeProvable + "]");
			if (defeasibleNotProvableCase1 && defeasibleNotProvableCase2) {
			} else {
				hasDefNegConflict = true;
				logMessage(Level.FINEST, 4, "==> hasDefNegConflict=" + hasDefNegConflict);
			}
		}
		if (!hasDefPosConflict) {
			logMessage(Level.FINER, 3, "==> add new defeasibly provable literal", literal);
			Conclusion defeasibleConclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal);
			addPendingConclusion(defeasibleConclusion);
			addRecord(defeasibleConclusion);
			addRecord(new Conclusion(ConclusionType.TENTATIVELY_PROVABLE, literal));
		}
		if (!hasDefNegConflict) {
			logMessage(Level.FINER, 3, "==> add new defeasibly NOT provable literal", literal);
			Conclusion defeasibleNotConclusion = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
			addPendingConclusion(defeasibleNotConclusion);
			addRecord(defeasibleNotConclusion);
			addRecord(new Conclusion(ConclusionType.TENTATIVELY_NOT_PROVABLE, literal));
		}
		// // check inference if the literal complement exist
		// // i.e. for the literal ~q
		// // defeasible provable, check
		// // +D ~q OR
		// // +tt ~q AND -D q AND -tt q
		// // not defeasible provable, check
		// // -D ~q AND
		// // -tt ~q OR +D q OR +tt q
		// if (hasLiteralComplement) {
		// boolean complementDefeasibleCase1 = isComplementDefiniteProvable;
		// boolean complementDefeasibleCase2 = isComplementTentativeProvable &&
		// isDefiniteNotProvable &&
		// isTentativeNotProvable;
		// if (complementDefeasibleCase1 || complementDefeasibleCase2) {
		// if (logger != null)
		// logger.onLogMessage("\t\t\t\t===> add new defeasibly provable literal:"
		// +
		// conflictLiteral);
		// Conclusion complmementDefeasibleConclusion = new
		// Conclusion(ConclusionType.DEFEASIBLY_PROVABLE,
		// conflictLiteral);
		// addPendingConclusion(complmementDefeasibleConclusion);
		// addRecord(complmementDefeasibleConclusion);
		// addRecord(new Conclusion(ConclusionType.TENTATIVELY_PROVABLE,
		// conflictLiteral));
		// isDone = true;
		// }
		//
		// boolean complementDefeasibleNotProvableCase1 =
		// isComplementDefiniteNotProvable;
		// boolean complementDefeasibleNotProvableCase2 =
		// isComplementNotTentativeProvable || isDefiniteProvable
		// ||
		// isTentativeProvable;
		// if (complementDefeasibleNotProvableCase1 &&
		// complementDefeasibleNotProvableCase2) {
		// if (logger != null)
		// logger.onLogMessage("\t\t\t\t===> add new defeasibly NOT provable literal:"
		// +
		// conflictLiteral);
		// Conclusion complementDefeasibleNotConclusion = new
		// Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE,
		// conflictLiteral);
		// addPendingConclusion(complementDefeasibleNotConclusion);
		// addRecord(complementDefeasibleNotConclusion);
		// addRecord(new Conclusion(ConclusionType.TENTATIVELY_NOT_PROVABLE,
		// conflictLiteral));
		// isDone = true;
		// }
		// }

		return ProcessStatus.SUCCESS;
	}

	protected void printEngineStatus(final String callerName) {
		String className = getClass().getName();
		String msg = getReasoningEngineUtilities().printEngineStatus(className + "." + callerName,//
				theory, conclusions, //
				pendingConclusions, ambiguousConclusions, records);
		logMessage(Level.INFO, 0, msg);
	}

	protected void printPendingConclusionSet(Set<Literal> unprovedStrictRuleLiterals,
			Set<Literal> unprovedDefeasibleRuleLiterals,//
			Set<Conclusion> tempPosDefiniteConclusionSet) {
		StringBuilder sb = new StringBuilder();
		sb.append("--tempPosDefiniteConclusionSet");
		for (Conclusion conclusion : tempPosDefiniteConclusionSet) {
			sb.append("\n    ").append(conclusion);
		}
		if (unprovedStrictRuleLiterals.size() > 0) {
			sb.append("\n--unprovedStrictRuleLiterals");
			for (Literal literal : unprovedStrictRuleLiterals) {
				sb.append("\n    ").append(literal);
			}
		}
		if (unprovedDefeasibleRuleLiterals.size() > 0) {
			sb.append("\n--unprovedDefeasibleRuleLiterals");
			for (Literal literal : unprovedDefeasibleRuleLiterals) {
				sb.append("\n    ").append(literal);
			}
		}
		logMessage(Level.INFO, 0, sb.toString());
	}

	@Override
	protected String getProgressMessage() {
		int pendingConclusionsCount = pendingConclusions[0].size() + pendingConclusions[1].size();
		pendingConclusionsCount += ambiguousConclusions[0].size();
		pendingConclusionsCount += ambiguousConclusions[1].size();
		long rulesCount = theory.getStrictRulesCount() + theory.getDefeasibleRulesCount();
		return (pendingConclusionsCount + " literal(s) pending to process, " + rulesCount + " rules remain in theory.");
	}
}
