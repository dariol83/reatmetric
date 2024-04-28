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

import eu.dariolucia.jfx.timeline.Timeline;
import eu.dariolucia.jfx.timeline.model.TaskItem;
import eu.dariolucia.jfx.timeline.model.TaskLine;
import eu.dariolucia.jfx.timeline.model.TimeCursor;
import eu.dariolucia.jfx.timeline.model.TimeInterval;
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
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.*;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
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
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.paint.Color;
import javafx.stage.*;

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
 * @author dario
 */
public class SchedulerViewController extends AbstractDisplayController implements IScheduledActivityDataSubscriber {

    private static final Logger LOG = Logger.getLogger(SchedulerViewController.class.getName());

    private static final int MAX_ENTRIES = 1000;
    private static final int INITIALIZATION_SECONDS_IN_PAST = 7200;
    public static final String CSS_CLASS_X_REATMETRIC_SCHEDULER_LINE = "x-reatmetric-scheduler-line";
    public static final String SCHEDULER_VIEW_ID = "SchedulerView";
    public static final String TASK_BGCOLOR_PREFIX = "task.bgcolor.";
    public static final String TASK_TEXTCOLOR_PREFIX = "task.textcolor.";

    public static final String TIMELINE_BGCOLOR_PREFIX = "timeline.bgcolor";
    public static final String TIMELINE_PANEL_BGCOLOR_PREFIX = "timeline.panel.bgcolor";
    public static final String TIMELINE_PANEL_FGCOLOR_PREFIX = "timeline.panel.fgcolor";
    public static final String TIMELINE_PANEL_BORDERCOLOR_PREFIX = "timeline.panel.bordercolor";
    public static final String TIMELINE_HEADER_BGCOLOR_PREFIX = "timeline.header.bgcolor";
    public static final String TIMELINE_HEADER_FGCOLOR_PREFIX = "timeline.header.fgcolor";
    public static final String TIMELINE_HEADER_BORDERCOLOR_PREFIX = "timeline.header.bordercolor";
    public static final String TIMELINE_SELECTCOLOR_PREFIX = "timeline.select.color";
    public static final String TIMELINE_CURSORCOLOR_PREFIX = "timeline.cursor.color";
    public static final String TIMELINE_INTERVALCOLOR_PREFIX = "timeline.interval.color";

    // Not final, applicable for the entire UI
    private static int ganttRangePast = 3600;
    private static int ganttRangeAhead = 3600;
    private static int ganttRetrievalRange = 7200;

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;
    @FXML
    protected CheckMenuItem toggleShowToolbarItem;
    @FXML
    protected MenuItem detachMenuItem;
    @FXML
    protected ToolBar toolbar;

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
    protected Timeline ganttChart;

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
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventEnabledCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, Duration> eventDurationCol;
    @FXML
    private TableColumn<ScheduledActivityOccurrenceDataWrapper, String> eventParentCol;

    @FXML
    protected MenuItem eventEditScheduledMenuItem;
    @FXML
    protected MenuItem eventDeleteScheduledMenuItem;

    // Bot Table
    @FXML
    protected TableView<BotStateDataWrapper> botDataItemTableView;

    @FXML
    private TableColumn<BotStateDataWrapper, String> botNameCol;
    @FXML
    private TableColumn<BotStateDataWrapper, String> botStateCol;
    @FXML
    private TableColumn<BotStateDataWrapper, String> botEnabledCol;

    @FXML
    protected MenuItem botEnableMenuItem;
    @FXML
    protected MenuItem botDisableMenuItem;

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
    private final Map<String, BotStateDataWrapper> botStatesMap = new TreeMap<>();
    private final Timer timer = new Timer("Reatmetric UI - Scheduler time tracker");
    private volatile TimerTask secondTicker;

    // Gantt tracked activities
    private final Map<ScheduledActivityOccurrenceDataWrapper, TaskItem> activity2data = new HashMap<>();
    private final Map<String, TaskLine> activityPath2series = new HashMap<>();

    private final TimeCursor currentTimeCursor = new TimeCursor(Instant.now());
    private final TimeInterval currentTimeInterval = new TimeInterval(null, currentTimeCursor.getTime());

