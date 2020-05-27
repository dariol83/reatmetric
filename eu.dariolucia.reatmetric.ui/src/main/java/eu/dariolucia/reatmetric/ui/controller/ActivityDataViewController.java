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
import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ActivityDataViewController extends AbstractDisplayController implements IActivityOccurrenceDataSubscriber {

    protected static final int MAX_ENTRIES = 100;

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

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Table
    @FXML
    protected TreeTableView<ActivityOccurrenceDataWrapper> dataItemTableView;

    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> nameCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, ActivityOccurrenceState> stateCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> statusCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> sourceCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> typeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> routeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Object> resultCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Instant> genTimeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Instant> execTimeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> parentCol;

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Popup selector for filter
    protected final Popup filterPopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    // Filter controller
    protected IFilterController<ActivityOccurrenceDataFilter> dataItemFilterController;

    // Temporary object queue
    private DataProcessingDelegator<ActivityOccurrenceData> delegator;

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
                moveToTime(this.dateTimePickerController.getSelectedTime());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.filterPopup.setAutoHide(true);
        this.filterPopup.setHideOnEscape(true);

        try {
            URL filterWidgetUrl = getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/ActivityOccurrenceDataFilterWidget.fxml");
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

        this.nameCol.setCellValueFactory(o -> o.getValue().getValue().nameProperty());
        this.stateCol.setCellValueFactory(o -> o.getValue().getValue().stateProperty());
        this.statusCol.setCellValueFactory(o -> o.getValue().getValue().statusProperty());
        this.sourceCol.setCellValueFactory(o -> o.getValue().getValue().sourceProperty());
        this.routeCol.setCellValueFactory(o -> o.getValue().getValue().routeProperty());
        this.typeCol.setCellValueFactory(o -> o.getValue().getValue().typeProperty());
        this.resultCol.setCellValueFactory(o -> o.getValue().getValue().resultProperty());
        this.genTimeCol.setCellValueFactory(o -> o.getValue().getValue().generationTimeProperty());
        this.execTimeCol.setCellValueFactory(o -> o.getValue().getValue().executionTimeProperty());
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getParentPathAsString()));

        this.genTimeCol.setCellFactory(InstantCellFactory.instantTreeCellFactory());
        this.execTimeCol.setCellFactory(InstantCellFactory.instantTreeCellFactory());

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
    }

    protected void applyFilter(ActivityOccurrenceDataFilter selectedFilter) {
        this.dataItemFilterController.setSelectedFilter(selectedFilter);
        // Apply the filter on the current table
        if(selectedFilter != null && !selectedFilter.isClear()) {
            ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).predicateProperty().setValue(new FilterWrapper(selectedFilter));
        } else {
            ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).predicateProperty().setValue(p -> true);
        }
        if(this.liveTgl == null || this.liveTgl.isSelected()) {
            if(selectedFilter == null || selectedFilter.isClear()) {
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
            if(selectedFilter == null || selectedFilter.isClear()) {
                markFilterDeactivated();
            } else {
                markFilterActivated();
            }
            moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, getNumVisibleRow(), selectedFilter);
        }
    }

    private void moveToTime(Instant selectedTime, RetrievalDirection direction, int n, ActivityOccurrenceDataFilter currentFilter) {
        if(this.selectTimeBtn != null) {
            this.selectTimeBtn.setText(formatTime(selectedTime));
        }

        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // TODO
                // List<ActivityOccurrenceData> messages = doRetrieve(selectedTime, n, direction, currentFilter);
                // addDataItemsBack(messages, n, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private int getNumVisibleRow() {
        double h = this.dataItemTableView.getHeight();
        h -= 30; // Header
        return (int) (h / this.dataItemTableView.getFixedCellSize()) + 1;
    }

    private void markFilterDeactivated() {
        this.filterBtn.setStyle("");
    }

    private void markFilterActivated() {
        this.filterBtn.setStyle("-fx-background-color: -fx-faint-focus-color");
    }


    protected Consumer<List<ActivityOccurrenceData>> buildIncomingDataDelegatorAction() {
        return (List<ActivityOccurrenceData> t) -> {
            addDataItems(t, true, true);
        };
    }

    protected void addDataItems(List<ActivityOccurrenceData> messages, boolean fromLive, boolean addOnTop) {
        if(fromLive) {
            // Revert the list
            Collections.reverse(messages);
        }
        Platform.runLater(() -> {
            if (!this.displayTitledPane.isDisabled() && (!fromLive || (this.liveTgl == null || this.liveTgl.isSelected()))) {
                if (addOnTop) {
                    // TODO
                } else {
                    // TODO
                }
                if (!fromLive) {
                    this.dataItemTableView.scrollTo(0);
                }

                updateSelectTime();
            }
        });
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
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
            startSubscription();
        } else {
            stopSubscription();
            updateSelectTime();
        }
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTime(Instant.EPOCH);
        }
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L));
        }
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    protected void moveToTime(Instant selectedTime) {
        this.selectTimeBtn.setText(formatTime(selectedTime));
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // TODO
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

    protected ActivityOccurrenceDataFilter getCurrentFilter() {
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

    protected void doServiceSubscribe(ActivityOccurrenceDataFilter selectedFilter) throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().subscribe(this, selectedFilter);
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().unsubscribe(this);
    }

    protected void updateSelectTime() {
        // Take the latest generation time from the table
        if (this.dataItemTableView.getRoot().getChildren().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            // TODO
        }
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        Platform.runLater(() -> {
            this.progressIndicator.setVisible(false);
        });
    }

    private boolean isProgressBusy() {
        return this.progressIndicator.isVisible();
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        return TableViewUtil.buildNodeForPrinting(this.dataItemTableView);
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(false);
        if (oldStatus) {
            persistColumnConfiguration();
        }
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(true);
        // Restore column configuration
        restoreColumnConfiguration();
        // Start subscription if there
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    protected String doGetComponentId() {
        return "ParameterDataView";
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
    public void onPurgeMenuItem(ActionEvent actionEvent) {
        // TODO
    }

    @Override
    public void dataItemsReceived(List<ActivityOccurrenceData> dataItems) {
        Platform.runLater(() -> {
            delegator.delegate(dataItems);
            updateSelectTime();
        });
    }

    public static class ActivityOccurrenceDataWrapper {

        private final SystemEntityPath path;
        private final SimpleObjectProperty<Object> property = new SimpleObjectProperty<>();

        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();
        private final SimpleObjectProperty<Instant> generationTime = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Instant> executionTime = new SimpleObjectProperty<>();
        private final SimpleStringProperty route = new SimpleStringProperty();
        private final SimpleStringProperty source = new SimpleStringProperty();
        private final SimpleStringProperty type = new SimpleStringProperty();
        private final SimpleObjectProperty<ActivityOccurrenceState> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Object> result = new SimpleObjectProperty<>();

        public ActivityOccurrenceDataWrapper(ActivityOccurrenceData data, SystemEntityPath path) {
            this.path = path;
            set(data);
        }

        public ActivityOccurrenceDataWrapper(ActivityOccurrenceReport data, SystemEntityPath path) {
            this.path = path;
            set(data);
        }

        private void set(ActivityOccurrenceReport data) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            executionTime.set(data.getExecutionTime());
            route.set("");
            source.set("");
            type.set("");
            state.set(data.getState());
            result.set(data.getResult());
            status.set(data.getStatus().name());
            name.set(data.getName());
        }

        public void set(ActivityOccurrenceData data) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            executionTime.set(data.getExecutionTime());
            route.set(data.getRoute());
            source.set(data.getSource());
            type.set(data.getType());
            state.set(data.getCurrentState());
            result.set(data.getResult());
            status.set("");
            name.set(data.getName());
        }

        public Object get() {
            return property.getValue();
        }

        public SimpleObjectProperty<Instant> generationTimeProperty() {
            return generationTime;
        }

        public SimpleObjectProperty<Instant> executionTimeProperty() {
            return executionTime;
        }

        public SimpleStringProperty routeProperty() {
            return route;
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public SimpleObjectProperty<ActivityOccurrenceState> stateProperty() {
            return state;
        }

        public SimpleObjectProperty<Object> resultProperty() {
            return result;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public SystemEntityPath getPath() {
            return this.path;
        }

        public SimpleStringProperty sourceProperty() {
            return source;
        }

        public String getParentPathAsString() {
            if(path != null) {
                return path.getParent().asString();
            } else {
                return "";
            }
        }
    }

    public static class FilterWrapper implements Predicate<ActivityOccurrenceDataWrapper> {

        private final ActivityOccurrenceDataFilter filter;

        public FilterWrapper(ActivityOccurrenceDataFilter selectedFilter) {
            this.filter = selectedFilter;
        }

        @Override
        public boolean test(ActivityOccurrenceDataWrapper activityOccurrenceDataWrapper) {
            if(activityOccurrenceDataWrapper.get() instanceof ActivityOccurrenceReport) {
                return true;
            } else {
                return filter.test((ActivityOccurrenceData) activityOccurrenceDataWrapper.get());
            }
        }
    }
}
