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
package com.app.utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class MapEntry<K extends Comparable<? super K>, V> //
		implements Map.Entry<K, V>, Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	private static <T> Comparable<T> toComparable(T o) {
		return (Comparable<T>) o;
	}

	private K k;
	private V v;

	private Comparator<? super K> comparator;

	public MapEntry() {
		this(null, null, null);
	}

	public MapEntry(K k, V v) {
		this(k, v, null);
	}

	public MapEntry(Comparator<? super K> comparator) {
		this(null, null, comparator);
	}

	public MapEntry(MapEntry<K, V> entry) {
		this(entry.k, entry.v, entry.comparator);
	}

	protected MapEntry(K k, V v, Comparator<? super K> comparator) {
		this.k = k;
		this.v = v;
		setComparator(comparator);
	}

	@Override
	public K getKey() {
		return k;
	}

	public K setKey(K k) {
		K old = this.k;
		this.k = k;
		return old;
	}

	@Override
	public V getValue() {
		return v;
	}

	@Override
	public V setValue(V v) {
		V old = this.v;
		this.v = v;
		return old;
	}

	public void setComparator(Comparator<? super K> comparator) {
		this.comparator = comparator;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((k == null) ? 0 : k.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof Map.Entry<?, ?>)) return false;

		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
		return (null == k ? null == entry.getKey() : k.equals(entry.getKey()))
				&& (null == v ? null == entry.getValue() : v.equals(entry.getValue()));
	}

	@Override
	public Object clone() {
		return new MapEntry<K, V>(this);
	}

	@Override
	public String toString() {
		return k + "=" + v;
	}

}
