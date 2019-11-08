/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.udd;

import java.time.Instant;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;

public abstract class AbstractChartManager extends Observable {

	private final Set<SystemEntityPath> parameters = new TreeSet<>();
	
	public AbstractChartManager(Observer informer) {
		addObserver(informer);
	}

	public Set<SystemEntityPath> getPlottedParameters() {
		return this.parameters;
	}
	
	protected void addPlottedParameter(SystemEntityPath path) {
		this.parameters.add(path);
		setChanged();
		notifyObservers();
	}
	
	public abstract void setBoundaries(Instant min, Instant max);
	
	public abstract void plot(List<ParameterData> data);

	public abstract void clear();
	
}
