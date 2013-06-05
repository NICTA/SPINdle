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
package spindle.engine.tdl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.app.utils.TextUtilities;
import com.app.utils.Utilities.ProcessStatus;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralComparator;
import spindle.core.dom.Mode;
import spindle.core.dom.ProvabilityLevel;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleExt;
import spindle.core.dom.RuleType;
import spindle.core.dom.Temporal;
import spindle.core.dom.TemporalStartComparator;
import spindle.core.dom.TheoryException;
import spindle.engine.ReasoningEngineException;
import spindle.engine.mdl.MdlReasoningEngine2;
import spindle.sys.AppConst;
import spindle.sys.AppFeatureConst;
import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;
import spindle.tools.explanation.RuleInferenceStatus;

/**
 * TDL Reasoning Engine.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.2.1
 * @version Last modified 2012.09.04
 */
// @SuppressWarnings(value={"unused"})
public class TdlReasoningEngine2 extends MdlReasoningEngine2 {
	private static final Temporal PERSISTENT_TEMPORAL = new Temporal();
//	private static final Temporal NEG_INF_TEPORAL = new Temporal(Long.MIN_VALUE, Long.MIN_VALUE);
	private static final Comparator<? super Literal> PLAIN_LITERAL_COMPARATOR = new LiteralComparator(false);
	private static final Comparator<? super Literal> LITERAL_TEMPORAL_START_COMPARATOR = new LiteralComparator(true,
			new TemporalStartComparator());
	private static final Comparator<? super Temporal> TEMPORAL_START_COMPARATOR = new TemporalStartComparator();

	private boolean isReasoningWithMixedTemporalLiterals = false;

	private TreeMap<Literal, TreeSet<Literal>> conflictLiteralsSet = null;
	//private TreeMap<Literal, TreeSet<Literal>> relatedConflictLiteralsSet = null;
	//private TreeSet<Literal> temporalLiteralsProvedSet = null;

	// private Deque<Literal>[] pendingExtendedTemporalLiterals = null;

	// private Deque<Conclusion>[] pendingTemporalConclusions = null;
	private Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> ambiguousTemporalConclusions[] = null;
	private Map<Literal,Map<ConclusionType,TreeSet<Temporal>>> temporalRecords=null;
	private Map<Literal,Map<ConclusionType,TreeSet<Temporal>>> consolidatedTemporalRecords=null;

 
	private LiteralDataStore literalDataStore = null;
	private boolean checkRuleBodyTemporalLiterals = true;
	private TreeSet<Literal> provableBodyLiterals[] = null;

	public TdlReasoningEngine2() throws ReasoningEngineException {
		super();
		if (AppConst.isDeploy)
			throw new ReasoningEngineException(getClass(), ErrorMessage.REASONING_ENGINE_NOT_SUPPORTED, new Object[] { "TDL" });
		isReasoningWithMixedTemporalLiterals = Conf.isReasoningWithMixedTemporalLiterals();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	protected void initialize() throws ReasoningEngineException {
		conflictLiteralsSet = new TreeMap<Literal, TreeSet<Literal>>();
	//	relatedConflictLiteralsSet = new TreeMap<Literal, TreeSet<Literal>>();
		//temporalLiteralsProvedSet = new TreeSet<Literal>();

		// pendingExtendedTemporalLiterals = new ArrayDeque[2];
		// pendingTemporalConclusions = new ArrayDeque[2];
		ambiguousTemporalConclusions = new TreeMap[2];
		provableBodyLiterals = new TreeSet[2];
		for (int i = 0; i < ambiguousTemporalConclusions.length; i++) {
			// pendingExtendedTemporalLiterals[i] = new
			// ArrayDeque<Literal>(AppConst.INITIAL_PENDING_CONCLUSIONS_QUEUE_CAPACITY);
			// pendingTemporalConclusions[i] = new
			// ArrayDeque<Conclusion>(AppConst.INITIAL_PENDING_CONCLUSIONS_QUEUE_CAPACITY);
			ambiguousTemporalConclusions[i] = new TreeMap<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>>(
					PLAIN_LITERAL_COMPARATOR);
			provableBodyLiterals[i] = new TreeSet<Literal>();
		}
		temporalRecords=new TreeMap<Literal,Map<ConclusionType,TreeSet<Temporal>>>(PLAIN_LITERAL_COMPARATOR);
		consolidatedTemporalRecords=new TreeMap<Literal,Map<ConclusionType,TreeSet<Temporal>>>(PLAIN_LITERAL_COMPARATOR);

		checkRuleBodyTemporalLiterals = true;

		literalDataStore = new LiteralDataStore(theory);
		literalDataStore.setAppLogger(logger);
		System.out.println("--- " + getClass().getName() + " - initialize - theory - start");
		System.out.println(theory.toString());
		System.out.println("--- " + getClass().getName() + " - initialize - theory - end");
		System.out.println("--- " + getClass().getName() + " - initialize - literal data store - start");
		System.out.println(literalDataStore.toString());
		System.out.println("--- " + getClass().getName() + " - initialize - literal data store - end");

		super.initialize();
	}

	@Override
	protected void terminate() throws ReasoningEngineException {
		if (!AppConst.isDeploy) printEngineStatus("terminate");

		if (AppFeatureConst.useLiteralDataStoreConclusions) {
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> dataStoreConclusions = literalDataStore
					.getAllConclusions();
			if (null != dataStoreConclusions) {
				conclusions.clear();
				conclusions.putAll(getReasoningEngineUtilities().transformConclusions(dataStoreConclusions));
			}
		}
		if (AppFeatureConst.isVerifyConclusionsAfterInference) conclusions = verifyConclusions(conclusions);
		setConclusion(conclusions);
	}

	@Override
	protected boolean containsUnprovedRuleInTheory(final Literal literal, final RuleType ruleType) {
		System.out.println(AppConst.IDENTATOR+"TdlReasoningEngine2.containsUnprovedRuleInTheory(" + literal + "," + ruleType + ")="
				+ super.containsUnprovedRuleInTheory(literal, ruleType));
		return super.containsUnprovedRuleInTheory(literal, ruleType);
		// return theory.containsUnprovedRule(literal, ruleType, false);
	}

	private boolean containsUnprovedRelatedRuleInTheory(Collection<Literal> literals,ProvabilityLevel provability){
		for (Literal literal:literals){
			if (containsUnprovedRelatedRuleInTheory(literal,provability))return true;
		}
		return false;		
	}
	private boolean containsUnprovedRelatedRuleInTheory(Literal literal,ProvabilityLevel provability){
		Temporal literalTemporal=literal.getTemporal();
		if (null==literalTemporal)literalTemporal=PERSISTENT_TEMPORAL;
		
		Set<Literal> relatedLiteralsInTheory=theory.getRelatedLiterals(literal, provability);
		if (null==relatedLiteralsInTheory)return false;
		
RuleType ruleType= ProvabilityLevel.DEFINITE.equals(provability)?RuleType.STRICT:RuleType.DEFEASIBLE;
		
		for (Literal relatedLiteral:relatedLiteralsInTheory){
			Temporal relatedLiteralTemporal=relatedLiteral.getTemporal();
			if (null==relatedLiteralTemporal || literalTemporal.overlap(relatedLiteralTemporal)){
				if (containsUnprovedRuleInTheory(relatedLiteral, ruleType))return true;
			}
		}
		return false;
		
	}
	@Override
	protected Set<Literal> getConflictLiteralListWithoutOperatorChange(final Literal literal) {
		Set<Literal>conflictLiterals=new TreeSet<Literal>();

		Literal literalComplement=literal.getComplementClone();
		conflictLiterals.addAll(getLiteralsWithSameConflictLiterals(literalComplement));
		
		Mode mode=literal.getMode();
		if (!"".equals(mode.getName())){
			Literal literalComplement2=literal.clone();
			literalComplement2.setMode(mode.getComplementClone());
			conflictLiterals.addAll(getLiteralsWithSameConflictLiterals(literalComplement2));			
		}
				
		System.out.println("  TdlReasoningEngine2.getConflictLiteralListWithoutOperatorChange("+literal+")="+conflictLiterals);
		return conflictLiterals;
	}

	


	@Override
	protected void removeComplementaryLiteralAmbiguity(int i) throws ReasoningEngineException {
		if (!AppConst.isDeploy) System.out.println("-- TdlReasoningEngine2.removeComplementLiteralAmbiguity(" + i + ") - start");
		logMessage(Level.FINE, 0, "=== removeComplementLiteralAmbiguity - start ===");
		Map<Conclusion, Set<Conclusion>> ambiguousConclusionsToAdd = new TreeMap<Conclusion, Set<Conclusion>>();
		// Map<Conclusion,Set<String>> ambiguousConclusionsToAdd = new TreeMap<Conclusion,Set<String>>();
		Set<Conclusion> ambiguousConclusionsToRemove = new TreeSet<Conclusion>();
		Set<Conclusion> conclusionsDerived = new TreeSet<Conclusion>();
		try {
			removeComplementaryTemporalLiteralAmbiguity(ambiguousTemporalConclusions[i], ambiguousConclusionsToAdd,
					ambiguousConclusionsToRemove, conclusionsDerived);
		} catch (LiteralDataStoreException e) {
			e.printStackTrace();
		}
		if (ambiguousConclusionsToAdd.size() > 0) {
			for (Entry<Conclusion, Set<Conclusion>> conclusionEntry : ambiguousConclusionsToAdd.entrySet()) {
				Conclusion origConclusion = conclusionEntry.getKey();
				ConclusionType origConclusionType = origConclusion.getConclusionType();
				ProvabilityLevel origProvability = origConclusionType.getProvabilityLevel();

				Temporal origTemporal = origConclusion.getTemporal();
				if (null == origTemporal) origTemporal = PERSISTENT_TEMPORAL;

				TreeMap<Temporal, Map<ConclusionType, Set<String>>> ambiguousConclusions = getAmbiguousConclusions(
						origConclusion.getLiteral(), origProvability);
				Map<ConclusionType, Set<String>> ctSet = ambiguousConclusions.get(origTemporal);

				Set<String> rulesSet = ctSet.get(origConclusionType);

				if (!AppConst.isDeploy) {
					for (Conclusion conclusionToAdd : conclusionEntry.getValue()) {
						System.out.println("    removeComplementaryLiteralAmbiguity.ambiguousConclusionsToAdd=" + conclusionToAdd
								+ ",rulesSet=" + rulesSet);
					}
				}
				for (Conclusion conclusionToAdd : conclusionEntry.getValue()) {
					logMessage(Level.FINE, 1, "ambiguousConclusionsToAdd", conclusionToAdd);
					addAmbiguousConclusion(conclusionToAdd, new TreeSet<String>(rulesSet));
					addRecord(conclusionToAdd);
					literalDataStore.addHeadLiteral(conclusionToAdd.getLiteral(), origProvability, false);
					conflictLiteralsSet.remove(conclusionToAdd.getLiteral());
				}
			}
			System.out.println("=== ---");
		}

		if (ambiguousConclusionsToRemove.size() > 0) {
			if (!AppConst.isDeploy) {
				for (Conclusion conclusion : ambiguousConclusionsToRemove) {
					System.out.println("    removeComplementaryLiteralAmbiguity.ambiguousConclusionsToRemove=" + conclusion);
				}
			}
			RuleType ruleType = i == 0 ? RuleType.STRICT : RuleType.DEFEASIBLE;
			try {
				for (Conclusion conclusion : ambiguousConclusionsToRemove) {
					logMessage(Level.FINE, 1, "ambiguousConclusionsToRemove", conclusion);
					removeAmbiguousConclusion(conclusion);
					removeRecord(conclusion);
					safeRemoveLiteralStoreHeadLiteral(conclusion.getLiteral(), ruleType);
				}
			} catch (LiteralDataStoreException e) {
				throw new ReasoningEngineException(getClass(), e);
			}
			System.out.println("=== ---");
		}
		
		if (conclusionsDerived.size() > 0) {
			if (!AppConst.isDeploy) {
				for (Conclusion conclusion : conclusionsDerived) {
					System.out.println("    conclusionsDerived" + conclusion);
				}
			}
			for (Conclusion conclusion : conclusionsDerived) {
				logMessage(Level.FINE, 1, "conclusionsDerived", conclusion);
			}
			logMessage(Level.FINE, 0, "=== removeComplementaryLiteralAmbiguity.conclusionsDerived:- start");
			for (Conclusion conclusion : conclusionsDerived) {
				conflictLiteralsSet.remove(conclusion.getLiteral());
				
				switch (conclusion.getConclusionType()) {
				case DEFINITE_PROVABLE:
					newLiteralFind_definiteProvable(conclusion.getLiteral(), true);
					break;
				case DEFINITE_NOT_PROVABLE:
					newLiteralFind_definiteNotProvable(conclusion.getLiteral(), true);
					break;
				case DEFEASIBLY_PROVABLE:
					newLiteralFind_defeasiblyProvable(conclusion.getLiteral(), true);
					break;
				case DEFEASIBLY_NOT_PROVABLE:
					newLiteralFind_defeasiblyNotProvable(conclusion.getLiteral(), true);
					break;
				default:
				}
			}
			logMessage(Level.FINE, 0, "=== removeComplementaryLiteralAmbiguity.conclusionsDerived:- end\n---");
		}
		// TODO remove record??
		// System.out.println("-- literalDataStore --");
		// System.out.println(literalDataStore.toString());
		System.out.println("-- TdlReasoningEngine2.removeComplementLiteralAmbiguity(" + i + ") - end");
		logMessage(Level.FINE, 0, "=== removeComplementLiteralAmbiguity -  end  ===");
	}
	
	
	
	private Temporal getProvableTemporalSegment_complementaryAmbiguousConclusions(Literal literal, ProvabilityLevel provability,
			Collection<Literal> conflictLiterals, boolean checkPrecedingLiterals) throws LiteralDataStoreException {
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;
		// long startTime = literalTemporal.getStartTime();
		long endTime = literalTemporal.getEndTime();

		for (Literal conflictLiteral : conflictLiterals) {
			if (!AppConst.isDeploy) System.out.println("    verify conflictLiteral:" + conflictLiteral);

			if (checkPrecedingLiterals) {
				if (hasUnprovedRelatedPrecedingLiterals(literalTemporal, conflictLiteral, provability)) {
					if (!AppConst.isDeploy)
						System.out
								.println("    ==> hasUnprovedRelatedPrecedingLiterals => null temporal segment: has preceding conflict literals unproved");
					return null;
				}
			}

			TreeMap<Temporal, Map<ConclusionType, Set<String>>> ambiguousConclusionsSet = getAmbiguousConclusions(conflictLiteral,
					provability);
			if (!AppConst.isDeploy) System.out.println("      ambiguousConclusionsSet=" + ambiguousConclusionsSet);
			if (null == ambiguousConclusionsSet) continue;
			for (Entry<Temporal, Map<ConclusionType, Set<String>>> conflictAmbiguousConclusionEntry : ambiguousConclusionsSet.entrySet()) {
				Temporal conflictTemporal = conflictAmbiguousConclusionEntry.getKey();
				if (conflictTemporal.overlap(literalTemporal)) {
					if (conflictTemporal.startBefore(literalTemporal)) {
						return null;
					} else if (conflictTemporal.sameStart(literalTemporal)) {
						if (endTime > conflictTemporal.getEndTime()) endTime = conflictTemporal.getEndTime();
					} else {
						if (endTime > conflictTemporal.getStartTime()) endTime = conflictTemporal.getStartTime();
					}
				}
			}

			TreeMap<Temporal, Literal> unprovedSucceedingLiterals = literalDataStore
					.getUnprovedSucceedingHeadLiterals(literal, provability);
			if (null == unprovedSucceedingLiterals) continue;
			for (Temporal succeedingTemporal : unprovedSucceedingLiterals.keySet()) {
				if (literalTemporal.overlap(succeedingTemporal) && endTime > succeedingTemporal.getStartTime()) {
					endTime = succeedingTemporal.getStartTime();
				}
			}
		}

		if (endTime == literalTemporal.getEndTime()) {
			return literalTemporal;
		} else {
			if (endTime == literalTemporal.getStartTime() && AppFeatureConst.isIntervalBasedTemporal) return null;
			return new Temporal(literalTemporal.getStartTime(), endTime);
		}
	}

	protected void removeComplementaryTemporalLiteralAmbiguity(
			Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> ambiguousConclusions, //
			Map<Conclusion, Set<Conclusion>> ambiguousConclusionsToAdd, Collection<Conclusion> ambiguousConclusionsToRemove,
			Collection<Conclusion> conclusionsDerived) throws LiteralDataStoreException {
		logMessage(Level.FINER, 1, "TdlReasoningEngine2.removeComplementaryTemporalLiteralAmbiguity - start");
		if (!AppConst.isDeploy) {
			System.out.println("-- TdlReasoningEngine2.removeComplementTemporalLiteralAmbiguity - start");
			System.out.flush();
			System.err.println("function not yet modified: removeComplementTemporalLiteralAmbiguity");
			System.err.flush();
			for (Entry<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> entry : ambiguousConclusions.entrySet()) {
				for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalEntry : entry.getValue().entrySet()) {
					Literal literal = entry.getKey().clone();
					Temporal literalTemporal = temporalEntry.getKey();
					literal.setTemporal(literalTemporal);
					logMessage(Level.FINEST, 2, "literal to verify", literal);
				}
			}
			logMessage(Level.FINEST, 2, "---");
		}

		for (Entry<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> entry : ambiguousConclusions.entrySet()) {
			for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalEntry : entry.getValue().entrySet()) {
				Literal literal = entry.getKey().clone();
				Temporal literalTemporal = temporalEntry.getKey();

				for (Entry<ConclusionType, Set<String>> conclusionTypeEntry : temporalEntry.getValue().entrySet()) {
					literal.setTemporal(literalTemporal);
					logMessage(Level.FINEST, 2, "Ambiguous literal to verify=", literal);

					Set<Literal> conflictLiterals = getConflictLiteralListWithoutOperatorChange(literal);

					ConclusionType conclusionType = conclusionTypeEntry.getKey();

					if (containsUnprovedRelatedRuleInTheory(conflictLiterals, conclusionType.getProvabilityLevel())) {
						logMessage(Level.FINEST, 3, "==> containsUnprovedRelatedRuleInTheory - literal skipped");
						continue;
					}

					Temporal provableTemporalSegment = getProvableTemporalSegment_complementaryAmbiguousConclusions(literal,
							conclusionType.getProvabilityLevel(), conflictLiterals, false);
					logMessage(Level.FINEST, 3, "getProvableTemporalSegment_complementaryAmbiguousConclusions(" + literal + ","
							+ conclusionType.getProvabilityLevel() + ")=" + provableTemporalSegment);

					if (null == provableTemporalSegment) continue;

					Literal literalToProve = literal.cloneWithNoTemporal();
					literalToProve.setTemporal(provableTemporalSegment);

					Set<Literal> ambiguousLiterals = getAmbiguousConclusionsWithSameStart(literalToProve, conclusionType);
					logMessage(Level.FINEST, 3, "==> ambiguous conclusions with same start=" + ambiguousLiterals);
					if (null == ambiguousLiterals || ambiguousLiterals.size() < 2) {
						logMessage(Level.FINEST, 4, "==> " + literal + ", NO ambiguous conclusion with same modality");
					} else {
						logMessage(Level.FINEST, 4, "==> " + literal + ", ambiguous conclusion exist");
						ConclusionType negativeCT = ConclusionType.DEFINITE_PROVABLE.equals(conclusionType) ? ConclusionType.DEFINITE_NOT_PROVABLE
								: ConclusionType.DEFEASIBLY_NOT_PROVABLE;

						Conclusion conclusionDerived = new Conclusion(negativeCT, literalToProve);
						conclusionsDerived.add(conclusionDerived);
						logMessage(Level.FINEST, 3, "conclusionsDerived=" + conclusionDerived);

						// remove the conclusion from the ambiguous conclusions set
						Conclusion conclusionToRemove = new Conclusion(conclusionType, literal);
						ambiguousConclusionsToRemove.add(conclusionToRemove);
						logMessage(Level.FINEST, 3, "ambiguousConclusionToRemove=" + conclusionToRemove);

						// add the ambiguous conclusion left after the provable time interval
						if (literalTemporal.endAfter(provableTemporalSegment)) {
							Literal residualLiteral = literal.cloneWithNoTemporal();
							residualLiteral.setTemporal(new Temporal(provableTemporalSegment.getEndTime(), literalTemporal.getEndTime()));
							Conclusion residualConclusion = new Conclusion(conclusionType, residualLiteral);

							Set<Conclusion> residualConclusionsSet = ambiguousConclusionsToAdd.get(conclusionToRemove);
							if (null == residualConclusionsSet) {
								residualConclusionsSet = new TreeSet<Conclusion>();
								ambiguousConclusionsToAdd.put(conclusionToRemove, residualConclusionsSet);
							}
							residualConclusionsSet.add(residualConclusion);
							logMessage(Level.FINEST, 3, "ambiguousConclusionToAdd=" + residualConclusion);
						}
					}
				}
				if (!AppConst.isDeploy) System.out.println("---");
			}
		}
		if (!AppConst.isDeploy) {
			System.out.println("ambiguousConclusionsToRemove=" + ambiguousConclusionsToRemove);
			System.out.println("-- TdlReasoningEngine2.removeComplementLiteralAmbiguity - end");
		}
		logMessage(Level.FINER, 1, "TdlReasoningEngine2.removeComplementaryTemporalLiteralAmbiguity - end");
	}

	@Override
	protected boolean hasAmbiguousConclusions(int i) {
		return ambiguousTemporalConclusions[i].size() > 0;
	}
	
	@Override
	protected void updateAmbiguousConclusions(int i) throws ReasoningEngineException {
		System.out.println("* TdlReasoningEngine2.updateAmbiguousConclusions(" + i + ") - start");
		if (ambiguousTemporalConclusions[i].size() == 0) return;
		System.out.println("* * not yet implemented!!");

		if (!AppConst.isDeploy) getReasoningEngineUtilities().printAmbiguousTemporalConclusions(ambiguousTemporalConclusions);

		// remove ambiguity caused by complementary literals
		removeComplementaryLiteralAmbiguity(i);

		logMessage(Level.FINE, 0, "updateAmbiguousConclusions(" + i + ")  - start");

		
	for (Entry<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> literalEntry : ambiguousTemporalConclusions[i].entrySet()) {
		for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalEntry : literalEntry.getValue().entrySet()) {
			Literal literalToCheck = literalEntry.getKey().clone();
			literalToCheck.setTemporal(temporalEntry.getKey());
			for (Entry<ConclusionType, Set<String>> conclusionEntry : temporalEntry.getValue().entrySet()) {
				ConclusionType conclusionType = conclusionEntry.getKey();
				logMessage(Level.FINE,1,"ambiguous temporal conclusion=",conclusionType.getSymbol()+" "+literalToCheck);
			}}}
	logMessage(Level.FINE,1,"---");
	
	Set<Literal> residualConclusionsSet=new TreeSet<Literal>();
	Set<Conclusion>ambiguousConclusionsToRemove=new TreeSet<Conclusion>();
