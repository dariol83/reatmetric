/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.udd;

import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

public class XYBarChartManager extends AbstractChartManager {

	private final BarChart<String, Number> chart;
	private final Map<SystemEntityPath, XYChart.Series<String, Number>> parameter2series = new HashMap<>();
	
    public XYBarChartManager(Observer informer, BarChart<String, Number> n) {
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
		
		XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(content.getName());
        this.parameter2series.put(content.getPath(), series);
        this.chart.getData().add(series);

        addPlottedParameter(content.getPath());
	}

	@Override
	public void plot(List<ParameterData> datas) {
		for(ParameterData pd : datas) {
			XYChart.Series<String, Number> s = parameter2series.get(pd.getPath());
			if(s != null && pd.getEngValue() != null) {
				XYChart.Data<String, Number> data = new XYChart.Data<>(pd.getPath().toString(), (Number) pd.getEngValue());
				s.getData().add(data);
				// data.getNode().setVisible(false);
				Tooltip.install(data.getNode(), new Tooltip(pd.getEngValue() + "\n" +
						(live ? pd.getReceptionTime().toString() : pd.getGenerationTime().toString())));
			}
		}
	}

	@Override
	public void clear() {
		this.parameter2series.values().forEach(a -> a.getData().clear());
	}

	@Override
	public void setBoundaries(Instant min, Instant max) {
		// Nothing to do
	}
}
