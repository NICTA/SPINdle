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
package com.app.utils;

import java.io.Serializable;
import java.util.Map;

public class NameValuePair<K, V> implements Map.Entry<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	protected K k;
	protected V v;

	public NameValuePair() {
		this(null, null);
	}

	public NameValuePair(K k, V v) {
		setKey(k);
		setValue(v);
	}

	public NameValuePair(NameValuePair<K, V> entry) {
		this(entry.k, entry.v);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((k == null) ? 0 : k.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		NameValuePair other = (NameValuePair) obj;
		if (k == null) {
			if (other.k != null) return false;
		} else if (!k.equals(other.k)) return false;
		if (v == null) {
			if (other.v != null) return false;
		} else if (!v.equals(other.v)) return false;
		return true;
	}

	@Override
	public Object clone() {
		return new NameValuePair<K, V>(this);
	}

	@Override
	public String toString() {
		return k + "=" + v;
	}

}
