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

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.app.utils.NameValuePair;
import com.app.utils.TextUtilities;

import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralComparator;
import spindle.core.dom.ProvabilityLevel;
import spindle.core.dom.Rule;
import spindle.core.dom.Temporal;
import spindle.core.dom.TemporalStartComparator;
import spindle.core.dom.Theory;
import spindle.engine.ReasoningEngineFactory;
import spindle.engine.ReasoningEngineFactoryException;
import spindle.sys.AppConst;
import spindle.sys.AppFeatureConst;
import spindle.sys.AppLogger;
import spindle.sys.AppModuleBase;
import spindle.sys.message.ErrorMessage;

/**
 * Literal data store.
 * For storing and manipulating (temporal) literals provability information.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.2.1
 * @version Last modified 2012.09.20
 */
public class LiteralDataStore extends AppModuleBase {
	private static final String IDENTATOR = "\n" + AppConst.IDENTATOR;
	private static final Temporal PERSISTENT_TEMPORAL = new Temporal();
	private static final Comparator<? super Literal> PLAIN_LITERAL_COMPARATOR = new LiteralComparator(false);
	@SuppressWarnings("unused")
	private static final Comparator<? super Literal> LITERAL_START_COMPARATOR = new LiteralComparator(true, new TemporalStartComparator());
	private static final Comparator<? super Temporal> TEMPORAL_START_COMPARATOR = new TemporalStartComparator();

	private Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> bodyLiterals;
	private Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> headLiterals;
	private Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> unprovedHeadLiterals;
	// store the set of proved literal which may or may not be appeared as head literals (due to temporal)
	private Map<ProvabilityLevel, TreeSet<Literal>> provedLiterals;

	private Map<ProvabilityLevel, Map<Literal, TreeSet<Temporal>>> temporalSegmentsToProve;

	private Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> conclusions;
	private Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> enforcedConclusions;

	private TdlConclusionUpdater conclusionUpdater;

	public LiteralDataStore(Theory theory) throws LiteralDataStoreException {
		headLiterals = new TreeMap<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>>();
		bodyLiterals = new TreeMap<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>>();

		unprovedHeadLiterals = new TreeMap<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>>();
		conclusions = new TreeMap<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>>();
		enforcedConclusions = new TreeMap<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>>();
		provedLiterals = new TreeMap<ProvabilityLevel, TreeSet<Literal>>();

		temporalSegmentsToProve = new TreeMap<ProvabilityLevel, Map<Literal, TreeSet<Temporal>>>();

		try {
			conclusionUpdater = ReasoningEngineFactory.getTdlConclusionUpdater();
		} catch (ReasoningEngineFactoryException e) {
			throw new LiteralDataStoreException(e);
		}

		for (Rule rule : theory.getFactsAndAllRules().values()) {
			for (Literal literal : rule.getBodyLiterals()) {
				addBodyLiteral(literal, rule.getRuleType().getProvabilityLevel());
			}
			for (Literal literal : rule.getHeadLiterals()) {
				addHeadLiteral(literal, rule.getRuleType().getProvabilityLevel(), false);
			}
		}
	}

	public void addRule(Rule rule) {
		for (Literal literal : rule.getBodyLiterals()) {
			addBodyLiteral(literal, rule.getRuleType().getProvabilityLevel());
		}
		for (Literal literal : rule.getHeadLiterals()) {
			addHeadLiteral(literal, rule.getRuleType().getProvabilityLevel(), false);
		}
	}

	public void removeLiteral(Literal literal, ProvabilityLevel provability) {
		try {
			removeLiteralFromSet(literal, provability, bodyLiterals, false);
			removeLiteralFromSet(literal, provability, headLiterals, false);
			removeLiteralFromSet(literal, provability, unprovedHeadLiterals, false);
		} catch (LiteralDataStoreException e) {
		}
		generateProvableTemporalSegments(literal, provability);
	}

	public void addHeadLiteral(Literal literal, ProvabilityLevel provability, boolean regenerateProvableTemporalSegment) {
		addLiteralToSet(literal, provability, headLiterals);

		if (AppFeatureConst.isUpdateTemporalSegmentsToProve) {
			if (regenerateProvableTemporalSegment) {
				generateProvableTemporalSegments(literal, provability);
			} else {
				updateProvableTemporalSegments(literal, provability);
			}
		}

		addUnprovedHeadLiteral(literal, provability);
	}

	private void addUnprovedHeadLiteral(Literal literal, ProvabilityLevel provability) {
		addLiteralToSet(literal, provability, unprovedHeadLiterals);
	}

	private void addBodyLiteral(Literal literal, ProvabilityLevel provability) {
		addLiteralToSet(literal, provability, bodyLiterals);
	}

	private void addLiteralToSet(Literal literal, ProvabilityLevel provability,
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> literalSet) {
		Map<Literal, TreeMap<Temporal, Literal>> provabilityEntry = literalSet.get(provability);
		if (null == provabilityEntry) {
			provabilityEntry = new TreeMap<Literal, TreeMap<Temporal, Literal>>(PLAIN_LITERAL_COMPARATOR);
			literalSet.put(provability, provabilityEntry);
		}
		TreeMap<Temporal, Literal> literals = provabilityEntry.get(literal);
		if (null == literals) {
			literals = new TreeMap<Temporal, Literal>();
			provabilityEntry.put(literal.cloneWithNoTemporal(), literals);
		}

		Temporal temporal = literal.getTemporal();
		if (null == temporal) temporal = PERSISTENT_TEMPORAL;
		literals.put(temporal, literal);
		if (AppFeatureConst.printDataStoreMessage) System.out.println(literal + ":" + literals);
	}

	public void generateProvableTemporalSegments(Literal literal, ProvabilityLevel provability) {
		Map<Temporal, Literal> headLiteralSet = getHeadLiteralSet(literal, provability, headLiterals);
		if (null == headLiteralSet || headLiteralSet.size() == 0) return;

		Map<Literal, TreeSet<Temporal>> literalsSet = temporalSegmentsToProve.get(provability);
		if (null == literalsSet) {
			literalsSet = new TreeMap<Literal, TreeSet<Temporal>>(PLAIN_LITERAL_COMPARATOR);
			temporalSegmentsToProve.put(provability, literalsSet);
		} else {
			// remove all old temporal segments generated
			literalsSet.remove(literal);
		}

		TreeSet<Temporal> temporalSegments = new TreeSet<Temporal>(headLiteralSet.keySet());
		literalsSet.put(literal.cloneWithNoTemporal(), temporalSegments);

		if (headLiteralSet.size() < 2) return;
		Temporal.consolidateTemporalSegments(temporalSegments);
	}

