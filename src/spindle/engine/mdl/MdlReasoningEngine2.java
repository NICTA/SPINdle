/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2013 NICTA Ltd.
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
import java.util.TreeSet;
import java.util.logging.Level;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.engine.ReasoningEngineException;
import spindle.engine.sdl.SdlReasoningEngine2;
import spindle.sys.AppConst;
import spindle.tools.explanation.RuleInferenceStatus;

/**
 * MDL Reasoning Engine (version 2).
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.09.29
 */
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

	protected Set<Literal> getConflictLiteralListWithoutOperatorChange(final Literal literal) {
System.out.println("-- MdlReasoningEngine2.getConflictLiteralListWithoutOperatorChange("+literal+")"); 
		Set<Literal> conflictLiteralList = new TreeSet<Literal>();

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
	protected void removeComplementaryLiteralAmbiguity(int i) throws ReasoningEngineException {
System.out.println("-- MdlReasoningEngine2.removeComplementLiteralAmbiguity("+i+")");		
		logMessage(Level.FINE, 1, "=== removeComplementLiteralAmbiguity - start ===");
		List<Conclusion> ambiguousConclusionsToRemove = new ArrayList<Conclusion>();
		removeComplementaryLiteralAmbiguity(ambiguousConclusions[i],ambiguousConclusionsToRemove);

		for (Conclusion conclusion : ambiguousConclusionsToRemove) {
			removeAmbiguousConclusion(conclusion);
		}
		// TODO remove record??
		logMessage(Level.FINE, 1, "=== removeComplementLiteralAmbiguity -  end  ===");
	}

	protected void removeComplementaryLiteralAmbiguity(Map<Conclusion,Set<String>>ambiguousConclusions,List<Conclusion>ambiguousConclusionsToRemove){
		for (Entry<Conclusion,Set<String>>entry:ambiguousConclusions.entrySet()){
			Conclusion conclusion=entry.getKey();
			Set<String>ruleLabels=entry.getValue();
			
			Literal literal=conclusion.getLiteral();
			Set<Literal>conflictLiterals=getConflictLiteralListWithoutOperatorChange(literal);
			
			switch (conclusion.getConclusionType()) {
			case DEFINITE_PROVABLE:
				logMessage(Level.FINEST, 2, "removeComplementLiteralAmbiguity, check literal (definite): ", literal);
				if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
					if (isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) {
						ambiguousConclusionsToRemove.add(conclusion);
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
						ambiguousConclusionsToRemove.add(conclusion);
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
	}
		
	

	@Override
	protected void updateAmbiguousConclusions(int i) throws ReasoningEngineException {
		System.out.println("-- MdlReasoningEngine2.updateAmbiguousConclusions(" + i + ")");
		if (ambiguousConclusions[i].size() == 0) return;
		logMessage(Level.FINE, 0, "-- MdlReasoningEngine2.updateAmbiguousConclusions - start, i=", i);
		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-before");

		// remove ambiguity due to complementary literals
		removeComplementaryLiteralAmbiguity(i);

		List<Conclusion> ambiguousConclusionToRemove = new ArrayList<Conclusion>();
		List<Conclusion> recordsToRemove = new ArrayList<Conclusion>();

		RuleType ruleType = null;
		ConclusionType negativeConclusionType = null;

		for (Entry<Conclusion, Set<String>> entry : ambiguousConclusions[i].entrySet()) {
			Conclusion conclusion = entry.getKey();
			Set<String> ruleLabels = entry.getValue();

			Literal literal = conclusion.getLiteral();
			ConclusionType conclusionType = conclusion.getConclusionType();

			switch (conclusionType) {
			case DEFINITE_PROVABLE:
				ruleType = RuleType.STRICT;
				negativeConclusionType = ConclusionType.DEFINITE_NOT_PROVABLE;
				break;
			case DEFEASIBLY_PROVABLE:
				ruleType = RuleType.DEFEASIBLE;
				negativeConclusionType = ConclusionType.DEFEASIBLY_NOT_PROVABLE;
				break;
			default:
			}

			Set<Rule> rulesWithLiteralAsHead = theory.getRulesWithHead(literal);
			boolean keepLiteralInAmbiguousSet = false;
			for (Rule r : rulesWithLiteralAsHead) {
				if (!ruleType.equals(r.getRuleType())) continue;
				for (Literal bodyLiteral : r.getBodyLiterals()) {
					if (bodyLiteral.isPlaceHolder()) keepLiteralInAmbiguousSet = true;
				}
			}

			Set<Literal> conflictLiterals = getConflictLiterals(literal);
			boolean conflictLiteralInSccGroup = false;
			for (Literal conflictLiteral : conflictLiterals) {
				if (null != getSccGroup(conflictLiteral)) conflictLiteralInSccGroup = true;
			}

			boolean chk = isRecordExist(conflictLiterals, conclusionType);

			if (keepLiteralInAmbiguousSet) {
				// defer the removal of literal until all rules generated
				// by the superiority removal process have been evaluated
				logMessage(Level.FINER, 2, "keep literal in ambiguous set temporary");
				conclusionType = null;
			} else if (!containsUnprovedRuleInTheory(conflictLiterals, ruleType)) {
				ambiguousConclusionToRemove.add(conclusion);
				if (chk) {
					// mdl modification - start
					// remove ambiguity based on modal operator strength
					conclusionType = evaluateAmbiguousConclusionsByModality(literal, conclusionType, conflictLiterals);

					if (conclusionType.isPositiveConclusion()) {
						// literal with strongest modal operator find
						// positive conclusion => conclusion type unchanged
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.APPICABLE);
					} else {
						// literal is not with strongest modal operator
						// defeated conclusion => remove record from the records set
						recordsToRemove.add(conclusion);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, ruleType, negativeConclusionType, literal,
									RuleInferenceStatus.DEFEATED);
					}
					// mdl modification - end
				} else {
					if (isLogInferenceProcess)
						getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.APPICABLE);
					// literal with no ambiguous conclusion find
					// positive conclusion => conclusion type unchanged
				}
			} else if (conflictLiteralInSccGroup && chk) {
				ambiguousConclusionToRemove.add(conclusion);
				recordsToRemove.add(conclusion);
				if (isLogInferenceProcess)
					getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.DISCARDED);
				// literal with conflict that appear in the SCC set
				// negative conclusion => change conclusion type to negative
				conclusionType = negativeConclusionType;
			}

			if (null == conclusionType) continue;

			// generate new conclusion based on the conclusion type derived above
			generateConclusionsWithLiteral(conclusionType, literal, true);
			
			// ============ commented on 2012.12.12 - start
//			switch (conclusion.getConclusionType()) {
//			case DEFINITE_PROVABLE:
//				logMessage(Level.FINER, 1, "updateAmbiguousConclusion [MDL], check literal (definite): ", literal);
//				for (Rule r:rulesWithLiteralAsHead){
//					if (!RuleType.STRICT.equals(r.getRuleType()))continue;
//					for (Literal bodyLiteral:r.getBodyLiterals()){
//						if (bodyLiteral.isPlaceHolder())keepLiteralInAmbiguousSet=true;
//					}
//				}
//				
//				boolean chk2 = isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
//
//				if (keepLiteralInAmbiguousSet){
//					// defer the removal of literal until all rules generated
//					// by the superiority removal process have been evaluated
//					logMessage(Level.FINER, 2, "keep literal in ambiguous set temporary");
//				} else if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
//					if (chk2) {
//						ambiguousConclusionToRemove.add(conclusion);
//
//						int conflictLiteralExistCount = 0;
//						int strongModeCount = 0;
//						for (Literal conflictLiteral : conflictLiterals) {
//							if (isRecordExist(conflictLiteral, ConclusionType.DEFINITE_PROVABLE)) {
//								conflictLiteralExistCount++;
//								if (hasStrongerMode(literal, conflictLiteral)) {
//									logMessage(Level.FINEST, 2, null, literal, " hasStrongerMode: ", conflictLiteral);
//									strongModeCount++;
//								}
//							}
//						}
//
//						// only conclusion with strongest modal operator is concluded
//						if (strongModeCount == conflictLiteralExistCount) {
//							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//									conclusion, RuleInferenceStatus.APPICABLE);
//							newLiteralFind_definiteProvable(literal,true);
//						} else {
//							recordsToRemove.add(conclusion);
//							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,RuleType.STRICT,
//									ConclusionType.DEFINITE_NOT_PROVABLE,literal, RuleInferenceStatus.DEFEATED);
//							newLiteralFind_definiteNotProvable(literal,true);
//						}
//					} else {
//						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//								conclusion, RuleInferenceStatus.APPICABLE);
//						newLiteralFind_definiteProvable(literal,true);
//					}
//				} else if (conflictLiteralInSccGroup && chk2){
//					ambiguousConclusionToRemove.add(conclusion);
//					recordsToRemove.add(conclusion);
//					if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//							conclusion, RuleInferenceStatus.DISCARDED);
//					newLiteralFind_defeasiblyNotProvable(literal,true);
//				}
//				break;
//			case DEFEASIBLY_PROVABLE:
//				logMessage(Level.FINER, 1, "updateAmbiguousConclusion [MDL], check literal (defeasible): ", literal);
//				for (Rule r:rulesWithLiteralAsHead){
//					if (!RuleType.DEFEASIBLE.equals(r.getRuleType()))continue;
//					for (Literal bodyLiteral:r.getBodyLiterals()){
//						if (bodyLiteral.isPlaceHolder())keepLiteralInAmbiguousSet=true;
//					}
//				}
//				boolean dchk2 = isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
//				
//				if (keepLiteralInAmbiguousSet){
//					// defer the removal of literal until all rules generated
//					// by the superiority removal process have been evaluated
//					logMessage(Level.FINER, 2, "keep literal in ambiguous set temporary");
//				} else if (!containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE)) {
//					ambiguousConclusionToRemove.add(conclusion);
//					if (dchk2) {
//						int conflictLiteralExistCount = 0;
//						int strongModeCount = 0;
//						for (Literal conflictLiteral : conflictLiterals) {
//							if (isRecordExist(conflictLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
//								conflictLiteralExistCount++;
//								if (hasStrongerMode(literal, conflictLiteral)) {
//									logMessage(Level.FINEST, 2, null, literal, " hasStrongerMode: ", conflictLiteral);
//									strongModeCount++;
//								}
//							}
//						}
//
//						// only conclusion with strongest modal operator is concluded
//						if (strongModeCount == conflictLiteralExistCount) {
//							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//									conclusion, RuleInferenceStatus.APPICABLE);
//							newLiteralFind_defeasiblyProvable(literal,true);
//						} else {
//							recordsToRemove.add(conclusion);
//							if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,RuleType.DEFEASIBLE,
//									ConclusionType.DEFEASIBLY_NOT_PROVABLE,literal, RuleInferenceStatus.DEFEATED);
//							newLiteralFind_defeasiblyNotProvable(literal,true);
//						}
//					} else {
//						if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//								conclusion, RuleInferenceStatus.APPICABLE);
//						newLiteralFind_defeasiblyProvable(literal,true);
//					}
//				} else if (conflictLiteralInSccGroup && dchk2){
//					ambiguousConclusionToRemove.add(conclusion);
//					recordsToRemove.add(conclusion);
//					if (isLogInferenceProcess) getInferenceLogger().updateRuleInferenceStatus(ruleLabels,
//							conclusion, RuleInferenceStatus.DISCARDED);
//					newLiteralFind_defeasiblyNotProvable(literal,true);
//				}
//				break;
//			default:
//			}
			// ============ commented on 2012.12.12 - end
		}

		for (Conclusion conclusion : ambiguousConclusionToRemove) {
			ambiguousConclusions[i].remove(conclusion);
		}

		for (Conclusion record : recordsToRemove) {
			removeRecord(record);
		}

		if (!AppConst.isDeploy) printEngineStatus("updateAmbiguousConclusions-after");
		logMessage(Level.FINE, 0, "-- MdlReasoningEngine2.updateAmbiguousConclusions - end, i=", i);
	}

	protected ConclusionType evaluateAmbiguousConclusionsByModality(Literal literal, ConclusionType conclusionType, //
			Collection<Literal> conflictLiterals) throws ReasoningEngineException {
		// no need to perform literal modal operators comparisons if the conclusion type is already negative
		if (conclusionType.isNegativeConclusion()) return conclusionType;

		// count the number of conflict modal literals that appear in the records 
		// and the number of literals weaker than the prescribed literal
		int conflictLiteralExistCount = 0;
		int strongModeCount = 0;		
		for (Literal conflictLiteral : conflictLiterals) {
			if (isRecordExist(conflictLiteral, conclusionType)) {
				conflictLiteralExistCount++;
				if (hasStrongerMode(literal, conflictLiteral)) {
					logMessage(Level.FINEST, 2, null, literal, " hasStrongerMode: ", conflictLiteral);
					strongModeCount++;
				}
			}
		}

		// only conclusion with strongest modality is positively concluded
		if (strongModeCount == conflictLiteralExistCount) {
			// literal with strongest modal operator find
			// positive conclusion => conclusion type unchanged
			return conclusionType;
		} else {
			// literal does not with strongest modality
			// defeated conclusion => change conclusion type to negative
			switch (conclusionType) {
			case DEFINITE_PROVABLE:
				return ConclusionType.DEFINITE_NOT_PROVABLE;
			case DEFEASIBLY_PROVABLE:
				return ConclusionType.DEFEASIBLY_NOT_PROVABLE;
			default:
				throw new ReasoningEngineException(getClass(), "unknown conclusion type: " + conclusionType);
			}
		}
	}
	
	protected boolean hasStrongerMode(Literal literal, Literal conflictLiteral) {
//System.out.println("-- MdlReasoningEngine2.hasStrongerMode("+literal+","+conflictLiteral+")");		
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
