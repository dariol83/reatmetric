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


package eu.dariolucia.reatmetric.ui.udd;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public abstract class AbstractChartManager {

	private final Consumer<AbstractChartManager> changeListener;

	private final Set<SystemEntityPath> parameters = new TreeSet<>();

	private final Set<SystemEntityPath> events = new TreeSet<>();

	protected volatile boolean live = true;

	public AbstractChartManager(Consumer<AbstractChartManager> informer) {
		changeListener = informer;
	}

	public Set<SystemEntityPath> getPlottedParameters() {
		return this.parameters;
	}

	public Set<SystemEntityPath> getPlottedEvents() {
		return this.events;
	}

	protected void addPlottedParameter(SystemEntityPath path) {
		this.parameters.add(path);
		notifyObservers();
	}

	protected void addPlottedEvent(SystemEntityPath path) {
		this.events.add(path);
		notifyObservers();
	}

	protected void notifyObservers() {
		if(changeListener != null) {
			changeListener.accept(this);
		}
	}

	public void switchToLive(boolean b) {
		this.live = b;
	}

	public abstract void setBoundaries(Instant min, Instant max);
	
	public abstract void plot(List<AbstractDataItem> data);

	public abstract void clear();

	public abstract String getChartType();

	public abstract List<String> getCurrentEntityPaths();

    public abstract void addItems(List<String> items);
}
