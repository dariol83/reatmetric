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
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.CssHandler;
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
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
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
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDisplayTabWidgetController extends AbstractDisplayController {

    // Live/retrieval controls
    @FXML
    protected ToggleButton liveTgl;
    @FXML
    protected Button goToStartBtn;
    @FXML
    protected Button goBackOneBtn;
    @FXML
    protected Button goToEndBtn;
    @FXML
    protected Button goForwardOneBtn;
    @FXML
    protected Button selectTimeBtn;

    // Print button
    @FXML
    protected Button printBtn;

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Table
    @FXML
    protected TableView<ParameterDataWrapper> dataItemTableView; // use a ParameterData wrapper class

    @FXML
    private TableColumn<ParameterDataWrapper, String> nameCol;
    @FXML
    private TableColumn<ParameterDataWrapper, String> descriptionCol;
    @FXML
    private TableColumn<ParameterDataWrapper, String> engValueCol;
    @FXML
    private TableColumn<ParameterDataWrapper, String> sourceValueCol;
    @FXML
    private TableColumn<ParameterDataWrapper, Validity> validityCol;
    @FXML
    private TableColumn<ParameterDataWrapper, AlarmState> alarmStateCol;
    @FXML
    private TableColumn<ParameterDataWrapper, Instant> genTimeCol;
    @FXML
    private TableColumn<ParameterDataWrapper, Instant> recTimeCol;
    @FXML
    private TableColumn<ParameterDataWrapper, String> parentCol;

    protected volatile ParameterDataFilter currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(), null, null, null, null);

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    // Model map
    private final Map<SystemEntityPath, ParameterDataWrapper> path2wrapper = new TreeMap<>();

    private volatile boolean live = false;
    private Stage independentStage;

    private double zoomFactor = Font.getDefault().getSize() * 10;

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.dataItemTableView.setPlaceholder(new Label(""));

        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());

        this.dateTimePopup.setAutoHide(true);
        this.dateTimePopup.setHideOnEscape(true);

        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            CssHandler.applyTo(dateTimePopup.getScene().getRoot());
            CssHandler.applyTo(dateTimePicker);
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

        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getName()));
        this.descriptionCol.setCellValueFactory(o -> o.getValue().descriptionProperty());
        this.engValueCol.setCellValueFactory(o -> o.getValue().engValueProperty());
        this.sourceValueCol.setCellValueFactory(o -> o.getValue().rawValueProperty());
        this.validityCol.setCellValueFactory(o -> o.getValue().validityProperty());

        this.genTimeCol.setCellValueFactory(o -> o.getValue().generationTimeProperty());
        this.recTimeCol.setCellValueFactory(o -> o.getValue().receptionTimeProperty());
        this.alarmStateCol.setCellValueFactory(o -> o.getValue().alarmStateProperty());
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(toStringParent(o.getValue().get().getPath())));

        Callback<TableColumn<ParameterDataWrapper, Instant>, TableCell<ParameterDataWrapper, Instant>> instantCellFactory = getInstantCellCallback();
        this.genTimeCol.setCellFactory(instantCellFactory);
        this.recTimeCol.setCellFactory(instantCellFactory);

        Callback<TableColumn<ParameterDataWrapper, String>, TableCell<ParameterDataWrapper, String>> normalTextCellFactory = getNormalTextCellCallback();
        this.descriptionCol.setCellFactory(normalTextCellFactory);
        this.engValueCol.setCellFactory(normalTextCellFactory);
        this.sourceValueCol.setCellFactory(normalTextCellFactory);
        this.parentCol.setCellFactory(normalTextCellFactory);

        this.nameCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                TableRow<ParameterDataWrapper> currentRow = getTableRow();
                if(currentRow != null && currentRow.getItem() instanceof RowSeparatorWrapper) {
                    setText(item);
                    CssHandler.updateStyleClass(currentRow, CssHandler.CSS_PARAMETER_RAW_SEPARATOR);
                } else {
                    if (item != null && !empty && !isEmpty()) {
                        setText(item);
                    } else {
                        setText("");
                    }
                    if(currentRow != null) {
                        CssHandler.updateStyleClass(currentRow, null);
                    }
                }
                setCellFontSize(this);
            }
        });
        this.validityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Validity item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case DISABLED:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_DISABLED);
                            break;
                        case INVALID:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_INVALID);
                            break;
                        case ERROR:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_ERROR);
                            break;
                        case UNKNOWN:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_UNKNOWN);
                            break;
                        default:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_VALID);
                            break;
                    }
                } else {
                    CssHandler.updateStyleClass(this, null);
                    setText("");
                    setGraphic(null);
                }
                setCellFontSize(this);
            }
        });
        this.alarmStateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(AlarmState item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case ALARM:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ALARM);
                            break;
                        case ERROR:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ERROR);
                            break;
                        case WARNING:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_WARNING);
                            break;
                        case VIOLATED:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_VIOLATED);
                            break;
                        case UNKNOWN:
                        case NOT_APPLICABLE:
                        case NOT_CHECKED:
                        case IGNORED:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_UNKNOWN);
                            break;
                        default:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_NOMINAL);
                            break;
                    }
                } else {
                    CssHandler.updateStyleClass(this, null);
                    setText("");
                    setGraphic(null);
                }
                setCellFontSize(this);
            }
        });

        ParameterDisplayCoordinator.instance().register(this);

        Platform.runLater(() -> updateZoomFactor(0));
    }

    private Callback<TableColumn<ParameterDataWrapper, String>, TableCell<ParameterDataWrapper, String>> getNormalTextCellCallback() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setCellFontSize(this);
            }
        };
    }

    private Callback<TableColumn<ParameterDataWrapper, Instant>, TableCell<ParameterDataWrapper, Instant>> getInstantCellCallback() {
        return column -> new TableCell<>() {
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

    private void setCellFontSize(TableCell<ParameterDataWrapper, ?> item) {
        double fontSize = zoomFactor/10;
        Font oldFont = item.getFont();
        Font newFont;
        if(oldFont != null) {
            newFont = Font.font(oldFont.getFamily(), item.getTableRow() != null && item.getTableRow().getItem() instanceof RowSeparatorWrapper ? FontWeight.BOLD : FontWeight.NORMAL, fontSize);
        } else {
            newFont = Font.font(Font.getDefault().getFamily(), item.getTableRow() != null && item.getTableRow().getItem() instanceof RowSeparatorWrapper ? FontWeight.BOLD : FontWeight.NORMAL, fontSize);
        }
        item.setFont(newFont);
        item.setStyle("-fx-padding: 1px 0 2px 0");
    }

    private String toStringParent(SystemEntityPath path) {
        if(path == null || path.getParent() == null) {
            return "";
        } else {
            return path.getParent().asString();
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
            // We need to do the following: find the parameter with the latest generation time (LGT) among all displayed parameters.
            // We request the retrieval of the state of the parameter linked to the LGT at the time LGT - 1 picosec, i.e. the first change going to the past.
            // But only of this parameter.
            // This retrieval will retrieve the previous state, which overrides the current one.
            fetchPreviousStateChange();
        }
    }

    protected void fetchNextStateChange() {
        markProgressBusy();
        // Retrieve the parameters sorted with the oldest generation time: this is a list sorted from oldest to latest
        List<ParameterData> oms = getParameterSamplesSortedByGenerationTimeAscending();
        if (oms.isEmpty()) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            // Keep looking for all parameters: as soon as one is returning a result, stop
            for (ParameterData om : oms) {
                try {
                    List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_FUTURE, new ParameterDataFilter(null, Collections.singletonList(om.getPath()), null, null, null, null));
                    if (!messages.isEmpty()) {
                        updateDataItems(messages);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            markProgressReady();
        });
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
            fetchNextStateChange();
        }
    }

    protected void fetchPreviousStateChange() {
        markProgressBusy();
        // Retrieve the parameter with the latest generation time
        ParameterData om = findOutLatestParameterSample();
        if (om == null) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_PAST, new ParameterDataFilter(null, Collections.singletonList(om.getPath()), null, null, null, null));
                updateDataItems(messages);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    protected void moveToTime(Instant selectedTime) {
        this.selectTimeBtn.setText(formatTime(selectedTime));
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(selectedTime, new ParameterDataFilter(null, new ArrayList<>(this.path2wrapper.keySet()), null, null, null, null));
                updateDataItems(messages);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    @FXML
    protected void onActionRemoveMenuItem(ActionEvent e) {
        // Get selected
        List<ParameterDataWrapper> selected = new ArrayList<>(this.dataItemTableView.getSelectionModel().getSelectedItems());
        // Remove them
        removeParameters(selected);
    }

    @FXML
    protected void onActionRemoveAllMenuItem(ActionEvent e) {
        // Get selected
        List<ParameterDataWrapper> selected = new ArrayList<>(this.dataItemTableView.getItems());
        // Remove them
        removeParameters(selected);
    }

    private void removeParameters(List<ParameterDataWrapper> selected) {
        if (selected == null || selected.isEmpty()) {
            return;
        }
        //
        if(this.liveTgl.isSelected()) {
            stopSubscription();
        }
        this.dataItemTableView.getItems().removeAll(selected);
        for (ParameterDataWrapper pdw : selected) {
            this.path2wrapper.remove(pdw.getPath());
        }
        updateFilter();
        if(this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    @FXML
    protected void onActionMoveUpMenuItem(ActionEvent e) {
        int selected = this.dataItemTableView.getSelectionModel().getSelectedIndex();
        if (selected >= 1) {
            ParameterDataWrapper pdw = this.dataItemTableView.getItems().get(selected);
            this.dataItemTableView.getItems().remove(selected);
            this.dataItemTableView.getItems().add(--selected, pdw);
            this.dataItemTableView.getSelectionModel().select(selected);
        }
    }

    @FXML
    protected void onActionMoveDownMenuItem(ActionEvent e) {
        int selected = this.dataItemTableView.getSelectionModel().getSelectedIndex();
        if (selected >= 0 && selected < this.dataItemTableView.getItems().size() - 1) {
            ParameterDataWrapper pdw = this.dataItemTableView.getItems().get(selected);
            this.dataItemTableView.getItems().remove(selected);
            this.dataItemTableView.getItems().add(++selected, pdw);
            this.dataItemTableView.getSelectionModel().select(selected);
        }
    }

    private void restoreColumnConfiguration() {
        if (this.system != null) {
            try {
                TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                e.printStackTrace();
                // Not important
            }
        }
    }

    private void persistColumnConfiguration() {
        if (this.system != null) {
            try {
                TableViewUtil.persistColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                e.printStackTrace();
                // Not important
            }
        }
    }

    public void startSubscription() {
        this.live = true;
    }

    public void stopSubscription() {
        this.live = false;
    }

    protected void updateFilter() {
        this.currentParameterFilter = new ParameterDataFilter(null, new ArrayList<>(this.path2wrapper.keySet()), null, null, null, null);
        // Update the subscriptions
        ParameterDisplayCoordinator.instance().filterUpdated();
    }

    protected void updateSelectTime() {
        // Take the latest generation time from the table
        if (this.dataItemTableView.getItems().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            ParameterData pd = findOutLatestParameterSample();
            if (pd == null || pd.getGenerationTime() == null) {
                this.selectTimeBtn.setText("---");
            } else {
                this.selectTimeBtn.setText(formatTime(pd.getGenerationTime()));
            }
        }
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        FxUtils.runLater(() -> {
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
        // If you are detached, close the stage
        if (independentStage != null) {
            independentStage.setOnCloseRequest(null);
            independentStage.close();
        }
    }

    public ParameterDataFilter getCurrentParameterFilter() {
        return currentParameterFilter;
    }

    public void setIndependentStage(Stage independentStage) {
        this.independentStage = independentStage;
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

    protected List<ParameterData> doRetrieve(ParameterData om, int n, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(om, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected List<ParameterData> doRetrieve(Instant selectedTime, ParameterDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(selectedTime, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected String doGetComponentId() {
        return "ParameterDataView";
    }

    private List<ParameterData> getParameterSamplesSortedByGenerationTimeAscending() {
        return this.path2wrapper.values().stream().map(ParameterDataWrapper::get).sorted((a, b) -> {
            int timeComparison = a.getGenerationTime().compareTo(b.getGenerationTime());
            if (timeComparison == 0) {
                if (a.getInternalId() != null && b.getInternalId() != null) {
                    return (int) (a.getInternalId().asLong() - b.getInternalId().asLong());
                } else if (a.getInternalId() != null) {
                    return 1;
                } else if (b.getInternalId() != null) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return timeComparison;
            }
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private ParameterData findOutLatestParameterSample() {
        ParameterData latest = null;
        for (ParameterDataWrapper pdw : this.path2wrapper.values()) {
            if (latest == null) {
                latest = pdw.get();
            } else if (pdw.get() != null) {
                if (latest.getGenerationTime() != null && pdw.get().getGenerationTime() != null) {
                    if (latest.getGenerationTime().isBefore(pdw.get().getGenerationTime()) ||
                            (latest.getInternalId() != null && pdw.get().getInternalId() != null && latest.getGenerationTime().equals(pdw.get().getGenerationTime()) && latest.getInternalId().asLong() < pdw.get().getInternalId().asLong())) {
                        latest = pdw.get();
                    }
                }
            }
        }
        return latest;
    }

    public void updateDataItems(List<ParameterData> messages) {
        FxUtils.runLater(() -> {
            for (ParameterData pd : messages) {
                ParameterDataWrapper pdw = this.path2wrapper.get(pd.getPath());
                if (pdw != null) {
                    pdw.set(pd);
                }
            }
            updateSelectTime();
        });
    }

    @FXML
    protected void onDragOver(DragEvent event) {
        if (event.getGestureSource() != this.dataItemTableView &&
                (
                        event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.PARAMETER)) ||
                                event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))
                )) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
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
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasContent(SystemEntityDataFormats.PARAMETER)) {
            addParameters((SystemEntity) db.getContent(SystemEntityDataFormats.PARAMETER));
            success = true;
        } else if (db.hasContent(SystemEntityDataFormats.CONTAINER)) {
            addContainer((List<SystemEntity>) db.getContent(SystemEntityDataFormats.CONTAINER));
            success = true;
        }

        event.setDropCompleted(success);

        event.consume();
    }

    private void addParameters(SystemEntity... systemEntities) {
        for (SystemEntity systemEntity : systemEntities) {
            if (systemEntity.getType() == SystemEntityType.PARAMETER) {
                if (!this.path2wrapper.containsKey(systemEntity.getPath())) {
                    // Add a fake item to initialise the entry
                    ParameterDataWrapper pdw = new ParameterDataWrapper(
                            new ParameterData(
                                    null,
                                    Instant.EPOCH,
                                    systemEntity.getExternalId(),
                                    systemEntity.getName(),
                                    systemEntity.getPath(),
                                    null,
                                    null,
                                    null,
                                    Validity.UNKNOWN, AlarmState.UNKNOWN, null,
                                    null, null),
                            systemEntity.getPath()
                    );
                    this.path2wrapper.put(systemEntity.getPath(), pdw);
                    this.dataItemTableView.getItems().add(pdw);
                }
            }
        }
        updateFilter();
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    private void addContainer(List<SystemEntity> list) {
        addParameters(list.toArray(new SystemEntity[list.size()]));
    }

    public void addItemsFromPreset(AndPreset p) {
        this.path2wrapper.clear();
        this.dataItemTableView.getItems().clear();
        for (AndPreset.Element systemEntity : p.getElements()) {
            SystemEntityPath sep = SystemEntityPath.fromString(systemEntity.getPath());
            int externalId = systemEntity.getId();
            if(systemEntity.getType() == AndPreset.TYPE_HEADING) {
                // Separator
                RowSeparatorWrapper pdw = new RowSeparatorWrapper(systemEntity.getPath());
                this.dataItemTableView.getItems().add(pdw);
            } else if (!this.path2wrapper.containsKey(sep)) {
                // Add a fake item to initialise the entry
                ParameterDataWrapper pdw = new ParameterDataWrapper(
                        new ParameterData(
                                null,
                                Instant.EPOCH,
                                externalId, //
                                sep.getLastPathElement(),
                                sep,
                                null,
                                null,
                                null,
                                Validity.UNKNOWN, AlarmState.UNKNOWN, null,
                                null, null),
                        sep
                );
                this.path2wrapper.put(sep, pdw);
                this.dataItemTableView.getItems().add(pdw);
            }
        }
        updateFilter();
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    public boolean isLive() {
        return this.live;
    }

    @Override
    public void dispose() {
        stopSubscription();
        ParameterDisplayCoordinator.instance().deregister(this);
        super.dispose();
    }

    public AndPreset getParameterDisplayDescription() {
        AndPreset props = new AndPreset();
        this.dataItemTableView.getItems().forEach((o) -> props.addElement(
                o instanceof RowSeparatorWrapper ? AndPreset.TYPE_HEADING : AndPreset.TYPE_ELEMENT,
                o.getPath().asString(),
                o.get().getExternalId()));
        return props;
    }

    @FXML
    public void onActionAddSeparatorMenuItem(ActionEvent actionEvent) {
        Optional<String> result = DialogUtils.input("", "Add table separator", null, "Please provide the text of the separator:");
        if (result.isPresent()){
            RowSeparatorWrapper row = new RowSeparatorWrapper(result.get());
            int selected = this.dataItemTableView.getSelectionModel().getSelectedIndex();
            if(selected == -1) { // Empty table or no selection
                this.dataItemTableView.getItems().add(row);
                this.dataItemTableView.getSelectionModel().select(0);
            } else {
                // Shift everything down
                this.dataItemTableView.getItems().add(selected, row);
                this.dataItemTableView.getSelectionModel().select(selected + 1);
            }
        }
    }

    @FXML
    private void locateItemAction(ActionEvent actionEvent) {
        ParameterDataWrapper ed = this.dataItemTableView.getSelectionModel().getSelectedItem();
        if(ed != null) {
            MainViewController.instance().getModelController().locate(ed.getPath());
        }
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
        Text text = new Text("AXq");
        text.setFont(f);
        new Scene(new Group(text));
        double height = text.getLayoutBounds().getHeight();
        dataItemTableView.setFixedCellSize(height + 4);
        dataItemTableView.refresh();
    }

    public static class ParameterDataWrapper {

        private final SystemEntityPath path;

        private final SimpleStringProperty description = new SimpleStringProperty();
        private final SimpleObjectProperty<ParameterData> property = new SimpleObjectProperty<>();

        private final SimpleObjectProperty<Instant> generationTime = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Instant> receptionTime = new SimpleObjectProperty<>();
        private final SimpleStringProperty rawValue = new SimpleStringProperty();
        private final SimpleStringProperty engValue = new SimpleStringProperty();
        private final SimpleObjectProperty<Validity> validity = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<AlarmState> alarmState = new SimpleObjectProperty<>();

        public ParameterDataWrapper(ParameterData data, SystemEntityPath path) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            receptionTime.set(data.getReceptionTime());
            rawValue.set(ValueUtil.toString(data.getSourceValue()));
            engValue.set(ValueUtil.toString(data.getEngValue()));
            validity.set(data.getValidity());
            alarmState.set(data.getAlarmState());
            this.path = path;
            if(data.getExternalId() != -1) {
                try {
                    AbstractSystemEntityDescriptor descriptor = MainViewController.instance().getModelController().getDescriptorOf(data.getExternalId());
                    if(descriptor instanceof ParameterDescriptor) {
                        description.set(((ParameterDescriptor) descriptor).getDescription());
                    }
                } catch (ReatmetricException | RemoteException e) {
                    description.set("");
                }
            } else {
                description.set("");
            }
        }

        public void set(ParameterData data) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            receptionTime.set(data.getReceptionTime());
            rawValue.set(ValueUtil.toString(data.getSourceValue()));
            engValue.set(ValueUtil.toString(data.getEngValue()));
            validity.set(data.getValidity());
            alarmState.set(data.getAlarmState());
        }

        public ParameterData get() {
            return property.getValue();
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }

        public SimpleObjectProperty<Instant> generationTimeProperty() {
            return generationTime;
        }

        public SimpleObjectProperty<Instant> receptionTimeProperty() {
            return receptionTime;
        }

        public SimpleStringProperty rawValueProperty() {
            return rawValue;
        }

        public SimpleStringProperty engValueProperty() {
            return engValue;
        }

        public SimpleObjectProperty<Validity> validityProperty() {
            return validity;
        }

        public SimpleObjectProperty<AlarmState> alarmStateProperty() {
            return alarmState;
        }

        public SystemEntityPath getPath() {
            return this.path;
        }

    }

    public static class RowSeparatorWrapper extends ParameterDataWrapper {

        public RowSeparatorWrapper(String name) {
            super(buildNullParameter(name), SystemEntityPath.fromString(name));
            generationTimeProperty().set(null);
        }

        private static ParameterData buildNullParameter(String name) {
            return new ParameterData(null, Instant.EPOCH, -1, name, null, "", "", null, null, null, null, null, null);
        }
    }

}
