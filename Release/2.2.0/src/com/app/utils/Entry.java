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

/**
 * Class used to represent a key-value pair.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @param <K> the key
 * @param <V> the corresponding value
 */
public class Entry<K extends Comparable<? super K>, V> implements Comparable<Object>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	private static <T> Comparable<T> toComparable(T o) {
		if (o == null) throw new NullPointerException();
		return (Comparable<T>) o;
	}

	private K k;
	private V v;

	private Comparator<? super K> comparator = null;

	public Entry() {
		k = null;
		v = null;
	}

	public Entry(K k, V v) {
		setKey(k);
		setValue(v);
	}

	public Entry(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	public K getKey() {
		return k;
	}

	public void setKey(K k) {
		this.k = k;
	}

	public V getValue() {
		return v;
	}

	public void setValue(V v) {
		this.v = v;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (null == o) return false;
		if (!(o instanceof Entry)) return false;
		Entry<?, ?> entry = (Entry<?, ?>) o;
		return ((null == k) ? null == entry.k : k.equals(entry.k))
				&& ((null == v) ? null == entry.v : v.equals(entry.v));
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		try {
			return (null == comparator) ? toComparable(k).compareTo((K) o) : comparator.compare(k, (K) o);
		} catch (Exception e) {
			throw new UnsupportedOperationException("Comparator not found for class: " + k.getClass().getName());
		}
	}

	@Override
	public Object clone() {
		return new Entry<K, V>(k, v);
	}

	@Override
	public String toString() {
		return ((null == k) ? "" : k.toString()) + "=" + ((null == v) ? "" : v.toString());
	}
}
