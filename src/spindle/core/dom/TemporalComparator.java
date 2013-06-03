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

public class TemporalComparator implements Comparator<Temporal> {

	@Override
	public int compare(Temporal t1, Temporal t2) {
		if (null == t1) {
			if (null != t2) {
				if (Long.MIN_VALUE == t2.startTime) return Long.MAX_VALUE == t2.endTime ? 0 : Integer.MAX_VALUE;
				return Integer.MIN_VALUE;
			}
		} else {
			if (t1 == t2) return 0;
			if (null == t2) {
				if (Long.MIN_VALUE == t1.startTime) return Long.MAX_VALUE == t1.endTime ? 0 : Integer.MIN_VALUE;
				return Integer.MAX_VALUE;
			} else {
				if (t1.startTime != t2.startTime) return t1.startTime < t2.startTime ? Integer.MIN_VALUE : Integer.MAX_VALUE;
				if (t1.endTime != t2.endTime) return t1.endTime < t2.endTime ? Integer.MIN_VALUE : Integer.MAX_VALUE;
			}
		}
		return 0;
	}
}
