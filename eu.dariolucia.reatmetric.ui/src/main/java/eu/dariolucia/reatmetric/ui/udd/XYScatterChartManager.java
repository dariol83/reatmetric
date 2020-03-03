/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.udd;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.event.Event;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.StringConverter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class XYScatterChartManager extends AbstractChartManager {

	private final ScatterChart chart;
	private final Map<String, ScatterChart.Series> event2series = new LinkedHashMap<>();
	private final Map<String, Integer> event2position = new TreeMap<>();
	private final Map<Integer, String> position2event = new HashMap<>();

	private final AtomicInteger eventCounter = new AtomicInteger(0);

    public XYScatterChartManager(Observer informer, ScatterChart<Instant, Number> n) {
    	super(informer);
		this.chart = n;
		this.chart.setOnDragOver(this::onDragOver);
		this.chart.setOnDragEntered(this::onDragEntered);
		this.chart.setOnDragExited(this::onDragExited);
		this.chart.setOnDragDropped(this::onDragDropped);
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
            addEvent((SystemEntity)db.getContent(SystemEntityDataFormats.EVENT));
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

	private void addEvent(SystemEntity content) {
		if(this.event2series.containsKey(content.getPath().asString())) {
        	return;
        }

		ScatterChart.Series series = new ScatterChart.Series();
        series.setName(content.getName());
        this.event2series.put(content.getPath().asString(), series);
        this.event2position.put(content.getPath().asString(), eventCounter.incrementAndGet());
		this.position2event.put(eventCounter.get(), content.getPath().asString());
		// Workaround to fix ScatterChart bug: https://stackoverflow.com/questions/30171290/javafx-scatterchart-does-not-display-legend-symbol-when-initialized-with-empty-d
		series.getData().add(new XYChart.Data<>(Instant.EPOCH, eventCounter.get()));
        this.chart.getData().add(series);
		// ((CategoryAxis) this.chart.getYAxis()).getCategories().add(content.getPath().asString());
        addPlottedEvent(content.getPath());
	}

	@Override
	public void plot(List<AbstractDataItem> datas) {
		for(AbstractDataItem item : datas) {
			if(item instanceof EventData) {
				EventData pd = (EventData) item;
				ScatterChart.Series s = event2series.get(pd.getPath().asString());
				if (s != null) {
					ScatterChart.Data data = new ScatterChart.Data(live ? pd.getReceptionTime() : pd.getGenerationTime(), event2position.get(pd.getPath().asString()));
					s.getData().add(data);
					// data.getNode().setVisible(false);
					Tooltip.install(data.getNode(), new Tooltip(live ? pd.getReceptionTime().toString() : pd.getGenerationTime().toString()));
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
		return new ArrayList<>(event2series.keySet());
	}

	@Override
	public void setBoundaries(Instant min, Instant max) {
		((InstantAxis) this.chart.getXAxis()).setLowerBound(min);
		((InstantAxis) this.chart.getXAxis()).setUpperBound(max);
	}
}
