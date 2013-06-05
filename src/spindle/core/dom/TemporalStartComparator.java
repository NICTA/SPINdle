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

public class TemporalStartComparator implements Comparator<Temporal> {

	@Override
	public int compare(Temporal t1, Temporal t2) {
		if (null == t1) {
			if (null == t2 || Long.MIN_VALUE == t2.startTime) return 0;
			return Integer.MIN_VALUE;
		} else {
			if (t1 == t2) return 0;
			if (null == t2) {
				if (Long.MIN_VALUE == t1.startTime) return 0;
				return Integer.MAX_VALUE;
			} else {
				long t1Start = t1.startTime;
				long t2Start = t2.startTime;
				return t1Start == t2Start ? 0 : (t1Start > t2Start ? Integer.MAX_VALUE : Integer.MIN_VALUE);
			}
		}
	}
}
