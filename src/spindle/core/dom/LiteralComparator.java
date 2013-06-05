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
package spindle.core.dom;

import java.util.Comparator;

/**
 * Literal comparator
 * that provides the freedom to select whether to compare the literals with or without using the temporal information.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.2.1
 * @version Last modified 2012.09.06
 * @see Literal
 * @see spindle.engine.tdl.LiteralDataStore
 */
public class LiteralComparator implements Comparator<Literal> {

	private boolean isCheckTemporal = true;
	private Comparator<Temporal> DEFAULT_TEMPORAL_COMPARTOR = new TemporalComparator();
	private Comparator<Temporal> temporalComparator = null;

	public LiteralComparator(boolean isCheckTemporal) {
		this(isCheckTemporal, null);
		// this.isCheckTemporal = isCheckTemporal;
	}

	public LiteralComparator(boolean isCheckTemporal, Comparator<Temporal> temporalComparator) {
		this.isCheckTemporal = isCheckTemporal;
		setTemporalComparator(temporalComparator);
	}

	public void setTemporalComparator(Comparator<Temporal> temporalComparator) {
		this.temporalComparator = temporalComparator;
	}

	// public void setCompareStartTimeOnly(boolean isCompareStartTimeOnly) {
	// temporalStartComparator = isCompareStartTimeOnly ? new TemporalStartComparator() : null;
	// }

	@Override
	public int compare(Literal l1, Literal l2) {
		if (l1 == l2) return 0;

		int c = l1.name.compareTo(l2.name);
		if (c != 0) return c;

		if (l1.isNegation != l2.isNegation) return l1.isNegation ? Integer.MAX_VALUE : Integer.MIN_VALUE;

		// same name, negation sign
		// check mode and temporal
		c = l1.mode.compareTo(l2.mode);
		if (c != 0) return c;

		if (isCheckTemporal) {
			c = null == temporalComparator ? DEFAULT_TEMPORAL_COMPARTOR.compare(l1.temporal, l2.temporal) : temporalComparator.compare(
					l1.temporal, l2.temporal);
			
		//	System.out.println("literalComparator.checkTemporal:("+l1+","+l2+")="+c);
			if (c != 0) return c;
			// if (null == l1.temporal) {
			// if (null != l2.temporal) {
			// Temporal t2=l2.temporal;
			// if (Long.MIN_VALUE == t2.startTime) return Long.MAX_VALUE==t2.endTime?0 :Integer.MAX_VALUE;
			// return Integer.MIN_VALUE;
			// }
			// } else {
			// Temporal t1=l1.temporal;
			// if (null == l2.temporal) {
			// if (Long.MIN_VALUE == t1.startTime) return Long.MAX_VALUE==t1.endTime?0: Integer.MIN_VALUE;
			// return Integer.MAX_VALUE;
			// } else {
			// Temporal t2 = l2.temporal;
			// c = null == temporalComparator ? t1.compareTo(t2) : temporalComparator.compare(t1, t2);
			// if (c != 0) return c;
			// }
			// }
		}

		c = l1.predicates.length - l2.predicates.length;
		if (c != 0) return c;
		for (int i = 0; i < l1.predicates.length; i++) {
			if (!l1.isPredicateGrounded(i) && !l2.isPredicateGrounded(i)) {
			} else if (l1.isPredicateGrounded(i) && l2.isPredicateGrounded(i)) {
				c = l1.predicates[i].compareTo(l2.predicates[i]);
				if (c != 0) return c;
			} else {
				return l1.isPredicateGrounded(i) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}
		}
		return 0;
	}
}
