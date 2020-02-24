/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.udd;

import java.time.Instant;
import java.util.*;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;

public abstract class AbstractChartManager extends Observable {

	private final Set<SystemEntityPath> parameters = new TreeSet<>();

	private final Set<SystemEntityPath> events = new TreeSet<>();

	protected volatile boolean live = true;

	public AbstractChartManager(Observer informer) {
		addObserver(informer);
	}

	public Set<SystemEntityPath> getPlottedParameters() {
		return this.parameters;
	}

	public Set<SystemEntityPath> getPlottedEvents() {
		return this.events;
	}

	protected void addPlottedParameter(SystemEntityPath path) {
		this.parameters.add(path);
		setChanged();
		notifyObservers();
	}

	protected void addPlottedEvent(SystemEntityPath path) {
		this.events.add(path);
		setChanged();
		notifyObservers();
	}
	
	public abstract void setBoundaries(Instant min, Instant max);
	
	public abstract void plot(List<AbstractDataItem> data);

	public abstract void clear();

	public void switchToLive(boolean b) {
		this.live = b;
	}
}
