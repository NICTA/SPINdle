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
package spindle.engine.tdl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralComparator;
import spindle.core.dom.Temporal;
import spindle.engine.ReasoningEngineException;

public class TdlReasoningUtilities {
	private static final Comparator<Literal> PLAIN_LITERAL_COMPARATOR = new LiteralComparator(false);
	private static final Temporal PERSISTENT_TEMPORAL = new Temporal();

	public static Map<Literal, Set<Literal>> consolidateLiteralsTemporals(Collection<Literal> literals) throws ReasoningEngineException {
		Map<Literal, Set<Temporal>> temporals = joinLiteralsTemporals(literals);
		Map<Literal, Set<Literal>> joinedLiterals = new TreeMap<Literal, Set<Literal>>(PLAIN_LITERAL_COMPARATOR);

		for (Entry<Literal, Set<Temporal>> entry : temporals.entrySet()) {
			Literal literal = entry.getKey();
			Set<Temporal> literalTemporals = entry.getValue();
			Set<Literal> literalsSet = new TreeSet<Literal>();

			for (Temporal temporal : literalTemporals) {
				literalsSet.add(temporal.hasTemporalInfo() ? literal : getNewLiteral(literal, temporal));
			}
			joinedLiterals.put(literal, literalsSet);
		}
		return joinedLiterals;
	}

	public static Map<Literal, Set<Temporal>> joinLiteralsTemporals(Collection<Literal> literals) {
		Map<Literal, Set<Temporal>> temporals = new TreeMap<Literal, Set<Temporal>>(PLAIN_LITERAL_COMPARATOR);

		for (Literal literal : literals) {
			Set<Temporal> literalTemporals = temporals.get(literal);
			if (null == literalTemporals) {
				literalTemporals = new TreeSet<Temporal>();
				temporals.put(literal.cloneWithNoTemporal(), literalTemporals);
			}
			Temporal t = literal.getTemporal();
			literalTemporals.add(null == t ? PERSISTENT_TEMPORAL : t);
		}
//		try {
			for (Entry<Literal, Set<Temporal>> entry : temporals.entrySet()) {				
				Set<Temporal> literalTemporals = entry.getValue();
				Temporal.consolidateTemporalSegments(literalTemporals);
//				if (literalTemporals.contains(PERSISTENT_TEMPORAL)) {
//					literalTemporals.clear();
//					literalTemporals.add(PERSISTENT_TEMPORAL);
//				} else {
//					Temporal newTemporal = null;
//					for (Temporal temporal : new TreeSet<Temporal>(literalTemporals)) {
//						if (null == newTemporal) {
//							literalTemporals.clear();
//							newTemporal = temporal;
//						} else {
//							if (newTemporal.overlapOrMeet(temporal)) {
//								newTemporal = newTemporal.join(temporal);
//							} else {
//								literalTemporals.add(newTemporal);
//								newTemporal = temporal;
//							}
//						}
//					}
//				}
			}
//		} catch (TemporalException e) {
//		}
		return temporals;
	}

	
	
	public static void generateEnforcedConclusionsSet(Conclusion enforcedConclusion, Set<Conclusion> defeatedConclusions, //
			boolean requireSameStartTime, //
			Set<Conclusion> consolidatedConclusions, Set<Conclusion> unenforcedConclusions) throws ReasoningEngineException {
		consolidatedConclusions.add(enforcedConclusion);
		ConclusionType enforcedConclusionType = enforcedConclusion.getConclusionType();

		// do nothing if the conclusion proved is negative
		if (enforcedConclusionType.isNegativeConclusion()) {
			consolidatedConclusions.addAll(defeatedConclusions);
			return;
		}

		Temporal enforcedTemporal = enforcedConclusion.getTemporal();

		// if the enforced conclusion has no temporal information
		// change all defeated conclusions to negatively provability and remove their temporals.
		if (null == enforcedTemporal) {
			if (requireSameStartTime) {
				for (Conclusion defeatedConclusion : defeatedConclusions) {
					Temporal defeatedTemporal = defeatedConclusion.getTemporal();
					if (null == defeatedTemporal || Long.MIN_VALUE != defeatedTemporal.getStartTime())
						throw new ReasoningEngineException(TdlReasoningUtilities.class, "start time are not the same");
					ConclusionType conflictDefeatedConclusionType = getNegativeConclusionType(defeatedConclusion.getConclusionType());
					consolidatedConclusions.add(new Conclusion(conflictDefeatedConclusionType, defeatedConclusion.getLiteral()
							.cloneWithNoTemporal()));
				}
			} else {
				for (Conclusion defeatedConclusion : defeatedConclusions) {
					ConclusionType conflictDefeatedConclusionType = getNegativeConclusionType(defeatedConclusion.getConclusionType());
					consolidatedConclusions.add(new Conclusion(conflictDefeatedConclusionType, defeatedConclusion.getLiteral()
							.cloneWithNoTemporal()));
				}
			}
			return;
		}

		for (Conclusion defeatedConclusion : defeatedConclusions) {
			Literal defeatedLiteral = defeatedConclusion.getLiteral();

			ConclusionType ctDefeated = defeatedConclusion.getConclusionType();
			ConclusionType ctNegative = getNegativeConclusionType(ctDefeated);

			Temporal defeatedTemporal = defeatedConclusion.getTemporal();
			if (null == defeatedTemporal) defeatedTemporal = PERSISTENT_TEMPORAL;

			if (requireSameStartTime) {
				if (!enforcedTemporal.sameStart(defeatedTemporal))
					throw new ReasoningEngineException(TdlReasoningUtilities.class, "start time are not the same");
			}

			if (enforcedTemporal.equals(defeatedTemporal)) {
				if (ctDefeated.isNegativeConclusion()) {
					consolidatedConclusions.add(defeatedConclusion);
				} else {
					consolidatedConclusions.add(new Conclusion(ctNegative, defeatedLiteral));
				}
			} else {
				consolidatedConclusions.add(getNewConclusion(ctNegative, defeatedLiteral, enforcedTemporal));

				Temporal precedingTemporal = defeatedTemporal.startBefore(enforcedTemporal) ? new Temporal(defeatedTemporal.getStartTime(),
						enforcedTemporal.getStartTime()) : null;
				Temporal succeedingTemporal = defeatedTemporal.endAfter(enforcedTemporal) ? new Temporal(enforcedTemporal.getEndTime(),
						defeatedTemporal.getEndTime()) : null;

				if (null != precedingTemporal) {
					unenforcedConclusions.add(getNewConclusion(ctDefeated, defeatedLiteral, precedingTemporal));
				}
				if (null != succeedingTemporal) {
					unenforcedConclusions.add(getNewConclusion(ctDefeated, defeatedLiteral, succeedingTemporal));
				}
			}
		}
	}

	private static ConclusionType getNegativeConclusionType(ConclusionType conclusionType) {
		if (conclusionType.isNegativeConclusion()) return conclusionType;
		switch (conclusionType) {
		case DEFINITE_PROVABLE:
			return ConclusionType.DEFINITE_NOT_PROVABLE;
		case DEFEASIBLY_PROVABLE:
			return ConclusionType.DEFEASIBLY_NOT_PROVABLE;
		default:
			return null;
		}
	}

	private static Literal getNewLiteral(Literal literal, Temporal temporal) {
		Literal newLiteral = literal.clone();
		newLiteral.setTemporal(temporal.clone());
		return newLiteral;
	}

	private static Conclusion getNewConclusion(ConclusionType conclusionType, Literal literal, Temporal temporal) {
		return new Conclusion(conclusionType, getNewLiteral(literal, temporal));
	}
}