	private void updateProvableTemporalSegments(Literal literal, ProvabilityLevel provability) {
		Map<Literal, TreeSet<Temporal>> literalsSet = temporalSegmentsToProve.get(provability);
		if (null == literalsSet) {
			literalsSet = new TreeMap<Literal, TreeSet<Temporal>>(PLAIN_LITERAL_COMPARATOR);
			temporalSegmentsToProve.put(provability, literalsSet);
		}
		TreeSet<Temporal> temporalSegments = literalsSet.get(literal);
		if (null == temporalSegments) {
			temporalSegments = new TreeSet<Temporal>();
			literalsSet.put(literal.cloneWithNoTemporal(), temporalSegments);
		}

		Temporal newTemporal = literal.getTemporal();
		if (null == newTemporal) newTemporal = new Temporal();
		temporalSegments.add(newTemporal);

		if (temporalSegments.size() < 2) return;
		Temporal.consolidateTemporalSegments(temporalSegments);
	}



	public void removeUnprovedHeadLiteral(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		removeLiteralFromSet(literal, provability, unprovedHeadLiterals, AppFeatureConst.throwLiteralDataStoreRemoveUnfoundException);
	}

	private void removeLiteralFromSet(Literal literal, ProvabilityLevel provability, //
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> masterLiteralsSet, //
			boolean throwUnfoundException) throws LiteralDataStoreException {
		if (AppFeatureConst.printDataStoreMessage)
		System.out.println("literalDataStore.removeLiteral(" + literal + "," + provability + ")");
		Map<Literal, TreeMap<Temporal, Literal>> provabilityEntry = masterLiteralsSet.get(provability);
		if (null == provabilityEntry) {
			if (throwUnfoundException) throw new LiteralDataStoreException(ErrorMessage.LITERAL_LITERAL_NOT_EXIST_IN_DATASTORE,
					new Object[] { literal });
			else return;
		}
		TreeMap<Temporal, Literal> literalsSet = provabilityEntry.get(literal);
		if (null == literalsSet) {
			if (throwUnfoundException) throw new LiteralDataStoreException(ErrorMessage.LITERAL_LITERAL_NOT_EXIST_IN_DATASTORE,
					new Object[] { literal });
			else return;
		}
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;
		literalsSet.remove(literalTemporal);

		if (literalsSet.size() == 0) {
			provabilityEntry.remove(literal);
			if (provabilityEntry.size() == 0) masterLiteralsSet.remove(provability);
		}
	}
	
	@Override
	public void setAppLogger(final AppLogger logger) {
			super.setAppLogger(logger);
		conclusionUpdater.setAppLogger(logger);
	}

	private Deque<NameValuePair<ConclusionType, Literal>> verifyEnforcedConclusion(Literal literal, ConclusionType conclusionType) {
		TreeMap<Temporal, ConclusionType> conclusionSet = getConclusionSet(literal, conclusionType.getProvabilityLevel(),
				enforcedConclusions, false);
		if (AppFeatureConst.printDataStoreMessage) {
			System.out.println("verifyEnforcedConclusion(" + conclusionType.getSymbol() + " " + literal + ")");
			System.out.println("  conclusionSet=" + conclusionSet);
		}
		if (null == conclusionSet || conclusionSet.size() == 0) return null;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		Temporal startTemporal = literalTemporal.getStartTimeAsInstance();
		Temporal endTemporal = literalTemporal.getEndTimeAsInstance();

		SortedMap<Temporal, ConclusionType> enforcedConclusions = new TreeMap<Temporal, ConclusionType>();

		Entry<Temporal, ConclusionType> lowerEntry = conclusionSet.lowerEntry(startTemporal);
		if (null != lowerEntry) enforcedConclusions.put(lowerEntry.getKey(), lowerEntry.getValue());

		SortedMap<Temporal, ConclusionType> conclusionSubset = conclusionSet.subMap(startTemporal, true, endTemporal, false);
		if (null != conclusionSubset) enforcedConclusions.putAll(conclusionSubset);

		Deque<NameValuePair<ConclusionType, Literal>> newConclusions = new ArrayDeque<NameValuePair<ConclusionType, Literal>>();

		Temporal currentTemporal = literalTemporal.clone();

		for (Entry<Temporal, ConclusionType> enforcedEntry : enforcedConclusions.entrySet()) {
			Temporal enforcedTemporal = enforcedEntry.getKey();
			if (!enforcedTemporal.overlap(currentTemporal)) continue;

			if (enforcedTemporal.startAfter(currentTemporal)) {
				Temporal excludedTemporal = new Temporal(currentTemporal.getStartTime(), enforcedTemporal.getStartTime());
				Literal excludedLiteral = literal.cloneWithNoTemporal();
				excludedLiteral.setTemporal(excludedTemporal);
				newConclusions.add(new NameValuePair<ConclusionType, Literal>(conclusionType, excludedLiteral));
				currentTemporal.setStartTime(enforcedTemporal.getStartTime());
			}
			if (currentTemporal.endAfter(enforcedTemporal)) {
				currentTemporal.setStartTime(enforcedTemporal.getEndTime());
			} else {
				currentTemporal = null;
			}
			if (null == currentTemporal) break;
		}
		if (null != currentTemporal) {
			if (!(currentTemporal.isTimeInstance() && !AppFeatureConst.isIntervalBasedTemporal)) {
				Literal excludedLiteral = literal.cloneWithNoTemporal();
				excludedLiteral.setTemporal(currentTemporal);
				newConclusions.add(new NameValuePair<ConclusionType, Literal>(conclusionType, excludedLiteral));
			}
		}

		if (!AppConst.isDeploy) System.out.println("verified conclusion set: " + newConclusions);

		return newConclusions;
	}

