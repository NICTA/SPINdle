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
package com.app.utils;

import java.util.Comparator;
import java.util.Map;

public class ComparableEntry<K extends Comparable<? super K>, V> extends NameValuePair<K, V> implements Comparable<Object> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	private static <K> Comparable<? super K> toComparable(K o) {
		return (Comparable<K>) o;
	}

	private Comparator<? super K> comparator;

	public ComparableEntry() {
		this(null, null, null);
	}

	public ComparableEntry(K k, V v) {
		this(k, v, null);
	}

	public ComparableEntry(java.util.Map.Entry<K, V> entry) {
		this(entry.getKey(), entry.getValue(), null);
		if (entry instanceof ComparableEntry<?, ?>) setComparator(((ComparableEntry<K, V>) entry).comparator);
	}

	public ComparableEntry(Comparator<? super K> comparator) {
		this(null, null, comparator);
	}

	protected ComparableEntry(K k, V v, Comparator<? super K> comparator) {
		super(k, v);
		setComparator(comparator);
	}

	public void setComparator(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	public Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	public Object clone() {
		return new ComparableEntry<K, V>(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Object obj) {
		if (this == obj) return 0;
		if (!(obj instanceof Map.Entry<?, ?>)) return getClass().getName().compareTo(obj.getClass().getName());

		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;

		if (null == k) {
			if (null == entry.getKey()) return 0;
			return Integer.MAX_VALUE;
		} else {
			if (null == entry.getKey()) return Integer.MIN_VALUE;
			try {
				K entryKey = (K) entry.getKey();
				return null == comparator ? toComparable(k).compareTo(entryKey) : comparator.compare(k, entryKey);
			} catch (UnsupportedOperationException e) {
				throw new UnsupportedOperationException("Comparator not found for class: " + k.getClass().getName());
			}
		}
	}

}
