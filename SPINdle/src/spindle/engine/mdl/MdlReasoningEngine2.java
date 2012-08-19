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
package spindle.engine.mdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Mode;
import spindle.core.dom.RuleType;
import spindle.engine.ReasoningEngineException;
import spindle.engine.sdl.SdlReasoningEngine2;
import spindle.sys.AppConst;
import spindle.tools.explanation.RuleInferenceStatus;

public class MdlReasoningEngine2 extends SdlReasoningEngine2 {
	protected Map<String, Set<String>> strongerModeSet = null;

	public MdlReasoningEngine2() {
		super();
	}

	@Override
	protected void initialize() throws ReasoningEngineException {
		strongerModeSet = theory.getStrongModeSet();
		super.initialize();
	}

	protected List<Literal> getConflictLiteralListWithoutOperatorChange(final Literal literal) {
		List<Literal> conflictLiteralList = new ArrayList<Literal>();

		conflictLiteralList.add(literal.getComplementClone());

		Mode literalMode = literal.getMode();
		if (!"".equals(literalMode.getName())) {
			Literal conflictLiteral = literal.clone();
			conflictLiteral.setMode(literalMode.getComplementClone());
			conflictLiteralList.add(conflictLiteral);
		}

		return conflictLiteralList;
	}

	// remove ambiguity caused by complementary literal
	protected void removeComplementLiteralAmbiguity(int i) {
		logMessage(Level.FINE, 1, "=== removeComplementLiteralAmbiguity - start ===");
		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();
		List<Conclusion> recordsToRemove = new ArrayList<Conclusion>();

		for (Entry<Conclusion, Set<String>> entry : ambiguousConclusions[i].entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			Literal literal = conclusion.getLiteral();
			List<Literal> conflictLiterals = getConflictLiteralListWithoutOperatorChange(literal);
			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE:
				logMessage(Level.FINEST, 2, "removeComplementLiteralAmbiguity, check literal (definite): ", literal);
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
				logMessage(Level.FINEST, 2, "removeComplementLiteralAmbiguity, check literal (defeasible): ", literal);
				if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
					if (isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) {
						ambiguousConclusionToRemove.add(conclusion);
						recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
								RuleType.DEFEASIBLE, ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal,
								RuleInferenceStatus.DEFEATED);
						newLiteralFind_defeasiblyNotProvable(literal, true);
					}
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

	@Override
	protected void updateAmbiguousConclusions(int i) {
		if (ambiguousConclusions[i].size() == 0) return;
		logMessage(Level.FINE, 0, "MdlReasoningEngine2.updateAmbiguousConclusions - start, i=", i);
		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-before");

		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();
		List<Conclusion> recordsToRemove = new ArrayList<Conclusion>();

		// remove ambiguity caused by complementary literals
		removeComplementLiteralAmbiguity(i);

		// remove ambiguity based on modal operator strength
		// for (Conclusion conclusion : ambiguousConclusions[i].keySet()) {
		for (Entry<Conclusion, Set<String>> entry : ambiguousConclusions[i].entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			Literal literal = conclusion.getLiteral();
			List<Literal> conflictLiterals = getConflictLiterals(literal);
			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE:
				logMessage(Level.FINER, 1, "updateAmbiguousConclusion [MDL], check literal (definite): ", literal);
				boolean chk1 = containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT);
				if (!chk1) {
					boolean chk2 = isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
					int conflictLiteralExistCount = 0;
					int strongModeCount = 0;
					if (chk2) {
						for (Literal conflictLiteral : conflictLiterals) {
							if (isRecordExist(conflictLiteral, ConclusionType.DEFINITE_PROVABLE)) {
								conflictLiteralExistCount++;
								if (hasStrongerMode(literal, conflictLiteral)) {
									logMessage(Level.FINEST, 2, null, literal, " hasStrongerMode: ", conflictLiteral);
									strongModeCount++;
								}
							}
						}

						// only conclusion with strongest modal operator is concluded
						if (strongModeCount == conflictLiteralExistCount) {
							addPendingConclusion(new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal));
							ambiguousConclusionToRemove.add(conclusion);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.APPICABLE);
						} else {
							recordsToRemove.add(new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal));
							Conclusion negConclusion = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
							addPendingConclusion(negConclusion);
							ambiguousConclusionToRemove.add(conclusion);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									negConclusion, RuleInferenceStatus.DEFEATED);
						}
					} else {
						addPendingConclusion(new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal));
						ambiguousConclusionToRemove.add(conclusion);
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
								conclusion, RuleInferenceStatus.APPICABLE);
					}
				}
				break;
			case DEFEASIBLY_PROVABLE:
				logMessage(Level.FINER, 1, "updateAmbiguousConclusion [MDL], check literal (defeasible): ", literal);
				boolean dchk1 = containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE);
				if (!dchk1) {
					boolean dchk2 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
					if (dchk2) {
						int conflictLiteralExistCount = 0;
						int strongModeCount = 0;
						for (Literal conflictLiteral : conflictLiterals) {
							if (isRecordExist(conflictLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
								conflictLiteralExistCount++;
								if (hasStrongerMode(literal, conflictLiteral)) {
									logMessage(Level.FINEST, 2, null, literal, " hasStrongerMode: ", conflictLiteral);
									strongModeCount++;
								}
							}
						}

						// only conclusion with strongest modal operator is concluded
						if (strongModeCount == conflictLiteralExistCount) {
							addPendingConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
							ambiguousConclusionToRemove.add(conclusion);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									conclusion, RuleInferenceStatus.APPICABLE);
						} else {
							recordsToRemove.add(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
							ambiguousConclusionToRemove.add(conclusion);
							Conclusion negConclusion = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
							addPendingConclusion(negConclusion);
							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
									negConclusion, RuleInferenceStatus.DEFEATED);
						}
					} else {
						addPendingConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal));
						ambiguousConclusionToRemove.add(conclusion);
						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
								conclusion, RuleInferenceStatus.APPICABLE);
					}
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

		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-after");
		logMessage(Level.FINE, 0, "MdlReasoningEngine2.updateAmbiguousConclusions - end, i=", i);
	}

	private boolean hasStrongerMode(Literal literal, Literal conflictLiteral) {
		Mode m1 = literal.getMode();
		Mode m2 = conflictLiteral.getMode();

		if (m1.getName().equals(m2.getName())) return false;

		Set<String> modeList = strongerModeSet.get(m1.getName());
		if (null == modeList) return false;

		String m2ModeName = m2.getName();
		if (modeList.contains(m2ModeName)) return true;
		return false;
	}

}
