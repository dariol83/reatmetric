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


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.gantt.GanttChart;
import eu.dariolucia.reatmetric.ui.udd.InstantAxis;
import eu.dariolucia.reatmetric.ui.utils.*;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * // TODO: implement support for history retrieval: Gantt follows the entries in the table.
 * @author dario
 */
public class SchedulerViewController extends AbstractDisplayController implements IScheduledActivityDataSubscriber {

    private static final Logger LOG = Logger.getLogger(SchedulerViewController.class.getName());

    private static final int MAX_ENTRIES = 1000;
    private static final int INITIALIZATION_SECONDS_IN_PAST = 7200;
    private static final String REATMETRIC_GANTT_STYLE_PREFIX = "reatmetric-gantt-";

    // Not final, applicable for the entire UI
    private static int GANTT_RANGE_PAST = 3600 * 1;
    private static int GANTT_RANGE_AHEAD = 3600 * 1;

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

    // Live/retrieval controls
    @FXML
    protected ToggleButton liveTgl;
    @FXML
    protected Button goToStartBtn;
    @FXML
    protected Button goBackFastBtn;
    @FXML
    protected Button goBackOneBtn;
    @FXML
    protected Button goToEndBtn;
    @FXML
    protected Button goForwardFastBtn;
    @FXML
    protected Button goForwardOneBtn;
    @FXML
    protected Button selectTimeBtn;

    @FXML
    protected Button filterBtn;

    // Print button
    @FXML
    protected Button printBtn;

    // Remove button
    @FXML
    protected Button removeBtn;

    // Enable/disable toggle
    @FXML
    protected ToggleButton enableTgl;

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Gantt chart
    @FXML
    protected GanttChart<String> ganttChart;

    @FXML
    protected Button updateTimeBoundariesBtn;

    // Table
    @FXML
    protected TableView<ScheduledActivityOccurrenceDataWrapper> dataItemTableView;

    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> extIdCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> nameCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, SchedulingState> stateCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> sourceCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> resourcesCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> triggerCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, Instant> startTimeCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, Instant> endTimeCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, Duration> durationCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> parentCol;

    @FXML
    protected MenuItem editScheduledMenuItem;
    @FXML
    protected MenuItem deleteScheduledMenuItem;

    // Event table
    @FXML
    protected TableView<ScheduledActivityOccurrenceDataWrapper> eventDataItemTableView;

    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventExtIdCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventNameCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventSourceCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventResourcesCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventTriggerCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, Duration> eventDurationCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventParentCol;

    @FXML
    protected MenuItem eventEditScheduledMenuItem;
    @FXML
    protected MenuItem eventDeleteScheduledMenuItem;

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Popup selector for filter
    protected final Popup filterPopup = new Popup();

    // Popup selector for Gantt boundaries
    protected final Popup ganttBoundariesPopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    // Filter controller
    protected IFilterController<ScheduledActivityDataFilter> dataItemFilterController;

    // Gantt time boundaries selector controller
    protected GanttTimeBoundariesPickerWidgetController ganttTimeBoundariesPickerController;

    // Temporary object queue
    private DataProcessingDelegator<ScheduledActivityData> delegator;

    private final Map<IUniqueId, ScheduledActivityOccurrenceDataWrapper> activityMap = new HashMap<>();
    private FilteredList<ScheduledActivityOccurrenceDataWrapper> filteredList;
    private ObservableList<ScheduledActivityOccurrenceDataWrapper> timeScheduledActivityList;
    private SortedList<ScheduledActivityOccurrenceDataWrapper> sortedList;

    private ObservableList<ScheduledActivityOccurrenceDataWrapper> eventTriggeredActivityList;

    private final Timer timer = new Timer("Reatmetric UI - Scheduler time tracker");
    private volatile TimerTask secondTicker;

    // Gantt tracked activities
    private final Map<ScheduledActivityOccurrenceDataWrapper, XYChart.Data<Instant, String>> activity2data = new HashMap<>();
    private final Map<String, XYChart.Series<Instant, String>> activityPath2series = new HashMap<>();

