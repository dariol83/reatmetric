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
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.value.Array;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.*;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ActivityDataViewController extends AbstractDisplayController implements IActivityOccurrenceDataSubscriber {

    private static final Logger LOG = Logger.getLogger(ActivityDataViewController.class.getName());
    protected static final int MAX_ENTRIES = 100;

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

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Table
    @FXML
    protected TreeTableView<ActivityOccurrenceDataWrapper> dataItemTableView;

    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> nameCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, IUniqueId> occIdCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, ActivityOccurrenceState> stateCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, ActivityReportState> statusCol;
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

    private final Map<IUniqueId, TreeItem<ActivityOccurrenceDataWrapper>> activityMap = new HashMap<>();
    private final Map<IUniqueId, Map<String, TreeItem<ActivityOccurrenceDataWrapper>>> activityProgressMap = new HashMap<>();

    private double zoomFactor = Font.getDefault().getSize() * 10;

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.dataItemTableView.setPlaceholder(new Label(""));
        this.dataItemTableView.setFixedCellSize(TABLE_ROW_HEIGHT);

        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());

        this.dateTimePopup.setAutoHide(true);
        this.dateTimePopup.setHideOnEscape(true);

        initialiseToolbarVisibility(this.displayTitledPane, toolbar, toggleShowToolbarItem);

        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            CssHandler.applyTo(dateTimePicker);
            this.dateTimePickerController = loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            CssHandler.applyTo(this.dateTimePopup.getScene().getRoot());
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, getNumVisibleRow(this.dataItemTableView) * 2, this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.filterPopup.setAutoHide(true);
        this.filterPopup.setHideOnEscape(true);

        try {
            URL filterWidgetUrl = getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/ActivityDataFilterWidget.fxml");
            FXMLLoader loader = new FXMLLoader(filterWidgetUrl);
            Parent filterSelector = loader.load();
            CssHandler.applyTo(filterSelector);
            this.dataItemFilterController = loader.getController();
            this.filterPopup.getContent().addAll(filterSelector);
            CssHandler.applyTo(this.filterPopup.getScene().getRoot());
            // Load the controller hide with select
            this.dataItemFilterController.setActionAfterSelection(() -> {
                this.filterPopup.hide();
                applyFilter(this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.nameCol.setCellValueFactory(o -> o.getValue().getValue().nameProperty());
        this.occIdCol.setCellValueFactory(o -> o.getValue().getValue().occIdProperty());
        this.stateCol.setCellValueFactory(o -> o.getValue().getValue().stateProperty());
        this.statusCol.setCellValueFactory(o -> o.getValue().getValue().statusProperty());
        this.sourceCol.setCellValueFactory(o -> o.getValue().getValue().sourceProperty());
        this.routeCol.setCellValueFactory(o -> o.getValue().getValue().routeProperty());
        this.typeCol.setCellValueFactory(o -> o.getValue().getValue().typeProperty());
        this.resultCol.setCellValueFactory(o -> o.getValue().getValue().resultProperty());
        this.genTimeCol.setCellValueFactory(o -> o.getValue().getValue().generationTimeProperty());
        this.execTimeCol.setCellValueFactory(o -> o.getValue().getValue().executionTimeProperty());
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getParentPathAsString()));

        this.genTimeCol.setCellFactory(getInstantCellCallback());
        this.execTimeCol.setCellFactory(getInstantCellCallback());
        this.nameCol.setCellFactory(getNormalTextCellCallback());
        this.occIdCol.setCellFactory(getNormalTextCellCallback());
        this.sourceCol.setCellFactory(getNormalTextCellCallback());
        this.routeCol.setCellFactory(getNormalTextCellCallback());
        this.typeCol.setCellFactory(getNormalTextCellCallback());
        this.resultCol.setCellFactory(getNormalTextCellCallback());
        this.parentCol.setCellFactory(getNormalTextCellCallback());
        this.stateCol.setCellFactory(zoomEnabledWrapper(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(ActivityOccurrenceState item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case RELEASE:
                        case CREATION:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_RELEASE);
                            break;
                        case TRANSMISSION:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_TRANSMISSION);
                            break;
                        case SCHEDULING:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_SCHEDULING);
                            break;
                        case EXECUTION:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_EXECUTION);
                            break;
                        case VERIFICATION:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_VERIFICATION);
                            break;
                        case COMPLETED:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATE_COMPLETED);
                            break;
                        default:
                            CssHandler.updateStyleClass(this, null);
                            break;
                    }
                } else {
                    CssHandler.updateStyleClass(this, null);
                    setText("");
                    setGraphic(null);
                }
            }
        }));
        this.statusCol.setCellFactory(zoomEnabledWrapper(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(ActivityReportState item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case OK:
                        case NOT_AVAILABLE:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_OK);
                            break;
                        case FAIL:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_FAIL);
                            break;
                        case FATAL:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_FATAL);
                            break;
                        case PENDING:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_PENDING);
                            break;
                        case EXPECTED:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_EXPECTED);
                            break;
                        case UNKNOWN:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_UNKNOWN);
                            break;
                        case ERROR:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_ERROR);
                            break;
                        case TIMEOUT:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_ACTIVITY_STATUS_TIMEOUT);
                            break;
                        default:
                            CssHandler.updateStyleClass(this, null);
                            break;
                    }
                } else {
                    CssHandler.updateStyleClass(this, null);
                    setText("");
                    setGraphic(null);
                }
            }
        }));

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
        this.dataItemTableView.setShowRoot(false);
        this.dataItemTableView.setRoot(new FilterableTreeItem<>(null, false));

        Platform.runLater(() -> updateZoomFactor(0));
    }

    private void openDetailsDialog(ActivityOccurrenceDataWrapper data) {
        if(!(data.get() instanceof ActivityOccurrenceData)) {
            return;
        }
        ActivityOccurrenceData actOccData = (ActivityOccurrenceData) data.get();
        try {
            ActivityRequest derivedRequest = toActivityRequest(actOccData);
            ActivityDescriptor descriptor = (ActivityDescriptor) MainViewController.instance().getModelController().getDescriptorOf(actOccData.getExternalId());
            Pair<Node, ActivityInvocationDialogController> activityDialogPair = ActivityInvocationDialogUtil.createActivityInvocationDialog(descriptor, derivedRequest,
                    () -> Collections.singletonList(new ActivityRouteState(actOccData.getRoute(), ActivityRouteAvailability.UNKNOWN)));
            activityDialogPair.getSecond().hideRouteControls();
            activityDialogPair.getSecond().makeReadOnly();
            // Load the controller hide with select
            DialogUtils.customInfoDialog(this.dataItemTableView.getScene().getWindow(), activityDialogPair.getFirst(), "Activity Occurrence Details - " + data.nameProperty().get());
        } catch (IOException | ReatmetricException e) {
            e.printStackTrace();
        }
    }


    protected <K> Callback<TreeTableColumn<ActivityOccurrenceDataWrapper, K>, TreeTableCell<ActivityOccurrenceDataWrapper, K>> getNormalTextCellCallback() {
        return column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(K item, boolean empty) {
                super.updateItem(item, empty);
                setText(Objects.toString(item, ""));
                setCellFontSize(this);
            }
        };
    }

    protected Callback<TreeTableColumn<ActivityOccurrenceDataWrapper, Instant>, TreeTableCell<ActivityOccurrenceDataWrapper, Instant>> getInstantCellCallback() {
        return column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Instant item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(InstantCellFactory.DATE_TIME_FORMATTER.format(item));
                } else {
                    setText("");
                }
                setCellFontSize(this);
            }
        };
    }

    protected <T, K> Callback<TreeTableColumn<T, K>, TreeTableCell<T, K>> zoomEnabledWrapper(Callback<TreeTableColumn<T, K>, TreeTableCell<T, K>> delegated) {
        return param -> {
            TreeTableCell<T, K> item = delegated.call(param);
            setCellFontSize(item);
            return item;
        };
    }

    @FXML
    public void minusZoomClick(ActionEvent mouseEvent) {
        updateZoomFactor(-10);
    }

    @FXML
    public void plusZoomClick(ActionEvent mouseEvent) {
        updateZoomFactor(+10);
    }

    private void updateZoomFactor(double value) {
        this.zoomFactor += value;
        if (this.zoomFactor <= 30) {
            this.zoomFactor = 30;
        }
        Font f = Font.font(Font.getDefault().getFamily(), FontWeight.NORMAL, this.zoomFactor/10);
        Text text = new Text("AXqg");
        text.setFont(f);
        new Scene(new Group(text));
        double height = text.getLayoutBounds().getHeight();
        dataItemTableView.setFixedCellSize(height + 4);
        dataItemTableView.refresh();
    }

    protected void setCellFontSize(TreeTableCell<?, ?> item) {
        double fontSize = zoomFactor/10;
        Font oldFont = item.getFont();
        Font newFont;
        if(oldFont != null) {
            newFont = Font.font(oldFont.getFamily(), FontWeight.NORMAL, fontSize);
        } else {
            newFont = Font.font(Font.getDefault().getFamily(), FontWeight.NORMAL, fontSize);
        }
        item.setFont(newFont);
        item.setStyle("-fx-padding: 1px 0 2px 0");
    }

    protected void applyFilter(ActivityOccurrenceDataFilter selectedFilter) {
        this.dataItemFilterController.setSelectedFilter(selectedFilter);
        // Apply the filter on the current table
        if (selectedFilter != null && !selectedFilter.isClear()) {
            ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).predicateProperty().setValue(new FilterWrapper(selectedFilter));
        } else {
            ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).predicateProperty().setValue(p -> true);
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
            moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, getNumVisibleRow(this.dataItemTableView), selectedFilter);
        }
    }

    private void markFilterDeactivated() {
        this.filterBtn.setStyle("");
    }

    private void markFilterActivated() {
        this.filterBtn.setStyle("-fx-background-color: -fx-faint-focus-color");
    }

    protected Consumer<List<ActivityOccurrenceData>> buildIncomingDataDelegatorAction() {
        return (List<ActivityOccurrenceData> t) -> addDataItems(t, true);
    }

    protected void addDataItems(List<ActivityOccurrenceData> messages, boolean fromLive) {
        FxUtils.runLater(() -> {
            if (!this.displayTitledPane.isDisabled() && (!fromLive || (this.liveTgl == null || this.liveTgl.isSelected()))) {
                for (ActivityOccurrenceData aod : messages) {
                    createOrUpdate(aod, true);
                }
                if (!fromLive) {
                    this.dataItemTableView.scrollTo(0);
                }
                // Check if MAX_ENTRIES is exceeded, remove one at the top or end, depending on addOnTop - remove also from maps
                removeExceedingEntries(true);
                updateSelectTime();
            }
        });
    }

    protected void addDataItemsBack(List<ActivityOccurrenceData> messages, int n, boolean clearTable) {
        FxUtils.runLater(() -> {
            if (!this.displayTitledPane.isDisabled()) {
                if (clearTable) {
                    clearTable();
                }
                int toRemoveTop = dataItemTableView.getRoot().getChildren().size() > n ? n : dataItemTableView.getRoot().getChildren().size() - 1;
                if (toRemoveTop > 0) {
                    removeActivities(0, toRemoveTop);
                }
                for (ActivityOccurrenceData aod : messages) {
                    createOrUpdate(aod, false);
                }
                this.dataItemTableView.scrollTo(0);
                this.dataItemTableView.refresh();
                updateSelectTime();
            }
        });
    }

    protected void clearTable() {
        ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).getSourceChildren().clear();
        activityProgressMap.clear();
        activityMap.clear();
        dataItemTableView.layout();
        dataItemTableView.refresh();
        updateSelectTime();
    }

    private void removeExceedingEntries(boolean newAddedOnTop) {
        int toRemove = dataItemTableView.getRoot().getChildren().size() - MAX_ENTRIES;
        if (toRemove > 0) {
            if (newAddedOnTop) {
                // Remove from the bottom, also from the maps
                removeActivities(dataItemTableView.getRoot().getChildren().size() - toRemove, dataItemTableView.getRoot().getChildren().size());
            } else {
                // Remove from the top, also from the maps
                removeActivities(0, toRemove);
            }
        }
    }

    private void removeActivities(int from, int to) {
        // Collect the FilteredTreeItem to remove
        Set<TreeItem<ActivityOccurrenceDataWrapper>> toRemove = new HashSet<>();
        for (int i = from; i < to; ++i) {
            toRemove.add(removeActivityAtPosition(i));
        }
        ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).getSourceChildren().removeAll(toRemove);
    }

    private TreeItem<ActivityOccurrenceDataWrapper> removeActivityAtPosition(int i) {
        ActivityOccurrenceDataWrapper w = dataItemTableView.getRoot().getChildren().get(i).getValue();
        activityMap.remove(w.getUniqueId());
        activityProgressMap.remove(w.getUniqueId());
        return dataItemTableView.getRoot().getChildren().get(i);
    }

    private void createOrUpdate(ActivityOccurrenceData aod, boolean addOnTop) {
        TreeItem<ActivityOccurrenceDataWrapper> wrapper = activityMap.get(aod.getInternalId());
        if (wrapper == null) {
            wrapper = new TreeItem<>(new ActivityOccurrenceDataWrapper(aod, aod.getPath()));
            activityMap.put(aod.getInternalId(), wrapper);
            if (addOnTop) {
                ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).getSourceChildren().add(0, wrapper);
            } else {
                ((FilterableTreeItem<ActivityOccurrenceDataWrapper>) dataItemTableView.getRoot()).getSourceChildren().add(wrapper);
            }
        }
        update(wrapper, aod);
    }

    private void update(TreeItem<ActivityOccurrenceDataWrapper> wrapper, ActivityOccurrenceData aod) {
        wrapper.getValue().set(aod);
        // Progress now
        Map<String, TreeItem<ActivityOccurrenceDataWrapper>> progresses = activityProgressMap.computeIfAbsent(aod.getInternalId(), k -> new LinkedHashMap<>());
        for (ActivityOccurrenceReport rep : aod.getProgressReports()) {
            TreeItem<ActivityOccurrenceDataWrapper> reportWrapper = progresses.get(rep.getName());
            if (reportWrapper == null) {
                reportWrapper = new TreeItem<>(new ActivityOccurrenceDataWrapper(rep, aod.getPath()));
                progresses.put(rep.getName(), reportWrapper);
                wrapper.getChildren().add(reportWrapper);
            }
            if (reportWrapper.getValue().isReportToUpdate(rep)) {
                reportWrapper.getValue().set(rep);
            }
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
            this.dateTimePopup.show(this.liveTgl.getScene().getWindow());
        }
        e.consume();
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
            clearTable();
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, getNumVisibleRow(this.dataItemTableView), this.dataItemFilterController.getSelectedFilter());
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
            fetchRecords(getNumVisibleRow(this.dataItemTableView), RetrievalDirection.TO_PAST);
        }
        e.consume();
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (isProcessingAvailable()) {
            moveToTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), RetrievalDirection.TO_PAST, getNumVisibleRow(this.dataItemTableView) * 2, this.dataItemFilterController.getSelectedFilter());
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
            fetchRecords(getNumVisibleRow(this.dataItemTableView), RetrievalDirection.TO_FUTURE);
        }
        e.consume();
    }

    protected void fetchRecords(int n, RetrievalDirection direction) {
        if (dataItemTableView.getRoot().getChildren().isEmpty()) {
            return;
        }
        // Get the first message in the table
        ActivityOccurrenceDataWrapper om = direction == RetrievalDirection.TO_FUTURE ? getFirst() : getLast();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ActivityOccurrenceData> messages = doRetrieve((ActivityOccurrenceData) om.get(), n, direction, this.dataItemFilterController.getSelectedFilter());
                if (direction == RetrievalDirection.TO_FUTURE) {
                    addDataItems(messages, false);
                } else {
                    addDataItemsBack(messages, n, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private List<ActivityOccurrenceData> doRetrieve(ActivityOccurrenceData activityOccurrenceData, int n, RetrievalDirection direction, ActivityOccurrenceDataFilter selectedFilter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().retrieve(activityOccurrenceData, n, direction,
                    selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    private List<ActivityOccurrenceData> doRetrieve(Instant time, int n, RetrievalDirection direction, ActivityOccurrenceDataFilter selectedFilter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().retrieve(time, n, direction,
                    selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    private ActivityOccurrenceDataWrapper getFirst() {
        return this.dataItemTableView.getRoot().getChildren().get(0).getValue();
    }

    private ActivityOccurrenceDataWrapper getLast() {
        return this.dataItemTableView.getRoot().getChildren().get(this.dataItemTableView.getRoot().getChildren().size() - 1).getValue();
    }

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (this.filterPopup.isShowing()) {
            this.filterPopup.hide();
        } else {
            Bounds b = this.filterBtn.localToScreen(this.filterBtn.getBoundsInLocal());
            this.filterPopup.setX(b.getMinX());
            this.filterPopup.setY(b.getMaxY());
            this.filterPopup.show(this.displayTitledPane.getScene().getWindow());
        }
        e.consume();
    }

    protected void moveToTime(Instant selectedTime, RetrievalDirection direction, int n, ActivityOccurrenceDataFilter currentFilter) {
        if (this.selectTimeBtn != null) {
            this.selectTimeBtn.setText(formatTime(selectedTime));
        }

        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ActivityOccurrenceData> messages = doRetrieve(selectedTime, n, direction, currentFilter);
                addDataItemsBack(messages, n, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void restoreColumnConfiguration() {
        if (this.system != null) {
            try {
                TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                // Nothing to do
                e.printStackTrace();
            }
        }
    }

    private void persistColumnConfiguration() {
        if (this.system != null) {
            try {
                TableViewUtil.persistColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                // Nothing to do
                e.printStackTrace();
            }
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
        try {
            ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().subscribe(this, selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getActivityOccurrenceDataMonitorService().unsubscribe(this);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected void updateSelectTime() {
        if (this.selectTimeBtn == null) {
            return;
        }
        // Take the first item from the table and use the generation time as value of the text
        if (dataItemTableView.getRoot().getChildren().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            Instant latest = null;
            for (TreeItem<ActivityOccurrenceDataWrapper> item : activityMap.values()) {
                if (latest == null) {
                    latest = item.getValue().generationTimeProperty().get();
                } else {
                    if (latest.isBefore(item.getValue().generationTimeProperty().get())) {
                        latest = item.getValue().generationTimeProperty().get();
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
            moveToTime(Instant.now(), RetrievalDirection.TO_PAST, getNumVisibleRow(this.dataItemTableView), this.dataItemFilterController.getSelectedFilter());
            startSubscription();
        }
    }

    protected String doGetComponentId() {
        return "ActivityDataView";
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
    public void onPurgeMenuItem(ActionEvent event) {
        List<TreeItem<ActivityOccurrenceDataWrapper>> selected = this.dataItemTableView.getSelectionModel().getSelectedItems();
        boolean confirm = DialogUtils.confirm("Purge activity occurrences", null, "If you continue, the monitoring of the selected occurrences will stop and the occurrences will be removed " +
                "from the processing model. Do you want to purge the selected occurrences?");
        if (!confirm) {
            return;
        }
        final List<Pair<Integer, IUniqueId>> purgeList = new LinkedList<>();
        for (TreeItem<ActivityOccurrenceDataWrapper> item : selected) {
            // If you select an activity report, you get a class cast exception here if you do not check
            if(item.getValue().get() instanceof ActivityOccurrenceData) {
                purgeList.add(Pair.of(((ActivityOccurrenceData) item.getValue().get()).getExternalId(), ((ActivityOccurrenceData) item.getValue().get()).getInternalId()));
            } else if(item.getValue().get() instanceof ActivityOccurrenceReport) {
                purgeList.add(Pair.of(((ActivityOccurrenceData) item.getParent().getValue().get()).getExternalId(), ((ActivityOccurrenceData) item.getParent().getValue().get()).getInternalId()));
            }
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                ReatmetricUI.selectedSystem().getSystem().getActivityExecutionService().purgeActivities(purgeList);
            } catch (ReatmetricException | RemoteException e) {
                e.printStackTrace();
            }
        });
        event.consume();
    }

    @FXML
    public void removeButtonSelected(ActionEvent event) {
        boolean confirm = DialogUtils.confirm("Remove activity occurrences", null, "If you continue, the display will be cleared from the currently displayed activity occurrences. " +
                "Do you want to continue?");
        if (!confirm) {
            return;
        }
        this.dataItemTableView.getRoot().getChildren().remove(0, this.dataItemTableView.getRoot().getChildren().size());
        event.consume();
    }

    @Override
    public void dataItemsReceived(List<ActivityOccurrenceData> dataItems) {
        FxUtils.runLater(() -> delegator.delegate(dataItems));
    }

    @FXML
    private void locateItemAction(ActionEvent actionEvent) {
        ActivityOccurrenceDataWrapper ed = this.dataItemTableView.getSelectionModel().getSelectedItem().getValue();
        if(ed != null) {
            MainViewController.instance().getModelController().locate(ed.getPath());
        }
    }

    @FXML
    public void invocationDetailsAction(ActionEvent actionEvent) {
        ActivityOccurrenceDataWrapper ed = this.dataItemTableView.getSelectionModel().getSelectedItem().getValue();
        if(ed != null) {
            openDetailsDialog(ed);
        }
    }

    @FXML
    private void replayItemAction(ActionEvent actionEvent) {
        ActivityOccurrenceDataWrapper ed = this.dataItemTableView.getSelectionModel().getSelectedItem().getValue();
        if(ed != null) {
            ActivityOccurrenceData activityOccurrenceData = (ActivityOccurrenceData) ed.get();
            ActivityRequest derivedRequest = toActivityRequest(activityOccurrenceData);
            MainViewController.instance().getModelController().requestActivity(derivedRequest.getPath().asString(), derivedRequest);
        }
    }

    private ActivityRequest toActivityRequest(ActivityOccurrenceData activityOccurrenceData) {
        return new ActivityRequest(activityOccurrenceData.getExternalId(), activityOccurrenceData.getPath(),
                toActivityArgumentList(activityOccurrenceData.getPath(), activityOccurrenceData.getArguments()),
                activityOccurrenceData.getProperties(),
                activityOccurrenceData.getRoute(),
                activityOccurrenceData.getSource());
    }

    private List<AbstractActivityArgument> toActivityArgumentList(SystemEntityPath path, Map<String, Object> arguments) {
        try {
            ActivityDescriptor descriptor = (ActivityDescriptor) MainViewController.instance().getModelController().getDescriptorOf(path.asString());
            List<AbstractActivityArgumentDescriptor> argumentDescriptors = descriptor.getArgumentDescriptors();
            List<AbstractActivityArgument> argumentList = new ArrayList<>(arguments.size());
            for(AbstractActivityArgumentDescriptor argDesc : argumentDescriptors) {
                Object value = arguments.get(argDesc.getName());
                if(argDesc instanceof ActivityPlainArgumentDescriptor) {
                    argumentList.add(PlainActivityArgument.of(argDesc.getName(), value, null, false));
                } else if(argDesc instanceof ActivityArrayArgumentDescriptor) {
                    argumentList.add(toArrayActivityArgument(argDesc.getName(), (Array) value));
                }
            }
            return argumentList;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot acquire descriptor for path " + path.asString() + ": " + e.getMessage(), e);
            // Error, return
            return Collections.emptyList();
        }
    }

    private ArrayActivityArgument toArrayActivityArgument(String name, Array value) {
        List<ArrayActivityArgumentRecord> recordList = new LinkedList<>();
        for(Array.Record record : value.getRecords()) {
            ArrayActivityArgumentRecord rec = toActivityArgumentRecord(record);
            recordList.add(rec);
        }
        return new ArrayActivityArgument(name, recordList);
    }

    private ArrayActivityArgumentRecord toActivityArgumentRecord(Array.Record record) {
        List<AbstractActivityArgument> recordContents = new ArrayList<>(record.getElements().size());
        for(Pair<String, Object> elem : record.getElements()) {
            if(elem.getSecond() instanceof Array) {
                recordContents.add(toArrayActivityArgument(elem.getFirst(), (Array) elem.getSecond()));
            } else {
                recordContents.add(PlainActivityArgument.of(elem.getFirst(), elem.getSecond(), null, false));
            }
        }
        return new ArrayActivityArgumentRecord(recordContents);
    }

    @Override
    public void dispose() {
        this.delegator.shutdown();
        super.dispose();
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

    public static class ActivityOccurrenceDataWrapper {

        private final SystemEntityPath path;
        private final SimpleObjectProperty<Object> property = new SimpleObjectProperty<>();

        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleObjectProperty<IUniqueId> occId = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<ActivityReportState> status = new SimpleObjectProperty<>();
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

        public void set(ActivityOccurrenceReport data) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            executionTime.set(data.getExecutionTime());
            route.set("");
            source.set("");
            type.set("");
            state.set(data.getState());
            result.set(data.getResult());
            status.set(data.getStatus());
            name.set(data.getName());
            occId.set(null);
        }

        private void deriveStatus(ActivityOccurrenceData data) {
            statusProperty().set(data.aggregateStatus());
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
            name.set(data.getName());
            occId.set(data.getInternalId());
            deriveStatus(data);
        }

        public Object get() {
            return property.getValue();
        }

        public SimpleObjectProperty<IUniqueId> occIdProperty() {
            return occId;
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

        public SimpleObjectProperty<ActivityReportState> statusProperty() {
            return status;
        }

        public SystemEntityPath getPath() {
            return this.path;
        }

        public SimpleStringProperty sourceProperty() {
            return source;
        }

        public String getParentPathAsString() {
            if (path != null) {
                SystemEntityPath parent = path.getParent();
                if (parent != null) {
                    return parent.asString();
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }

        public boolean isReportToUpdate(ActivityOccurrenceReport rep) {
            if (get() instanceof ActivityOccurrenceReport) {
                ActivityOccurrenceReport current = (ActivityOccurrenceReport) get();
                if (current.getStatus() == ActivityReportState.PENDING || current.getStatus() == ActivityReportState.EXPECTED) {
                    // If you have an update here, you replace it
                    return true;
                } else if (current.getGenerationTime().isBefore(rep.getGenerationTime())) {
                    // If the report is newer, you replace it
                    return true;
                } else if (current.getGenerationTime().equals(rep.getGenerationTime())) {
                    // If the report has the same time... depends on the state
                    return rep.getStatus().compareTo(current.getStatus()) > 0;
                } else {
                    // Nothing here
                    return false;
                }
            } else {
                return false;
            }
        }

        public IUniqueId getUniqueId() {
            if (get() instanceof ActivityOccurrenceData) {
                return ((ActivityOccurrenceData) get()).getInternalId();
            } else {
                return null;
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
            if (activityOccurrenceDataWrapper.get() instanceof ActivityOccurrenceReport) {
                return true;
            } else {
                return filter.test((ActivityOccurrenceData) activityOccurrenceDataWrapper.get());
            }
        }
    }
}
