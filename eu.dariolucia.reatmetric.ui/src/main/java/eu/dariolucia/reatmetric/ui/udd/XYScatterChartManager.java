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
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class XYScatterChartManager extends AbstractChartManager {

	private final ScatterChart<Instant, Number> chart;
	private final Map<SystemEntityPath, ScatterChart.Series<Instant, Number>> event2series = new LinkedHashMap<>();
	private final Map<SystemEntityPath, Integer> event2position = new TreeMap<>();

	private final AtomicInteger eventCounter = new AtomicInteger(0);

	public XYScatterChartManager(Consumer<AbstractChartManager> informer, ScatterChart<Instant, Number> n) {
		this(informer, n, true);
	}

    public XYScatterChartManager(Consumer<AbstractChartManager> informer, ScatterChart<Instant, Number> n, boolean registerDnD) {
    	super(informer);
		this.chart = n;
		if(registerDnD) {
			this.chart.setOnDragOver(this::onDragOver);
			this.chart.setOnDragEntered(this::onDragEntered);
			this.chart.setOnDragExited(this::onDragExited);
			this.chart.setOnDragDropped(this::onDragDropped);
		}
	}

	protected void onDragOver(DragEvent event) {
        if (event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.EVENT))) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }
    
    private void onDragEntered(DragEvent event) {
        event.consume();
    }
    
    private void onDragExited(DragEvent event) {
        event.consume();        
    }
    
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasContent(SystemEntityDataFormats.EVENT)) {
            addEvent(((SystemEntity)db.getContent(SystemEntityDataFormats.EVENT)).getPath());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

	private void addEvent(SystemEntityPath content) {
		if(this.event2series.containsKey(content)) {
        	return;
        }

		ScatterChart.Series<Instant, Number> series = new ScatterChart.Series<>();
        series.setName(content.getLastPathElement());
        this.event2series.put(content, series);
        this.event2position.put(content, eventCounter.incrementAndGet());
		// Workaround to fix ScatterChart bug: https://stackoverflow.com/questions/30171290/javafx-scatterchart-does-not-display-legend-symbol-when-initialized-with-empty-d
		series.getData().add(new XYChart.Data<>(Instant.EPOCH, eventCounter.get()));
        this.chart.getData().add(series);
		// ((CategoryAxis) this.chart.getYAxis()).getCategories().add(content.getPath().asString());
        addPlottedEvent(content);
	}

	@Override
	public void plot(List<AbstractDataItem> datas) {
		for(AbstractDataItem item : datas) {
			if(item instanceof EventData) {
				EventData pd = (EventData) item;
				ScatterChart.Series<Instant, Number> s = event2series.get(pd.getPath());
				if (s != null) {
					ScatterChart.Data<Instant, Number> data = new ScatterChart.Data<>(pd.getGenerationTime(), event2position.get(pd.getPath()));
					s.getData().add(data);
					// data.getNode().setVisible(false);
					Tooltip.install(data.getNode(), new Tooltip(pd.getGenerationTime().toString()));
				}
			}
		}
	}

	@Override
	public void clear() {
		this.event2series.values().forEach(a -> a.getData().clear());
	}

	@Override
	public String getChartType() {
		return "scatter";
	}

	@Override
	public List<String> getCurrentEntityPaths() {
		return event2series.keySet().stream().map(SystemEntityPath::asString).collect(Collectors.toList());
	}

	@Override
	public void setBoundaries(Instant min, Instant max) {
		((InstantAxis) this.chart.getXAxis()).setLowerBound(min);
		((InstantAxis) this.chart.getXAxis()).setUpperBound(max);
	}

	@Override
	public void addItems(List<String> items) {
		for(String item : items) {
			addEvent(SystemEntityPath.fromString(item));
		}
	}
}
