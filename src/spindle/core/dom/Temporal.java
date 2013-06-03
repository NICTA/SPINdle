/**
 * SPINdle (version 2.2.0)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * DOM for representing the temporal information in a literal/rule.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @version Last modified 2012.07.11
 */
public class Temporal implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private static final char TEMPORAL_START = DomConst.Literal.TIMESTAMP_START;
	private static final char TEMPORAL_END = DomConst.Literal.TIMESTAMP_END;
	private static final char TEMPORAL_SEPARATOR = DomConst.Literal.LITERAL_SEPARATOR;

	protected long startTime, endTime;

	public Temporal() {
		this(Long.MIN_VALUE, Long.MAX_VALUE);
	}

	public Temporal(long startTime) {
		this(startTime, Long.MAX_VALUE);
	}

	public Temporal(long startTime, long endTime) {
		setStartTime(startTime);
		setEndTime(endTime);
	}

	public Temporal(Temporal temporal) {
		this(temporal.startTime, temporal.endTime);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		if (startTime > endTime)
			throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.TEMPORAL_STARTTIME_ENDTIME));
		this.endTime = endTime;
	}

	public Temporal clone() {
		return new Temporal(this);
	}

	/**
	 * Check if this temporal represents an instance of time, i.e., start time equals end time.
	 * 
	 * @return true if it represents an instance of time; false otherwise.
	 */
	public boolean isTimeInstance() {
		return startTime == endTime;
	}

	public boolean equalsStartTime(Temporal temporal) {
		return startTime == temporal.startTime;
	}

	public boolean equalsEndTime(Temporal temporal) {
		return endTime == temporal.endTime;
	}

	/**
	 * Check if the two temporal intersect.
	 * 
	 * @param temporal Temporal to be checked.
	 * @return true if the two temporals intersect; false otherwise.
	 */
	public boolean intersect(Temporal temporal) {
		if (null == temporal) return false;
		if (startTime > temporal.endTime || endTime < temporal.startTime) return false;
		return true;
	}

	/**
	 * Check if the input temporal lies inside this temporal.
	 * 
	 * @param temporal Temporal to be checked.
	 * @return true if the input temporal is inside the interval of this temporal; false otherwise.
	 */
	public boolean contains(Temporal temporal) {
		if (null == temporal) return false;
		return (startTime <= temporal.startTime && temporal.endTime <= endTime);
	}

	/**
	 * Return the union of the two temporals.
	 * 
	 * @param temporal Temporal to be union with.
	 * @return Union of the two temporals.
	 * @throws TemporalException If the two temporals are not intersect with each other.
	 */
	public Temporal getUnion(Temporal temporal) throws TemporalException {
		if (!intersect(temporal))
			throw new TemporalException(ErrorMessage.TEMPORAL_NOT_INTERSECTED, new Object[] { this, temporal });
		long st = startTime > temporal.startTime ? temporal.startTime : startTime;
		long et = endTime > temporal.endTime ? endTime : temporal.endTime;
		return new Temporal(st, et);
	}

	/**
	 * Return the intersection of the two temporals.
	 * 
	 * @param temporal Temporal to be intersected with.
	 * @return Intersection of the two temporals.
	 * @throws TemporalException If the two temporals are not intersect with each other.
	 */
	public Temporal getIntersection(Temporal temporal) throws TemporalException {
		if (!intersect(temporal))
			throw new TemporalException(ErrorMessage.TEMPORAL_NOT_INTERSECTED, new Object[] { this, temporal });
		long st = startTime > temporal.startTime ? startTime : temporal.startTime;
		long et = endTime > temporal.endTime ? temporal.endTime : endTime;
		return new Temporal(st, et);
	}

	/**
	 * @param temporal Temporal
	 * @return Time segments of the two temporal.
	 * @throws TemporalException If the two temporals are not intersect with each other.
	 */
	public List<Temporal> getTimeSegments(Temporal temporal) throws TemporalException {
		if (!intersect(temporal))
			throw new TemporalException(ErrorMessage.TEMPORAL_NOT_INTERSECTED, new Object[] { this, temporal });
		List<Temporal> segments = new ArrayList<Temporal>();

		if (isTimeInstance() && temporal.isTimeInstance()) {
			segments.add(clone());
		} else if (isTimeInstance() || temporal.isTimeInstance()) {
			Temporal inst = null;
			Temporal temp = null;
			if (isTimeInstance()) {
				inst = this;
				temp = temporal;
			} else {
				inst = temporal;
				temp = this;
			}
			if (temp.startTime != inst.startTime) segments.add(new Temporal(temp.startTime, inst.startTime));
			segments.add(inst.clone());
			if (temp.endTime != inst.endTime) segments.add(new Temporal(inst.endTime, temp.endTime));
		}

		if (segments.size() > 0) return segments;

		long t1, t2, t3, t4;

		if (startTime > temporal.startTime) {
			t1 = temporal.startTime;
			t2 = startTime;
		} else {
			t1 = startTime;
			t2 = temporal.startTime;
		}

		if (endTime > temporal.endTime) {
			t3 = temporal.endTime;
			t4 = endTime;
		} else {
			t3 = endTime;
			t4 = temporal.endTime;
		}

		if (t1 != t2) segments.add(new Temporal(t1, t2));
		if (t2 != t3) segments.add(new Temporal(t2, t3));
		if (t3 != t4) segments.add(new Temporal(t3, t4));

		return segments;
	}

	/**
	 * check if the temporal object is empty.
	 * 
	 * @return true if there is no temporal information; and false otherwise.
	 */
	public boolean containsTemporalInfo() {
		return !(Long.MIN_VALUE == startTime && Long.MAX_VALUE == endTime);
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (!(o instanceof Temporal)) return getClass().getName().compareTo(o.getClass().getName());
		Temporal t = (Temporal) o;
		if (startTime != t.startTime) return startTime < t.startTime ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		if (endTime != t.endTime) return endTime < t.endTime ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (endTime ^ (endTime >>> 32));
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (null == obj) return false;
		if (getClass() != obj.getClass()) return false;

		Temporal other = (Temporal) obj;
		if (endTime != other.endTime) return false;
		if (startTime != other.startTime) return false;
		return true;
	}

	@Override
	public String toString() {
		if (!containsTemporalInfo()) return "";
		return String.valueOf(TEMPORAL_START) + startTime
				+ (Long.MAX_VALUE == endTime ? "" : String.valueOf(TEMPORAL_SEPARATOR) + endTime) + TEMPORAL_END;
	}
}