	public void updateConclusion(Literal literal, ConclusionType conclusionType) throws LiteralDataStoreException {
		// Conclusion conclusion=new Conclusion(conclusionType,literal);
		// public void updateConclusion(Conclusion conclusion) throws LiteralDataStoreException {
		Deque<NameValuePair<ConclusionType, Literal>> newConclusionSet = verifyEnforcedConclusion(literal, conclusionType);
		if (null == newConclusionSet) {
			// updateConclusion(conclusion, false);
			updateConclusion(literal, conclusionType, false);
		} else {
			if (newConclusionSet.size() == 0) return;
			for (NameValuePair<ConclusionType, Literal> newConclusion : newConclusionSet) {
				updateConclusion(newConclusion.getValue(), newConclusion.getKey(), false);
			}
		}
	}

	public void updateEnforcedConclusion(Literal literal, ConclusionType conclusionType) throws LiteralDataStoreException {
		updateConclusion(literal, conclusionType, true);
	}

	/**
	 * Update the conclusions set with the new conclusion.
	 * Note that for temporal literals, the conclusions will be updated according to the strategy defined in the TDL
	 * conclusion updater,
	 * which can be modified according to different variants.
	 * 
	 * @param conclusion Conclusion to be updated.
	 * @throws LiteralDataStoreException
	 * @see TdlConclusionUpdater
	 */
	private void updateConclusion(Literal literal, ConclusionType conclusionType, boolean enforceNewConclusion)
			throws LiteralDataStoreException {
		ProvabilityLevel provability = conclusionType.getProvabilityLevel();
		TreeMap<Temporal, ConclusionType> conclusionSet = getConclusionSet(literal, provability, conclusions, true);

		if (!AppConst.isDeploy) {
			System.out.println("=== === update start ===");
			System.out.println("literalDataStore.updateConclusion(" + conclusionType.getSymbol() + " " + literal + ") - start");
			System.out.println("  conclusionSet=" + conclusionSet);
			System.out.println("---");
		}

		try {
			conclusionUpdater.updateTemporalConclusion(literal, conclusionType, conclusionSet, enforceNewConclusion);
			if (AppFeatureConst.printDataStoreMessage)
				System.out.println("literalDataStore.updateConclusion1: " + conclusionType.getSymbol() + " " + literal + ":"
						+ conclusionSet);
			addLiteralProved(literal, provability);
			removeUnprovedHeadLiteral(literal, provability);

			// update the enforced conclusions set if necessary
			if (enforceNewConclusion) {
				conclusionSet = getConclusionSet(literal, provability, enforcedConclusions, true);
				conclusionUpdater.updateTemporalConclusion(literal, conclusionType, conclusionSet, true);
			}
		} catch (TdlConclusionUpdaterException e) {
			throw new LiteralDataStoreException(e);
		}

		if (!AppConst.isDeploy) {
			System.out.println("---");
			System.out.println("literalDataStore.updateConclusion(" + conclusionType.getSymbol() + " " + literal + ") - after");
			System.out.println("  conclusionSet=" + conclusionSet);
			System.out.println("=== === update end ===");
		}
	}

	private void addLiteralProved(Literal literal, ProvabilityLevel provability) {
		TreeSet<Literal> provedSet = provedLiterals.get(provability);
		if (null == provedSet) {
			provedSet = new TreeSet<Literal>();
			provedLiterals.put(provability, provedSet);
		}
		provedSet.add(literal);
	}

	public boolean isLiteralProved(Literal literal, ProvabilityLevel provability) {
		TreeSet<Literal> provedSet = provedLiterals.get(provability);
		if (null == provedSet) return false;
		return provedSet.contains(literal);
	}

	public TreeMap<Temporal, ConclusionType> getAllConclusions(Literal literal, ProvabilityLevel provability) {
		return getConclusionSet(literal, provability, conclusions, false);
	}

	public Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> getAllConclusions() {
		return conclusions;
	}

	private TreeMap<Temporal, ConclusionType> getConclusionSet(Literal literal, ProvabilityLevel provability, //
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> conclusionsSet, //
			boolean createNew) {
		Map<Literal, TreeMap<Temporal, ConclusionType>> literalSet = conclusionsSet.get(provability);
		if (null == literalSet) {
			if (!createNew) return null;
			literalSet = new TreeMap<Literal, TreeMap<Temporal, ConclusionType>>(PLAIN_LITERAL_COMPARATOR);
			conclusionsSet.put(provability, literalSet);
		}
		TreeMap<Temporal, ConclusionType> conclusionSet = literalSet.get(literal);
		if (null == conclusionSet) {
			if (!createNew) return null;
			conclusionSet = new TreeMap<Temporal, ConclusionType>(TEMPORAL_START_COMPARATOR);
			literalSet.put(literal.cloneWithNoTemporal(), conclusionSet);
		}
		return conclusionSet;
	}

	private TreeMap<Temporal, ConclusionType> getConclusionsStored(Literal literal, ProvabilityLevel provability,
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> conclusionsSet) {
		TreeMap<Temporal, ConclusionType> conclusionStored = getConclusionSet(literal, provability, conclusionsSet, false);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("* * * conclusionStored=" + conclusionStored);
		if (null == conclusionStored || conclusionStored.size() == 0) {
			if (AppFeatureConst.printDataStoreMessage) System.out.println("* * no conclusions have been derived.");
			return null;
		}

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		Temporal startTemporal = literalTemporal.getStartTimeAsInstance();
		Temporal endTemporal = literalTemporal.getEndTimeAsInstance();

		TreeMap<Temporal, ConclusionType> conclusionExtracted = new TreeMap<Temporal, ConclusionType>();

		Entry<Temporal, ConclusionType> lowerEntry = conclusionStored.lowerEntry(startTemporal);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("-- conclusionStored.lowerEntry=" + lowerEntry);
		if (null != lowerEntry && lowerEntry.getKey().overlap(literalTemporal))
			conclusionExtracted.put(lowerEntry.getKey(), lowerEntry.getValue());
		SortedMap<Temporal, ConclusionType> conclusionSubset = conclusionStored.subMap(startTemporal, true, endTemporal, false);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("-- conclusionStored.conclusionSubset=" + conclusionSubset);
		if (null != conclusionSubset) conclusionExtracted.putAll(conclusionSubset);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("-- -> conclusionStored.conclusionExtracted=" + conclusionExtracted);

		return conclusionExtracted;
	}

