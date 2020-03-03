/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