    private final Properties viewConfiguration = new Properties();
    private final Map<SchedulingState, Color> state2bgColor = new EnumMap<>(SchedulingState.class);
    private final Map<SchedulingState, Color> state2textColor = new EnumMap<>(SchedulingState.class);

    private final ISchedulerSubscriber scheduleSubscriber = new ISchedulerSubscriber() {
        @Override
        public void schedulerEnablementChanged(boolean enabled) {
            internalSchedulerEnablementChanged(enabled);
        }

        @Override
        public void botStateUpdated(List<BotStateData> botStates) {
            internalBotStateUpdated(botStates);
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

        setupBotTable();

        setupPropertyMapDefaults();

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

        // Bots
        this.botDataItemTableView.setItems(FXCollections.observableList(FXCollections.observableArrayList(),
                data -> new Observable[]{data.nameProperty(), data.stateNameProperty(), data.enabledProperty()}));

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());

        // Chart initialisation
        CssHandler.applyTo(this.ganttChart);
        Instant now = Instant.now();
        updateChartLocation(now.minusSeconds(ganttRangePast), now.plusSeconds(ganttRangeAhead));
        this.ganttChart.getSelectionModel().selectedItemProperty().addListener((e,o,n) -> taskSelected(n));
        this.currentTimeInterval.endTimeProperty().bind(currentTimeCursor.timeProperty());
        this.currentTimeInterval.setForeground(false);
        this.currentTimeInterval.setColor(Color.BEIGE.brighter());
        loadGanttBoundariesPopup();

        initialiseToolbarVisibility(displayTitledPane, toolbar, toggleShowToolbarItem);
    }