	public ConclusionType getConclusion(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		if (!isProvable(literal, provability)) {
			if (AppFeatureConst.printDataStoreMessage) System.out.println("literal: " + literal + " not lies provable in range");
			return ProvabilityLevel.DEFINITE.equals(provability) ? ConclusionType.DEFINITE_NOT_PROVABLE
					: ConclusionType.DEFEASIBLY_NOT_PROVABLE;
		}

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		TreeMap<Temporal, ConclusionType> conclusionsExtracted = getConclusionsStored(literal, provability, conclusions);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("* * conclusionsExtracted=" + conclusionsExtracted);
		if (null == conclusionsExtracted || conclusionsExtracted.size() == 0) return null;

		TreeMap<Temporal, Literal> unprovedLiterals = getUnprovedRelatedHeadLiterals(literal, provability);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("unprovedLiterals=" + unprovedLiterals);

		Temporal precedingTemporal = null;
		TreeSet<Temporal> succeedingTemporals = new TreeSet<Temporal>();

		if (null != unprovedLiterals && unprovedLiterals.size() > 0) {
			for (Entry<Temporal, Literal> unprovedEntry : unprovedLiterals.entrySet()) {
				Temporal unprovedLiteralTemporal = unprovedEntry.getKey();

				if (unprovedLiteralTemporal.startBefore(literalTemporal)) {
					// look for unproved literal that is closest to the prescribed literal
					if (null == precedingTemporal || unprovedLiteralTemporal.startAfter(precedingTemporal)) {
						precedingTemporal = unprovedLiteralTemporal;
					}
				} else {
					succeedingTemporals.add(unprovedLiteralTemporal);
				}
			}
		}
		if (null != precedingTemporal) {
			if (AppFeatureConst.printDataStoreMessage) System.out.println("=== precedingTemporal: " + precedingTemporal);

			Entry<Temporal, ConclusionType> conc = conclusionsExtracted.lowerEntry(literalTemporal);
			if (null != conc) {
				if (AppFeatureConst.printDataStoreMessage) System.out.println("* conc=" + conc);
				Temporal concTemporal = conc.getKey();
				if (concTemporal.startAfter(precedingTemporal) && concTemporal.overlap(literalTemporal)) {
					if (AppFeatureConst.printDataStoreMessage) System.out.println("* * 1");
					ConclusionType ct = conc.getValue();// .getConclusionType();
					if (ct.isNegativeConclusion()) return ct;
				}
			}
		}
		if (succeedingTemporals.size() > 0) {
			if (AppFeatureConst.printDataStoreMessage) System.out.println("=== succeeding set: " + succeedingTemporals);
			Temporal lastLiteralTemporal = literalTemporal;
			boolean hasSameLiteralTemporalStart = false;
			long timeSkippable = literalTemporal.getStartTime();
			for (Temporal succeedingTemporal : succeedingTemporals) {
				if (AppFeatureConst.printDataStoreMessage) System.out.println("succeedingTemporal=" + succeedingTemporal);
				// check if the succeeding literal has the same start temporal with the queried literal
				if (succeedingTemporal.sameStart(literalTemporal)) hasSameLiteralTemporalStart = true;

				// determine if any negative conclusions are derived between the gap of two intervals
				// and return negative provability if found
				if (!succeedingTemporal.sameStart(lastLiteralTemporal)) {
					if (timeSkippable < succeedingTemporal.getStartTime()) {
						Temporal verifyStart = Temporal.getTemporalInstance(timeSkippable);
						Temporal verifyEnd = succeedingTemporal.getStartTimeAsInstance();

						if (AppFeatureConst.printDataStoreMessage)
							System.out.println("    verifyStart=" + verifyStart.getStartTime() + ", verifyEnd=" + verifyEnd.getStartTime()
									+ ", next gap start=" + timeSkippable);
						ConclusionType ct = extractNegativeProvabilityInConclusionSet(verifyStart, verifyEnd, conclusionsExtracted);
						if (null != ct) return ct;
					}
				}

				long succeedingEndTime = succeedingTemporal.getEndTime();
				if (succeedingEndTime > timeSkippable) timeSkippable = succeedingEndTime;
				lastLiteralTemporal = succeedingTemporal;
			}
			if (timeSkippable < literalTemporal.getEndTime()) {
				Temporal verifyStart = Temporal.getTemporalInstance(timeSkippable);
				Temporal verifyEnd = literalTemporal.getEndTimeAsInstance();

				if (AppFeatureConst.printDataStoreMessage)
					System.out.printf(AppConst.IDENTATOR + "verifyStart=%d, verifyEnd=%d, time gap start=%d\n", verifyStart.getStartTime(),
							verifyEnd.getStartTime(), timeSkippable);

				ConclusionType ct = extractNegativeProvabilityInConclusionSet(verifyStart, verifyEnd, conclusionsExtracted);
				if (null != ct) return ct;
			}
			if (hasSameLiteralTemporalStart) {
				if (AppFeatureConst.printDataStoreMessage) System.out.println("* * * hasSameLiteralTemporalStart");
				return null;
			}
		}
		if (AppFeatureConst.printDataStoreMessage) System.out.println("=== final stage");

		ConclusionType conclusionType = ProvabilityLevel.DEFINITE.equals(provability) ? ConclusionType.DEFINITE_NOT_PROVABLE
				: ConclusionType.DEFEASIBLY_NOT_PROVABLE;
		if (conclusionsExtracted.size() > 1) {
			conclusionType = null;
			for (ConclusionType conc : conclusionsExtracted.values()) {
				// ConclusionType ct = conc.getConclusionType();
				// if (ct.isNegativeConclusion()) {
				// conclusionType = ct;
				if (conc.isNegativeConclusion()) {
					conclusionType = conc;
					break;
				}
			}
		} else {
			Entry<Temporal, ConclusionType> entry = conclusionsExtracted.entrySet().iterator().next();
			Temporal provedTemporal = entry.getKey();
			if (AppFeatureConst.printDataStoreMessage)
				System.out.println("provedTemporal=" + provedTemporal + ", literalTemporal=" + literalTemporal);
			if (provedTemporal.getStartTime() <= literalTemporal.getStartTime()) {
				conclusionType = provedTemporal.getEndTime() >= literalTemporal.getEndTime() ? entry.getValue() : null;
			} else {
				if (entry.getValue().isPositiveConclusion()) conclusionType = null;
			}
		}
		return conclusionType;
	}

