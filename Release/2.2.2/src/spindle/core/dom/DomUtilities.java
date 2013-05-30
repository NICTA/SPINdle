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
package spindle.core.dom;

import java.util.Comparator;

import spindle.sys.Conf;

/**
 * Utilities for generating the document object of defeasible theory.
 * It is recommended that users should use this utilities class to create a new DOM object than creating the object
 * using the "new" operator since different DOM objects will be created under different reasoning engines versions.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.30
 */
public class DomUtilities {
	private static final Temporal PERSISTENT_TEMPORAL = new Temporal();
	private static final Temporal MINIMUM_TEMPORAL_INSTANCE = new Temporal(Long.MIN_VALUE, Long.MIN_VALUE);

	private static final Comparator<? super Literal> LITERAL_COMPARATOR = new LiteralComparator(true);
	private static final Comparator<? super Literal> PLAIN_LITERAL_COMPARATOR = new LiteralComparator(false);
	private static final Comparator<? super Literal> LITERAL_TEMPORAL_START_COMPARATOR = new LiteralComparator(true,
			new TemporalStartComparator());
	private static final Comparator<? super Temporal> TEMPORAL_COMPARATOR = new TemporalComparator();
	private static final Comparator<? super Temporal> TEMPORAL_START_COMPARATOR = new TemporalStartComparator();

	private static AppConstants appConstants = AppConstants.getInstance(null);

	public static LiteralVariable getLiteralVariable(final String name, boolean isNegation) {
		return getLiteralVariable(name, isNegation, (Mode) null);
	}

	public static LiteralVariable getLiteralVariable(final String name, final boolean isNegation, //
			final Mode mode) {
		LiteralVariable literalVariable = new LiteralVariable(name, isNegation, mode);
		return validateLiteralVariable(literalVariable);
	}

	public static LiteralVariable getLiteralVariable(final String name, final boolean isNegation, final String[] predicates) {
		LiteralVariable lv = getLiteralVariable(name, isNegation, (Mode) null);
		lv.setPredicates(predicates);
		return validateLiteralVariable(lv);
	}

	public static LiteralVariable getLiteralVariable(final String name, final boolean isNegation, final Literal[] literalPredicates) {
		LiteralVariable lv = getLiteralVariable(name, isNegation, (Mode) null);
		lv.setLiteralPredicates(literalPredicates);
		return lv;
	}

	public static LiteralVariable getLiteralVariable(final Literal literal) {
		LiteralVariable literalVariable = new LiteralVariable(literal);
		return validateLiteralVariable(literalVariable);
	}

	private static LiteralVariable validateLiteralVariable(LiteralVariable literalVariable) {
		try {
			return (appConstants.isAppConstant(literalVariable)) ? appConstants.getAppConstantAsLiteralVariable(literalVariable)
					: literalVariable;
		} catch (Exception e) {
			return literalVariable;
		}
	}

	public static LiteralVariable getLiteralVariableWithNoArgument(final String name, final boolean isNegation) {
		return new LiteralVariable(name, isNegation);
	}

	public static Literal getLiteral(final String name, final boolean isNegation) {
		return getLiteral(name, isNegation, null, null, null, false);
	}

	public static Literal getLiteral(final String name, final boolean isNegation,//
			final String modeName, final boolean isModeNegation) {
		return getLiteral(name, isNegation, modeName, isModeNegation, null, null, false);
	}

	public static Literal getLiteral(final String name, final boolean isNegation,//
			final Temporal temporal) {
		return getLiteral(name, isNegation, null, temporal, null, false);
		// final long startTime, final long endTime) {
		// return getLiteral(name, isNegation, null, new Temporal(startTime, endTime), null, false);
	}

	public static Literal getLiteral(final String name, final boolean isNegation,//
			final String modeName, final boolean isModeNegation, //
			// final long startTime, final long endTime,
			final Temporal temporal) {
		return getLiteral(name, isNegation, null == modeName ? null : new Mode(modeName, isModeNegation), //
				temporal, null, false);
		// new Temporal(startTime, endTime), null, false);
	}

	public static Literal getLiteral(final String name, final boolean isNegation,//
			final String modeName, final boolean isModeNegation, //
			final Temporal temporal, //
			final String[] predicates) {
		Mode mode = null == modeName ? null : new Mode(modeName, isModeNegation);
		return getLiteral(name, isNegation, mode, temporal, predicates, false);
	}

	public static Literal getLiteral(final String name, final boolean isNegation, //
			final String modeName, final boolean isModeNegation,//
			final Temporal temporal, final String[] predicates, //
			final boolean isPlaceHolder) {
		Mode mode = null == modeName ? null : new Mode(modeName, isModeNegation);
		return getLiteral(name, isNegation, mode, temporal, predicates, isPlaceHolder);
	}

	public static Literal getLiteral(final String name, final boolean isNegation, //
			final Mode mode,//
			final Temporal temporal, final String[] predicates, //
			final boolean isPlaceHolder) {
		return new Literal(name, isNegation, mode, temporal, predicates, isPlaceHolder);
	}

	public static Literal getLiteral(Literal literal) {
		return new Literal(literal);
	}

	public static Rule getRule(String ruleLabel, RuleType ruleType) {
		switch (Conf.getReasonerVersion()) {
		case 1:
			return new spindle.core.dom.Rule(ruleLabel, ruleType);
		default:
			return new spindle.core.dom.RuleExt(ruleLabel, ruleType);
		}
	}

	public static Temporal getTemporalInstance(long t) {
		return Temporal.getTemporalInstance(t);
	}

	public static Temporal getPersistentTemporal() {
		return PERSISTENT_TEMPORAL;
	}

	public static Temporal getMinimumTemporalInstance() {
		return MINIMUM_TEMPORAL_INSTANCE;
	}

	public static Comparator<? super Literal> getLiteralComparator() {
		return LITERAL_COMPARATOR;
	}

	public static Comparator<? super Literal> getPlainLiteralComparator() {
		return PLAIN_LITERAL_COMPARATOR;
	}

	public static Comparator<? super Literal> getLiteralTemporalStartComparator() {
		return LITERAL_TEMPORAL_START_COMPARATOR;
	}

	public static Comparator<? super Temporal> getTemporalComparator() {
		return TEMPORAL_COMPARATOR;
	}

	public static Comparator<? super Temporal> getTemporalStartComparator() {
		return TEMPORAL_START_COMPARATOR;
	}
}
