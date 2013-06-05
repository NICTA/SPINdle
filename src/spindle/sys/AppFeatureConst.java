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
package spindle.sys;

public interface AppFeatureConst {
	boolean isVerifyConflictRules = true;

	boolean isPrintConclusionByType = true;
	boolean isPrintExtendedRuleInfo = false;

	boolean isCloneRuleBodyLiterals = false;

	boolean isVerifyConclusionsAfterInference = true;

	boolean isUpdateTemporalSegmentsToProve = true;
	boolean isIntervalBasedTemporal = true;
	boolean checkPrecedingLiteralsWithUnprovedLiteralsSet = true;

	boolean checkConflictLiteralsWithTemporalStartOnly = false;
	boolean throwLiteralDataStoreRemoveUnfoundException = false;

	// boolean isCheckEndTemporalInLiteralSegment = true;

	boolean printDataStoreMessage = false;
	boolean printConflictLiteralsInfo = false;

	// boolean checkSameLiteralWhenJoinTemporal = false;

	boolean superiorityWithSameStartTemporalOnly = false;

	boolean useLiteralDataStoreConclusions = false;

	boolean simpleConflictTemporalLiteralsGeneration = false; // problems with conflict temporal literals may appear
																// if simple conflict temporal literals generation is
																// used in the reasoning engine!!

}