    private final ISchedulerSubscriber scheduleSubscriber = new ISchedulerSubscriber() {
        @Override
        public void schedulerEnablementChanged(boolean enabled) {
            internalSchedulerEnablementChanged(enabled);
        }
    };

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());

        loadTimePopup();

        loadFilterPopup();

        setupScheduledActivitiesTable();

        setupEventTriggersTable();

        // Timed activities
        this.timeScheduledActivityList = FXCollections.observableList(FXCollections.observableArrayList(),
                data -> new Observable[]{data.setLineProperty(), data.startTimeProperty(), data.endTimeProperty(), data.durationProperty(), data.stateProperty(), data.nameProperty(), data.resourcesProperty(), data.triggerProperty()});
        this.filteredList = new FilteredList<>(this.timeScheduledActivityList, o -> true);
        this.sortedList = new SortedList<>(filteredList, Comparator.reverseOrder());
        this.dataItemTableView.setItems(sortedList);

        // Event triggers
        this.eventTriggeredActivityList = FXCollections.observableList(FXCollections.observableArrayList(),
                data -> new Observable[]{data.startTimeProperty(), data.endTimeProperty(), data.durationProperty(), data.stateProperty(), data.nameProperty(), data.resourcesProperty(), data.triggerProperty(), data.eventTriggerProperty()});
        this.eventDataItemTableView.setItems(eventTriggeredActivityList);

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());

        // Chart initialisation
        this.ganttChart.getStylesheets().add(getClass().getClassLoader().getResource("eu/dariolucia/reatmetric/ui/fxml/css/gantt.css").toExternalForm());
        updateChartLocation(Instant.now());
        this.ganttChart.getXAxis().setAutoRanging(false);
        this.ganttChart.registerInformationExtractor(this::extractEndTime, this::extractStyleClass);
        this.ganttChart.registerTooltipExtractor(this::extractTooltip);
        this.ganttChart.registerTaskSelectionListener(this::taskSelected);

        loadGanttBoundariesPopup();
    }

    private void taskSelected(XYChart.Data<Instant, String> item) {
        if(item == null) {
            this.dataItemTableView.getSelectionModel().clearSelection();
        } else {
            ScheduledActivityOccurrenceDataWrapper w = (ScheduledActivityOccurrenceDataWrapper) item.getExtraValue();
            this.dataItemTableView.getSelectionModel().select(w);
        }
    }

    private String extractTooltip(Object o) {
        ScheduledActivityOccurrenceDataWrapper w = (ScheduledActivityOccurrenceDataWrapper) o;
        return w.getPath().asString() + "\n" + "Resources: " + w.get().getResources();
    }

    private String extractStyleClass(Object o) {
        ScheduledActivityOccurrenceDataWrapper w = (ScheduledActivityOccurrenceDataWrapper) o;
        SchedulingState ss = w.get().getState();
        return REATMETRIC_GANTT_STYLE_PREFIX + ss.name();
    }

    private Instant extractEndTime(Object o) {
        ScheduledActivityOccurrenceDataWrapper wrap = (ScheduledActivityOccurrenceDataWrapper) o;
        return wrap.endTime.get();
    }

    private void loadFilterPopup() {
        this.filterPopup.setAutoHide(true);
        this.filterPopup.setHideOnEscape(true);

        try {
            URL filterWidgetUrl = getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/SchedulerFilterWidget.fxml");
            FXMLLoader loader = new FXMLLoader(filterWidgetUrl);
            Parent filterSelector = loader.load();
            this.dataItemFilterController = loader.getController();
            this.filterPopup.getContent().addAll(filterSelector);
            // Load the controller hide with select
            this.dataItemFilterController.setActionAfterSelection(() -> {
                this.filterPopup.hide();
                applyFilter(this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTimePopup() {
        this.dateTimePopup.setAutoHide(true);
        this.dateTimePopup.setHideOnEscape(true);

        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            this.dateTimePickerController = loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_FUTURE, MAX_ENTRIES * 2, this.dataItemFilterController.getSelectedFilter(), false);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGanttBoundariesPopup() {
        this.ganttBoundariesPopup.setAutoHide(true);
        this.ganttBoundariesPopup.setHideOnEscape(true);

        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/GanttTimeBoundariesPickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            this.ganttTimeBoundariesPickerController = loader.getController();
            this.ganttBoundariesPopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.ganttTimeBoundariesPickerController.setActionAfterSelection(() -> {
                this.ganttBoundariesPopup.hide();
                GANTT_RANGE_PAST = ganttTimeBoundariesPickerController.getPastDuration();
                GANTT_RANGE_AHEAD = ganttTimeBoundariesPickerController.getFutureDuration();
                updateChartLocation(Instant.now());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupEventTriggersTable() {
        this.eventDataItemTableView.setPlaceholder(new Label(""));

        this.eventExtIdCol.setCellValueFactory(o -> o.getValue().externalIdProperty());
        this.eventNameCol.setCellValueFactory(o -> o.getValue().nameProperty());
        this.eventSourceCol.setCellValueFactory(o -> o.getValue().sourceProperty());
        this.eventTriggerCol.setCellValueFactory(o -> o.getValue().eventTriggerProperty());
        this.eventResourcesCol.setCellValueFactory(o -> o.getValue().resourcesProperty());
        this.eventDurationCol.setCellValueFactory(o -> o.getValue().durationProperty());
        this.eventParentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));
    }

    private void setupScheduledActivitiesTable() {
        this.dataItemTableView.setPlaceholder(new Label(""));

        this.extIdCol.setCellValueFactory(o -> o.getValue().externalIdProperty());
        this.nameCol.setCellValueFactory(o -> o.getValue().nameProperty());
        this.stateCol.setCellValueFactory(o -> o.getValue().stateProperty());
        this.sourceCol.setCellValueFactory(o -> o.getValue().sourceProperty());
        this.triggerCol.setCellValueFactory(o -> o.getValue().triggerProperty());
        this.resourcesCol.setCellValueFactory(o -> o.getValue().resourcesProperty());
        this.durationCol.setCellValueFactory(o -> o.getValue().durationProperty());
        this.startTimeCol.setCellValueFactory(o -> o.getValue().startTimeProperty());
        this.endTimeCol.setCellValueFactory(o -> o.getValue().endTimeProperty());
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

        this.startTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
        this.endTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());

        this.stateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(SchedulingState item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case IGNORED:
                            setTextFill(Color.BLACK);
                            break;
                        case SCHEDULED:
                            setTextFill(Color.BLUE);
                            break;
                        case WAITING:
                            setTextFill(Color.LIGHTBLUE);
                            break;
                        case RUNNING:
                            setTextFill(Color.DARKCYAN);
                            break;
                        case FINISHED_NOMINAL:
                            setTextFill(Color.DARKGREEN);
                            break;
                        case FINISHED_FAIL:
                        case ABORTED:
                            setTextFill(Color.DARKRED);
                            break;
                        case DISABLED:
                            setTextFill(Color.GRAY);
                            break;
                        case UNKNOWN:
                            setTextFill(Color.DARKORANGE);
                            break;
                        default:
                            setTextFill(null);
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });

        this.dataItemTableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            public void updateItem(ScheduledActivityOccurrenceDataWrapper item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setStyle("");
                } else if (item.setLineProperty().get()) {
                    setStyle("-fx-background-color: lightgreen;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    protected void applyFilter(ScheduledActivityDataFilter selectedFilter) {
        this.dataItemFilterController.setSelectedFilter(selectedFilter);
        // Apply the filter on the current table
        if (selectedFilter != null && !selectedFilter.isClear()) {
            this.filteredList.predicateProperty().setValue(new FilterWrapper(selectedFilter));
        } else {
            this.filteredList.predicateProperty().setValue(p -> true);
        }
        if (this.liveTgl == null || this.liveTgl.isSelected()) {
            if (selectedFilter == null || selectedFilter.isClear()) {
                ReatmetricUI.threadPool(getClass()).execute(() -> {
                    try {
                        doServiceSubscribe(selectedFilter);
                        markFilterDeactivated();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                ReatmetricUI.threadPool(getClass()).execute(() -> {
                    try {
                        doServiceSubscribe(selectedFilter);
                        markFilterActivated();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            if (selectedFilter == null || selectedFilter.isClear()) {
                markFilterDeactivated();
            } else {
                markFilterActivated();
            }
            moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, MAX_ENTRIES, selectedFilter, false);
        }
    }

    private void markFilterDeactivated() {
        this.filterBtn.setStyle("");
    }

    private void markFilterActivated() {
        this.filterBtn.setStyle("-fx-background-color: -fx-faint-focus-color");
    }

    protected Consumer<List<ScheduledActivityData>> buildIncomingDataDelegatorAction() {
        return (List<ScheduledActivityData> t) -> addDataItems(t, true);
    }

    protected void addDataItems(List<ScheduledActivityData> messages, boolean fromLive) {
        Platform.runLater(() -> {
            if (!this.displayTitledPane.isDisabled() && (!fromLive || (this.liveTgl == null || this.liveTgl.isSelected()))) {
                boolean refreshLine = false;
                for (ScheduledActivityData aod : messages) {
                    refreshLine |= createOrUpdate(aod);
                }
                updateSelectTime();
                if (refreshLine) {
                    updateToBeExecutedLine(Instant.now(), true);
                }
            }
        });
    }

    protected void clearTable() {
        clearTimeBasedActivities();
        eventDataItemTableView.getItems().clear();
        dataItemTableView.layout();
        dataItemTableView.refresh();
        eventDataItemTableView.layout();
        eventDataItemTableView.refresh();
        updateSelectTime();
    }

    private boolean createOrUpdate(ScheduledActivityData aod) {
        boolean refreshTime = false;
        ScheduledActivityOccurrenceDataWrapper wrapper = activityMap.get(aod.getInternalId());
        if (wrapper == null) {
            wrapper = new ScheduledActivityOccurrenceDataWrapper(aod, aod.getRequest().getPath());
            activityMap.put(aod.getInternalId(), wrapper);
            if (aod.getTrigger() instanceof EventBasedSchedulingTrigger) {
                eventDataItemTableView.getItems().add(wrapper);
            } else {
                addToActivityList(wrapper);
            }
            refreshTime = true;
        } else if (aod.getState() == SchedulingState.REMOVED) {
            // Remove the wrapper from the table and map and return
            activityMap.remove(aod.getInternalId());
            if (aod.getTrigger() instanceof EventBasedSchedulingTrigger) {
                eventDataItemTableView.getItems().remove(wrapper);
            } else {
                removeFromActivityList(wrapper);
            }
            return true;
        }
        update(wrapper, aod);
        return refreshTime;
    }

    private void removeFromActivityList(ScheduledActivityOccurrenceDataWrapper wrapper) {
        timeScheduledActivityList.remove(wrapper);
        // Remove from series and maps
        removeFromChart(wrapper);
    }

    private void removeFromChart(ScheduledActivityOccurrenceDataWrapper wrapper) {
        XYChart.Series<Instant, String> series = activityPath2series.get(wrapper.getPath().asString());
        XYChart.Data<Instant, String> data = activity2data.remove(wrapper);
        if (data != null) {
            series.getData().remove(data);
            if (series.getData().isEmpty()) {
                activityPath2series.remove(wrapper.getPath().asString());
                ganttChart.getData().remove(series);
                ((CategoryAxis)ganttChart.getYAxis()).getCategories().remove(wrapper.getPath().asString());
            }
        }
    }

    private void removeCompletedActivities() {
        List<ScheduledActivityOccurrenceDataWrapper> toBeRemoved = timeScheduledActivityList.stream().filter(o -> o.stateProperty().get() != SchedulingState.RUNNING && o.stateProperty().get() != SchedulingState.WAITING && o.stateProperty().get() != SchedulingState.SCHEDULED).collect(Collectors.toList());
        timeScheduledActivityList.removeIf(o -> o.stateProperty().get() != SchedulingState.RUNNING && o.stateProperty().get() != SchedulingState.WAITING && o.stateProperty().get() != SchedulingState.SCHEDULED);
        for (ScheduledActivityOccurrenceDataWrapper aodw : toBeRemoved) {
            activityMap.remove(aodw.get().getInternalId());
            // Remove from series and maps
            removeFromChart(aodw);
        }
    }

    private void addToActivityList(ScheduledActivityOccurrenceDataWrapper wrapper) {
        timeScheduledActivityList.add(wrapper);
        // Create series if not existing
        XYChart.Series<Instant, String> series = activityPath2series.get(wrapper.getPath().asString());
        if(series == null) {
            series = new XYChart.Series<>();
            activityPath2series.put(wrapper.getPath().asString(), series);
            if(!((CategoryAxis)ganttChart.getYAxis()).getCategories().contains(wrapper.getPath().asString())) {
                ((CategoryAxis)ganttChart.getYAxis()).getCategories().add(wrapper.getPath().asString());
            }
            ganttChart.getData().add(series);
        }
        // Create data
        XYChart.Data<Instant, String> data = new XYChart.Data<>(wrapper.startTimeProperty().get(), wrapper.getPath().asString(), wrapper);
        // Add data to series
        series.getData().add(data);
        // Add data to map
        activity2data.put(wrapper, data);
    }

    private void clearTimeBasedActivities() {
        for (ScheduledActivityOccurrenceDataWrapper saod : timeScheduledActivityList) {
            activityMap.remove(saod.get().getInternalId());
            // Remove from series and maps
            removeFromChart(saod);
        }
        timeScheduledActivityList.clear();
    }

    private void update(ScheduledActivityOccurrenceDataWrapper wrapper, ScheduledActivityData aod) {
        AbstractSchedulingTrigger oldtrigger = wrapper.get().getTrigger();
        if (oldtrigger instanceof EventBasedSchedulingTrigger && !(aod.getTrigger() instanceof EventBasedSchedulingTrigger)) {
            // From event to time based
            eventDataItemTableView.getItems().remove(wrapper);
            wrapper.set(aod);
            addToActivityList(wrapper);
        } else if (!(oldtrigger instanceof EventBasedSchedulingTrigger) && aod.getTrigger() instanceof EventBasedSchedulingTrigger) {
            // From time based to event
            removeFromActivityList(wrapper);
            wrapper.set(aod);
            eventDataItemTableView.getItems().add(wrapper);
        } else {
            wrapper.set(aod);
            this.ganttChart.updateNode(activity2data.get(wrapper));
        }
    }

    @FXML
    protected void selectTimeButtonSelected(ActionEvent e) {
        if (this.dateTimePopup.isShowing()) {
            this.dateTimePopup.hide();
        } else {
            Bounds b = this.selectTimeBtn.localToScreen(this.selectTimeBtn.getBoundsInLocal());
            this.dateTimePopup.setX(b.getMinX());
            this.dateTimePopup.setY(b.getMaxY());
            this.dateTimePopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
            this.dateTimePopup.show(this.liveTgl.getScene().getWindow());
        }
        e.consume();
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
            clearTable();
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, MAX_ENTRIES, this.dataItemFilterController.getSelectedFilter(), true);
            startSubscription();
        } else {
            stopSubscription();
            updateSelectTime();
        }
        e.consume();
    }

    private int getNumVisibleRow() {
        double h = this.dataItemTableView.getHeight();
        h -= 30; // Header
        return (int) (h / this.dataItemTableView.getFixedCellSize()) + 1;
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if (isProcessingAvailable()) {
            // Yes, it is weird, this is to avoid retrieving event-based activities
            clearTimeBasedActivities();
            moveToTime(Instant.EPOCH.plusSeconds(3600), RetrievalDirection.TO_FUTURE, getNumVisibleRow(), this.dataItemFilterController.getSelectedFilter(), false);
        }
        e.consume();
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchRecords(1, RetrievalDirection.TO_PAST);
        }
        e.consume();
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchRecords(getNumVisibleRow(), RetrievalDirection.TO_PAST);
        }
        e.consume();
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (isProcessingAvailable()) {
            clearTimeBasedActivities();
            moveToTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), RetrievalDirection.TO_PAST, getNumVisibleRow(), this.dataItemFilterController.getSelectedFilter(), false);
        }
        e.consume();
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchRecords(1, RetrievalDirection.TO_FUTURE);
        }
        e.consume();
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchRecords(getNumVisibleRow(), RetrievalDirection.TO_FUTURE);
        }
        e.consume();
    }

    protected void fetchRecords(int n, RetrievalDirection direction) {
        // Get the first item in the table: if looking in the past, use the last item, if looking in the future, use the first item
        ScheduledActivityOccurrenceDataWrapper om = direction == RetrievalDirection.TO_FUTURE ? getFirst() : getLast();
        // No message: use the current time

        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // Complication coming from the fact that event-based events should not appear in historical retrieves: if you spot one, keep going
                List<ScheduledActivityData> messages;
                if (om != null) {
                    messages = doRetrieve(om.get(), n, direction, this.dataItemFilterController.getSelectedFilter());
                } else {
                    messages = doRetrieve(Instant.now(), n, direction, this.dataItemFilterController.getSelectedFilter());
                }
                addDataItems(messages, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private List<ScheduledActivityData> doRetrieve(ScheduledActivityData activityOccurrenceData, int n, RetrievalDirection direction, ScheduledActivityDataFilter selectedFilter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getScheduler().retrieve(activityOccurrenceData, n, direction,
                    selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    private List<ScheduledActivityData> doRetrieve(Instant time, int n, RetrievalDirection direction, ScheduledActivityDataFilter selectedFilter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getScheduler().retrieve(time, n, direction,
                    selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    private ScheduledActivityOccurrenceDataWrapper getFirst() {
        if (this.dataItemTableView.getItems().isEmpty()) {
            return null;
        }
        return this.dataItemTableView.getItems().get(0);
    }

    private ScheduledActivityOccurrenceDataWrapper getLast() {
        if (this.dataItemTableView.getItems().isEmpty()) {
            return null;
        }
        return this.dataItemTableView.getItems().get(this.dataItemTableView.getItems().size() - 1);
    }

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (this.filterPopup.isShowing()) {
            this.filterPopup.hide();
        } else {
            Bounds b = this.filterBtn.localToScreen(this.filterBtn.getBoundsInLocal());
            this.filterPopup.setX(b.getMinX());
            this.filterPopup.setY(b.getMaxY());
            this.filterPopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
            this.filterPopup.show(this.displayTitledPane.getScene().getWindow());
        }
        e.consume();
    }

    protected void moveToTime(Instant selectedTime, RetrievalDirection direction, int n, ScheduledActivityDataFilter currentFilter, boolean initialisation) {
        if (this.selectTimeBtn != null) {
            this.selectTimeBtn.setText(formatTime(selectedTime));
        }

        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ScheduledActivityData> messages = doRetrieve(selectedTime, n, direction, currentFilter);
                if (initialisation) {
                    Instant limit = selectedTime.minusSeconds(INITIALIZATION_SECONDS_IN_PAST);
                    addDataItems(messages.stream().filter(o -> o.getStartTime().isAfter(limit)).collect(Collectors.toList()), true);
                } else {
                    addDataItems(messages, this.liveTgl.isSelected());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void restoreColumnConfiguration() {
        if (this.system != null) {
            String name = null;
            try {
                name = this.system.getName();
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            }
            TableViewUtil.restoreColumnConfiguration(name, this.user, doGetComponentId(), this.dataItemTableView);
            TableViewUtil.restoreColumnConfiguration(name, this.user, doGetComponentId() + "_event", this.eventDataItemTableView);
        }
    }

    private void persistColumnConfiguration() {
        if (this.system != null) {
            String name = null;
            try {
                name = this.system.getName();
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            }
            TableViewUtil.persistColumnConfiguration(name, this.user, doGetComponentId(), this.dataItemTableView);
            TableViewUtil.persistColumnConfiguration(name, this.user, doGetComponentId() + "_event", this.eventDataItemTableView);
        }
    }

    protected final void startSubscription() {
        this.eventDataItemTableView.setDisable(false);
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            this.delegator.resume();
            try {
                doServiceSubscribe(getCurrentFilter());
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
        if (this.secondTicker == null) {
            this.secondTicker = new TimerTask() {
                @Override
                public void run() {
                    tick();
                }
            };
            this.timer.schedule(secondTicker, 1000, 10000);
        }
        this.ganttChart.setCurrentTimeMarker(true);
    }

    // This is not call by the UI thread!
    private void tick() {
        Platform.runLater(this::refreshTimeBasedState);
    }

    private void refreshTimeBasedState() {
        // Update the button time
        updateSelectTime();
        // Remove old entries
        removeOldEntries();
        Instant now = Instant.now();
        // Update next to be executed line in time-based schedule table to show a greenish background
        updateToBeExecutedLine(now, true);
        // Update chart location
        updateChartLocation(now);
    }

    private void updateChartLocation(Instant now) {
        ((InstantAxis) this.ganttChart.getXAxis()).setLowerBound(now.minusSeconds(GANTT_RANGE_PAST));
        ((InstantAxis) this.ganttChart.getXAxis()).setUpperBound(now.plusSeconds(GANTT_RANGE_AHEAD));
    }

    private void removeOldEntries() {
        Instant limit = Instant.now().minusSeconds(INITIALIZATION_SECONDS_IN_PAST);
        int start = this.sortedList.size() - 1;
        while (start >= 0) {
            if (this.sortedList.get(start).get().getStartTime().isBefore(limit)) {
                removeFromActivityList(this.sortedList.get(start));
                --start;
            } else {
                return;
            }
        }
    }

    private void updateToBeExecutedLine(Instant timeToMark, boolean mark) {
        boolean found = false;
        for (int i = sortedList.size(); i-- > 0; ) {
            ScheduledActivityOccurrenceDataWrapper wraps = sortedList.get(i);
            if (!mark) {
                wraps.setLineProperty().set(false);
            } else {
                if (wraps.get().getTrigger() instanceof EventBasedSchedulingTrigger) {
                    wraps.setLineProperty().set(false);
                } else if (wraps.get().getStartTime().isAfter(timeToMark)) {
                    if (!found) {
                        wraps.setLineProperty().set(true);
                        found = true;
                    } else {
                        wraps.setLineProperty().set(false);
                    }
                } else {
                    wraps.setLineProperty().set(false);
                }
            }
        }
        dataItemTableView.refresh();
    }

    protected ScheduledActivityDataFilter getCurrentFilter() {
        return this.dataItemFilterController != null ? this.dataItemFilterController.getSelectedFilter() : null;
    }

    protected final void stopSubscription() {
        this.ganttChart.setCurrentTimeMarker(false);
        if (this.secondTicker != null) {
            this.secondTicker.cancel();
            this.secondTicker = null;
        }
        updateToBeExecutedLine(null, false);
        this.eventDataItemTableView.setDisable(true);
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                doServiceUnsubscribe();
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
            this.delegator.suspend();
        });
    }

    protected void doServiceSubscribe(ScheduledActivityDataFilter selectedFilter) throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getScheduler().subscribe(this, selectedFilter);
            ReatmetricUI.selectedSystem().getSystem().getScheduler().subscribe(scheduleSubscriber);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getScheduler().unsubscribe(this);
            ReatmetricUI.selectedSystem().getSystem().getScheduler().unsubscribe(scheduleSubscriber);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public void dispose() {
        stopSubscription();
        super.dispose();
    }

    protected void updateSelectTime() {
        if (this.selectTimeBtn == null) {
            return;
        }
        // Take the first item from the table and use the generation time as value of the text
        if (this.liveTgl.isSelected()) {
            Instant now = Instant.now();
            this.selectTimeBtn.setText(formatTime(now));
            this.dateTimePickerController.setSelectedTime(now);
        } else {
            Instant latest = null;
            if (!this.dataItemTableView.getItems().isEmpty()) {
                latest = this.dataItemTableView.getItems().get(0).startTimeProperty().get();
            }
            if (latest == null) {
                this.selectTimeBtn.setText("---");
                this.dateTimePickerController.setSelectedTime(null);
            } else {
                this.selectTimeBtn.setText(formatTime(latest));
                this.dateTimePickerController.setSelectedTime(latest);
            }
        }
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        Platform.runLater(() -> this.progressIndicator.setVisible(false));
    }

    private boolean isProcessingAvailable() {
        return !this.progressIndicator.isVisible();
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        return TableViewUtil.buildNodeForPrinting(this.dataItemTableView);
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(false);
        this.displayTitledPane.setDisable(true);
        if (oldStatus) {
            persistColumnConfiguration();
        }
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(true);
        this.displayTitledPane.setDisable(false);
        // Restore column configuration
        restoreColumnConfiguration();
        // Start subscription if there
        if (this.liveTgl == null || this.liveTgl.isSelected()) {
            clearTable();
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, MAX_ENTRIES, this.dataItemFilterController.getSelectedFilter(), true);
            startSubscription();
        }
    }

    protected String doGetComponentId() {
        return "SchedulerView";
    }

    @FXML
    protected void onDragOver(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragExited(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        event.consume();
    }

    @FXML
    public void onRemoveMenuItem(ActionEvent event) {
        List<ScheduledActivityOccurrenceDataWrapper> selected = this.dataItemTableView.getSelectionModel().getSelectedItems();
        removeScheduledActivity(event, selected);
    }

    @FXML
    public void onEventRemoveMenuItem(ActionEvent event) {
        List<ScheduledActivityOccurrenceDataWrapper> selected = this.eventDataItemTableView.getSelectionModel().getSelectedItems();
        removeScheduledActivity(event, selected);
    }

    private void removeScheduledActivity(ActionEvent event, List<ScheduledActivityOccurrenceDataWrapper> selected) {
        boolean confirm = DialogUtils.confirm("Remove scheduled items", null, "If you continue, the selected scheduled items will be removed " +
                "from the scheduler. Do you want to remove the selected scheduled items?");
        if (!confirm) {
            return;
        }
        final List<IUniqueId> purgeList = new LinkedList<>();
        for (ScheduledActivityOccurrenceDataWrapper item : selected) {
            purgeList.add(item.get().getInternalId());
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            for (IUniqueId i : purgeList) {
                try {
                    ReatmetricUI.selectedSystem().getSystem().getScheduler().remove(i);
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Error when removing scheduled ID " + i, e);
                }
            }
        });
        event.consume();
    }

    @FXML
    public void removeCompletedActivitiesButtonSelected(ActionEvent event) {
        boolean confirm = DialogUtils.confirm("Remove terminated activity occurrences", null, "If you continue, the display will be cleared from the currently displayed terminated activity occurrences. " +
                "Do you want to continue?");
        if (!confirm) {
            return;
        }
        removeCompletedActivities();
        event.consume();
    }

    @FXML
    public void onModifyMenuItem(ActionEvent actionEvent) {
        ScheduledActivityOccurrenceDataWrapper selected = this.dataItemTableView.getSelectionModel().getSelectedItem();
        modifyScheduledActivity(selected);
    }

    @FXML
    public void onEventModifyMenuItem(ActionEvent actionEvent) {
        ScheduledActivityOccurrenceDataWrapper selected = this.eventDataItemTableView.getSelectionModel().getSelectedItem();
        modifyScheduledActivity(selected);
    }

    private void modifyScheduledActivity(ScheduledActivityOccurrenceDataWrapper selected) {
        if (selected == null) {
            return;
        }
        try {
            // If the activity is in SCHEDULED
            if (selected.get().getState() == SchedulingState.SCHEDULED) {
                // Get the descriptor
                AbstractSystemEntityDescriptor descriptor = ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().getDescriptorOf(selected.get().getRequest().getId());
                // Get the route list
                Supplier<List<ActivityRouteState>> routeList = () -> {
                    try {
                        return ReatmetricUI.selectedSystem().getSystem().getActivityExecutionService().getRouteAvailability(((ActivityDescriptor) descriptor).getActivityType());
                    } catch (ReatmetricException | RemoteException e) {
                        LOG.log(Level.WARNING, "Cannot retrieve the list of routes for activity type " + ((ActivityDescriptor) descriptor).getActivityType() + ": " + e.getMessage(), e);
                        return Collections.emptyList();
                    }
                };
                Pair<Node, ActivityInvocationDialogController> activityDialogPair = ActivityInvocationDialogUtil.createActivityInvocationDialog((ActivityDescriptor) descriptor, selected.get().getRequest(), routeList);
                activityDialogPair.getSecond().hideRouteControls();
                Pair<Node, ActivitySchedulingDialogController> scheduleDialogPair = ActivityInvocationDialogUtil.createActivitySchedulingDialog(buildRequestFromData(selected.get())); // To select the resources, scheduling source, triggering condition
                // Create the popup
                Dialog<ButtonType> d = new Dialog<>();
                d.setTitle("Schedule activity " + descriptor.getPath().getLastPathElement());
                d.initModality(Modality.APPLICATION_MODAL);
                d.initOwner(dataItemTableView.getScene().getWindow());
                d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

                Tab scheduleTab = new Tab("Schedule Information");
                scheduleTab.setContent(scheduleDialogPair.getFirst());
                Tab activityTab = new Tab("Activity Execution");
                activityTab.setContent(activityDialogPair.getFirst());
                TabPane innerTabPane = new TabPane(activityTab, scheduleTab);
                d.getDialogPane().setContent(innerTabPane);
                Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
                ok.disableProperty().bind(Bindings.or(activityDialogPair.getSecond().entriesValidProperty().not(), scheduleDialogPair.getSecond().entriesValidProperty().not()));
                Optional<ButtonType> result = d.showAndWait();
                if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                    updateScheduleActivity(selected.get().getInternalId(), activityDialogPair.getSecond(), scheduleDialogPair.getSecond());
                }
            }
        } catch (IOException | ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot complete the requested operation: " + e.getMessage(), e);
        }
    }

    private SchedulingRequest buildRequestFromData(ScheduledActivityData scheduledActivityData) {
        return new SchedulingRequest(scheduledActivityData.getRequest(), scheduledActivityData.getResources(), scheduledActivityData.getSource(), scheduledActivityData.getExternalId(), scheduledActivityData.getTrigger(), scheduledActivityData.getLatestInvocationTime(), scheduledActivityData.getConflictStrategy(), scheduledActivityData.getDuration());
    }

    private void updateScheduleActivity(IUniqueId originalId, ActivityInvocationDialogController actExec, ActivitySchedulingDialogController actScheduling) {
        ActivityRequest request = actExec.buildRequest();
        SchedulingRequest schedulingRequest = actScheduling.buildRequest(request);
        CreationConflictStrategy creationStrategy = actScheduling.getCreationStrategy();
        boolean confirm = DialogUtils.confirm("Update scheduled activity", actExec.getPath(), "Do you want to update the scheduling request " + originalId + "?");
        if (confirm) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    ReatmetricUI.selectedSystem().getSystem().getScheduler().update(originalId, schedulingRequest, creationStrategy);
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Cannot complete the requested operation: " + e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void dataItemsReceived(List<ScheduledActivityData> dataItems) {
        Platform.runLater(() -> delegator.delegate(dataItems));
    }

    public void internalSchedulerEnablementChanged(boolean enabled) {
        Platform.runLater(() -> {
            enableTgl.setSelected(enabled);
            enableTgl.setText(enabled ? "Disable" : "Enable");
        });
    }

    @FXML
    public void enableToggleSelected(ActionEvent event) {
        boolean enabled = enableTgl.isSelected();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                if (enabled) {
                    ReatmetricUI.selectedSystem().getSystem().getScheduler().enable();
                } else {
                    ReatmetricUI.selectedSystem().getSystem().getScheduler().disable();
                }
            } catch (ReatmetricException | RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void menuAboutToShow(WindowEvent windowEvent) {
        deleteScheduledMenuItem.setVisible(this.liveTgl.isSelected() && !dataItemTableView.getSelectionModel().getSelectedItems().isEmpty());
        editScheduledMenuItem.setVisible(this.liveTgl.isSelected() && dataItemTableView.getSelectionModel().getSelectedItems().size() == 1);
        eventDeleteScheduledMenuItem.setVisible(this.liveTgl.isSelected() && !eventDataItemTableView.getSelectionModel().getSelectedItems().isEmpty());
        eventEditScheduledMenuItem.setVisible(this.liveTgl.isSelected() && eventDataItemTableView.getSelectionModel().getSelectedItems().size() == 1);
        if (!this.liveTgl.isSelected()) {
            windowEvent.consume();
        }
    }

    @FXML
    public void updateTimeBoundariesButtonSelected(ActionEvent e) {
        if (this.ganttBoundariesPopup.isShowing()) {
            this.ganttBoundariesPopup.hide();
        } else {
            Bounds b = this.updateTimeBoundariesBtn.localToScreen(this.updateTimeBoundariesBtn.getBoundsInLocal());
            this.ganttTimeBoundariesPickerController.setInterval(GANTT_RANGE_PAST, GANTT_RANGE_AHEAD);
            this.ganttBoundariesPopup.setX(b.getMinX());
            this.ganttBoundariesPopup.setY(b.getMaxY());
            this.ganttBoundariesPopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
            this.ganttBoundariesPopup.show(this.liveTgl.getScene().getWindow());
        }
        e.consume();
    }

    public static class ScheduledActivityOccurrenceDataWrapper implements Comparable<ScheduledActivityOccurrenceDataWrapper> {

        private final SystemEntityPath path;
        private final SimpleObjectProperty<ScheduledActivityData> property = new SimpleObjectProperty<>();

        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleObjectProperty<Instant> startTime = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Instant> endTime = new SimpleObjectProperty<>();
        private final SimpleStringProperty resources = new SimpleStringProperty();
        private final SimpleStringProperty externalId = new SimpleStringProperty();
        private final SimpleStringProperty source = new SimpleStringProperty();
        private final SimpleStringProperty trigger = new SimpleStringProperty();
        private final SimpleStringProperty eventTrigger = new SimpleStringProperty();
        private final SimpleObjectProperty<SchedulingState> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Duration> duration = new SimpleObjectProperty<>();

        private final BooleanProperty setLine = new SimpleBooleanProperty(false);

        public ScheduledActivityOccurrenceDataWrapper(ScheduledActivityData data, SystemEntityPath path) {
            this.path = path;
            set(data);
        }

        public void set(ScheduledActivityData data) {
            property.set(data);
            startTime.set(data.getStartTime());
            endTime.set(data.getEndTime());
            resources.set(data.getResources().toString());
            source.set(data.getSource());
            trigger.set(data.getTrigger().toString());
            state.set(data.getState());
            duration.set(data.getDuration());
            externalId.set(String.valueOf(data.getExternalId()));
            name.set(data.getRequest().getPath().getLastPathElement());
            if (data.getTrigger() instanceof EventBasedSchedulingTrigger) {
                eventTrigger.set(String.valueOf(((EventBasedSchedulingTrigger) data.getTrigger()).getEvent()));
            } else {
                eventTrigger.set("");
            }
        }

        public ScheduledActivityData get() {
            return property.getValue();
        }

        public SimpleObjectProperty<Instant> startTimeProperty() {
            return startTime;
        }

        public SimpleObjectProperty<Instant> endTimeProperty() {
            return endTime;
        }

        public SimpleStringProperty resourcesProperty() {
            return resources;
        }

        public SimpleStringProperty triggerProperty() {
            return trigger;
        }

        public SimpleObjectProperty<SchedulingState> stateProperty() {
            return state;
        }

        public SimpleObjectProperty<Duration> durationProperty() {
            return duration;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty externalIdProperty() {
            return externalId;
        }

        public SystemEntityPath getPath() {
            return this.path;
        }

        public SimpleStringProperty sourceProperty() {
            return source;
        }

        public SimpleStringProperty eventTriggerProperty() {
            return eventTrigger;
        }

        public BooleanProperty setLineProperty() {
            return setLine;
        }

        @Override
        public int compareTo(ScheduledActivityOccurrenceDataWrapper o) {
            // Compare by time and, in case equal, compare by uniqueId
            int result = startTime.get().compareTo(o.startTime.get());
            if (result == 0) {
                result = (int) (property.get().getInternalId().asLong() - o.get().getInternalId().asLong());
            }
            return result;
        }
    }

    public static class FilterWrapper implements Predicate<ScheduledActivityOccurrenceDataWrapper> {

        private final ScheduledActivityDataFilter filter;

        public FilterWrapper(ScheduledActivityDataFilter selectedFilter) {
            this.filter = selectedFilter;
        }

        @Override
        public boolean test(ScheduledActivityOccurrenceDataWrapper activityOccurrenceDataWrapper) {
            return filter.test(activityOccurrenceDataWrapper.get());
        }
    }
}