Set<Conclusion>recordsToRemove=new TreeSet<Conclusion>();
		
		for (Entry<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> literalEntry : ambiguousTemporalConclusions[i].entrySet()) {
			for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalEntry : literalEntry.getValue().entrySet()) {
				Literal literal = literalEntry.getKey().clone();
				literal.setTemporal(temporalEntry.getKey());
				for (Entry<ConclusionType, Set<String>> conclusionEntry : temporalEntry.getValue().entrySet()) {
					ConclusionType conclusionType = conclusionEntry.getKey();
					Conclusion conclusion=new Conclusion(conclusionType,literal);
	logMessage(Level.FINER, 1, "verify ambiguous conclusion(" + conclusion + ")");
					
					Set<Literal>conflictLiterals=getConflictLiterals(literal);
					
					Temporal provableTemporalSegment=getProvableTemporalSegment(literal, conclusionType.getProvabilityLevel(), conflictLiterals);
					logMessage(Level.FINEST,2,"provableTemporalSegment="+provableTemporalSegment);
					if (null==provableTemporalSegment)continue;
					
					Literal literalToCheck=literal.cloneWithNoTemporal();
					literalToCheck.setTemporal(provableTemporalSegment);
					logMessage(Level.FINEST,2,"literalToCheck="+literalToCheck);
					
					Set<String>ruleLabels=conclusionEntry.getValue();				
					ConclusionType negativeCT=ConclusionType.DEFINITE_PROVABLE.equals(conclusionType)?ConclusionType.DEFINITE_NOT_PROVABLE:ConclusionType.DEFEASIBLY_NOT_PROVABLE;					
					Conclusion conclusionToCheck=new Conclusion(conclusionType,literalToCheck);

					boolean ambiguousExist = isAmbiguousConclusionExist(literalToCheck, conclusionEntry.getKey());
				
					System.out.println("check ambiguous (" + conclusionType.getSymbol() + " " + literalToCheck + "): ambiguous="
							+ ambiguousExist);
					logMessage(Level.FINER, 2, "check ambiguous(" + conclusionType.getSymbol() + " " + literalToCheck + "): ambiguous="
							+ ambiguousExist+", ruleLabels="+ruleLabels);
					logMessage(Level.FINER, 2, "conflictLiterals(" + conclusionType.getSymbol() + " " + literalToCheck + ")="+conflictLiterals);

					
					if (!containsUnprovedRelatedRuleInTheory(conflictLiterals, conclusionType.getProvabilityLevel())) {
						//ambiguousConclusionsToRemove.add(conclusion);
						
						logMessage(Level.FINEST, 2, "containsUnprovedRelatedRuleInTheory=false");
						Set<Literal> ambiguousLiterals = getAmbiguousConclusionsWithSameStart(literalToCheck, conclusionType);
						for (Literal conflictLiteral:conflictLiterals){
							ambiguousLiterals.addAll(getAmbiguousConclusionsWithSameStart(conflictLiteral, conclusionType));
						}
						
						logMessage(Level.FINEST, 3, "ambiguousLiterals(" + literalToCheck + ")=" + ambiguousLiterals + ")");
						
						
						Set<Literal> trimmedAmbiguousLiterals=new TreeSet<Literal>();
						for (Literal ambiguousLiteral:ambiguousLiterals){
							Temporal ambiguousTemporal=ambiguousLiteral.getTemporal();
							if (null==ambiguousTemporal)ambiguousTemporal=PERSISTENT_TEMPORAL;
							
							if (provableTemporalSegment.contains(ambiguousTemporal)){
								logMessage(Level.FINEST, 3, "trimmedAmbiguousLiterals(" + ambiguousLiteral + ").1");
								trimmedAmbiguousLiterals.add(ambiguousLiteral);								
							} else{
								logMessage(Level.FINEST, 3, "trimmedAmbiguousLiterals(" + ambiguousLiteral + ").2");
								Literal trimmedLiteral=ambiguousLiteral.cloneWithNoTemporal();
								trimmedLiteral.setTemporal(provableTemporalSegment.clone());
								trimmedAmbiguousLiterals.add(trimmedLiteral);
								
								Literal residualLiteral=ambiguousLiteral.cloneWithNoTemporal();
								residualLiteral.setTemporal(new Temporal(provableTemporalSegment.getEndTime(),ambiguousTemporal.getEndTime()));								
								residualConclusionsSet.add(residualLiteral);
							}
						}
						logMessage(Level.FINEST, 3, "trimmedAmbiguousLiterals(" + literalToCheck + ")=" + trimmedAmbiguousLiterals + ")");

						if (trimmedAmbiguousLiterals.size()>1){trimmedAmbiguousLiterals.remove(literalToCheck);
							conclusionType=evaluateAmbiguousConclusionsByModality(literalToCheck, conclusionType, trimmedAmbiguousLiterals);
							logMessage(Level.FINEST, 3, "evaluateAmbiguousConclusionsByModality(" + literalToCheck + ")=" + conclusionType + ")");
							
							if (conclusionType.isPositiveConclusion()){
							} else{
						//		recordsToRemove.add(conclusion);
							}
						}
					} else {
						logMessage(Level.FINEST, 2, "containsUnprovedRelatedRuleInTheory=true");
					}
					
					if (null!=conclusionType)
					logMessage(Level.FINEST,2,"==> conclusion generated: "+conclusionType.getSymbol()+" "+literalToCheck);
					else logMessage(Level.FINEST,2,"==> no conclusion is generated");
					//generateConclusionsWithLiteral(conclusionType,literalToCheck,true);
				}
			}
		}
		
		for (Literal literal:residualConclusionsSet){
			logMessage(Level.FINEST,1," residual conclusion generated: "+literal);
		}
		
		logMessage(Level.FINE, 0, "updateAmbiguousConclusions(" + i + ")  - end");

		// super.updateAmbiguousConclusions(i);
		// updateAmbiguousTemporalConclusions(i);
		// removeComplementTemporalLiteralAmbiguity(i);

		for (Conclusion conclusion:ambiguousConclusionsToRemove){
			removeAmbiguousConclusion(conclusion);
		}
		for (Conclusion conclusion:recordsToRemove){
			this.removeRecord(conclusion);		
		}
		
		
		if (!AppConst.isDeploy) {
			// getReasoningEngineUtilities().printAmbiguousConclusions( ambiguousConclusions);
			getReasoningEngineUtilities().printAmbiguousTemporalConclusions(ambiguousTemporalConclusions);
		}
		System.out.println("* TdlReasoningEngine2.updateAmbiguousConclusions(" + i + ") - end");
	}
	
	@Override
	protected ConclusionType evaluateAmbiguousConclusionsByModality(Literal literal, ConclusionType conclusionType, //
			Collection<Literal> conflictLiterals) throws ReasoningEngineException {
		return super.evaluateAmbiguousConclusionsByModality(literal,conclusionType,conflictLiterals);
	}
	


	@Override
	protected void generatePendingConclusions(boolean isDefeasibleRuleOnly) throws ReasoningEngineException, TheoryException {
		logMessage(Level.FINE, 0, "=== TdlReasoningEngine2.generatePendingConclusions - start ===");
		logMessage(Level.FINE, 1, "isDefeasibleRuleOnly=", isDefeasibleRuleOnly);
		System.out.println("generatePendingConclusions...start");
		System.out.println(literalDataStore.toString());
		Set<Literal> unprovedStrictRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());
		Set<Literal> unprovedDefeasibleRuleLiterals = new TreeSet<Literal>(theory.getAllLiteralsInRules());

		logMessage(Level.FINER, 1, "=== +ve set - start ===");

		removeDefeatedRulesInTheory();

		Set<String> rulesToDelete = new TreeSet<String>();
		Map<Conclusion, Set<String>> tempPosDefiniteConclusionSet = new TreeMap<Conclusion, Set<String>>();
		Map<Conclusion, Set<String>> tempPosDefeasibleConclusionSet = new TreeMap<Conclusion, Set<String>>();
		Set<String> ruleSet = null;

		for (Rule r : theory.getFactsAndAllRules().values()) {
			if (r.getHeadLiterals().size() > 1)
				throw new TheoryException(ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_MULTIPLE_HEADS_RULE, new Object[] { r.getLabel() });
			RuleExt rule = (RuleExt) r;
			Literal literal = rule.getHeadLiterals().get(0);
			switch (rule.getRuleType()) {
			case STRICT:
				provableBodyLiterals[0].add(literal);
				if (!isDefeasibleRuleOnly) {
					if (rule.isEmptyBody()) {
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal);
						addRecord(conclusion);

						ruleSet = tempPosDefiniteConclusionSet.get(conclusion);
						if (null == ruleSet) {
							ruleSet = new TreeSet<String>();
							tempPosDefiniteConclusionSet.put(conclusion, ruleSet);
						}
						ruleSet.add(rule.getOriginalLabel());

						rulesToDelete.add(rule.getLabel());
					}
					unprovedStrictRuleLiterals.remove(literal);
				}
				break;
			case DEFEASIBLE:
				provableBodyLiterals[1].add(literal);
				if (rule.isEmptyBody() && rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0) {
					Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal);
					addRecord(conclusion);

					ruleSet = tempPosDefeasibleConclusionSet.get(conclusion);
					if (null == ruleSet) {
						ruleSet = new TreeSet<String>();
						tempPosDefeasibleConclusionSet.put(conclusion, ruleSet);
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

		// printEngineStatus("generatePendingConconlusion - xxx");
		// printPendingConclusionSet(unprovedStrictRuleLiterals, unprovedDefeasibleRuleLiterals,
		// tempPosDefiniteConclusionSet.keySet(),tempPosDefeasibleConclusionSet.keySet());

		System.out.println("check unproved extended temporal literals in body - start");
		System.out.println(literalDataStore.toString());

		if (checkRuleBodyTemporalLiterals) checkLiteralsProvability(unprovedStrictRuleLiterals, unprovedDefeasibleRuleLiterals);

		unprovedStrictRuleLiterals.removeAll(provableBodyLiterals[0]);
		unprovedDefeasibleRuleLiterals.removeAll(provableBodyLiterals[1]);

		if (!AppConst.isDeploy) {
			System.out.println("---");
			System.out.println("provableBodyLiterals[0]=" + provableBodyLiterals[0].toString());
			System.out.println("provableBodyLiterals[1]=" + provableBodyLiterals[1].toString());
			System.out.println("unprovedStrictRuleLiterals=" + unprovedStrictRuleLiterals);
			System.out.println("unprovedDefeasibleRuleLiterals=" + unprovedDefeasibleRuleLiterals);
			System.out.println("---");
		}

		System.out.println("check unproved extended temporal literals in body - end");

		if (!AppConst.isDeploy) {
			printPendingConclusionSet(unprovedStrictRuleLiterals, unprovedDefeasibleRuleLiterals, tempPosDefiniteConclusionSet.keySet(),
					tempPosDefeasibleConclusionSet.keySet());
			System.out.println(TextUtilities.generateHighLightedMessage("verify initial temporary conclusions - start"));
		}

		TreeSet<Literal> tempPosDefiniteConclusions = extractLiteralsFromConclusions(tempPosDefiniteConclusionSet.keySet());
		TreeSet<Literal> tempPosDefeasibleConclusions = extractLiteralsFromConclusions(tempPosDefeasibleConclusionSet.keySet());
		if (!isDefeasibleRuleOnly) {
			for (Entry<Conclusion, Set<String>> entry : tempPosDefiniteConclusionSet.entrySet()) {
				Conclusion conclusion = entry.getKey();
				// ConclusionType conclusionType = conclusion.getConclusionType();
				Set<String> ruleLabels = entry.getValue();

				System.out.println(TextUtilities.generateHighLightedMessage("verifying conclusion:" + conclusion));

				boolean ambiguousExist = false, pos = true;
				Literal literal = conclusion.getLiteral();
				Set<Literal> conflictLiterals = getConflictLiterals(literal);
				// switch (conclusionType) {
				// case DEFINITE_PROVABLE:
				// if (!isDefeasibleRuleOnly) {
				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosDefiniteConclusions);// ,
																										// ConclusionType.DEFINITE_PROVABLE);
				System.out.println("        ambiguousExist.1.1=" + ambiguousExist);
				if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT);
				System.out.println("        ambiguousExist.1.2=" + ambiguousExist);
				if (ambiguousExist) {
					logMessage(Level.FINEST, 1, "==> generatePendingConclusions: ==> add (+D Ambiguous)", literal);
					addAmbiguousConclusion(conclusion, ruleLabels);
				} else {
					if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) pos = false;
					if (isRecordExist(literal, ConclusionType.DEFINITE_NOT_PROVABLE)) pos = false;
					logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
					if (pos) {
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.APPICABLE);
						newLiteralFind_definiteProvable(literal, false);
					} else {
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.DISCARDED);
						newLiteralFind_definiteNotProvable(literal, false);
					}
				}
			}
			// break;
		}

		for (Entry<Conclusion, Set<String>> entry : tempPosDefeasibleConclusionSet.entrySet()) {
			Conclusion conclusion = entry.getKey();
			// ConclusionType conclusionType = conclusion.getConclusionType();
			Set<String> ruleLabels = entry.getValue();

			System.out.println("verifying conclusion:" + conclusion);

			boolean ambiguousExist = false, pos = true;
			Literal literal = conclusion.getLiteral();
			Set<Literal> conflictLiterals = getConflictLiterals(literal);

			// case DEFEASIBLY_PROVABLE:
			if (!pendingConclusions[1].contains(conclusion) //
					// if (!pendingConclusions[1].contains(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE,
					// literal)) //
					|| !isRecordExist(literal, ConclusionType.DEFEASIBLY_PROVABLE)) {
				ambiguousExist = isTempConclusionExist(conflictLiterals, tempPosDefeasibleConclusions);// ,
																										// ConclusionType.DEFEASIBLY_PROVABLE);
				System.out.println("        ambiguousExist.2.1=" + ambiguousExist);
				if (!ambiguousExist) ambiguousExist = containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE);
				System.out.println("        ambiguousExist.2.2=" + ambiguousExist);
				if (ambiguousExist) {
					logMessage(Level.FINEST, 1, "==> generatePendingConclusions: ==> add (+d Ambiguous)", literal);
					addAmbiguousConclusion(conclusion, ruleLabels);
				} else {
					if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) pos = false;
					if (isRecordExist(literal, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) pos = false;
					logMessage(Level.FINEST, 1, "02, ambiguousExist=", ambiguousExist, ", pos=", pos);
					if (pos) {
						addPendingConclusion(conclusion);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.APPICABLE);
						newLiteralFind_defeasiblyProvable(literal, false);
					} else {
						Conclusion c = new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE, literal);
						addPendingConclusion(c);
						if (isLogInferenceProcess)
							getInferenceLogger().updateRuleInferenceStatus(ruleLabels, conclusion, RuleInferenceStatus.DISCARDED);
						newLiteralFind_defeasiblyNotProvable(literal, false);
					}
				}
			}
			// break;
			// default:
			// }
		}
		System.out.println(TextUtilities.generateHighLightedMessage("verify initial temporary conclusions - +ve set end"));
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
		logMessage(Level.FINE, 0, "=== TdlReasoningEngine2.generatePendingConclusions -  end  ===");

		System.out.println("generatePendingConclusions...end");
		if (!AppConst.isDeploy) {
			// getReasoningEngineUtilities().printAmbiguousConclusions( ambiguousConclusions);
			getReasoningEngineUtilities().printAmbiguousTemporalConclusions(ambiguousTemporalConclusions);
		}
	}

	@Override
	protected ProcessStatus addConclusion(Conclusion conclusion) throws ReasoningEngineException {
		try {
			System.out.println("addConclusion=" + conclusion);
			literalDataStore.updateConclusion(conclusion.getLiteral(), conclusion.getConclusionType());
			return super.addConclusion(conclusion);
		} catch (LiteralDataStoreException e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	private void checkLiteralsProvability(Set<Literal> definiteSet, Set<Literal> defeasibleSet) throws LiteralDataStoreException {
		if (!checkRuleBodyTemporalLiterals) return;
		checkRuleBodyTemporalLiterals = false;

		if (null != definiteSet) {
			for (Literal literal : new TreeSet<Literal>(definiteSet)) {
				if (literalDataStore.isProvable(literal, ProvabilityLevel.DEFINITE)) {
					// if (literal.hasTemporalInfo() && literalDataStore.isProvable(literal, ProvabilityLevel.DEFINITE))
					// {
					if (theory.containsInRuleBody(literal, RuleType.STRICT)) {
						provableBodyLiterals[0].add(literal);
					}
				}
			}
		}
		if (null != defeasibleSet) {
			for (Literal literal : new TreeSet<Literal>(defeasibleSet)) {
				if (literalDataStore.isProvable(literal, ProvabilityLevel.DEFEASIBLE)) {
					// if (literal.hasTemporalInfo() && literalDataStore.isProvable(literal,
					// ProvabilityLevel.DEFEASIBLE)) {
					if (theory.containsInRuleBody(literal, RuleType.DEFEASIBLE)) {
						provableBodyLiterals[1].add(literal);
					}
				}
			}
		}
	}

	
	
	@Override
	protected ProcessStatus addAmbiguousConclusion(Conclusion conclusion, String ruleLabel)  {
		System.out.println("TdlReasoningEngine2.addAmbiguousConclusion(" + conclusion + "," + ruleLabel + ")");
		if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;

		ConclusionType conclusionType = conclusion.getConclusionType();
		int provability = conclusionType.getProvabilityLevel().ordinal();
		Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> ambiguousConclusions = ambiguousTemporalConclusions[provability];

		Literal literal = conclusion.getLiteral();
		TreeMap<Temporal, Map<ConclusionType, Set<String>>> temporalsSet = ambiguousConclusions.get(literal);
		if (null == temporalsSet) {
			temporalsSet = new TreeMap<Temporal, Map<ConclusionType, Set<String>>>();
			ambiguousConclusions.put(literal.cloneWithNoTemporal(), temporalsSet);
		}
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		Map<ConclusionType, Set<String>> conclusionSet = temporalsSet.get(literalTemporal);
		if (null == conclusionSet) {
			conclusionSet = new TreeMap<ConclusionType, Set<String>>();
			temporalsSet.put(literalTemporal, conclusionSet);
		}

		Set<String> ruleSet = conclusionSet.get(conclusionType);
		if (null == ruleSet) {
			ruleSet = new TreeSet<String>();
			conclusionSet.put(conclusionType, ruleSet);
		}
		ruleSet.add(ruleLabel);

		logMessage(Level.FINE, 3, "ambiguous conclusion added: ", conclusion);

		addRecord(conclusion);

	//	if (!AppConst.isDeploy) getReasoningEngineUtilities().printAmbiguousTemporalConclusions(ambiguousTemporalConclusions);

		return ProcessStatus.SUCCESS;
	}

	

	@Override
	protected ProcessStatus removeAmbiguousConclusion(Conclusion conclusion) {
		if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;
		logMessage(Level.FINER, 3, "ambiguous conclusion to remove: ", conclusion);

		Literal literal = conclusion.getLiteral();
		ConclusionType conclusionType = conclusion.getConclusionType();
		int provability = conclusionType.getProvabilityLevel().ordinal();

		TreeMap<Temporal, Map<ConclusionType, Set<String>>> literalTemporalSet = ambiguousTemporalConclusions[provability].get(literal);
		if (null == literalTemporalSet) return ProcessStatus.SUCCESS;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		Map<ConclusionType, Set<String>> conclusionSet = literalTemporalSet.get(literalTemporal);
		if (null == conclusionSet) return ProcessStatus.SUCCESS;

		conclusionSet.remove(conclusionType);

		if (conclusionSet.size() == 0) {
			literalTemporalSet.remove(literalTemporal);
			if (literalTemporalSet.size() == 0) ambiguousTemporalConclusions[provability].remove(literal);
		}

		removeRecord(conclusion);
		return ProcessStatus.SUCCESS;
	}
	
	
//	private ProcessStatus removeAmbiguousConclusion(ConclusionType conclusionType,Literal literal){
	//	return removeAmbiguousConclusion(new Conclusion(conclusionType,literal));
//	}
	
//	private boolean containsAmbiguousConclusionWithExactTemporal(Collection<Literal> literals ,ConclusionType conclusionType){
//		for (Literal literal:literals){
//			if (containsAmbiguousConclusionWithExactTemporal(literal,conclusionType))return true;
//		}
//		return false;
//	}
//	private boolean containsAmbiguousConclusionWithExactTemporal(Literal literal ,ConclusionType conclusionType){
//		ProvabilityLevel provability = conclusionType.getProvabilityLevel();
//		TreeMap<Temporal, Map<ConclusionType, Set<String>>> literalTemporalSet = getAmbiguousConclusions(literal, provability);
//		if (null == literalTemporalSet) return false;
//
//		Temporal literalTemporal = literal.getTemporal();
//		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;
//
//		Map<ConclusionType,Set<String>> conclusionSet=literalTemporalSet.get(literalTemporal);
//		if (null==conclusionSet)return false;
//		
//		return conclusionSet.containsKey(conclusionType);
//	}

	@Override
	protected boolean isAmbiguousConclusionExist(Literal literal, ConclusionType conclusionType) {
		System.out.println("    TdlReasoningEngine2.isAmbiguousConclusionExist(" + literal + "," + conclusionType + ")");
		ProvabilityLevel provability = conclusionType.getProvabilityLevel();
		TreeMap<Temporal, Map<ConclusionType, Set<String>>> literalTemporalSet = getAmbiguousConclusions(literal, provability);
		if (null == literalTemporalSet) return false;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		Temporal startTemporal = literalTemporal.getStartTimeAsInstance();
		Temporal endTemporal = literalTemporal.getEndTimeAsInstance();

		SortedMap<Temporal, Map<ConclusionType, Set<String>>> extractedAmbiguousConclusions = literalTemporalSet.subMap(startTemporal,
				true, endTemporal, false);
		if (null != extractedAmbiguousConclusions) {
			System.out.println("      check ambiguous conclusion:" + literal);
			for (Entry<Temporal, Map<ConclusionType, Set<String>>> entry : extractedAmbiguousConclusions.entrySet()) {
				System.out.println("        temporal: " + entry.getKey() + ":" + entry.getValue().keySet());
				if (entry.getKey().sameStart(literalTemporal)) {
					if (entry.getValue().containsKey(conclusionType)) return true;
				}
			}
		}
		
		return false;
	}
	
	private Set<Literal> getAmbiguousConclusionsWithSameStart(Literal literal, ConclusionType conclusionType) {
		if (!AppConst.isDeploy)
			System.out.println("      TdlReasoningEngine2.getAmbiguousConclusionsWithSameStart(" + literal + "," + conclusionType + ")");
		ProvabilityLevel provability = conclusionType.getProvabilityLevel();

		Set<Literal> literalsToCheck = new TreeSet<Literal>();
		literalsToCheck.add(literal);
		literalsToCheck.addAll(getConflictLiterals(literal));

		Set<Literal> ambiguousConclusionsSet = new TreeSet<Literal>();
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;
		Temporal startTemporal = literalTemporal.getStartTimeAsInstance();
		Temporal endTemporal = literalTemporal.getEndTimeAsInstance();

		if (!AppConst.isDeploy) {
			System.out.println("        literalsToCheck=" + literalsToCheck);
			System.out.println("        startTemporal=" + startTemporal + ", endTemporal=" + endTemporal);
		}

		for (Literal l : literalsToCheck) {
			Literal literalToCheck = l.clone();
			literalToCheck.setTemporal(literalTemporal);
			if (!AppConst.isDeploy) System.out.println("      literalToCheck=" + literalToCheck);
			TreeMap<Temporal, Map<ConclusionType, Set<String>>> literalTemporalSet = getAmbiguousConclusions(literalToCheck, provability);
			if (null == literalTemporalSet) continue;

			for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalEntry : literalTemporalSet.entrySet()) {
				Temporal acTemporal = temporalEntry.getKey();
				if (!AppConst.isDeploy) System.out.println("          temporal: " + acTemporal + ":" + temporalEntry.getValue().keySet());
				if (literalTemporal.startAfter(acTemporal)) continue;
				if (acTemporal.sameStart(literalTemporal)) {
					if (temporalEntry.getValue().containsKey(conclusionType)) {
						Literal acl = l.clone();
						acl.setTemporal(acTemporal);
						ambiguousConclusionsSet.add(acl);
					}
				}
				if (acTemporal.getStartTime() > literalTemporal.getEndTime()) break;
			}
		}

		return ambiguousConclusionsSet.size() == 0 ? null : ambiguousConclusionsSet;
	}
	
	private TreeMap<Temporal, Map<ConclusionType, Set<String>>> getAmbiguousConclusions(Literal  literal,ProvabilityLevel provability){
		TreeMap<Temporal, Map<ConclusionType, Set<String>>> ambiguousSet= ambiguousTemporalConclusions[provability.ordinal()].get(literal);
		if (null==ambiguousSet)return null;		
		return ambiguousSet;		
	}

	// private Map<ConclusionType, Set<String>> getAmbiguousConclusionsSetX(Literal literal, ConclusionType
	// conclusionType,
	// boolean createNew) {
	// int ind = conclusionType.getProvabilityLevel().ordinal();
	// TreeMap<Temporal,Map<ConclusionType,Set<String>>>temporalsSet=ambiguousTemporalConclusions[ind].get(literal);
	// if (null==temporalsSet){
	// if (!createNew)return null;
	// temporalsSet=new TreeMap<Temporal,Map<ConclusionType,Set<String>>>();
	// ambiguousTemporalConclusions[ind].put(getPlainLiteral(literal), temporalsSet);
	// }
	// Temporal literalTemporal=literal.getTemporal();
	// if (null==literalTemporal)literalTemporal=PERSISTENT_TEMPORAL;
	// Map<ConclusionType,Set<String>> conclusionSet=temporalsSet.get(literalTemporal);
	// if (null==conclusionSet){
	// if (!createNew)return null;
	// conclusionSet=new TreeMap<ConclusionType,Set<String>>();
	// temporalsSet.put(literalTemporal,conclusionSet);
	// }
	// return conclusionSet;
	//
	// // Map<ConclusionType,Set<String>>conclusionSet=ambiguousTemporalConclusions[ind].get(literal);
	// // if (null==conclusionSet){
	// // if (!createNew)return null;
	// // conclusionSet=new TreeMap<ConclusionType,Set<String>>();
	// // ambiguousTemporalConclusions[ind].put(literal, conclusionSet);
	// // }
	// //
	// //
	// // TreeMap<Conclusion, Set<String>> conclusionsSet = ambiguousTemporalConclusions[ind].get(literal);
	// // if (null == conclusionsSet) {
	// // if (!createNew) return null;
	// // conclusionsSet = new TreeMap<Conclusion, Set<String>>();
	// // Literal l =getPlainLiteral(literal);
	// // Temporal t = literal.getTemporal();
	// // if (null==t)t=new Temporal(Long.MIN_VALUE,Long.MIN_VALUE);
	// // l.setTemporal(new Temporal(t.getStartTime(),t.getStartTime()));
	// // ambiguousTemporalConclusions[ind].put(l, conclusionsSet);
	// // }
	// // return conclusionsSet;
	// }

	// private void updateExtendedTemporalLiterals(int i) throws ReasoningEngineException, TheoryException {
	// ProvabilityLevel provabilityLevel = i == 0 ? ProvabilityLevel.DEFINITE : ProvabilityLevel.DEFEASIBLE;
	// for (Literal literal : pendingExtendedTemporalLiterals[i]) {
	// ConclusionType conclusionType = literalDataStore.checkLiteralProvability(literal, provabilityLevel);
	// if (null == conclusionType) continue;
	// switch (conclusionType) {
	// case DEFINITE_PROVABLE:
	// generateConclusions_definiteProvable(literal);
	// break;
	// case DEFINITE_NOT_PROVABLE:
	// generateConclusions_definiteNotProvable(literal);
	// break;
	// case DEFEASIBLY_PROVABLE:
	// generateConclusions_defeasiblyProvable(literal);
	// break;
	// case DEFEASIBLY_NOT_PROVABLE:
	// generateConclusions_defeasiblyNotProvable(literal);
	// break;
	// default:
	// throw new ReasoningEngineException(getClass(), ErrorMessage.CONCLUSION_UNKNOWN_CONCLUSION_TYPE,
	// new Object[] { conclusionType });
	// }
	// }
	// }

	// @Override
	// protected ProcessStatus addPendingConclusion(Conclusion conclusion) {
	// if (isConclusionExist(conclusion)) return ProcessStatus.SUCCESS;
	// logMessage(Level.FINE, 3, "pending conclusion added: ", conclusion);
	//
	// int provability = conclusion.getProvabilityLevel().ordinal();
	// if (conclusion.hasTemporalInfo()) pendingTemporalConclusions[provability].add(conclusion);
	// else pendingConclusions[provability].add(conclusion);
	// return ProcessStatus.SUCCESS;
	// }

	@Override
	protected TreeSet<Literal> extractLiteralsFromConclusions(Collection<Conclusion> conclusions) {
		TreeSet<Literal> literals = new TreeSet<Literal>(LITERAL_TEMPORAL_START_COMPARATOR);
		for (Conclusion conclusion : conclusions) {
			literals.add(conclusion.getLiteral());
		}
		return literals;
	}

	// Override
	// protected boolean isTempConclusionExist(Collection<Literal> literalsList, TreeSet<Literal> tempConclusionSet) {
	// // protected boolean isTempConclusionExist(Collection<Literal> literalsList, TreeSet<Literal> tempConclusionSet,
	// // ConclusionType conclusionType) {
	// for (Literal literal : literalsList) {
	// // Conclusion c = new Conclusion(conclusionType, literal);
	// // if (tempConclusionSet.contains(c)) return true;
	// if (tempConclusionSet.contains(literal)) return true;
	// }
	// return false;
	// }

	// /**
	// * Return a copy of the prescribed literal in a form of plain literal (i.e., literal with no temporal
	// information).
	// *
	// * @param literal Literal to be processed.
	// * @return A copy of the same literal in a form of plain literal.
	// * @see spindle.core.dom.DomUtilities#getPlainLiteral(Literal literal)
	// */
	// private static Literal getPlainLiteral(final Literal literal) {
	// return DomUtilities.getPlainLiteral(literal);
	// }
	@Override
	protected void printEngineStatus(final String callerName) {
		String className = getClass().getName();
		String msg = getReasoningEngineUtilities().generateEngineInferenceStatusMessage(className + "." + callerName,//
				theory, conclusions, //
				pendingConclusions,  //
				ambiguousConclusions, ambiguousTemporalConclusions, getRecords());

		System.out.println(msg);
		logMessage(Level.INFO, 0, msg);
	}

	// Override to include the checking of pending temporal conclusions.
	// @Override
	// protected boolean hasPendingConclusions(int i) {
	// return pendingConclusions[i].size() > 0 || pendingTemporalConclusions[i].size() > 0;
	// }

	// /**
	// * TODO check for preceding literals before return the pending conclusion
	// * TODO update provability of temporal literals in rule bodies
	// */
	// @Override
	// protected Conclusion getNextPendingConclusion() throws ReasoningEngineException {
	// // Conclusion pendingConclusion = null;
	// int totalPendingConclusionsCount = 0;
	//
	// for (int i = 0; i < pendingConclusions.length; i++) {
	// // for (int i = 0; i < pendingConclusions.length && null == pendingConclusion; i++) {
	// totalPendingConclusionsCount += pendingConclusions[i].size();
	// // -- modification for TDL - start
	// // totalPendingConclusionsCount += (pendingConclusions[i].size() + pendingTemporalConclusions[i].size());
	// // -- modification for TDL - end
	//
	// if (totalPendingConclusionsCount == 0) {
	// // flag used to indicate the last update of the scc literals groups
	// int sccLiteralUpdated = -1;
	//
	// // modification start
	// // try generating the conclusion using superiority relations
	// if (i == ProvabilityLevel.DEFEASIBLE.ordinal() && !hasPendingConclusions(i)) {
	// // if (i == 1 && pendingConclusions[i].size() == 0) {
	// if (!Conf.isReasoningWithWellFoundedSemantics() && ambiguousConclusions[i].size() == 0) {
	// logMessage(Level.FINEST, 1, "check for inferiorly defeated rules");
	// int theorySize = theory.getFactsAndAllRules().size();
	// do {
	// theorySize = theory.getFactsAndAllRules().size();
	// Set<RuleExt> weakestRules = new TreeSet<RuleExt>();
	// for (Superiority sup : theory.getAllSuperiority()) {
	// RuleExt infRule = (RuleExt) theory.getRule(sup.getInferior());
	// if (infRule.getWeakerRulesCount() == 0 && infRule.getStrongerRulesCount() > 0) weakestRules.add(infRule);
	// }
	// if (weakestRules.size() > 0) {
	// Set<RuleExt> rulesToVerify = new TreeSet<RuleExt>();
	// Set<String> rulesToRemove = new TreeSet<String>();
	// for (RuleExt rule : weakestRules) {
	// rulesToVerify.add(rule);
	// }
	// getSccLiteralsGroupInTheory();
	// sccLiteralUpdated = 1;
	// verifyWeakestRuleInTheory(weakestRules, rulesToRemove);
	// if (rulesToRemove.size() > 0) {
	// if (isLogInferenceProcess) {
	// for (String ruleLabel : rulesToRemove) {
	// Rule rule = theory.getRule(ruleLabel);
	// getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
	// ConclusionType.DEFEASIBLY_NOT_PROVABLE, rule.getHeadLiterals().get(0),
	// RuleInferenceStatus.DEFEATED);
	// }
	// }
	// removeRules(rulesToRemove);
	// }
	// }
	// } while (theorySize > theory.getFactsAndAllRules().size());
	// }
	// try {
	// generatePendingConclusions(true);
	// } catch (Exception e) {
	// throw new ReasoningEngineException(getClass(), e);
	// }
	// }
	// // modification end
	//
	// if (!hasPendingConclusions(i) && ambiguousTemporalConclusions[i].size() > 0) {
	// // if (!hasPendingConclusions(i) && ambiguousConclusions[i].size() > 0) {
	// // if (pendingConclusions[i].size() == 0 && ambiguousConclusions[i].size() > 0) {
	// // update ambiguous conclusions found
	// if (!AppConst.isDeploy) logMessage(Level.FINEST, 1, "search for ambiguous conclusions, lvl=1, i=", i);
	//
	// if (sccLiteralUpdated != 1) getSccLiteralsGroupInTheory();
	// sccLiteralUpdated = 2;
	// updateAmbiguousConclusions(i);
	// totalPendingConclusionsCount += pendingConclusions[i].size();
	// }
	// if (!hasPendingConclusions(i) && ambiguousConclusions[i].size() > 0) {
	// // if (pendingConclusions[i].size() == 0 && ambiguousConclusions[i].size() > 0) {
	// updateSccAmbiguousConclusions(i, Conf.isReasoningWithWellFoundedSemantics());
	// }
	//
	// // well-found semantics
	// // - check for strongly connected literals if no new pending conclusions found after ambiguity check
	// if (Conf.isReasoningWithWellFoundedSemantics() //
	// && i == pendingConclusions.length - 1 //
	// && totalPendingConclusionsCount == 0 //
	// && theory.getFactsAndAllRules().size() > 0) {
	// if (!AppConst.isDeploy) logMessage(Level.FINEST, 1, "Analyse theory for strongly connected literals, i=", i);
	//
	// try {
	// if (updateStronglyConnectedComponents(sccLiteralUpdated != 2) > 0) {
	// int jj = 0;
	// while (jj < pendingConclusions.length && pendingConclusions[jj].size() == 0) {
	// jj++;
	// }
	// if (i > jj) i = jj;
	// }
	// } catch (ReasoningEngineException e) {
	// throw e;
	// } catch (Exception e) {
	// throw new ReasoningEngineException(getClass(), "Exception throw while generating strongly connected components",
	// e);
	// }
	// }
	// }
	//
	// if (pendingConclusions[i].size() > 0) {
	// return pendingConclusions[i].removeFirst();
	// // // -- modification for TDL - start
	// // } else if (pendingTemporalConclusions[i].size() > 0) {
	// // return pendingTemporalConclusions[i].removeFirst();
	// // // -- modification for TDL - end
	// } else {
	// // some loop in the theory may occur
	// }
	// }
	// // return pendingConclusion;
	// return null;
	// }

	// private TreeMap<Temporal,Literal>getRelatedTemporlLiterals(Literal literal) throws LiteralDataStoreException{
	// return literalDataStore.getUnprovedRelatedHeadLiterals(literal);
	// }

	// private boolean isConflictLiterals(Literal literal1, Literal literal2) {
	// Set<Literal> conflictLiterals = super.getConflictLiterals(literal1);
	//
	// if (!conflictLiterals.contains(literal2)) return false;
	//
	// if (literal1.hasTemporalInfo() && literal2.hasTemporalInfo()){
	// Temporal t1 = literal1.getTemporal();
	// Temporal t2 = literal2.getTemporal();
	// return t1.isIntersect(t2);
	// }else return true;
	// }

	// private void addTemporalLiteralProved(Literal literal){
	// temporalLiteralsProvedSet.add(literal);
	// }
	// private Set<Literal>getTemporalLiteralsProved(){
	// return temporalLiteralsProvedSet;
	// }

	// private void consolidateConclusions(Conclusion provedConclusion,Set<Conclusion>defeatedConclusions, //
	// Set<Conclusion>consolidatedConclusions, Set<Conclusion>unenforcedConclusions) throws ReasoningEngineException{
	// TdlReasoningUtilities.generateEnforcedConclusionsSet(provedConclusion, defeatedConclusions, true,
	// consolidatedConclusions, unenforcedConclusions);
	// }


	
	private Set<Literal> getLiteralsWithSameConflictLiterals(Literal literal) {
		Set<Literal> conflictLiterals = new TreeSet<Literal>();

		Set<Literal> literalsToCheck = new TreeSet<Literal>();
		literalsToCheck.add(literal);

		TreeMap<Temporal, Literal> sameStartLiterals = literalDataStore.getHeadLiteralsWithSameStartTemporal(literal);
		if (null != sameStartLiterals) literalsToCheck.addAll(sameStartLiterals.values());
		
		for (Literal sameStartLiteral : literalsToCheck) {
			Temporal sslTemporal = sameStartLiteral.getTemporal();
			Set<Literal> ssls = theory.getLiteralsWithSameConflictLiterals(sameStartLiteral);
			for (Literal ssl : ssls) {
				Literal ssl2 = ssl.clone();
				ssl2.setTemporal(sslTemporal);
				conflictLiterals.add(ssl2);
				TreeMap<Temporal, Literal> ssls2 = literalDataStore.getHeadLiteralsWithSameStartTemporal(ssl2);
				if (null != ssls2) conflictLiterals.addAll(ssls2.values());
			}
		}
		
		return conflictLiterals;
	}
	
	/**
	 * Return the set of conflict literals (with temporal information).
	 */
	@Override
	protected Set<Literal> getConflictLiterals(final Literal literal) {
		//if (!AppConst.isDeploy) System.out.println(AppConst.IDENTATOR + "** TdlReasoningEngine2.getConflictLiterals(" + literal + ")");

		TreeSet<Literal> conflictLiterals = conflictLiteralsSet.get(literal);
		if (null == conflictLiterals) {
			Temporal literalTemporal = literal.getTemporal();
			conflictLiterals = new TreeSet<Literal>();

			if (AppFeatureConst.simpleConflictTemporalLiteralsGeneration) {
				for (Literal plainConflictLiteral : super.getConflictLiterals(literal)) {
					Literal pcl = plainConflictLiteral.clone();
					pcl.setTemporal(literalTemporal);
					conflictLiterals.add(pcl);
				}
			} else {
				for (Literal plainConflictLiteral : super.getConflictLiterals(literal)) {
					Literal pcl = plainConflictLiteral.clone();
					pcl.setTemporal(literalTemporal);
					conflictLiterals.addAll(getLiteralsWithSameConflictLiterals(pcl));
				}
			}
			
			conflictLiteralsSet.put(literal, conflictLiterals);
			if (!AppConst.isDeploy) {
				if (AppFeatureConst.printConflictLiteralsInfo) {
					System.out.println(AppConst.IDENTATOR + TextUtilities.repeatStringPattern("=", 70));
					System.out.println(AppConst.IDENTATOR + "== conflictLiteralsSet - start ==");
					System.out.println(AppConst.IDENTATOR + "== =========================== ==");
					for (Entry<Literal, TreeSet<Literal>> entry : conflictLiteralsSet.entrySet()) {
						System.out.println(AppConst.IDENTATOR + "  " + entry.getKey() + ":" + entry.getValue());
					}
					System.out.println(AppConst.IDENTATOR + "== ========================= ==");
					System.out.println(AppConst.IDENTATOR + "== conflictLiteralsSet - end ==");
					System.out.println(AppConst.IDENTATOR + TextUtilities.repeatStringPattern("=", 70));
				}
			}
		}
	//	logMessage(Level.FINER, 2, "TdlReasoningEngine2.getConflictLiterals(", literal, ")", conflictLiterals);
		 //if (!AppConst.isDeploy)
		//System.out.println(AppConst.IDENTATOR + "** TdlReasoningEngine2.getConflictLiterals(" + literal + ")=" + conflictLiterals);
		return conflictLiterals;
	}

	

	@Override
	protected ProcessStatus generateConclusions_definiteProvable(final Literal literal) throws ReasoningEngineException, TheoryException {
		System.out.println("generateConclusions_definiteProvable(" + literal + ")...start");
		try {
			super.generateConclusions_definiteProvable(literal);
			/*
	// copy from SDLReasoningEngine2.generateConclusions_definiteProvable
	System.out.println("SdlReasoningEngine.generateConclusions_definiteProvable...start");
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
				Set<Literal> conflictLiterals = null;
				switch (rule.getRuleType()) {
				case STRICT:
					logMessage(Level.FINEST, 1, "==> (strict) ", literal);
					rulesToDelete.add(rule.getLabel());
					conflictLiterals = getConflictLiterals(headLiteral);
					if (containsUnprovedRuleInTheory(conflictLiterals, RuleType.STRICT)) {
						logMessage(Level.FINEST, 2, "==>1.1 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
						Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
						addRecord(conclusion);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
					} else {
						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE);
						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE);
						if (chk1 || chk2) {
							logMessage(Level.FINEST, 2, "==>1.2 generateConclusions_definiteProvable: ==> add ambiguous (+D)", headLiteral);
							Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_PROVABLE, headLiteral);
							addRecord(conclusion);
							addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						} else {
							boolean hasConflictRecord = false;
							if (isRecordExist(conflictLiterals, ConclusionType.DEFINITE_PROVABLE)) hasConflictRecord = true;
							if (isRecordExist(headLiteral, ConclusionType.DEFINITE_NOT_PROVABLE)) hasConflictRecord = true;
							if (hasConflictRecord) {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.STRICT,
											ConclusionType.DEFINITE_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
								newLiteralFind_definiteNotProvable(headLiteral, true);
							} else {
								if (isLogInferenceProcess)
									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.STRICT,
											ConclusionType.DEFINITE_PROVABLE, headLiteral, RuleInferenceStatus.APPICABLE);
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
							logMessage(Level.FINEST, 2, "==>1.5 generateConclusions_definiteProvable: ==> add ambiguous (+d)", headLiteral);
							Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
							addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
							addRecord(conclusion);
						} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
							// do nothing
						} else {
							boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
							boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
							if (chk1 || chk2) {
								logMessage(Level.FINEST, 2, "==>1.6 generateConclusions_definiteProvable: ==> add ambiguous (+D)",
										headLiteral);
								addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral),
										rule.getOriginalLabel());
							} else {
								boolean hasConflictRecord = false;
								if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) hasConflictRecord = true;
								if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) hasConflictRecord = true;
								if (hasConflictRecord) {
									if (isLogInferenceProcess)
										getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
												ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
									newLiteralFind_defeasiblyNotProvable(headLiteral, true);
								} else {
									if (isLogInferenceProcess)
										getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
												ConclusionType.DEFEASIBLY_PROVABLE, headLiteral, RuleInferenceStatus.APPICABLE);
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
		System.out.println("SdlReasoningEngine.generateConclusions_definiteProvable...end");
					 */
			
// TODO need to modify to check if the conclusion generated by related literals are positive or not
			Map<ConclusionType, Set<Literal>> relatedConclusions = generateRelatedBodyLiteralsConclusions_positive(literal,
					ConclusionType.DEFINITE_PROVABLE);
			for (Entry<ConclusionType, Set<Literal>> conclusionEntry : relatedConclusions.entrySet()) {
				ConclusionType conclusionType = conclusionEntry.getKey();
				Set<Literal> literalsProved = conclusionEntry.getValue();
				System.out.println("  relatedConclusion generate:" + conclusionType + ":" + literalsProved);
				generateConclusionsWithLiteral(conclusionType, literalsProved, true);
			}
			return ProcessStatus.SUCCESS;
		} finally {
			System.out.println("generateConclusions_definiteProvable...end");
		}
	}

	@Override
	protected ProcessStatus generateConclusions_definiteNotProvable(final Literal literal) throws ReasoningEngineException {
		logMessage(Level.FINE,1,"generateConclusions_definiteNotProvable(" + literal + ")...start");
		try {
			super.generateConclusions_definiteNotProvable(literal);
			TreeMap<Temporal, Literal> negativeLiterals = literalDataStore.getRelatedBodyLiteralsInProvableRange(literal, ProvabilityLevel.DEFINITE);
			if (null != negativeLiterals && negativeLiterals.size() > 0) {
				for (Literal negativeLiteral:negativeLiterals.values()){
					logMessage(Level.FINE,3,"generateRelatedBodyLiteralsConclusions_negative(-d " + literal + ").negativeLiteral="+negativeLiteral);
					super.generateConclusions_definiteNotProvable(negativeLiteral);
				}
			}			
			return ProcessStatus.SUCCESS;
		} finally {
			logMessage(Level.FINE,1,"generateConclusions_definiteNotProvable(" + literal + ")...end");
		}
	}
	
	private boolean hasUnprovedRelatedPrecedingLiterals(Temporal intervalToVerify,Literal literal,ProvabilityLevel provability) throws LiteralDataStoreException{
		TreeMap<Temporal, Literal> unprovedRelatedLiterals = literalDataStore.getUnprovedPrecedingHeadLiterals(literal,
				provability);
		System.out.println("      unprovedRelatedLiterals=" + unprovedRelatedLiterals);
		
		if (null==unprovedRelatedLiterals||unprovedRelatedLiterals.size()==0)return false;		
		if (null==intervalToVerify)return true;
		for (Temporal precedingTemporal : unprovedRelatedLiterals.keySet()) {
				if (intervalToVerify.overlap(precedingTemporal)) {
					logMessage(Level.FINEST,5,"hasUnprovedRelatedPrecedingLiterals",intervalToVerify,literal,provability,"has unproved preceding temporal ==> ",precedingTemporal );
					return true;
				}
		}
		return false;
	}
	private boolean hasUnprovedRelatedPrecedingLiterals(Temporal intervalToVerify,Collection<Literal> literals,ProvabilityLevel provability) throws LiteralDataStoreException{
		for (Literal literal:literals){
			if (hasUnprovedRelatedPrecedingLiterals(intervalToVerify,literal,provability))return true;
		}
		return false;
	}
	/**
	 * Check if there are any unproved preceding (conflict) literals appear in the theory.
	 * 
	 * @param literal Literal to check
	 * @param provability Provability level
	 * @return true if no preceding literals are found in the theory; false otherwise
	 * @throws LiteralDataStoreException
	 */
	private boolean hasUnprovedPrecedingHeadLiterals(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		boolean hasPrecedingHeadLiterals = literalDataStore.hasUnprovedPrecedingHeadLiterals(literal, provability);
		System.out.printf(AppConst.IDENTATOR + "  (%1s,%2s).hasPrecedingHeadLiterals=%3b\n", literal, provability, hasPrecedingHeadLiterals);
		if (!AppConst.isDeploy && hasPrecedingHeadLiterals) {
			System.out.printf(AppConst.IDENTATOR + "  (%1s,%2s).precedingLiterals=%3s\n", literal, provability, //
					literalDataStore.getUnprovedPrecedingHeadLiterals(literal, provability).toString());
		}
		return hasPrecedingHeadLiterals;
	}
	
	private boolean hasUnprovedPrecedingHeadLiterals(Collection<Literal>literals,ProvabilityLevel provability) throws LiteralDataStoreException{
		for (Literal literal:literals){
			if (hasUnprovedPrecedingHeadLiterals(literal,provability))return true;
		}
		return false;
	}
	
	private boolean isReadyToProve(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		Temporal literalTemporal=literal.getTemporal();
		// check for any unproved preceding literals
		boolean hasUnprovedPrecedingHeadLiterals = hasUnprovedRelatedPrecedingLiterals(literalTemporal,literal, provability);
//		boolean hasUnprovedPrecedingHeadLiterals = hasUnprovedPrecedingHeadLiterals(literal, provability);
		if (hasUnprovedPrecedingHeadLiterals) return false;

		// check for any unproved preceding conflicting literals
		Set<Literal> conflictLiterals = getConflictLiterals(literal);
		if (hasUnprovedRelatedPrecedingLiterals(literalTemporal,conflictLiterals,provability))return false;
//		if (hasUnprovedPrecedingHeadLiterals(conflictLiterals,provability))return false;
		
		// check for any ambiguous conclusions pending to process
		ConclusionType ct = ProvabilityLevel.DEFINITE.equals(provability) ? ConclusionType.DEFINITE_PROVABLE
				: ConclusionType.DEFEASIBLY_PROVABLE;
		if (isAmbiguousConclusionExist(literal, ct)) return false;
		if (isAmbiguousConclusionExist(conflictLiterals, ct)) return false;
		
		return true;
	}


	@Override
	protected ProcessStatus generateConclusions_defeasiblyProvable(final Literal literal) throws ReasoningEngineException, TheoryException {
		System.out.println("generateConclusions_defeasiblyProvable(" + literal + ")...start");
		try {
			//super.generateConclusions_defeasiblyProvable(literal);
			
//			System.out.println("SdlReasoningEngine.generateConclusions_defeasiblyProvable(" + literal + ")...start");
			logMessage(Level.FINE, 1, "generate inference: defeasibly provable: ", literal);

			Set<String> rulesToRemove = new TreeSet<String>();
			Set<Rule> rulesModified = theory.removeBodyLiteralFromRules(literal, RuleType.DEFEASIBLE);
			
			// =================================================================
			// tdl modification - start
			System.out.println("  rule modified:");
			for (Rule r : rulesModified) {
				System.out.println("    " + r.toString());
			}
			
			// related body literals checking
			Map<ConclusionType, Set<Literal>> relatedConclusions = generateRelatedBodyLiteralsConclusions_positive(literal,
					ConclusionType.DEFEASIBLY_PROVABLE);
			for (Entry<ConclusionType, Set<Literal>> conclusionEntry : relatedConclusions.entrySet()) {
				ConclusionType conclusionType = conclusionEntry.getKey();
				Set<Literal> relatedBodyLiterals = conclusionEntry.getValue();
				if (conclusionType.isPositiveConclusion()) {
					for (Literal bodyLiteral : relatedBodyLiterals) {
						if (isReadyToProve(bodyLiteral, ProvabilityLevel.DEFEASIBLE)) {
							System.out.println("  (" + bodyLiteral + ").readyToProve=true");
							Set<Rule>additionRulesModified=theory.removeBodyLiteralFromRules(bodyLiteral, RuleType.DEFEASIBLE);
							for (Rule r:additionRulesModified){
								System.out.println("    ==> additionalRulesModified="+r);
							}
							rulesModified.addAll(additionRulesModified);
						} else {
							System.out.println("  (" + bodyLiteral + ").readyToProve=false");
						}
					}
				} else {
					generateConclusionsWithLiteral(conclusionType, relatedBodyLiterals, true);
				}
			}
			if (rulesModified.size()>0){
				for (Rule r:rulesModified){
					logMessage(Level.FINER, 1, "rules modified", r);
				}
			}else {
				logMessage(Level.FINER,1,"No rules are modified!");
			}
			// tdl modification - end
			// =================================================================

			// modified for new algorithm - start
			provableRulesSuperiorityUpdate(rulesModified);
			// modified for new algorithm - end

			if (rulesModified.size() == 0) return ProcessStatus.SUCCESS;
			Set<Literal>headLiteralsToJustify=new TreeSet<Literal>();

			for (Rule r : rulesModified) {
		//		System.out.println("r=" + r.getLabel());
				RuleExt rule = (RuleExt) r;
				logMessage(Level.FINER, 2, null, literal, ": rule=", rule, " is empty body=", rule.isEmptyBody());
				// tdl modification - start
				Literal headLiteral = evaluateHeadLiteralProvability_defeasiblyProvable(rule,rulesToRemove);
				if (null!=headLiteral)	headLiteralsToJustify.add(headLiteral);
				
				//
				// the followings are moved to evaluateHeadLiteralProvability_defeasiblyProvable
				//
//				if (null != newRulesToRemove && newRulesToRemove.size()>0) rulesToRemove.addAll(newRulesToRemove);
//				if (rule.isEmptyBody() && rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0) {
//					Literal headLiteral = rule.getHeadLiterals().get(0);
//					// tdl modification - start
//					Temporal headTemporal=headLiteral.getTemporal();
//					if (null==headTemporal)headTemporal=PERSISTENT_TEMPORAL;
//					// tdl modification - end
//					rulesToRemove.add(rule.getLabel());
//					Set<Literal> conflictLiterals = getConflictLiterals(headLiteral);
//		//			System.out.println("generateConclusions_defeasiblyProvable..1");
//					boolean containsUnprovedRuleInTheory = containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE);
//					logMessage(Level.FINEST, 2, "==>2.0 conflictLiterals: ", conflictLiterals, ",unproved rule in theory="
//							+ containsUnprovedRuleInTheory);
//			//		System.out.println("generateConclusions_defeasiblyProvable..2");
//					if (containsUnprovedRuleInTheory) {
//						logMessage(Level.FINEST, 2, "==>2.1 generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)", headLiteral);
//						System.out.printf(AppConst.IDENTATOR+"generateConclusions_defeasiblyProvable ==> add ambiguous (+d) %1s\n", headLiteral);
//						Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
//						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
//						addRecord(conclusion);
//				//		System.out.println("generateConclusions_defeasiblyProvable..3");
//					} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
//					//	System.out.println("generateConclusions_defeasiblyProvable..4");
//					} else {
//						//System.out.println("generateConclusions_defeasiblyProvable..5");
//						boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
//						boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
//						if (chk1 || chk2) {
//							//System.out.println("generateConclusions_defeasiblyProvable..6");
//							logMessage(Level.FINEST, 2, "==>2.2 generateConclusions_defeasiblyProvable: ==> add ambiguous (+D)", headLiteral);
//							addAmbiguousConclusion(new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral), rule.getOriginalLabel());
//						} else {
////							System.out.println("generateConclusions_defeasiblyProvable..7");
//							boolean hasConflictRecord = false;
//							if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)) hasConflictRecord = true;
//							if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) hasConflictRecord = true;
//							if (hasConflictRecord) {
//		//						System.out.println("generateConclusions_defeasiblyProvable..8");
//								if (isLogInferenceProcess)
//									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
//											ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
//								newLiteralFind_defeasiblyNotProvable(headLiteral, true);
//							} else {
//			//					System.out.println("generateConclusions_defeasiblyProvable..9");
//								if (isLogInferenceProcess)
//									getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
//											ConclusionType.DEFEASIBLY_PROVABLE, headLiteral, RuleInferenceStatus.APPICABLE);
//								newLiteralFind_defeasiblyProvable(headLiteral, true);
//							}
//						}
//					}
//				}
				// tdl modification - end
			}
//			System.out.println("generateConclusions_defeasiblyProvable..10");
			logMessage(Level.FINE, 2, "generateConclusions_defeasiblyProvable.removeRules=", rulesToRemove);

			removeRules(rulesToRemove);
			
			// check to see if the literals still appear in other defeasible rules of the theory
			// and remove it from the data store if no rules are found.
			for (Literal headLiteral:headLiteralsToJustify){
				safeRemoveLiteralStoreHeadLiteral(headLiteral,RuleType.DEFEASIBLE);
			}
						
			return ProcessStatus.SUCCESS;
		} finally {
			System.out.println("generateConclusions_defeasiblyProvable...end");
		}
	}
	
	private void safeRemoveLiteralStoreHeadLiteral(Literal headLiteral,RuleType ruleType) throws LiteralDataStoreException{
		if (!theory.containsInRuleHead(headLiteral, ruleType)) {
			literalDataStore.removeUnprovedHeadLiteral(headLiteral,ruleType.getProvabilityLevel());					
		}
		// make sure that the conflict literals set will be re-generated according to the new literals set 
		conflictLiteralsSet.remove(headLiteral);
	}

	
	private Literal evaluateHeadLiteralProvability_defeasiblyProvable(RuleExt rule,Set<String>rulesToRemove) throws ReasoningEngineException {
		if (!(rule.isEmptyBody() && rule.getStrongerRulesCount() == 0 && rule.getWeakerRulesCount() == 0)) return null;
System.out.println(AppConst.IDENTATOR+"evaluateHeadLiteralProvability_defeasibleProvable: "+rule);
		//Set<String> rulesToRemove = new TreeSet<String>();
logMessage(Level.FINEST, 2, "==> evaluateHeadLiteralProvability_defeasibleProvable: ", rule);

		rulesToRemove.add(rule.getLabel());
		Literal literalJustified = null;
		Literal headLiteral = rule.getHeadLiterals().get(0);

		Set<Literal> conflictLiterals = getConflictLiterals(headLiteral);
		boolean containsUnprovedRuleInTheory = containsUnprovedRuleInTheory(conflictLiterals, RuleType.DEFEASIBLE);
		logMessage(Level.FINEST, 2, "==>2.0 conflictLiterals: ", conflictLiterals, ",unproved rule in theory="
				+ containsUnprovedRuleInTheory);
		if (containsUnprovedRuleInTheory) {
			logMessage(Level.FINEST, 2, "==>2.1 generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)", headLiteral);
			System.out.printf(AppConst.IDENTATOR + "generateConclusions_defeasiblyProvable ==> add ambiguous (+d) %1s\n", headLiteral);
			Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
			addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
			addRecord(conclusion);
		} else if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_PROVABLE)) {
			logMessage(Level.FINEST, 2, "==>2.2 +d "+ headLiteral);
			// do nothing if record already exist
		} else {
			logMessage(Level.FINEST, 2, "==>2.3");
			boolean chk1 = isAmbiguousConclusionExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE);
			boolean chk2 = isAmbiguousConclusionExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE);
			System.out.println(AppConst.IDENTATOR+"  ambiguous check: chk1="+chk1+", chk2="+chk2);
			if (chk1 || chk2) {
				logMessage(Level.FINEST, 2, "==>2.3.1 generateConclusions_defeasiblyProvable: ==> add ambiguous (+d)", headLiteral);
				Conclusion c=new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE,headLiteral);
				addAmbiguousConclusion(c, rule.getOriginalLabel());
				addRecord(c);
			} else {
				logMessage(Level.FINEST, 2, "==>2.3.2" );
				boolean hasConflictRecord = false;
				if (isRecordExist(conflictLiterals, ConclusionType.DEFEASIBLY_PROVABLE)){
					logMessage(Level.FINEST, 3, "["+headLiteral+"]- has conflict record1");
					hasConflictRecord = true;
				}
				if (isRecordExist(headLiteral, ConclusionType.DEFEASIBLY_NOT_PROVABLE)) {
					logMessage(Level.FINEST, 3, "["+headLiteral+"]- has conflict record2");
					hasConflictRecord = true;
				}
				if (hasConflictRecord) {
					logMessage(Level.FINEST, 2, "==>2.3.2.1 ["+headLiteral+"]- has conflict record");
		//			if (isLogInferenceProcess)
	//					getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
//								ConclusionType.DEFEASIBLY_NOT_PROVABLE, headLiteral, RuleInferenceStatus.DISCARDED);
//					Conclusion c=new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE,headLiteral);
	//				addAmbiguousConclusion(c, rule.getOriginalLabel());
		//			addRecord(c);
					newLiteralFind_defeasiblyNotProvable(headLiteral, true);
				} else {
					logMessage(Level.FINEST, 2, "==>2.3.2.2");
					// tdl modification - start
					ProvabilityLevel provability = ProvabilityLevel.DEFEASIBLE;

					Temporal headTemporal = headLiteral.getTemporal();
					if (null == headTemporal) headTemporal = PERSISTENT_TEMPORAL;

					Temporal provableTemporalSegment = getProvableTemporalSegment(headLiteral, provability, conflictLiterals);
					System.out.println(AppConst.IDENTATOR + "  provableTemporalSegment=" + provableTemporalSegment);

					if (null == provableTemporalSegment) {
						logMessage(Level.FINEST, 2, "==>2.3.2.2.1 provableTemporalSegment=null");
						Conclusion conclusion = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, headLiteral);
						addAmbiguousConclusion(conclusion, rule.getOriginalLabel());
						addRecord(conclusion);
					} else {
						logMessage(Level.FINEST, 2, "==>2.3.2.2.2");
						if (headTemporal.equals(provableTemporalSegment)) {
							logMessage(Level.FINEST, 2, "==>2.3.2.2.2.1 +d "+headLiteral);
							if (isLogInferenceProcess)
								getInferenceLogger().updateRuleInferenceStatus(rule.getOriginalLabel(), RuleType.DEFEASIBLE,
										ConclusionType.DEFEASIBLY_PROVABLE, headLiteral, RuleInferenceStatus.APPICABLE);
							newLiteralFind_defeasiblyProvable(headLiteral, true);
						} else {
							Literal newHeadLiteral1 = headLiteral.cloneWithNoTemporal();
							newHeadLiteral1.setTemporal(provableTemporalSegment);
							Literal newHeadLiteral2 = headLiteral.cloneWithNoTemporal();
							newHeadLiteral2.setTemporal(new Temporal(provableTemporalSegment.getEndTime(), headTemporal.getEndTime()));

							literalJustified = headLiteral;
							literalDataStore.addHeadLiteral(newHeadLiteral1, provability, false);
							literalDataStore.addHeadLiteral(newHeadLiteral2, provability, false);

							newLiteralFind_defeasiblyProvable(newHeadLiteral1, true);

							Conclusion conclusion2 = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, newHeadLiteral2);
							addAmbiguousConclusion(conclusion2, rule.getOriginalLabel());
							addRecord(conclusion2);

							logMessage(Level.FINEST, 2, "==>2.3.2.2.2.2: justify head literal to:"+newHeadLiteral1);
							logMessage(Level.FINEST, 2, "==>2.3.2.2.2.2: add ambiguous (+d):"+newHeadLiteral2);
						}
					}
					// tdl modification - end
				}
			}
		}

		return literalJustified;
	}
	
	private Temporal getProvableTemporalSegment(Literal literal, ProvabilityLevel provability,Collection<Literal>conflictLiterals) throws LiteralDataStoreException {
	//	Set<Literal> conflictLiterals = getConflictLiterals(literal);
logMessage(Level.FINEST,3,"getProvableTemporalSegment",literal,provability,conflictLiterals);
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		long startTime = literalTemporal.getStartTime();
		long endTime = literalTemporal.getEndTime();

		for (Literal conflictHeadLiteral : conflictLiterals) {
			// check the temporal information of preceding literals
			if (hasUnprovedRelatedPrecedingLiterals(literalTemporal,conflictHeadLiteral,provability)){
				System.out.println("    getProvableTemporalSegment => null temporal segment: has preceding conflict literals unproved");
				logMessage(Level.FINEST,4,"getProvableTemporalSegment =>null temporal segment","has preceding conflict literals unproved");
				return null;
			}
//			TreeMap<Temporal, Literal> unprovedPrecedingConflictHeadLiterals = literalDataStore.getUnprovedPrecedingHeadLiterals(
//					conflictHeadLiteral, provability);
//			if (null != unprovedPrecedingConflictHeadLiterals) {
//				for (Temporal precedingTemporal : unprovedPrecedingConflictHeadLiterals.keySet()) {
//					// check to prevent conclusion being defeated due to modal operator in the later process
//					if (literalTemporal.overlap(precedingTemporal) ){// && startTime < precedingTemporal.getEndTime()) {
//						System.out.println("    getProvableTemporalSegment => null temporal segment: has preceding conflict literals unproved");
//						return null;
//					}
//				}
//			}

			TreeMap<Temporal, Map<ConclusionType, Set<String>>> ambiguousConclusionsSet = getAmbiguousConclusions(conflictHeadLiteral,
					provability);
			if (null != ambiguousConclusionsSet) { // check the temporal information of ambiguous conclusions
				SortedMap<Temporal, Map<ConclusionType, Set<String>>> extractedConclusions = ambiguousConclusionsSet.headMap(Temporal
						.getTemporalInstance(endTime));
				if (null != extractedConclusions) {
					for (Entry<Temporal, Map<ConclusionType, Set<String>>> extractedEntry : extractedConclusions.entrySet()) {
						Temporal acTemporal = extractedEntry.getKey();
						if (literalTemporal.overlap(acTemporal)) {
							long acStart = acTemporal.getStartTime();
							// check to prevent conclusion being defeated due to modal operator in the later process
							if (literalTemporal.getStartTime() >= acStart) {
								System.out.println("    getProvableTemporalSegment => null temporal segment: has preceding ambiguous conclusion unresolved");
								logMessage(Level.FINEST,4,"getProvableTemporalSegment => null temporal segment:","has preceding ambiguous conclusion unresolved");
								return null;
							} else {
								if (endTime > acStart) endTime = acStart;
							}
						}
					}
				}
			}

			// check the temporal information of succeeding literals
			TreeMap<Temporal, Literal> unprovedSucceedingConflictHeadLiterals = literalDataStore.getUnprovedSucceedingHeadLiterals(
					conflictHeadLiteral, provability);
			if (null == unprovedSucceedingConflictHeadLiterals) continue;
			for (Temporal succeedingTemporal : unprovedSucceedingConflictHeadLiterals.keySet()) {
				if (literalTemporal.overlap(succeedingTemporal) && endTime > succeedingTemporal.getStartTime())
					endTime = succeedingTemporal.getStartTime();
			}
		}
		
		if (endTime == literalTemporal.getEndTime()) {
			return literalTemporal;
		} else {
			if (endTime == literalTemporal.getStartTime() && AppFeatureConst.isIntervalBasedTemporal) return null;
			return new Temporal(literalTemporal.getStartTime(), endTime);
		}
		// return endTime == headTemporal.getEndTime() ? headTemporal : new Temporal(headTemporal.getStartTime(), endTime);
	}

	

	
	@Override
	protected ProcessStatus generateConclusions_defeasiblyNotProvable(final Literal literal) throws ReasoningEngineException {
		logMessage(Level.FINE,1,"generateConclusions_defeasiblyNotProvable(" + literal + ")...start");
		try {
			super.generateConclusions_defeasiblyNotProvable(literal);
			TreeMap<Temporal, Literal> negativeLiterals = literalDataStore.getRelatedBodyLiteralsInProvableRange(literal, ProvabilityLevel.DEFEASIBLE);
			if (null != negativeLiterals && negativeLiterals.size() > 0) {
				for (Literal negativeLiteral:negativeLiterals.values()){
					logMessage(Level.FINE,3,"generateRelatedBodyLiteralsConclusions_negative(-d " + literal + ").negativeLiteral="+negativeLiteral);
					super.generateConclusions_defeasiblyNotProvable(negativeLiteral);
				}
			}			
			return ProcessStatus.SUCCESS;
		} finally {
			logMessage(Level.FINE,1,"generateConclusions_defeasiblyNotProvable(" + literal + ")...end");
		}
	}

	


	private Map<String, Rule> getRelatedRulesInTheory(Literal literal, Temporal temporalInterval, ProvabilityLevel provability) {
		logMessage(Level.FINE,1,"generateRelatedBodyLiteralsConclusions_negative(" + literal + ")...start");

		Set<Literal> literalsInTheory = theory.getRelatedLiterals(literal, provability);
		if (null == literalsInTheory) return null;

		Map<String, Rule> relatedRuleHeadLiterals = new TreeMap<String, Rule>();
		RuleType ruleType = ProvabilityLevel.DEFEASIBLE.equals(provability) ? RuleType.STRICT : RuleType.DEFEASIBLE;

		for (Literal relatedLiteral : literalsInTheory) {
			Temporal relatedLiteralTemporal = relatedLiteral.getTemporal();
			if (null == relatedLiteralTemporal && Long.MIN_VALUE == temporalInterval.getStartTime() //
					|| temporalInterval.overlap(relatedLiteralTemporal)) {
				Set<Rule> rulesInTheory = theory.getRulesWithHead(relatedLiteral);
				if (null == rulesInTheory || rulesInTheory.size() == 0) continue;
				for (Rule rule : rulesInTheory) {
					if (!ruleType.equals(rule.getRuleType())) continue;
					relatedRuleHeadLiterals.put(rule.getLabel(), rule);
				}

			}
		}
		logMessage(Level.FINE,1,"getRelatedRulesInTheory(" + literal + ").relatedRuleHeadLiterals="+relatedRuleHeadLiterals);

		return relatedRuleHeadLiterals.size() == 0 ? null : relatedRuleHeadLiterals;
	}