    private void setupPropertyMapDefaults() {
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.SCHEDULED, Color.BEIGE.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.RUNNING, Color.CYAN.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.WAITING, Color.CYAN.brighter().toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.ABORTED, Color.DARKRED.darker().toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.FINISHED_FAIL, Color.DARKRED.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.FINISHED_NOMINAL, Color.GREEN.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.DISABLED, Color.LIGHTGRAY.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.REMOVED, Color.LIGHTGRAY.brighter().toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.IGNORED, Color.LIGHTYELLOW.toString());
        this.viewConfiguration.put(TASK_BGCOLOR_PREFIX + SchedulingState.UNKNOWN, Color.SALMON.toString());

        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.SCHEDULED, Color.BLACK.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.RUNNING, Color.BLACK.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.WAITING, Color.BLACK.brighter().toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.ABORTED, Color.WHITE.darker().toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.FINISHED_FAIL, Color.WHITE.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.FINISHED_NOMINAL, Color.BLACK.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.DISABLED, Color.BLACK.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.REMOVED, Color.BLACK.brighter().toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.IGNORED, Color.BLACK.toString());
        this.viewConfiguration.put(TASK_TEXTCOLOR_PREFIX + SchedulingState.UNKNOWN, Color.BLACK.toString());

        this.viewConfiguration.put(TIMELINE_BGCOLOR_PREFIX, Color.WHITE.toString());
        this.viewConfiguration.put(TIMELINE_PANEL_BGCOLOR_PREFIX, Color.LIGHTGRAY.toString());
        this.viewConfiguration.put(TIMELINE_PANEL_FGCOLOR_PREFIX, Color.BLACK.toString());
        this.viewConfiguration.put(TIMELINE_PANEL_BORDERCOLOR_PREFIX, Color.LIGHTGRAY.darker().toString());
        this.viewConfiguration.put(TIMELINE_HEADER_BGCOLOR_PREFIX, Color.LIGHTGRAY.toString());
        this.viewConfiguration.put(TIMELINE_HEADER_FGCOLOR_PREFIX, Color.BLACK.toString());
        this.viewConfiguration.put(TIMELINE_HEADER_BORDERCOLOR_PREFIX, Color.LIGHTGRAY.darker().toString());
        this.viewConfiguration.put(TIMELINE_SELECTCOLOR_PREFIX, Color.BLACK.toString());
        this.viewConfiguration.put(TIMELINE_CURSORCOLOR_PREFIX, Color.BLACK.toString());
        this.viewConfiguration.put(TIMELINE_INTERVALCOLOR_PREFIX, Color.BEIGE.brighter().brighter().toString());

        initializeColorMaps();
    }

    private void initializeColorMaps() {
        for(SchedulingState ss : SchedulingState.values()) {
            this.state2bgColor.put(ss, Color.valueOf(this.viewConfiguration.getProperty(TASK_BGCOLOR_PREFIX + ss, Color.BEIGE.toString())));
            this.state2textColor.put(ss, Color.valueOf(this.viewConfiguration.getProperty(TASK_TEXTCOLOR_PREFIX + ss, Color.BLACK.toString())));
        }

        this.ganttChart.setBackgroundColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_BGCOLOR_PREFIX)));
        this.ganttChart.setPanelBackground(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_PANEL_BGCOLOR_PREFIX)));
        this.ganttChart.setPanelForegroundColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_PANEL_FGCOLOR_PREFIX)));
        this.ganttChart.setPanelBorderColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_PANEL_BORDERCOLOR_PREFIX)));
        this.ganttChart.setHeaderBackground(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_HEADER_BGCOLOR_PREFIX)));
        this.ganttChart.setHeaderForegroundColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_HEADER_FGCOLOR_PREFIX)));
        this.ganttChart.setHeaderBorderColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_HEADER_BORDERCOLOR_PREFIX)));
        this.ganttChart.setSelectBorderColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_SELECTCOLOR_PREFIX)));
        this.currentTimeCursor.setColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_CURSORCOLOR_PREFIX)));
        this.currentTimeInterval.setColor(Color.valueOf(this.viewConfiguration.getProperty(TIMELINE_INTERVALCOLOR_PREFIX)));
    }

    @FXML
    private void detachAttachItemAction(ActionEvent actionEvent) {
        if(DetachedTabUtil.isDetached((Stage) displayTitledPane.getScene().getWindow())) {
            DetachedTabUtil.attachTab((Stage) displayTitledPane.getScene().getWindow());
            informDisplayAttached();
        }
    }

    @Override
    protected void informDisplayAttached() {
        detachMenuItem.setDisable(true);
    }

    @Override
    protected void informDisplayDetached() {
        detachMenuItem.setDisable(false);
    }

    private void taskSelected(TaskItem item) {
        if(item == null) {
            this.dataItemTableView.getSelectionModel().clearSelection();
        } else {
            ScheduledActivityOccurrenceDataWrapper w = (ScheduledActivityOccurrenceDataWrapper) item.getUserData();
            this.dataItemTableView.getSelectionModel().select(w);
        }
    }

    private void loadFilterPopup() {
        this.filterPopup.setAutoHide(true);
        this.filterPopup.setHideOnEscape(true);

        try {
            URL filterWidgetUrl = getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/SchedulerFilterWidget.fxml");
            FXMLLoader loader = new FXMLLoader(filterWidgetUrl);
            Parent filterSelector = loader.load();
            CssHandler.applyTo(filterSelector);
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
            CssHandler.applyTo(dateTimePicker);
            this.dateTimePickerController = loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                Instant time = this.dateTimePickerController.getSelectedTime();
                // If you want to move to time, first you clear the table
                fetchAtTime(time, RetrievalDirection.TO_PAST);
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
            CssHandler.applyTo(dateTimePicker);
            this.ganttTimeBoundariesPickerController = loader.getController();
            this.ganttBoundariesPopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.ganttTimeBoundariesPickerController.setActionAfterSelection(() -> {
                this.ganttBoundariesPopup.hide();
                ganttRangePast = ganttTimeBoundariesPickerController.getPastDuration();
                ganttRangeAhead = ganttTimeBoundariesPickerController.getFutureDuration();
                ganttRetrievalRange = ganttTimeBoundariesPickerController.getRetrievalDuration();
                if(this.liveTgl.isSelected()) {
                    Instant now = Instant.now();
                    updateChartLocation(now.minusSeconds(ganttRangePast), now.plusSeconds(ganttRangeAhead));
                } else {
                    // Take the max and extend back
                    fetchTimeInterval(ganttChart.getMaxTime().minusSeconds(ganttRetrievalRange), ganttChart.getMaxTime());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupBotTable() {
        this.botDataItemTableView.setPlaceholder(new Label(""));

        this.botStateCol.setCellValueFactory(o -> o.getValue().stateNameProperty());
        this.botNameCol.setCellValueFactory(o -> o.getValue().nameProperty());
        this.botEnabledCol.setCellValueFactory(o -> o.getValue().enabledProperty());
    }

    private void setupEventTriggersTable() {
        this.eventDataItemTableView.setPlaceholder(new Label(""));

        this.eventExtIdCol.setCellValueFactory(o -> o.getValue().externalIdProperty());
        this.eventNameCol.setCellValueFactory(o -> o.getValue().nameProperty());
        this.eventSourceCol.setCellValueFactory(o -> o.getValue().sourceProperty());
        this.eventTriggerCol.setCellValueFactory(o -> o.getValue().eventTriggerProperty());
        this.eventEnabledCol.setCellValueFactory(o -> o.getValue().eventEnabledProperty());
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
                    setTextFill(stateToBgColor(item));
                } else {
                    setText("");
                    setGraphic(null);
                    setTextFill(null);
                }
            }
        });

        this.dataItemTableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            public void updateItem(ScheduledActivityOccurrenceDataWrapper item, boolean empty) {
                super.updateItem(item, empty);
                this.getStyleClass().remove(CSS_CLASS_X_REATMETRIC_SCHEDULER_LINE);
                if (item != null && item.setLineProperty().get()) {
                    this.getStyleClass().add(CSS_CLASS_X_REATMETRIC_SCHEDULER_LINE);
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
            registerFilterActivation(selectedFilter, selectedFilter != null && !selectedFilter.isClear());
        } else {
            markFilterActivation(selectedFilter != null && !selectedFilter.isClear());
            fetchAtTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST);
        }
    }

    private void registerFilterActivation(ScheduledActivityDataFilter selectedFilter, boolean activated) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                doServiceSubscribe(selectedFilter);
                Platform.runLater(() -> {
                    markFilterActivation(activated);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void markFilterActivation(boolean activated) {
        this.filterBtn.setStyle(activated ? "-fx-background-color: -fx-faint-focus-color" : "");
    }

    protected Consumer<List<ScheduledActivityData>> buildIncomingDataDelegatorAction() {
        return this::addDataItems;
    }

    protected void addDataItems(List<ScheduledActivityData> messages) {
        FxUtils.runLater(() -> {
            if (!this.displayTitledPane.isDisabled() && ((this.liveTgl == null || this.liveTgl.isSelected()))) {
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
        activityMap.remove(wrapper.get().getInternalId());
        // Remove from series and maps
        removeFromChart(wrapper);
    }

    private void removeFromChart(ScheduledActivityOccurrenceDataWrapper wrapper) {
        TaskLine series = activityPath2series.get(wrapper.getPath().asString());
        TaskItem data = activity2data.remove(wrapper);
        if (data != null) {
            series.getItems().remove(data);
            if (series.getItems().isEmpty()) {
                activityPath2series.remove(wrapper.getPath().asString());
                ganttChart.getItems().remove(series);
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
        // Create task line if not existing
        TaskLine series = activityPath2series.get(wrapper.getPath().asString());
        if(series == null) {
            series = new TaskLine(wrapper.getPath().asString());
            activityPath2series.put(wrapper.getPath().asString(), series);
            ganttChart.getItems().add(series);
        }
        // Create data
        TaskItem data = new TaskItem(wrapper.getPath().getLastPathElement(), wrapper.startTimeProperty().get(), wrapper.durationProperty().get().toSeconds());
        data.setUserData(wrapper);
        data.startTimeProperty().bind(wrapper.startTimeProperty());
        data.expectedDurationProperty().bind(wrapper.durationProperty().map(Duration::toSeconds));
        data.actualDurationProperty().bind(wrapper.actualDurationProperty().map(Duration::getSeconds));
        data.taskBackgroundProperty().bind(wrapper.stateProperty().map(this::stateToBgColor));
        data.taskTextColorProperty().bind(wrapper.stateProperty().map(this::stateToTextColor));
        // Add data to series
        series.getItems().add(data);
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
            // TaskItem updated based on property bind
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
            CssHandler.applyTo(this.dateTimePopup.getScene().getRoot());
            this.dateTimePopup.show(this.liveTgl.getScene().getWindow());
        }
        e.consume();
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        ganttTimeBoundariesPickerController.setLive(this.liveTgl.isSelected());
        if (this.liveTgl.isSelected()) {
            clearTable();
            initialiseDisplayFromTime(Instant.now(), this.dataItemFilterController.getSelectedFilter());
            startSubscription();
        } else {
            stopSubscription();
            updateSelectTime();
        }
        e.consume();
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchAtTime(Instant.EPOCH.plusSeconds(3600), RetrievalDirection.TO_FUTURE);
        }
        e.consume();
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if (isProcessingAvailable()) {
            // Interval
            Duration delta = Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()).dividedBy(10);
            fetchTimeInterval(ganttChart.getMinTime().minus(delta), ganttChart.getMaxTime().minus(delta));
        }
        e.consume();
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if (isProcessingAvailable()) {
            // Interval
            Duration delta = Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime());
            fetchTimeInterval(ganttChart.getMinTime().minus(delta), ganttChart.getMaxTime().minus(delta));
        }
        e.consume();
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (isProcessingAvailable()) {
            fetchAtTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), RetrievalDirection.TO_PAST);
        }
        e.consume();
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if (isProcessingAvailable()) {
            // Interval
            Duration delta = Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()).dividedBy(10);
            fetchTimeInterval(ganttChart.getMinTime().plus(delta), ganttChart.getMaxTime().plus(delta));
        }
        e.consume();
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if (isProcessingAvailable()) {
            // Interval
            Duration delta = Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime());
            fetchTimeInterval(ganttChart.getMinTime().plus(delta), ganttChart.getMaxTime().plus(delta));
        }
        e.consume();
    }

    /**
     * This method fetches all scheduled activities in the provided time interval and renders them in the Gantt chart
     * and in the table.
     *
     * @param min start time
     * @param max end time
     */
    private void fetchTimeInterval(Instant min, Instant max) {
        markProgressBusy();
        ScheduledActivityDataFilter filter = this.dataItemFilterController.getSelectedFilter();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // Retrieve everything in the interval
                List<ScheduledActivityData> messages = doRetrieve(min, max, filter);
                // Remove event-based triggers
                messages.removeIf(scheduledActivityData -> scheduledActivityData.getTrigger() instanceof EventBasedSchedulingTrigger);
                // Add retrieved elements to the table
                FxUtils.runLater(() -> {
                    renderExtractedScheduledActivities(min, max, messages);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void renderExtractedScheduledActivities(Instant min, Instant max, List<ScheduledActivityData> messages) {
        if (!this.displayTitledPane.isDisabled()) {
            // Move chart
            updateChartLocation(min, max);
            // Add the elements
            for (ScheduledActivityData aod : messages) {
                createOrUpdate(aod);
            }
            // Remove the elements falling outside the boundaries: endTime < min || startTime > max
            List<ScheduledActivityOccurrenceDataWrapper> toRemove = dataItemTableView.getItems().stream()
                    .filter(o -> o.get().getStartTime().isAfter(max) || o.get().getEndTime().isBefore(min))
                    .collect(Collectors.toList());
            for(ScheduledActivityOccurrenceDataWrapper toRem : toRemove) {
                removeFromActivityList(toRem);
            }
            // Update the time
            updateSelectTime();
        }
    }

    private List<ScheduledActivityData> doRetrieve(Instant min, Instant max, ScheduledActivityDataFilter filter) throws ReatmetricException {
        List<ScheduledActivityData> messages;
        try {
            messages = ReatmetricUI.selectedSystem().getSystem().getScheduler().retrieve(min, max, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
        return messages;
    }

    /**
     * Used by go to start and go to end methods.
     *
     * @param time start time
     * @param direction direction of retrieval
     */
    private void fetchAtTime(Instant time, RetrievalDirection direction) {
        markProgressBusy();
        ScheduledActivityDataFilter filter = this.dataItemFilterController.getSelectedFilter();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // First we retrieve with the provided time and direction
                List<ScheduledActivityData> messages = doRetrieve(time, 1000, direction, filter);
                // Remove event-based triggers
                messages.removeIf(scheduledActivityData -> scheduledActivityData.getTrigger() instanceof EventBasedSchedulingTrigger);
                // Add retrieved elements to the table
                FxUtils.runLater(() -> {
                    renderExtractedScheduledActivities(time, direction, messages);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void renderExtractedScheduledActivities(Instant time, RetrievalDirection direction, List<ScheduledActivityData> messages) {
        if (!this.displayTitledPane.isDisabled()) {
            clearTimeBasedActivities();
            if(!messages.isEmpty()) {
                // Depending on the direction: TO_FUTURE -> min time is first occurrence start time, max time is min time plus configured window
                // Depending on the direction: TO_PAST -> max time is first occurrence end time, min time is max time minus configured window
                Instant min;
                Instant max;
                if (direction == RetrievalDirection.TO_FUTURE) {
                    min = messages.get(0).getStartTime();
                    max = min.plus(Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()));
                } else {
                    max = messages.get(0).getEndTime();
                    min = max.minus(Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()));
                }
                // Move chart
                updateChartLocation(min, max);
                // Add the elements
                for (ScheduledActivityData aod : messages) {
                    createOrUpdate(aod);
                }
                // Remove the elements falling outside the boundaries: endTime < min || startTime > max
                List<ScheduledActivityOccurrenceDataWrapper> toRemove = dataItemTableView.getItems().stream()
                        .filter(o -> o.get().getStartTime().isAfter(max) || o.get().getEndTime().isBefore(min))
                        .collect(Collectors.toList());
                for (ScheduledActivityOccurrenceDataWrapper toRem : toRemove) {
                    removeFromActivityList(toRem);
                }
                // Update the time
                updateSelectTime();
            } else {
                // Nothing... put empty time period
                Instant min;
                Instant max;
                if (direction == RetrievalDirection.TO_FUTURE) {
                    min = time;
                    max = min.plus(Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()));
                } else {
                    max = time;
                    min = max.minus(Duration.between(ganttChart.getMinTime(), ganttChart.getMaxTime()));
                }
                // Move chart
                updateChartLocation(min, max);
            }
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

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (this.filterPopup.isShowing()) {
            this.filterPopup.hide();
        } else {
            Bounds b = this.filterBtn.localToScreen(this.filterBtn.getBoundsInLocal());
            this.filterPopup.setX(b.getMinX());
            this.filterPopup.setY(b.getMaxY());
            CssHandler.applyTo(this.filterPopup.getScene().getRoot());
            this.filterPopup.show(this.displayTitledPane.getScene().getWindow());
        }
        e.consume();
    }

    protected void initialiseDisplayFromTime(Instant selectedTime, ScheduledActivityDataFilter currentFilter) {
        if (this.selectTimeBtn != null) {
            this.selectTimeBtn.setText(formatTime(selectedTime));
        }

        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ScheduledActivityData> messages = doRetrieve(selectedTime, SchedulerViewController.MAX_ENTRIES, RetrievalDirection.TO_PAST, currentFilter);
                Instant limit = selectedTime.minusSeconds(INITIALIZATION_SECONDS_IN_PAST);
                addDataItems(messages.stream().filter(o -> o.getStartTime().isAfter(limit)).collect(Collectors.toList()));
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
        activateCurrentTimeMarker();

        Instant now = Instant.now();
        updateChartLocation(now.minusSeconds(ganttRangePast), now.plusSeconds(ganttRangeAhead));
    }

    private void activateCurrentTimeMarker() {
        if(currentTimeCursor.getTimeline() == null) {
            ganttChart.getTimeCursors().add(currentTimeCursor);
            ganttChart.getTimeIntervals().add(currentTimeInterval);
        }
    }

    private void deactivateCurrentTimeMarker() {
        if(currentTimeCursor.getTimeline() != null) {
            ganttChart.getTimeCursors().remove(currentTimeCursor);
            ganttChart.getTimeIntervals().remove(currentTimeInterval);
        }
    }

    // This is not call by the UI thread!
    private void tick() {
        FxUtils.runLater(this::refreshTimeBasedState);
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
        updateChartLocation(now.minusSeconds(ganttRangePast), now.plusSeconds(ganttRangeAhead));
        // Update current time cursor
        currentTimeCursor.setTime(now);
        // Update progress for all running tasks
        updateRunningTaskProgress(now);
    }

    private void updateRunningTaskProgress(Instant now) {
        for(ScheduledActivityOccurrenceDataWrapper t : this.activity2data.keySet()) {
            t.updateActualDurationWithTime(now);
        }
    }

    private void updateChartLocation(Instant past, Instant future) {
        this.ganttChart.setMinTime(past);
        this.ganttChart.setMaxTime(future);
        this.ganttChart.setViewPortStart(past);
        this.ganttChart.setViewPortDuration(Duration.between(past, future).toSeconds());
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
        deactivateCurrentTimeMarker();
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
        this.delegator.shutdown();
        super.dispose();
    }

    protected void updateSelectTime() {
        if (this.selectTimeBtn == null) {
            return;
        }

        if (this.liveTgl.isSelected()) {
            // If live, current time
            Instant now = Instant.now();
            this.selectTimeBtn.setText(formatTime(now));
            this.dateTimePickerController.setSelectedTime(now);
        } else {
            // If not live, take the max time on the chart
            this.selectTimeBtn.setText(formatTime(this.ganttChart.getMaxTime()));
            this.dateTimePickerController.setSelectedTime(this.ganttChart.getMaxTime());
        }
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        FxUtils.runLater(() -> this.progressIndicator.setVisible(false));
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
            persistViewConfiguration();
        }
        // Clear bot data
        botStatesMap.clear();
        botDataItemTableView.getItems().clear();
    }

    private void persistViewConfiguration() {
        if (this.system != null) {
            storeViewConfiguration(doGetComponentId() + "_prefs", this.viewConfiguration);
        }
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(true);
        this.displayTitledPane.setDisable(false);
        // Restore view configuration
        restoreViewConfiguration();
        // Restore column configuration
        restoreColumnConfiguration();
        // Start subscription if there
        if (this.liveTgl == null || this.liveTgl.isSelected()) {
            clearTable();
            initialiseDisplayFromTime(Instant.now(), this.dataItemFilterController.getSelectedFilter());
            startSubscription();
        }
    }

    private void restoreViewConfiguration() {
        if(this.system != null) {
            Properties viewProps = loadViewConfiguration(doGetComponentId() + "_prefs");
            if (viewProps == null) {
                storeViewConfiguration(doGetComponentId() + "_prefs", this.viewConfiguration);
            } else {
                this.viewConfiguration.putAll(viewProps);
            }
            initializeColorMaps();
        }
    }

    protected String doGetComponentId() {
        return SCHEDULER_VIEW_ID;
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
    public void onBotEnableMenuItem(ActionEvent actionEvent) {
        BotStateDataWrapper selected = this.botDataItemTableView.getSelectionModel().getSelectedItem();
        setBotEnable(selected, true);
    }

    @FXML
    public void onBotDisableMenuItem(ActionEvent actionEvent) {
        BotStateDataWrapper selected = this.botDataItemTableView.getSelectionModel().getSelectedItem();
        setBotEnable(selected, false);
    }

    private void setBotEnable(BotStateDataWrapper selected, boolean enablement) {
        boolean confirm = DialogUtils.confirm((enablement ? "Enable" : "Disable") + " bot " + selected.getName(), null,
                "Request to " + (enablement ? "enable" : "disable") + " bot " + selected.getName() + ". Do you want to continue?");
        if(confirm) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    if(enablement) {
                        ReatmetricUI.selectedSystem().getSystem().getScheduler().enableBot(selected.getName());
                    } else {
                        ReatmetricUI.selectedSystem().getSystem().getScheduler().disableBot(selected.getName());
                    }
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Cannot complete the requested operation: " + e.getMessage(), e);
                }
            });
        }
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
                AbstractSystemEntityDescriptor descriptor = SystemEntityResolver.getResolver().getDescriptorOf(selected.get().getRequest().getId());
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
        FxUtils.runLater(() -> delegator.delegate(dataItems));
    }

    public void internalSchedulerEnablementChanged(boolean enabled) {
        FxUtils.runLater(() -> {
            enableTgl.setSelected(enabled);
            enableTgl.setText(enabled ? "Disable" : "Enable");
            ganttChart.setBackgroundColor(enabled ? Color.WHITE : Color.LIGHTGRAY);
            currentTimeInterval.setColor(enabled ? Color.BEIGE.brighter() : Color.LIGHTGRAY);
        });
    }

    private void internalBotStateUpdated(List<BotStateData> botStates) {
        FxUtils.runLater(() -> {
            for(BotStateData bsd : botStates) {
                BotStateDataWrapper bsdw = botStatesMap.get(bsd.getName());
                if(bsdw == null) {
                    bsdw = new BotStateDataWrapper(bsd);
                    botStatesMap.put(bsd.getName(), bsdw);
                    botDataItemTableView.getItems().add(bsdw);
                }
                bsdw.set(bsd);
            }
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
        botEnableMenuItem.setVisible(botDataItemTableView.getSelectionModel().getSelectedItems().size() == 1 &&
                !botDataItemTableView.getSelectionModel().getSelectedItem().getProperty().isEnabled());
        botDisableMenuItem.setVisible(botDataItemTableView.getSelectionModel().getSelectedItems().size() == 1 &&
                botDataItemTableView.getSelectionModel().getSelectedItem().getProperty().isEnabled());
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
            this.ganttTimeBoundariesPickerController.setInterval(ganttRangePast, ganttRangeAhead, ganttRetrievalRange);
            this.ganttBoundariesPopup.setX(b.getMinX());
            this.ganttBoundariesPopup.setY(b.getMaxY());
            CssHandler.applyTo(this.ganttBoundariesPopup.getScene().getRoot());
            this.ganttBoundariesPopup.show(this.liveTgl.getScene().getWindow());
        }
        e.consume();
    }

    public Color stateToBgColor(SchedulingState schedulingState) {
        return this.state2bgColor.get(schedulingState);
    }

    public Color stateToTextColor(SchedulingState schedulingState) {
        return this.state2textColor.get(schedulingState);
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
        private final SimpleStringProperty eventEnabled = new SimpleStringProperty();
        private final SimpleObjectProperty<SchedulingState> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Duration> duration = new SimpleObjectProperty<>();

        private final SimpleObjectProperty<Duration> actualDuration = new SimpleObjectProperty<>(Duration.ZERO);

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
                eventEnabled.set(((EventBasedSchedulingTrigger) data.getTrigger()).isEnabled() ? "enabled" : "disabled");
            } else {
                eventEnabled.set("N/A");
                eventTrigger.set("");
            }
            switch(data.getState()) {
                case SCHEDULED:
                case IGNORED:
                case DISABLED:
                case WAITING:
                case REMOVED:
                    actualDuration.setValue(Duration.ZERO);
                    break;
                case RUNNING:
                    actualDuration.setValue(Duration.between(data.getStartTime(), Instant.now()));
                    break;
                case FINISHED_FAIL:
                case FINISHED_NOMINAL:
                case ABORTED:
                    actualDuration.setValue(data.getDuration());
                    break;
                case UNKNOWN:
                    // No change
                    break;
            }
        }

        public void updateActualDurationWithTime(Instant currentTime) {
            if(state.get() == SchedulingState.RUNNING) {
                actualDuration.setValue(Duration.between(startTime.get(), Instant.now()));
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

        public SimpleStringProperty eventEnabledProperty() {
            return eventEnabled;
        }

        public BooleanProperty setLineProperty() {
            return setLine;
        }

        public SimpleObjectProperty<Duration> actualDurationProperty() {
            return actualDuration;
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

    public static class BotStateDataWrapper {

        private final SimpleObjectProperty<BotStateData> property = new SimpleObjectProperty<>();

        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty stateName = new SimpleStringProperty();
        private final SimpleStringProperty enabled = new SimpleStringProperty();

        public BotStateDataWrapper(BotStateData data) {
            set(data);
        }

        public void set(BotStateData data) {
            property.set(data);
            name.set(data.getName());
            stateName.set(data.getStateName());
            enabled.set(data.isEnabled() ? "Enabled" : "Disabled");
        }

        public BotStateData getProperty() {
            return property.get();
        }

        public SimpleObjectProperty<BotStateData> propertyProperty() {
            return property;
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getStateName() {
            return stateName.get();
        }

        public SimpleStringProperty stateNameProperty() {
            return stateName;
        }

        public String getEnabled() {
            return enabled.get();
        }

        public SimpleStringProperty enabledProperty() {
            return enabled;
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
