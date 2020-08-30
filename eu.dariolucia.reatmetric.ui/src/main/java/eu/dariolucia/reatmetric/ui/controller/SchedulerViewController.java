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
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.utils.TableViewUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class SchedulerViewController extends AbstractDisplayController implements IScheduledActivityDataSubscriber, ISchedulerSubscriber {

    protected static final int MAX_ENTRIES = 1000;

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

    // Table
    @FXML
    protected TableView<ScheduledActivityOccurrenceDataWrapper> dataItemTableView;

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

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Popup selector for filter
    protected final Popup filterPopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    // Filter controller
    protected IFilterController<ScheduledActivityDataFilter> dataItemFilterController;

    // Temporary object queue
    private DataProcessingDelegator<ScheduledActivityData> delegator;

    private final Map<IUniqueId, ScheduledActivityOccurrenceDataWrapper> activityMap = new HashMap<>();
    private FilteredList<ScheduledActivityOccurrenceDataWrapper> filteredList;
    private ObservableList<ScheduledActivityOccurrenceDataWrapper> originalList;
    private SortedList<ScheduledActivityOccurrenceDataWrapper> sortedList;

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.dataItemTableView.setPlaceholder(new Label(""));

        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());

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
                moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, MAX_ENTRIES * 2, this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                            setTextFill(Color.LAWNGREEN);
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

        this.originalList = FXCollections.observableList(FXCollections.observableArrayList(),
                data -> new Observable[] { data.startTimeProperty(), data.endTimeProperty(), data.durationProperty(), data.stateProperty(), data.nameProperty(), data.resourcesProperty(), data.triggerProperty() });
        this.filteredList = new FilteredList<>(this.originalList, o -> true);
        this.sortedList = new SortedList<>(filteredList, Comparator.naturalOrder());
        this.dataItemTableView.setItems(sortedList);

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
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
            moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, MAX_ENTRIES, selectedFilter);
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
                for (ScheduledActivityData aod : messages) {
                    createOrUpdate(aod);
                }
                updateSelectTime();
            }
        });
    }

    protected void clearTable() {
        originalList.clear();
        activityMap.clear();
        dataItemTableView.layout();
        dataItemTableView.refresh();
        updateSelectTime();
    }

    private void createOrUpdate(ScheduledActivityData aod) {
        ScheduledActivityOccurrenceDataWrapper wrapper = activityMap.get(aod.getInternalId());
        if (wrapper == null) {
            wrapper = new ScheduledActivityOccurrenceDataWrapper(aod, aod.getRequest().getPath());
            activityMap.put(aod.getInternalId(), wrapper);
            originalList.add(wrapper);
        } else if(aod.getState() == SchedulingState.REMOVED) {
            // Remove the wrapper from the table and map and return
            activityMap.remove(aod.getInternalId());
            originalList.remove(wrapper);
            return;
        }
        update(wrapper, aod);
    }

    private void update(ScheduledActivityOccurrenceDataWrapper wrapper, ScheduledActivityData aod) {
        wrapper.set(aod);
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
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, MAX_ENTRIES, this.dataItemFilterController.getSelectedFilter());
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
            moveToTime(Instant.EPOCH, RetrievalDirection.TO_FUTURE, 1, this.dataItemFilterController.getSelectedFilter());
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
            fetchRecords(MAX_ENTRIES, RetrievalDirection.TO_PAST);
        }
        e.consume();
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (isProcessingAvailable()) {
            moveToTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), RetrievalDirection.TO_PAST, MAX_ENTRIES * 2, this.dataItemFilterController.getSelectedFilter());
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
            fetchRecords(MAX_ENTRIES, RetrievalDirection.TO_FUTURE);
        }
        e.consume();
    }

    protected void fetchRecords(int n, RetrievalDirection direction) {
        if (dataItemTableView.getItems().isEmpty()) {
            return;
        }
        // Get the first message in the table
        ScheduledActivityOccurrenceDataWrapper om = direction == RetrievalDirection.TO_FUTURE ? getFirst() : getLast();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ScheduledActivityData> messages = doRetrieve(om.get(), n, direction, this.dataItemFilterController.getSelectedFilter());
                addDataItems(messages, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private List<ScheduledActivityData> doRetrieve(ScheduledActivityData activityOccurrenceData, int n, RetrievalDirection direction, ScheduledActivityDataFilter selectedFilter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getScheduler().retrieve(activityOccurrenceData, n, direction,
                selectedFilter);
    }

    private List<ScheduledActivityData> doRetrieve(Instant time, int n, RetrievalDirection direction, ScheduledActivityDataFilter selectedFilter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getScheduler().retrieve(time, n, direction,
                selectedFilter);
    }

    private ScheduledActivityOccurrenceDataWrapper getFirst() {
        return this.dataItemTableView.getItems().get(0);
    }

    private ScheduledActivityOccurrenceDataWrapper getLast() {
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

    protected void moveToTime(Instant selectedTime, RetrievalDirection direction, int n, ScheduledActivityDataFilter currentFilter) {
        if (this.selectTimeBtn != null) {
            this.selectTimeBtn.setText(formatTime(selectedTime));
        }

        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ScheduledActivityData> messages = doRetrieve(selectedTime, n, direction, currentFilter);
                addDataItems(messages, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void restoreColumnConfiguration() {
        if (this.system != null) {
            TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }

    private void persistColumnConfiguration() {
        if (this.system != null) {
            TableViewUtil.persistColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }

    protected final void startSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            this.delegator.resume();
            try {
                doServiceSubscribe(getCurrentFilter());
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    protected ScheduledActivityDataFilter getCurrentFilter() {
        return this.dataItemFilterController != null ? this.dataItemFilterController.getSelectedFilter() : null;
    }

    protected final void stopSubscription() {
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
        ReatmetricUI.selectedSystem().getSystem().getScheduler().subscribe(this, selectedFilter);
        ReatmetricUI.selectedSystem().getSystem().getScheduler().subscribe(this);
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getScheduler().unsubscribe((IScheduledActivityDataSubscriber) this);
        ReatmetricUI.selectedSystem().getSystem().getScheduler().unsubscribe((ISchedulerSubscriber) this);
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
        if (dataItemTableView.getItems().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            Instant latest = null;
            for (ScheduledActivityOccurrenceDataWrapper item : activityMap.values()) {
                if (latest == null) {
                    latest = item.startTimeProperty().get();
                } else {
                    if (latest.isBefore(item.startTimeProperty().get())) {
                        latest = item.startTimeProperty().get();
                    }
                }
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
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, MAX_ENTRIES, this.dataItemFilterController.getSelectedFilter());
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
                } catch (ReatmetricException e) {
                    e.printStackTrace();
                }
            }
        });
        event.consume();
    }

    @FXML
    public void removeButtonSelected(ActionEvent event) {
        boolean confirm = DialogUtils.confirm("Remove terminated activity occurrences", null, "If you continue, the display will be cleared from the currently displayed terminated activity occurrences. " +
                "Do you want to continue?");
        if (!confirm) {
            return;
        }
        originalList.removeIf(o -> o.stateProperty().get() != SchedulingState.RUNNING && o.stateProperty().get() != SchedulingState.WAITING && o.stateProperty().get() != SchedulingState.SCHEDULED);
        event.consume();
    }

    @Override
    public void dataItemsReceived(List<ScheduledActivityData> dataItems) {
        Platform.runLater(() -> delegator.delegate(dataItems));
    }

    @Override
    public void schedulerEnablementChanged(boolean enabled) {
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
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    public static class ScheduledActivityOccurrenceDataWrapper implements Comparable<ScheduledActivityOccurrenceDataWrapper> {

        private final SystemEntityPath path;
        private final SimpleObjectProperty<ScheduledActivityData> property = new SimpleObjectProperty<>();

        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleObjectProperty<Instant> startTime = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Instant> endTime = new SimpleObjectProperty<>();
        private final SimpleStringProperty resources = new SimpleStringProperty();
        private final SimpleStringProperty source = new SimpleStringProperty();
        private final SimpleStringProperty trigger = new SimpleStringProperty();
        private final SimpleObjectProperty<SchedulingState> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Duration> duration = new SimpleObjectProperty<>();

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
            name.set(data.getRequest().getPath().getLastPathElement());
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

        public SystemEntityPath getPath() {
            return this.path;
        }

        public SimpleStringProperty sourceProperty() {
            return source;
        }

        @Override
        public int compareTo(ScheduledActivityOccurrenceDataWrapper o) {
            // Compare by time and, in case equal, compare by uniqueId
            int result = startTime.get().compareTo(o.startTime.get());
            if(result == 0) {
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
