/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package eu.dariolucia.reatmetric.ui.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class OrderedProperties extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4594096759849556998L;

	private final Map<Object, Integer> key2position = new HashMap<>();
	
	private int nextPosition = 0;
	
	private final Comparator<Object> insertionComparator = Comparator.comparingInt(key2position::get);
	
	
	@Override
    public synchronized Enumeration<Object> keys() {
		Set<Object> theSet = new TreeSet<Object>(this.insertionComparator);
		theSet.addAll(super.keySet());
        return Collections.enumeration(theSet);
    }
	
	@Override
	public synchronized Set<Object> keySet() {
		Set<Object> theSet = new TreeSet<Object>(this.insertionComparator);
		theSet.addAll(super.keySet());
		return theSet;
	}
	
	@Override
	public synchronized Object put(Object key, Object value) {
		addToInsertion(key);
		return super.put(key, value);
	}
	
	@Override
	public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
		for(Map.Entry<? extends Object, ? extends Object> e : t.entrySet()) {
			addToInsertion(e.getKey());
		}
		super.putAll(t);
	}
	
	@Override
	public synchronized Object putIfAbsent(Object key, Object value) {
		Object oldValue = super.putIfAbsent(key, value);
		if(oldValue == null) {
			// Insertion done
			addToInsertion(key);
		}
		return oldValue;
	}

	private void addToInsertion(Object key) {
		this.key2position.put(key, nextPosition++);
	}
}
