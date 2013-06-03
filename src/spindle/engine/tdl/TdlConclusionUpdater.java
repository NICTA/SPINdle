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

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;


import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Temporal;
import spindle.core.dom.TemporalException;
import spindle.sys.AppConst;
import spindle.sys.AppFeatureConst;
import spindle.sys.AppModuleBase;
import spindle.sys.message.ErrorMessage;

public abstract class TdlConclusionUpdater extends AppModuleBase {

	public void updateTemporalConclusion(Literal literal, ConclusionType conclusionType,
			NavigableMap<Temporal, ConclusionType> conclusionSet, boolean enforceNewConclusion) throws TdlConclusionUpdaterException {
		if (null == literal) throw new TdlConclusionUpdaterException(ErrorMessage.LITERAL_NULL_LITERAL);
		System.out.println("  updateTemporalConclusion.conclusionSet="+conclusionSet);
		// System.out.println("  enforceNewConclusion="+enforceNewConclusion);
		logMessage(Level.FINER, 1, "updateConclusion, literal=", literal, "conclusionType=", conclusionType, "conclusionSet="
				+ conclusionSet);

		Temporal temporal = literal.getTemporal();
		if (null == temporal) temporal = new Temporal();
		if (conclusionSet.size() == 0) {
			conclusionSet.put(temporal, conclusionType);
			return;
		}

		try {
			if (enforceNewConclusion) doUpdateConclusionWithEnforcement(literal, conclusionType, conclusionSet);
			else doUpdateConclusion(literal, conclusionType, conclusionSet);
		} catch (TdlConclusionUpdaterException e) {
			throw e;
		} catch (Exception e) {
			throw new TdlConclusionUpdaterException(e);
		}

	}

	private void doUpdateConclusionWithEnforcement(Literal literal, ConclusionType conclusionType,
			NavigableMap<Temporal, ConclusionType> conclusionSet) throws TdlConclusionUpdaterException, TemporalException {
		if (!AppConst.isDeploy) {
			System.out.println("  doUpdateConclusionWithEnforcement(" + literal + "," + conclusionType + ")");
			System.out.println("    conclusionSet=" + conclusionSet);
		}

		Temporal temporal = literal.getTemporal();
		if (null == temporal) temporal = new Temporal();
		long temporalStart = temporal.getStartTime();

		Entry<Temporal, ConclusionType> precedingEntry = conclusionSet.floorEntry(temporal);
		if (AppFeatureConst.printDataStoreMessage) System.out.println("  precedingEntry1=" + precedingEntry);

		boolean isCheckSucceedingEntry = true;
		if (AppFeatureConst.printDataStoreMessage) {
			if (null != precedingEntry) System.out.println("  overlapOrMeet=" + temporal.overlapOrMeet(precedingEntry.getKey()));
		}
		
		if (null == precedingEntry || !temporal.overlapOrMeet(precedingEntry.getKey())) {
			conclusionSet.put(temporal, conclusionType);
		} else {
			Temporal precedingTemporal = precedingEntry.getKey();
			ConclusionType precedingConclusionType = precedingEntry.getValue();
			if (conclusionType.equals(precedingConclusionType)) {
				// preceding conclusion and new conclusion are of the same type
				if (precedingTemporal.contains(temporal)) {
					isCheckSucceedingEntry = false;
				} else {
					Temporal joinedTemporal = temporal.join(precedingTemporal);
					conclusionSet.remove(precedingTemporal);
					temporal = joinedTemporal;
					conclusionSet.put(temporal, conclusionType);
				}
			} else {
				// preceding conclusion and new conclusion are of the different types
				if (precedingTemporal.meet(temporal)) {
					// do nothing as the two conclusion just meeting each others
				} else {
					conclusionSet.remove(precedingTemporal);
					Temporal nt = new Temporal(precedingTemporal.getStartTime(), temporalStart);
					conclusionSet.put(nt, precedingConclusionType);
					if (precedingTemporal.endAfter(temporal)) {
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

		if (!isCheckSucceedingEntry) return;

		NavigableMap<Temporal, ConclusionType> succeedingEntries = conclusionSet.tailMap(temporal, false);
		logMessage(Level.FINER, 2, "succeedingEntry", new Object[] { (null == succeedingEntries ? "null" : succeedingEntries) });
		if (!AppFeatureConst.printDataStoreMessage) System.out.println("    conclusionSetEntry=" + succeedingEntries);
		if (null == succeedingEntries || succeedingEntries.size() == 0) return;

		for (Entry<Temporal, ConclusionType> succeedingEntry : (new TreeMap<Temporal, ConclusionType>(succeedingEntries)).entrySet()) {
			Temporal temporalToVerify = succeedingEntry.getKey();
			ConclusionType succeedingConclusionType = succeedingEntry.getValue();
			if (!temporal.overlapOrMeet(temporalToVerify)) return;

			if (temporal.contains(temporalToVerify)) {
				conclusionSet.remove(temporalToVerify);
			} else {
				if (conclusionType.equals(succeedingConclusionType)) {
					Temporal joinedTemporal = temporal.join(temporalToVerify);
					conclusionSet.remove(temporal);
					conclusionSet.remove(temporalToVerify);
					conclusionSet.put(joinedTemporal, succeedingConclusionType);
					return;
				} else {
					// return if the two conclusions interval only meet each others
					if (temporal.meet(temporalToVerify)) return;

					conclusionSet.remove(temporalToVerify);
					if (temporal.endBefore(temporalToVerify)) {
						Temporal residualTemporal = new Temporal(temporal.getEndTime(), temporalToVerify.getEndTime());
						conclusionSet.put(residualTemporal, succeedingConclusionType);// succeedingConclusionEntry.getValue());
					}
					return;
				}
			}
		}
	}

	protected abstract void doUpdateConclusion(Literal literal, ConclusionType conclusionType,
			NavigableMap<Temporal, ConclusionType> conclusionSet) throws TdlConclusionUpdaterException, TemporalException;

}