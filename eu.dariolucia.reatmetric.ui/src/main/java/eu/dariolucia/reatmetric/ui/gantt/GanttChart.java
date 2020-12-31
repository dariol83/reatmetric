/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.ui.gantt;

import eu.dariolucia.reatmetric.ui.udd.InstantAxis;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Based on Roland's on StackOverflow: https://stackoverflow.com/a/27978436.
 * <p>
 * Code commented and updated by Dario Lucia.
 *
 * @param <Y>
 */
public class GanttChart<Y> extends XYChart<Instant, Y> {

    private double blockHeight = 40;

    private Function<Object, Instant> timeExtractor;
    private Function<Object, String> styleClassExtractor;
    private Function<Object, String> tooltipExtractor;

    private final List<Consumer<XYChart.Data<Instant, Y>>> taskListeners = new ArrayList<>();

    private Pair<Rectangle, Line> currentTimeMarker;

    public GanttChart(@NamedArg("xAxis") InstantAxis xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.observableArrayList());
    }

    public GanttChart(@NamedArg("xAxis") InstantAxis xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<Instant, Y>> data) {
        super(xAxis, yAxis);
        if (!(yAxis instanceof CategoryAxis)) {
            throw new IllegalArgumentException("Axis type incorrect, Y should be CategoryAxis");
        }
        setData(data);
        // A click on the chart informs about a null selection
        onMouseClickedProperty().set(e -> {
            taskClicked(null);
        });
    }

    public void registerInformationExtractor(Function<Object, Instant> timeExtractor, Function<Object, String> styleClassExtractor) {
        this.timeExtractor = timeExtractor;
        this.styleClassExtractor = styleClassExtractor;
    }

    public void registerTooltipExtractor(Function<Object, String> tooltipFunction) {
        this.tooltipExtractor = tooltipFunction;
    }

    public void registerTaskSelectionListener(Consumer<XYChart.Data<Instant, Y>> listener) {
        this.taskListeners.add(listener);
    }

    public void deregisterTaskSelectionListener(Consumer<XYChart.Data<Instant, Y>> listener) {
        this.taskListeners.remove(listener);
    }

    public void setCurrentTimeMarker(boolean marker) {
        if(marker) {
            if(this.currentTimeMarker == null) {
                this.currentTimeMarker = new Pair<>(null, null);

                Line line = new Line();
                line.getStrokeDashArray().addAll(5d, 5d);
                getPlotChildren().add(line);

                Rectangle r = new Rectangle();
                r.setFill(new Color(0.2, 0.8, 0.7, 0.2));
                getPlotChildren().add(r);

                this.currentTimeMarker = new Pair<>(r, line);
            }
        } else {
            if(this.currentTimeMarker != null) {
                getPlotChildren().remove(this.currentTimeMarker.getKey());
                getPlotChildren().remove(this.currentTimeMarker.getValue());
                this.currentTimeMarker = null;
            }
        }
        layoutPlotChildren();
    }

    @Override
    protected void layoutPlotChildren() {
        for (Series<Instant, Y> series :  getData()) {
            Iterator<Data<Instant, Y>> iter = getDisplayedDataIterator(series);
            while (iter.hasNext()) {
                Data<Instant, Y> item = iter.next();
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y = getYAxis().getDisplayPosition(item.getYValue());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }
                Node block = item.getNode();
                Rectangle rectangle;
                if (block != null) {
                    if (block instanceof StackPane) {
                        StackPane region = (StackPane) item.getNode();
                        double width = computeItemWidth(x, getXAxis().getDisplayPosition(getEndTime(item)));
                        if (region.getShape() == null) {
                            rectangle = new Rectangle(width, getBlockHeight());
                        } else if (region.getShape() instanceof Rectangle) {
                            rectangle = (Rectangle) region.getShape();
                        } else {
                            return;
                        }
                        rectangle.setWidth(width);
                        rectangle.setHeight(getBlockHeight());

                        y -= getBlockHeight() / 2.0;

                        // Note: workaround for RT-7689 - saw this in ProgressControlSkin
                        // The region doesn't update itself when the shape is mutated in place, so we
                        // null out and then restore the shape in order to force invalidation.
                        region.setShape(null);
                        region.setShape(rectangle);
                        region.setScaleShape(false);
                        region.setCenterShape(false);
                        region.setCacheShape(false);

                        block.setLayoutX(x);
                        block.setLayoutY(y);

                        String computeTooltip = getTooltip(item.getExtraValue());
                        if(computeTooltip != null) {
                            Tooltip.install(item.getNode(), new Tooltip(computeTooltip));
                        }
                    }
                }
            }
        }

        if(currentTimeMarker != null) {
            Instant now = Instant.now();
            Rectangle rect = currentTimeMarker.getKey();
            rect.setWidth(getXAxis().getDisplayPosition(now));
            rect.setHeight(getBoundsInLocal().getHeight());
            rect.setX(0);
            rect.setY(0);
            rect.toFront();
            // The following part is coming from StackOverflow: https://stackoverflow.com/a/28955561
            Line line = currentTimeMarker.getValue();
            line.setStartX(getXAxis().getDisplayPosition(now) + 0.5);  // 0.5 for crispness
            line.setEndX(line.getStartX());
            line.setStartY(0d);
            line.setEndY(getBoundsInLocal().getHeight());
            line.toFront();
        }
    }

    private String getTooltip(Object extraValue) {
        if(this.tooltipExtractor == null) {
            return null;
        } else {
            return tooltipExtractor.apply(extraValue);
        }
    }

    private Instant getEndTime(Data<Instant, Y> item) {
        if(this.timeExtractor == null) {
            return item.getXValue();
        } else {
            return timeExtractor.apply(item.getExtraValue());
        }
    }

    private double computeItemWidth(double absStart, double absEnd) {
        double wdt = absEnd - absStart;
        return wdt < 2 ? 2 : wdt;
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(double blockHeight) {
        this.blockHeight = blockHeight;
    }

    @Override
    protected void dataItemAdded(Series<Instant, Y> series, int itemIndex, Data<Instant, Y> item) {
        Node block = createContainer(item);
        getPlotChildren().add(block);
    }

    @Override
    protected void dataItemRemoved(final Data<Instant, Y> item, final Series<Instant, Y> series) {
        final Node block = item.getNode();
        getPlotChildren().remove(block);
        removeDataItemFromDisplay(series, item);
    }

    @Override
    protected void dataItemChanged(Data<Instant, Y> item) {
    }

    @Override
    protected void seriesAdded(Series<Instant, Y> series, int seriesIndex) {
        for (int j = 0; j < series.getData().size(); j++) {
            Data<Instant, Y> item = series.getData().get(j);
            Node container = createContainer(item);
            getPlotChildren().add(container);
        }
    }

    @Override
    protected void seriesRemoved(final Series<Instant, Y> series) {
        for (XYChart.Data<Instant, Y> d : series.getData()) {
            final Node container = d.getNode();
            getPlotChildren().remove(container);
            container.onMouseClickedProperty().set(null);
        }
        removeSeriesFromDisplay(series);
    }

    /**
     * Inform the Gantt chart that the item was updated.
     *
     * @param item the updated item
     */
    public void updateNode(final Data<Instant, Y> item) {
        if(item == null) {
            return;
        }
        createContainer(item);
        layoutPlotChildren();
    }

    private Node createContainer(final Data<Instant, Y> item) {
        Node container = item.getNode();
        if (container == null) {
            container = new StackPane();
            container.onMouseClickedProperty().set(e -> {
                taskClicked(item);
                e.consume();
            });
            item.setNode(container);
        }
        String style = getStyleClass(item);
        if(style != null) {
            container.getStyleClass().clear();
            container.getStyleClass().add(style);
        }
        return container;
    }

    private void taskClicked(Data<Instant, Y> item) {
        for(Consumer<Data<Instant, Y>> c : taskListeners) {
            c.accept(item);
        }
    }

    private String getStyleClass(Data<Instant, Y> item) {
        if(styleClassExtractor == null) {
            return null;
        } else {
            return styleClassExtractor.apply(item.getExtraValue());
        }
    }

    @Override
    protected void updateAxisRange() {
        final Axis<Instant> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<Instant> xData = null;
        List<Y> yData = null;
        if (xa.isAutoRanging()) xData = new ArrayList<>();
        if (ya.isAutoRanging()) yData = new ArrayList<>();
        if (xData != null || yData != null) {
            for (Series<Instant, Y> series : getData()) {
                for (Data<Instant, Y> data : series.getData()) {
                    if (xData != null) {
                        xData.add(data.getXValue());
                        xData.add(xa.toRealValue(xa.toNumericValue(getEndTime(data))));
                    }
                    if (yData != null) {
                        yData.add(data.getYValue());
                    }
                }
            }
            if (xData != null) xa.invalidateRange(xData);
            if (yData != null) ya.invalidateRange(yData);
        }
    }

    public Instant getMaxTime() {
        return ((InstantAxis)getXAxis()).getUpperBound();
    }

    public Instant getMinTime() {
        return ((InstantAxis)getXAxis()).getLowerBound();
    }
}