	private ConclusionType extractNegativeProvabilityInConclusionSet(Temporal startTemporal, Temporal endTemporl,
			TreeMap<Temporal, ConclusionType> conclusionSet) {
		Entry<Temporal, ConclusionType> lowerEntry = conclusionSet.lowerEntry(startTemporal);
		if (null != lowerEntry && lowerEntry.getKey().overlap(startTemporal)) {
			ConclusionType ct = lowerEntry.getValue();// .getConclusionType();
			if (ct.isNegativeConclusion()) return ct;
		}
		SortedMap<Temporal, ConclusionType> conclusionSubset = conclusionSet.subMap(startTemporal, true, endTemporl, false);
		if (null != conclusionSubset && conclusionSubset.size() > 0) {
			for (ConclusionType conc : conclusionSubset.values()) {
				if (conc.isNegativeConclusion()) return conc;
				// ConclusionType ct = conc.getConclusionType();
				// if (ct.isNegativeConclusion()) return ct;
			}
		}
		return null;
	}

	public TreeMap<Temporal, Literal> getImmediatePrecedingLiterals(Literal literal, ProvabilityLevel provability) {
		TreeMap<Temporal, Literal> precedingLiterals = getHeadLiteralSet(literal, provability, unprovedHeadLiterals, true, false, false);
		if (null == precedingLiterals) return null;

		long precedingTemporalStart = precedingLiterals.lastEntry().getKey().getStartTime();
		TreeMap<Temporal, Literal> literalsToProve = new TreeMap<Temporal, Literal>();
		for (Entry<Temporal, Literal> entry : precedingLiterals.descendingMap().entrySet()) {
			if (entry.getKey().getStartTime() == precedingTemporalStart) literalsToProve.put(entry.getKey(), entry.getValue());
			else break;
		}
		return literalsToProve;
	}

	public TreeMap<Temporal, Literal> getImmediateSucceedingLiterals(Literal literal, ProvabilityLevel provability) {
		TreeMap<Temporal, Literal> succeedingLiterals = getHeadLiteralSet(literal, provability, unprovedHeadLiterals, false, false, true);
		if (null == succeedingLiterals) return null;

		long nextTemporalStart = succeedingLiterals.firstEntry().getKey().getStartTime();
		TreeMap<Temporal, Literal> literalsToProve = new TreeMap<Temporal, Literal>();
		for (Entry<Temporal, Literal> entry : succeedingLiterals.entrySet()) {
			if (entry.getKey().getStartTime() == nextTemporalStart) literalsToProve.put(entry.getKey(), entry.getValue());
			else break;
		}
		return literalsToProve;
	}

	public TreeMap<Temporal, Literal> getHeadLiteralsWithSameStartTemporal(Literal literal) {
		TreeMap<Temporal, Literal> sameStartLiterals = new TreeMap<Temporal, Literal>();
		for (ProvabilityLevel p : ProvabilityLevel.values()) {
			if (ProvabilityLevel.DEFEASIBLE.compareTo(p) < 0) break;
			SortedMap<Temporal, Literal> rtnSet = getHeadLiteralsWithSameStartTemporal(literal, p);
			if (null != rtnSet) sameStartLiterals.putAll(rtnSet);
		}

		return sameStartLiterals.size() == 0 ? null : sameStartLiterals;
	}

	public TreeMap<Temporal, Literal> getHeadLiteralsWithSameStartTemporal(Literal literal, ProvabilityLevel provability) {
		return getHeadLiteralSet(literal, provability, headLiterals, false, true, false);
	}

	public boolean hasUnprovedHeadLiteralsWithSameStartTemporal(Literal literal, ProvabilityLevel provability)
			throws LiteralDataStoreException {
		TreeMap<Temporal, Literal> relatedLiterals = getUnprovedHeadLiteralsWithSameStartTemporal(literal, provability);
		return !(null == relatedLiterals || relatedLiterals.size() == 0);
	}

	public TreeMap<Temporal, Literal> getUnprovedHeadLiteralsWithSameStartTemporal(Literal literal, ProvabilityLevel provability)
			throws LiteralDataStoreException {
		return getHeadLiteralSet(literal, provability, unprovedHeadLiterals, false, true, false);
	}

	public boolean hasUnprovedRelatedHeadLiterals(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		TreeMap<Temporal, Literal> relatedLiterals = getUnprovedRelatedHeadLiterals(literal, provability);
		return !(null == relatedLiterals || relatedLiterals.size() == 0);
	}

	public TreeMap<Temporal, Literal> getUnprovedRelatedHeadLiterals(Literal literal, ProvabilityLevel provability)
			throws LiteralDataStoreException {
		return getRelatedLiteralsSet(literal, provability, unprovedHeadLiterals);
	}

	public TreeMap<Temporal,Literal>getRelatedHeadLiteralsInProvableRange(Literal literal,ProvabilityLevel provability){
		return getRelatedLiteralsSet(literal, provability, headLiterals);
	}
	
	public TreeMap<Temporal, Literal> getRelatedBodyLiteralsInProvableRange(Literal literal, ProvabilityLevel provability) {
		return getRelatedLiteralsSet(literal, provability, bodyLiterals);
	}

	public TreeMap<Temporal, Literal> getBodyLiteralsWithSameStartTemporal(Literal literal) {
		TreeMap<Temporal, Literal> sameStartLiterals = new TreeMap<Temporal, Literal>();
		for (ProvabilityLevel p : ProvabilityLevel.values()) {
			if (ProvabilityLevel.DEFEASIBLE.compareTo(p) < 0) break;
			SortedMap<Temporal, Literal> rtnSet = getBodyLiteralsWithSameStartTemporal(literal, p);
			if (null != rtnSet) sameStartLiterals.putAll(rtnSet);
		}

		return sameStartLiterals.size() == 0 ? null : sameStartLiterals;
	}

	public TreeMap<Temporal, Literal> getBodyLiteralsWithSameStartTemporal(Literal literal, ProvabilityLevel provability) {
		TreeMap<Temporal, Literal> bodyLiteralsSet = getBodyLiteralSet(literal, provability);
		if (null == bodyLiteralsSet) return null;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		SortedMap<Temporal, Literal> extractedBodyLiterals = bodyLiteralsSet.subMap(literalTemporal.getStartTimeAsInstance(), true,
				literalTemporal.getEndTimeAsInstance(), false);
		if (null == extractedBodyLiterals || extractedBodyLiterals.size() == 0) return null;

		TreeMap<Temporal, Literal> sameStartSet = new TreeMap<Temporal, Literal>();
		for (Entry<Temporal, Literal> entry : extractedBodyLiterals.entrySet()) {
			if (literalTemporal.sameStart(entry.getKey())) sameStartSet.put(entry.getKey(), entry.getValue());
		}
		return sameStartSet.size() == 0 ? null : sameStartSet;
	}