//	private static Literal dummyLiteral=DomUtilities.getLiteral("aabc", false, null, new Temporal(123,234), null,false);
	

	
	private Map<ConclusionType, Set<Literal>> generateRelatedBodyLiteralsConclusions_positive(final Literal literal, final ConclusionType conclusionType)
			throws LiteralDataStoreException {
		Map<ConclusionType, Set<Literal>> conclusionsGenerated = new TreeMap<ConclusionType, Set<Literal>>();

		ProvabilityLevel provability = conclusionType.getProvabilityLevel();
		TreeMap<Temporal, Literal> literalsToCheck = literalDataStore.getRelatedBodyLiteralsInProvableRange(literal, provability);
		logMessage(Level.FINER, 1, "generateRelatedBodyLiteralsConclusions_positive - literalsToCheck", null == literalsToCheck ? "null"
				: literalsToCheck.values());

	//	if (literal.equals(dummyLiteral) && ProvabilityLevel.DEFEASIBLE.equals(provability))		
		System.out.println("  generateRelatedBodyLiteralsConclusions_positive("+literal+","+provability+").literalsToCheck="+literalsToCheck);
		
		if (null == literalsToCheck) return conclusionsGenerated;

		for (Entry<Temporal, Literal> entry : literalsToCheck.entrySet()) {
			Literal bodyLiteral = entry.getValue();
			if(!theory.contains(bodyLiteral))continue;
			Temporal bodyLiteralTemporal=bodyLiteral.getTemporal();
			if (null==bodyLiteralTemporal)bodyLiteralTemporal=PERSISTENT_TEMPORAL;

			ConclusionType ct = literalDataStore.getConclusion(bodyLiteral, provability);
			System.out.println("    bodyLiteral="+bodyLiteral+", ct="+ct);
			if (null == ct || literalDataStore.isLiteralProved(bodyLiteral, provability)) continue;

			if (ct.isPositiveConclusion()) { // check conflict literal provability for +ve conclusion derived
				Set<Literal>conflictBodyLiterals=getConflictLiterals(bodyLiteral);
				for (Literal conflictBodyLiteral : conflictBodyLiterals) {
					ConclusionType ctConflict = literalDataStore.getConclusion(conflictBodyLiteral, provability);
					if (null == ctConflict) {
						ct = null;
					} else if (ctConflict.isPositiveConclusion()) { // under the current reasoning strategies,
																	// not sure for whether it is possible for this to appear
																	// as all conflicting conclusions should be rebutted.
//TreeMap<Temporal,Literal>literalsInTheory=literalDataStore.getHeadLiteralsWithSameStartTemporal(conflictBodyLiteral);
//if (null!=literalsInTheory) {
//	//	k
//	ct = null;	
//	if (this.hasStrongerMode(bodyLiteral, conflictBodyLiteral)) {
//		
//	}
//}
					}
					if (null==ct)break;
				}			
			}
			if (null==ct)continue;

			Set<Literal> literalSet = conclusionsGenerated.get(ct);
			if (null == literalSet) {
				literalSet = new TreeSet<Literal>();
				conclusionsGenerated.put(ct, literalSet);
			}
			literalSet.add(bodyLiteral);
			//System.out.println("conclusionType=" + conclusionType);
			if (ct.isPositiveConclusion()) provableBodyLiterals[conclusionType.getProvabilityLevel().ordinal()].add(bodyLiteral);
		}
		// System.out.println("** conclusionsGenerated="+conclusionsGenerated);
		logMessage(Level.FINER, 1, "generateRelatedBodyLiteralsConclusions_positive - conclusionsGenerated", conclusionsGenerated);

		return conclusionsGenerated;
	}

	
	private ConclusionType checkLiteralProvability(Literal literal,ProvabilityLevel provability){
		return null;
	}

	
	private ProcessStatus generateConclusionsWithLiteral(ConclusionType conclusionType, Collection<Literal> literals,
			boolean checkInference) throws ReasoningEngineException {
		if (null == literals || literals.size() == 0) return ProcessStatus.SUCCESS;
		switch (conclusionType) {
		case DEFINITE_PROVABLE:
			for (Literal literal : literals) {
				newLiteralFind_definiteProvable(literal, checkInference);
			}
			break;
		case DEFINITE_NOT_PROVABLE:
			for (Literal literal : literals) {
				newLiteralFind_definiteNotProvable(literal, checkInference);
			}
			break;
		case DEFEASIBLY_PROVABLE:
			for (Literal literal : literals) {
				newLiteralFind_defeasiblyProvable(literal, checkInference);
			}
			break;
		case DEFEASIBLY_NOT_PROVABLE:
			for (Literal literal : literals) {
				//addPendingConclusion(new Conclusion(ConclusionType.DEFEASIBLY_NOT_PROVABLE,literal));
				newLiteralFind_defeasiblyNotProvable(literal, checkInference);
			}
			break;
		default:
			throw new ReasoningEngineException(getClass(), ErrorMessage.CONCLUSION_UNSUPPORTED_CONCLUSION_TYPE,
					new Object[] { conclusionType });
		}
		return ProcessStatus.SUCCESS;
	}


	
	@Override
	protected ProcessStatus addRecord(ConclusionType conclusionType,Literal literal){
		TreeSet<Temporal>temporals=getRecordTemporals(temporalRecords,conclusionType,literal,true);
		Temporal literalTemporal=literal.getTemporal();
		if (null==literalTemporal)literalTemporal=PERSISTENT_TEMPORAL;
		temporals.add(literalTemporal);
		removeConsolidatedTemporalRecords(conclusionType,literal);
		return ProcessStatus.SUCCESS;		
	}
	

	
	@Override
	protected ProcessStatus removeRecord(Conclusion conclusion){
		Literal literal=conclusion.getLiteral();
		ConclusionType conclusionType=conclusion.getConclusionType();
		TreeSet<Temporal>temporals=getRecordTemporals(temporalRecords,conclusionType,literal,false);
		if (null!=temporals){
			Temporal conclusionTemporal=literal.getTemporal();
			if (null==conclusionTemporal)conclusionTemporal=PERSISTENT_TEMPORAL;
			temporals.remove(conclusionTemporal);
			
			if (temporals.size()==0){
				Map<ConclusionType ,TreeSet<Temporal>> conclusionTypeSet=temporalRecords.get(literal);
				conclusionTypeSet.remove(conclusionType);
				if (conclusionTypeSet.size()==0)temporalRecords.remove(literal);
			}
		}
		removeConsolidatedTemporalRecords(conclusionType,literal);
		return ProcessStatus.SUCCESS;
	}
	
	@Override 
	protected boolean isRecordExist(Literal literal,ConclusionType conclusionType){
		TreeSet<Temporal>consolidatedTemporals=getRecordTemporals(consolidatedTemporalRecords,conclusionType,literal,false);
		
		if (null==consolidatedTemporals){
			TreeSet<Temporal>temporals=getRecordTemporals(temporalRecords,conclusionType,literal,false);
			if (null==temporals)return false;
		
			consolidatedTemporals=new TreeSet<Temporal>(temporals);
			Temporal.consolidateTemporalSegments(consolidatedTemporals);
			
			Map<ConclusionType ,TreeSet<Temporal>> conclusionTypeSet=consolidatedTemporalRecords.get(literal);
			if (null==conclusionTypeSet){
				conclusionTypeSet=new TreeMap<ConclusionType,TreeSet<Temporal>>();
				consolidatedTemporalRecords.put(literal.cloneWithNoTemporal(),conclusionTypeSet);
			}
			conclusionTypeSet.put(conclusionType,consolidatedTemporals);
		}
		logMessage(Level.FINEST,4,"isRecordExist("+literal+","+conclusionType+")1="+consolidatedTemporals);
		Temporal literalTemporal=literal.getTemporal();
		if (null==literalTemporal)literalTemporal=PERSISTENT_TEMPORAL;
		
		
		SortedSet<Temporal>extractedTemporalSet=consolidatedTemporals.subSet(literalTemporal.getStartTimeAsInstance(),true,literalTemporal.getEndTimeAsInstance(),false);
		logMessage(Level.FINEST,4,"isRecordExist("+literal+","+conclusionType+")2:"+literalTemporal+":"+extractedTemporalSet);
		if (null!=extractedTemporalSet && extractedTemporalSet.size()>0){
			for (Temporal temporal:extractedTemporalSet){
				if (temporal.contains(literalTemporal))return true;
			}
		}
		Temporal extractedTemporal=consolidatedTemporals.floor(literalTemporal.getStartTimeAsInstance());
		logMessage(Level.FINEST,4,"isRecordExist("+literal+","+conclusionType+")3:"+literalTemporal+":"+extractedTemporal);
		if (null!=extractedTemporal){
			if (extractedTemporal.contains(literalTemporal))return true;
		}
		return false;
	}
	
	private void removeConsolidatedTemporalRecords(ConclusionType conclusionType,Literal literal){
		Map<ConclusionType ,TreeSet<Temporal>> conclusionTypeSet=consolidatedTemporalRecords.get(literal);
		if (null==conclusionTypeSet)return;
		conclusionTypeSet.remove(conclusionType);		
	}
	
	
	protected TreeSet<Temporal> getRecordTemporals(Map<Literal,Map<ConclusionType,TreeSet<Temporal>>>records,//
			ConclusionType conclusionType,Literal literal,boolean addNew){
		Map<ConclusionType ,TreeSet<Temporal>> conclusionTypeSet=records.get(literal);
		if (null==conclusionTypeSet){
			if (!addNew)return null;
			conclusionTypeSet=new TreeMap<ConclusionType,TreeSet<Temporal>>();
			temporalRecords.put(literal.cloneWithNoTemporal(),conclusionTypeSet);
		}
		TreeSet<Temporal>temporals=conclusionTypeSet.get(conclusionType);
		if(null==temporals){
			if (!addNew)return null;
			temporals=new TreeSet<Temporal>();
			conclusionTypeSet.put(conclusionType, temporals);
		}
		return temporals;
	}
	
	
	@Override
	protected ProcessStatus newLiteralFind_defeasiblyNotProvable(final Literal literal, final boolean isCheckInference) {
		logMessage(Level.FINE, 1, "newLiteralFind_defeasiblyNotProvable", literal);
		Conclusion conclusion = new Conclusion(ConclusionType.TENTATIVELY_NOT_PROVABLE, literal);
		addRecord(conclusion);
		if (isCheckInference) {
logMessage(Level.FINEST,0,			getTemporalRecordsString("temporalRecords",temporalRecords));
			checkInference(conclusion);
		}
		return ProcessStatus.SUCCESS;
	}
	
	
	
private String getTemporalRecordsString(String label,		Map<Literal,Map<ConclusionType,TreeSet<Temporal>>>records){
	
	StringBuilder sb=new StringBuilder("---------------------------------------------------------------------------");
	
	if (null!=label&&!"".equals(label.trim())){
		String s=label.trim();
		sb.append("\n"+s+"\n---------------");
	}
	
	for (Entry<Literal,Map<ConclusionType,TreeSet<Temporal>>>recordEntry:records.entrySet()){
		sb.append("\n").append(recordEntry.getKey());
		for (Entry<ConclusionType,TreeSet<Temporal>>conclusionTypes:recordEntry.getValue().entrySet()){
			sb.append("\n  ").append(conclusionTypes.getKey()).append(":").append(conclusionTypes.getValue());
		}
	}
	sb.append("\n").append("---------------------------------------------------------------------------");
	return sb.toString();
}
	
}
