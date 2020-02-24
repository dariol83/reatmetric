/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.udd;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observer;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class XYTimeChartManager extends AbstractChartManager {

	private final XYChart chart;
	private final Map<SystemEntityPath, XYChart.Series> parameter2series = new HashMap<>();
	
    public XYTimeChartManager(Observer informer, XYChart<Instant, ? extends Number> n) {
    	super(informer);
		this.chart = n;
		this.chart.setOnDragOver(this::onDragOver);
		this.chart.setOnDragEntered(this::onDragEntered);
		this.chart.setOnDragExited(this::onDragExited);
		this.chart.setOnDragDropped(this::onDragDropped);
	}

	protected void onDragOver(DragEvent event) {
        if (event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.PARAMETER))) {
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
        if (db.hasContent(SystemEntityDataFormats.PARAMETER)) {
            addParameter((SystemEntity)db.getContent(SystemEntityDataFormats.PARAMETER));
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

	private void addParameter(SystemEntity content) {
		if(this.parameter2series.containsKey(content.getPath())) {
        	return;
        }
		
		XYChart.Series series = new XYChart.Series();
        series.setName(content.getName());
        this.parameter2series.put(content.getPath(), series);
        this.chart.getData().add(series);
        
        addPlottedParameter(content.getPath());
	}

	@Override
	public void plot(List<AbstractDataItem> datas) {
		for(AbstractDataItem item : datas) {
			if(item instanceof ParameterData) {
				ParameterData pd = (ParameterData) item;
				XYChart.Series s = parameter2series.get(pd.getPath());
				if (s != null && pd.getEngValue() != null) {
					XYChart.Data data = new XYChart.Data(live ? pd.getReceptionTime() : pd.getGenerationTime(), pd.getEngValue());
					s.getData().add(data);
					// data.getNode().setVisible(false);
					Tooltip.install(data.getNode(), new Tooltip(Objects.toString(pd.getEngValue()) + "\n" +
							(live ? pd.getReceptionTime().toString() : pd.getGenerationTime().toString())));
				}
			}
		}
	}

	@Override
	public void clear() {
		this.parameter2series.values().forEach(a -> a.getData().clear());
	}

	@Override
	public void setBoundaries(Instant min, Instant max) {
		((InstantAxis) this.chart.getXAxis()).setLowerBound(min);
		((InstantAxis) this.chart.getXAxis()).setUpperBound(max);
	}
}