	private TreeMap<Temporal, Literal> getBodyLiteralSet(Literal literal, ProvabilityLevel provability) {
		Map<Literal, TreeMap<Temporal, Literal>> literalsSet = bodyLiterals.get(provability);
		if (null == literalsSet) return null;
		TreeMap<Temporal, Literal> temporalSet = literalsSet.get(literal);
		return null == temporalSet ? null : new TreeMap<Temporal, Literal>(temporalSet);
	}

	private TreeMap<Temporal, Literal> getRelatedLiteralsSet(Literal literal, ProvabilityLevel provability,
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> masterLiteralsSet) {
		TreeMap<Temporal, Literal> literalsSetExtracted = getHeadLiteralSet(literal, provability, masterLiteralsSet, true, true, true);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("* * literalsSetExtracted=" + literalsSetExtracted);
		if (null == literalsSetExtracted) return null;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) return literalsSetExtracted;

		TreeMap<Temporal, Literal> relatedLiterals = new TreeMap<Temporal, Literal>();
		long literalTemporalEnd = literalTemporal.getEndTime();
		for (Entry<Temporal, Literal> entry : literalsSetExtracted.entrySet()) {
			Temporal t = entry.getKey();
			if (literalTemporal.overlap(t)) {
				relatedLiterals.put(t, entry.getValue());
			} else if (t.getStartTime() > literalTemporalEnd) break;
		}

