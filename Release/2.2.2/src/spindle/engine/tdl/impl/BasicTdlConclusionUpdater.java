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
package spindle.engine.tdl.impl;

import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Temporal;
import spindle.core.dom.TemporalException;
import spindle.engine.tdl.TdlConclusionUpdaterException;
import spindle.engine.tdl.TdlConclusionUpdater;
import spindle.sys.AppFeatureConst;
import spindle.sys.message.ErrorMessage;

public class BasicTdlConclusionUpdater extends TdlConclusionUpdater {

	@Override
	protected void doUpdateConclusion(Literal literal, ConclusionType conclusionType, NavigableMap<Temporal, ConclusionType> conclusionSet)
			throws TdlConclusionUpdaterException, TemporalException {
//		if (AppFeatureConst.printDataStoreMessage)
			System.out.printf("  BasicTdlConclusionUpdater.doUpdateTemporalConclusion: %s,%s\n", conclusionType.getSymbol(), literal);
			System.out.printf("                           .conclusionSet: %s\n", conclusionSet);
		Temporal temporal = literal.getTemporal();
		if (null == temporal) temporal = new Temporal();

		long temporalStart = temporal.getStartTime();
		boolean checkSucceedingEntry = true;

		// update preceding entry
		Entry<Temporal, ConclusionType> precedingEntry = conclusionSet.floorEntry(temporal.getStartTimeAsInstance());
		if (AppFeatureConst.printDataStoreMessage) System.out.println("  precedingEntry1=" + precedingEntry);

		logMessage(Level.FINER, 2, "precedingEntry", (null == precedingEntry ? "null" : precedingEntry));
		if (AppFeatureConst.printDataStoreMessage) {
			if (null != precedingEntry) System.out.println("  overlapOrMeet=" + temporal.overlapOrMeet(precedingEntry.getKey()));
		}

		if (null == precedingEntry || !temporal.overlapOrMeet(precedingEntry.getKey())) {
			conclusionSet.put(temporal, conclusionType);
		} else {
			Temporal precedingTemporal = precedingEntry.getKey();
			ConclusionType precedingConclusionType = precedingEntry.getValue();
			if (conclusionType.equals(precedingConclusionType)) {
				// if (conclusionType.equals(precedingConclusion.getConclusionType())) {
				// preceding conclusion and new conclusion are of the same type
				if (precedingTemporal.contains(temporal)) {
					checkSucceedingEntry = false;
				} else {
					Temporal joinedTemporal = temporal.join(precedingTemporal);
					conclusionSet.remove(precedingTemporal);
					temporal = joinedTemporal;
					conclusionSet.put(temporal, conclusionType);
				}
			} else {
				// preceding conclusion and new conclusion are of different types
				if (precedingTemporal.meet(temporal)) {
					// do nothing as the two conclusions just meeting each others
				} else {
					if (precedingTemporal.getStartTime() == temporalStart)
						throw new TdlConclusionUpdaterException(ErrorMessage.CONCLUSION_CONFLICTING_CONCLUIONS_PROVED_WITH_SAME_START_TIME,
								new Object[] {literal, precedingTemporal,precedingConclusionType.getSymbol(), temporal,conclusionType.getSymbol()});
					conclusionSet.remove(precedingTemporal);
					Temporal nt = new Temporal(precedingTemporal.getStartTime(), temporalStart);
					conclusionSet.put(nt, precedingConclusionType);
					if (temporal.endBefore(precedingTemporal)) {
						Temporal residualTemporal = new Temporal(temporal.getEndTime(), precedingTemporal.getEndTime());
						conclusionSet.put(residualTemporal, precedingConclusionType);
					}
				}
				conclusionSet.put(temporal, conclusionType);
			}
		}

		if (AppFeatureConst.printDataStoreMessage) {
			System.out.println("    temporal=" + temporal);
			System.out.println("    conclusionSet=" + conclusionSet);
		}

		if (!checkSucceedingEntry) return;

		NavigableMap<Temporal, ConclusionType> succeedingEntries = conclusionSet.tailMap(temporal, false);
		logMessage(Level.FINER, 2, "succeedingEntry", new Object[] { (null == succeedingEntries ? "null" : succeedingEntries) });
		if (!AppFeatureConst.printDataStoreMessage) System.out.println("    succeedingEntires=" + succeedingEntries);
		if (null == succeedingEntries || succeedingEntries.size() == 0) return;

		for (Entry<Temporal, ConclusionType> succeedingEntry : (new TreeMap<Temporal, ConclusionType>(succeedingEntries)).entrySet()) {
			Temporal succeedingTemporal = succeedingEntry.getKey();
			ConclusionType succeedingConclusionType = succeedingEntry.getValue();

			if (!temporal.overlapOrMeet(succeedingTemporal)) return;

			if (conclusionType.equals(succeedingConclusionType)) {
				if (temporal.contains(succeedingTemporal)) {
					conclusionSet.remove(succeedingTemporal);
				} else {
					Temporal joinedTemporal = temporal.join(succeedingTemporal);
					conclusionSet.remove(temporal);
					conclusionSet.remove(succeedingTemporal);
					conclusionSet.put(joinedTemporal, conclusionType);
					return;
				}
			} else {
				// return if the two conclusion interval only meet each others
				if (temporal.meet(succeedingTemporal)) return;

				conclusionSet.remove(temporal);
				Temporal residualTemporal = new Temporal(temporal.getStartTime(), succeedingTemporal.getStartTime());
				conclusionSet.put(residualTemporal, conclusionType);
				return;
			}

		}

		// Temporal succeedingTemporal = succeedingEntry.getKey();
		// Conclusion succeedingConclusion = succeedingEntry.getValue();
		// if (!(temporal.isOverlap(succeedingTemporal) || temporal.meet(succeedingTemporal))) return;
		//
		// if (conclusionType.equals(succeedingConclusion.getConclusionType())) {
		// Temporal joinedTemporal = temporal.join(succeedingTemporal);
		// conclusionSet.remove(succeedingTemporal);
		// if (temporal.equals(joinedTemporal)) {
		// // do nothing as the succeeding conclusion already included in the new conclusion
		// } else {
		// conclusionSet.remove(temporal);
		// conclusionSet.put(joinedTemporal, generateNewConclusion(conclusion, joinedTemporal));
		// }
		// } else {
		// if (temporal.meet(succeedingTemporal)) {
		// // do nothing as the two conclusion just meeting each others
		// } else {
		// conclusionSet.remove(temporal);
		// Temporal nt = new Temporal(temporal.getStartTime(), succeedingTemporal.getStartTime());
		// conclusionSet.put(nt, generateNewConclusion(conclusion, nt));
		// }
		// }
		// }
	}
}
