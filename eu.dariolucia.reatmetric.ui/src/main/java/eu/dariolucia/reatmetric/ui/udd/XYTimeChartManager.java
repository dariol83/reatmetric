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
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class XYTimeChartManager extends AbstractChartManager {

	private final XYChart<Instant, Number> chart;
	private final Map<SystemEntityPath, XYChart.Series<Instant, Number>> parameter2series = new LinkedHashMap<>();
	private volatile Instant maxGenerationTimeOnChart = Instant.EPOCH;

	public XYTimeChartManager(Consumer<AbstractChartManager> informer, XYChart<Instant, Number> n) {
		this(informer, n, true);
	}

    public XYTimeChartManager(Consumer<AbstractChartManager> informer, XYChart<Instant, Number> n, boolean registerDnD) {
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
            addParameter(((SystemEntity)db.getContent(SystemEntityDataFormats.PARAMETER)).getPath());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

	private void addParameter(SystemEntityPath content) {
		if(this.parameter2series.containsKey(content)) {
        	return;
        }
		
		XYChart.Series<Instant, Number> series = new XYChart.Series<>();
        series.setName(content.getLastPathElement());
        this.parameter2series.put(content, series);
        this.chart.getData().add(series);
        
        addPlottedParameter(content);
	}

	@Override
	public void plot(List<AbstractDataItem> datas) {
		for(AbstractDataItem item : datas) {
			if(item instanceof ParameterData) {
				ParameterData pd = (ParameterData) item;
				XYChart.Series<Instant, Number> s = parameter2series.get(pd.getPath());
				if (s != null && pd.getEngValue() != null) {
					// if not a number, remove the parameter from the plot
					if(pd.getEngValue() instanceof Number) {
						XYChart.Data<Instant, Number> data = new XYChart.Data<>(pd.getGenerationTime(), (Number) pd.getEngValue());
						s.getData().add(data);
						// data.getNode().setVisible(false);
						Tooltip.install(data.getNode(), new Tooltip(pd.getEngValue() + "\n" +
								(pd.getGenerationTime().toString())));
						if(pd.getGenerationTime().isAfter(maxGenerationTimeOnChart)) {
							maxGenerationTimeOnChart = pd.getGenerationTime();
						}
					} else {
						parameter2series.remove(pd.getPath());
						chart.getData().remove(s);
					}
				}
			}
		}
	}

	@Override
	public void clear() {
		this.parameter2series.values().forEach(a -> a.getData().clear());
		this.maxGenerationTimeOnChart = Instant.EPOCH;
	}

	@Override
	public String getChartType() {
		if(chart instanceof AreaChart) {
			return "area";
		} else if(chart instanceof LineChart) {
			return "line";
		} else {
			return "unknown";
		}
	}

	@Override
	public List<String> getCurrentEntityPaths() {
		return parameter2series.keySet().stream().map(SystemEntityPath::asString).collect(Collectors.toList());
	}

	@Override
	public void setBoundaries(Instant min, Instant max) {
    	((InstantAxis) this.chart.getXAxis()).setLowerBound(min);
		((InstantAxis) this.chart.getXAxis()).setUpperBound(max);
	}

	@Override
	public void addItems(List<String> items) {
		for(String item : items) {
			addParameter(SystemEntityPath.fromString(item));
		}
	}

	@Override
	public Instant getLatestReceivedGenerationTime() {
		return maxGenerationTimeOnChart;
	}
}