		return relatedLiterals.size() == 0 ? null : relatedLiterals;
	}

	public boolean hasUnprovedPrecedingHeadLiterals(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		return hasUnprovedPrecedingHeadLiterals(literal, provability, false);
	}

	private boolean hasUnprovedPrecedingHeadLiterals(Literal literal, ProvabilityLevel provability, boolean isStartTimeIncluded)
			throws LiteralDataStoreException {
		TreeMap<Temporal, Literal> precedingLiterals = getUnprovedHeadLiteralsSet(literal, provability, true, isStartTimeIncluded, false);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("precedingLiterals=" + precedingLiterals);
		return !(null == precedingLiterals || precedingLiterals.size() == 0);
	}

	public TreeMap<Temporal, Literal> getUnprovedPrecedingHeadLiterals(Literal literal, ProvabilityLevel provability)
			throws LiteralDataStoreException {
		return getUnprovedHeadLiteralsSet(literal, provability, true, false, false);
	}

	public boolean hasUnprovedSucceedingHeadLiterals(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		return hasUnprovedSucceedingHeadLiterals(literal, provability, false);
	}

	private boolean hasUnprovedSucceedingHeadLiterals(Literal literal, ProvabilityLevel provability, boolean isStartTimeIncluded)
			throws LiteralDataStoreException {
		TreeMap<Temporal, Literal> succeedingLiterals = getUnprovedHeadLiteralsSet(literal, provability, false, isStartTimeIncluded, true);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("succeedingLiterals=" + succeedingLiterals);
		return !(null == succeedingLiterals || succeedingLiterals.size() == 0);
	}

	public TreeMap<Temporal, Literal> getUnprovedSucceedingHeadLiterals(Literal literal, ProvabilityLevel provability)
			throws LiteralDataStoreException {
		return getUnprovedHeadLiteralsSet(literal, provability, false, false, true);
	}

	private TreeMap<Temporal, Literal> getUnprovedHeadLiteralsSet(Literal literal, ProvabilityLevel provability, //
			boolean includePrecedingSet, boolean isStartTimeIncluded, boolean includeSucceedingSet) throws LiteralDataStoreException {
		return getHeadLiteralSet(literal, provability, unprovedHeadLiterals, includePrecedingSet, isStartTimeIncluded, includeSucceedingSet);
	}

	private TreeMap<Temporal, Literal> getHeadLiteralSet(Literal literal, ProvabilityLevel provability,//
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> masterLiteralsSet) {
		Map<Literal, TreeMap<Temporal, Literal>> literalsSet = masterLiteralsSet.get(provability);
		return null == literalsSet ? null : literalsSet.get(literal);
	}

	/**
	 * Retrieve the set of literals related to the prescribed literal within the same provable interval segment.
	 * 
	 * @param literal Literal to be extracted.
	 * @param provability Provability level.
	 * @param masterLiteralsSet Literals set to be extracted from.
	 * @param includePrecedingSet Whether to include literals that precede the given literal in the provable interval.
	 * @param isStartTimeIncluded Whether to include literals that have the same start time as the given literal.
	 * @param includeSucceedingSet Whether to include literals that success the given literal in the provable interval.
	 * @return The set of literals as identified by the given arguments.
	 * @throws LiteralDataStoreException
	 */
	private TreeMap<Temporal, Literal> getHeadLiteralSet(Literal literal, ProvabilityLevel provability, //
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> masterLiteralsSet, //
			// boolean checkProvableTemporalSegment, //
			boolean includePrecedingSet, boolean includeSameStartTime, boolean includeSucceedingSet) {
		TreeMap<Temporal, Literal> unprovedLiteralsSet = getHeadLiteralSet(literal, provability, masterLiteralsSet);
		if (AppFeatureConst.printDataStoreMessage) {
			System.out.printf(AppConst.IDENTATOR + "getHeadLiteralSet(%s,%s,%b,%b,%b)\n", literal, provability, //
					includePrecedingSet, includeSameStartTime, includeSucceedingSet);
			System.out.println(AppConst.IDENTATOR + "* * unprovedLiteralsSet="
					+ (null == unprovedLiteralsSet ? "null" : unprovedLiteralsSet.values()));
		}
		if (null == unprovedLiteralsSet) return null;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		TreeSet<Temporal> temporalSegmentToProve = getTemporalSegmentsToProve(literal, provability);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("* * temporalSegmentToProve=" + temporalSegmentToProve);
		if (null == temporalSegmentToProve) return null;

		Temporal firstTemporal = temporalSegmentToProve.first();
		Temporal lastTemporal = temporalSegmentToProve.last();

		Temporal startTemporal = includePrecedingSet ? firstTemporal.getStartTimeAsInstance() : literalTemporal.getStartTimeAsInstance();
		Temporal endTemporal = includeSucceedingSet ? lastTemporal.getEndTimeAsInstance() : literalTemporal.getEndTimeAsInstance();

		if (AppFeatureConst.printDataStoreMessage) {
			System.out.printf(AppConst.IDENTATOR + AppConst.IDENTATOR + "firstTemporal=%s, lastTemporal=%s\n", firstTemporal, lastTemporal);
			System.out.printf(AppConst.IDENTATOR + AppConst.IDENTATOR + "startTemporal=%s, endTemporal=%s\n", startTemporal, endTemporal);
		}

		SortedMap<Temporal, Literal> extractedLiteralSet = unprovedLiteralsSet.subMap(startTemporal, true, endTemporal, false);
		if (AppFeatureConst.printDataStoreMessage)
			System.out.printf(AppConst.IDENTATOR + AppConst.IDENTATOR + "extractedLiteralSet=%s\n", extractedLiteralSet.values());
		if (null == extractedLiteralSet || extractedLiteralSet.size() == 0) return null;

		TreeMap<Temporal, Literal> literalsExtracted = new TreeMap<Temporal, Literal>();

		if (includePrecedingSet && includeSucceedingSet) {
			if (includeSameStartTime) {
				literalsExtracted.putAll(extractedLiteralSet);
			} else {
				// exclude literals with same start time
				for (Entry<Temporal, Literal> literalEntry : extractedLiteralSet.entrySet()) {
					Temporal lt = literalEntry.getKey();
					if (!literalTemporal.sameStart(lt)) literalsExtracted.put(lt, literalEntry.getValue());
				}
			}
		} else if (!includePrecedingSet && !includeSucceedingSet && includeSameStartTime) {
			// include only literal with same start time
			for (Entry<Temporal, Literal> literalEntry : extractedLiteralSet.entrySet()) {
				Temporal lt = literalEntry.getKey();
				if (literalTemporal.sameStart(lt)) literalsExtracted.put(lt, literalEntry.getValue());
			}
		} else { // check literals for other cases
			for (Entry<Temporal, Literal> literalEntry : extractedLiteralSet.entrySet()) {
				Temporal lt = literalEntry.getKey();
				if (includePrecedingSet) {
					if (includeSameStartTime) {
						if (lt.startOnOrBefore(literalTemporal)) literalsExtracted.put(lt, literalEntry.getValue());
					} else {
						if (lt.startBefore(literalTemporal)) literalsExtracted.put(lt, literalEntry.getValue());
					}
				}
				if (includeSucceedingSet) {
					if (includeSameStartTime) {
						if (lt.startOnOrAfter(literalTemporal)) literalsExtracted.put(lt, literalEntry.getValue());
					} else {
						if (lt.startAfter(literalTemporal)) literalsExtracted.put(lt, literalEntry.getValue());
					}
				}
			}
		}

		if (AppFeatureConst.printDataStoreMessage) System.out.println("literalsExtracted=" + literalsExtracted);

		return literalsExtracted.size() == 0 ? null : literalsExtracted;
	}

	/**
	 * Get all the temporal segments that a literal is provable.
	 * 
	 * @param literal Literal to be checked.
	 * @param provabilityLevel Provability level of the literal.
	 * @return All temporal segments that the specified literal is provable.
	 */
	private TreeSet<Temporal> getAllProvableSegments(Literal literal, ProvabilityLevel provability) {
		Map<Literal, TreeSet<Temporal>> literalsSet = temporalSegmentsToProve.get(provability);
		return null == literalsSet ? null : literalsSet.get(literal);
	}

	/**
	 * Get the temporal segment that a temporal literal is to be proved.
	 * 
	 * @param literal Literal to be checked.
	 * @param provabilityLevel Provability level of the literal.
	 * @return The temporal segment containing the prescribed literal to be proved.
	 * @throws LiteralDataStoreException Exception throw when the prescribed literal does not appear in the theory.
	 */
	private TreeSet<Temporal> getTemporalSegmentsToProve(Literal literal, ProvabilityLevel provability) {
		TreeSet<Temporal> temporalSegments = getAllProvableSegments(literal, provability);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("* allTemporalSegments=" + temporalSegments);
		if (null == temporalSegments) return null;

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		TreeSet<Temporal> temporalsExtracted = new TreeSet<Temporal>();
		for (Temporal temporal : temporalSegments) {
			if (temporal.overlap(literalTemporal)) temporalsExtracted.add(temporal);
			else if (temporal.getStartTime() >= literalTemporal.getEndTime()) break;
		}
		return temporalsExtracted.size() == 0 ? null : temporalsExtracted;
	}

	/**
	 * Determine whether a literal is provable under the specified {@link ProvabilityLevel provability level}.
	 * If the prescribed literal is a temporal literal,
	 * then provability of that literal also means whether the whole literal is provable under the specified temporal
	 * interval.
	 * 
	 * @param literal Literal to be checked.
	 * @param provability Provability level (Definite or Defeasible)
	 * @return True if the conditions above are satisfied; false otherwise.
	 * @throws LiteralDataStoreException
	 * @see ProvabilityLevel
	 */
	public boolean isProvable(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		Set<Temporal> temporalSegmentsToProve = getTemporalSegmentsToProve(literal, provability);
		if (null == temporalSegmentsToProve || temporalSegmentsToProve.size() != 1) return false;

		Temporal temporal = temporalSegmentsToProve.iterator().next();
		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;
		if (AppFeatureConst.printDataStoreMessage)
			System.out.printf("isProvable(%s,%s)=%b\n", literal, provability, temporal.contains(literalTemporal));

		return temporal.contains(literalTemporal);
	}

	// public boolean isHeadLiteral(Literal literal)
	// {
	// for (ProvabilityLevel p:ProvabilityLevel.values()){
	// if (ProvabilityLevel.DEFEASIBLE.compareTo(p)<0) break;
	// if (isHeadLiteral(literal,p))return true;
	// }
	// return false; }
	//
	// public boolean isHeadLiteral(Literal literal,ProvabilityLevel provability){
	// TreeMap<Temporal,Literal> extractedSet =getHeadLiteralSet(literal,provability,headLiterals);
	// if (null==extractedSet)return false;
	//
	// Temporal literalTemporal=literal.getTemporal();
	// if (null==literalTemporal)literalTemporal=PERSISTENT_TEMPORAL;
	// return extractedSet.containsKey(literalTemporal);
	// }

	public boolean isHeadLiteralProved(Literal literal, ProvabilityLevel provability) throws LiteralDataStoreException {
		Map<Temporal, Literal> headLiteralsInTheory = getHeadLiteralSet(literal, provability, headLiterals, false, true, false);
		if (null == headLiteralsInTheory)
			throw new LiteralDataStoreException(ErrorMessage.LITERAL_DATA_STORE_LITERAL_NOT_IN_PROVABLE_SET, new Object[] { literal,
					provability });

		Temporal literalTemporal = literal.getTemporal();
		if (null == literalTemporal) literalTemporal = PERSISTENT_TEMPORAL;

		if (!headLiteralsInTheory.containsKey(literalTemporal))
			throw new LiteralDataStoreException(ErrorMessage.LITERAL_DATA_STORE_LITERAL_NOT_IN_PROVABLE_SET, new Object[] { literal,
					provability });

		TreeMap<Temporal, Literal> unprovedLiterals = getHeadLiteralSet(literal, provability, unprovedHeadLiterals, false, true, false);
		if (null == unprovedLiterals || unprovedLiterals.size() == 0 || !unprovedLiterals.containsKey(literalTemporal)) return true;

		return false;
	}

	public void reset() {
		// clear all conclusions information
		conclusions.clear();
		enforcedConclusions.clear();
		provedLiterals.clear();

		// duplicate the set of temporal literals to unproved set
		unprovedHeadLiterals.clear();
		for (Entry<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> provabilityEntry : headLiterals.entrySet()) {
			Map<Literal, TreeMap<Temporal, Literal>> newProvabilityEntry = new TreeMap<Literal, TreeMap<Temporal, Literal>>(
					PLAIN_LITERAL_COMPARATOR);
			unprovedHeadLiterals.put(provabilityEntry.getKey(), newProvabilityEntry);
			for (Entry<Literal, TreeMap<Temporal, Literal>> literalsSet : provabilityEntry.getValue().entrySet()) {
				TreeMap<Temporal, Literal> newLiteralsSet = new TreeMap<Temporal, Literal>(literalsSet.getValue());
				newProvabilityEntry.put(literalsSet.getKey(), newLiteralsSet);
			}
		}
	}

	private String toString_literal(String label, Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> literalsSet) {
		if (literalsSet.size() > 0) {
			StringBuilder sb = new StringBuilder();
			if (null != label && !"".equals(label.trim())) {
				String line = TextUtilities.repeatStringPattern("-", label.trim().length());
				sb.append(line).append("\n").append(label.trim()).append("\n").append(line);
			}
			for (Entry<ProvabilityLevel, Map<Literal, TreeMap<Temporal, Literal>>> provabilitySetEntry : literalsSet.entrySet()) {
				sb.append(sb.length() > 0 ? "\n" : "").append(provabilitySetEntry.getKey());
				for (Entry<Literal, TreeMap<Temporal, Literal>> literalSetEntry : provabilitySetEntry.getValue().entrySet()) {
					sb.append(IDENTATOR).append(literalSetEntry.getKey()).append(" (").append(literalSetEntry.getValue().size())
							.append(")");
					for (Literal literal : literalSetEntry.getValue().values()) {
						sb.append(IDENTATOR).append(AppConst.IDENTATOR).append(literal);
					}
				}
			}
			return sb.toString();
		} else return "";
	}

	private String toString_conclusions(String label, Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> conclusionsSet) {
		if (conclusionsSet.size() > 0) {
			StringBuilder sb = new StringBuilder();
			if (null != label && !"".equals(label.trim())) {
				String line = TextUtilities.repeatStringPattern("-", label.trim().length());
				sb.append(line).append("\n").append(label.trim()).append("\n").append(line);
			}
			for (Entry<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> provabilityEntry : conclusionsSet.entrySet()) {
				sb.append(sb.length() > 0 ? "\n" : "").append(provabilityEntry.getKey());
				for (Entry<Literal, TreeMap<Temporal, ConclusionType>> literalEntry : provabilityEntry.getValue().entrySet()) {
					sb.append(IDENTATOR).append(literalEntry.getKey());
					for (Entry<Temporal, ConclusionType> conclusionEntry : literalEntry.getValue().entrySet()) {
						sb.append(IDENTATOR).append(AppConst.IDENTATOR).append(conclusionEntry.getKey()).append(":")
								.append(conclusionEntry.getValue().getSymbol());
					}
				}
			}
			return sb.toString();
		} else return "";
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (headLiterals.size() > 0) sb.append(toString_literal("Head literals", headLiterals));
		if (bodyLiterals.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "");
			sb.append(toString_literal("Body literals", bodyLiterals));
		}
		if (unprovedHeadLiterals.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "");
			sb.append(toString_literal("Unproved head literals", unprovedHeadLiterals));
		}
		if (temporalSegmentsToProve.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "").append(
					"--------------------------\nProvable temporal segments\n--------------------------");
			for (Entry<ProvabilityLevel, Map<Literal, TreeSet<Temporal>>> entry : temporalSegmentsToProve.entrySet()) {
				sb.append("\n").append(entry.getKey());
				for (Entry<Literal, TreeSet<Temporal>> entry2 : entry.getValue().entrySet()) {
					String str = entry2.getValue().toString();
					sb.append(IDENTATOR).append(entry2.getKey() + ": " + str.substring(1, str.length() - 1));
				}
			}
		}
		if (provedLiterals.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "").append("-----------------\nProvable literals\n-----------------");
			for (Entry<ProvabilityLevel, TreeSet<Literal>> entry : provedLiterals.entrySet()) {
				sb.append("\n").append(entry.getKey());
				for (Literal l : entry.getValue()) {
					sb.append(IDENTATOR).append(l);
				}
			}
		}
		if (conclusions.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "");
			sb.append(toString_conclusions("Conclusions", conclusions));
		}
		if (enforcedConclusions.size() > 0) {
			sb.append(sb.length() > 0 ? "\n" : "");
			sb.append(toString_conclusions("Enforced conclusions", enforcedConclusions));
		}
		return sb.toString();
	}
}
