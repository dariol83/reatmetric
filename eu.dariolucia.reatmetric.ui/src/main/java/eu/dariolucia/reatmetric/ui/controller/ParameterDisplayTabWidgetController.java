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
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
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
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import eu.dariolucia.reatmetric.ui.widgets.EnhancedTitledPane;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDisplayTabWidgetController extends AbstractDisplayController {

    private static final Logger LOG = Logger.getLogger(ParameterDisplayTabWidgetController.class.getName());

    @FXML
    protected TitledPane displayTitledPane;
    @FXML
    protected ToolBar toolbar;
    @FXML
    protected CheckMenuItem toggleShowToolbarItem;
    @FXML
    protected Menu loadMenu;
    @FXML
    protected MenuItem detachMenuItem;
    @FXML
    protected Button renameButton;
    @FXML
    protected Button saveButton;

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

    private static final String CSS_PARAMETER_DISPLAY_DROP_BEFORE = "parameter-display-drop-before";
    private static final String CSS_PARAMETER_DISPLAY_DROP_AFTER = "parameter-display-drop-after";
    private static final String PARAMETER_DISPLAY_ROW_DRAG_KEY = "reatmetric-parameter-display-row-drag";

    private int dragDropInsertionIndex = -1;
    private TableRow<ParameterDataWrapper> dragDropTargetRow;
    private boolean dragDropBeforeRow;
    private List<ParameterDataWrapper> dragCandidateRows = Collections.emptyList();
    private List<ParameterDataWrapper> draggedRows = Collections.emptyList();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    // Model map
    private final Map<SystemEntityPath, ParameterDataWrapper> path2wrapper = new ConcurrentHashMap<>();
    // Buffer map
    private final Map<SystemEntityPath, ParameterDataBuffer> path2buffer = new ConcurrentHashMap<>();
    private final PresetStorageManager presetManager = new PresetStorageManager();

    private volatile boolean live = false;
    private Stage independentStage;
    private Tab mainTab;
    private String displayName;
    private String presetName;
    private boolean dirty;

    private double zoomFactor = Font.getDefault().getSize() * 10;

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.dataItemTableView.setPlaceholder(new Label(""));
        this.dataItemTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        initialiseParameterDisplayDragAndDrop();

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

        if(this.loadMenu != null) {
            this.loadMenu.setOnShowing(this::onShowingPresetMenu);
        }
        if(this.displayTitledPane != null && this.toolbar != null && this.toggleShowToolbarItem != null) {
            initialiseToolbarVisibility(this.displayTitledPane, this.toolbar, this.toggleShowToolbarItem);
            if(this.displayTitledPane instanceof EnhancedTitledPane &&
                    ((EnhancedTitledPane) this.displayTitledPane).getTitleContextMenu() != null) {
                ((EnhancedTitledPane) this.displayTitledPane).getTitleContextMenu().setOnShowing(this::onShowingPresetMenu);
            }
        }
        if(this.detachMenuItem != null) {
            this.detachMenuItem.setDisable(true);
        }

        Platform.runLater(() -> updateZoomFactor(0));
    }

    @Override
    protected void mainTabConfigured(Tab tab) {
        this.mainTab = tab;
        setDisplayName(tab.getText());
    }

    @FXML
    public void detachAttachItemAction(ActionEvent actionEvent) {
        Window window = retrieveWindow();
        if(window instanceof Stage && DetachedTabUtil.isDetached((Stage) window)) {
            DetachedTabUtil.attachTab((Stage) window);
            informDisplayAttached();
        }
    }

    @Override
    protected void informDisplayAttached() {
        if(this.detachMenuItem != null) {
            this.detachMenuItem.setDisable(true);
        }
    }

    @Override
    protected void informDisplayDetached() {
        if(this.detachMenuItem != null) {
            this.detachMenuItem.setDisable(false);
        }
    }

    private void onShowingPresetMenu(Event event) {
        IReatmetricSystem currentSystem = getActiveSystem();
        if(this.loadMenu == null || currentSystem == null) {
            if(this.loadMenu != null) {
                this.loadMenu.getItems().clear();
                MenuItem noSystem = new MenuItem("No system connected");
                noSystem.setDisable(true);
                this.loadMenu.getItems().add(noSystem);
            }
            return;
        }
        String systemName;
        try {
            systemName = currentSystem.getName();
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot show presets, error contacting system", e);
            return;
        }

        this.loadMenu.getItems().clear();
        List<String> presets = this.presetManager.getAvailablePresets(systemName, user, doGetComponentId());
        if(presets.isEmpty()) {
            MenuItem noPresets = new MenuItem("No ANDs available");
            noPresets.setDisable(true);
            this.loadMenu.getItems().add(noPresets);
            return;
        }
        for(String preset : presets) {
            MenuItem mi = new MenuItem(preset);
            mi.setOnAction(actionEvent -> openPresetFromMenu(preset));
            this.loadMenu.getItems().add(mi);
        }
    }

    private void openPresetFromMenu(String preset) {
        if(isPristineDisplay()) {
            open(preset);
        } else {
            AbstractDisplayController controller = MainViewController.instance().openPerspective("Monitoring AND");
            if(controller instanceof ParameterDisplayTabWidgetController) {
                ((ParameterDisplayTabWidgetController) controller).open(preset);
            }
        }
    }

    public void open(String preset) {
        FxUtils.runLater(() -> ReatmetricUI.threadPool(getClass()).submit(() -> {
            IReatmetricSystem currentSystem = getActiveSystem();
            if(currentSystem == null) {
                return;
            }
            String systemName;
            try {
                systemName = currentSystem.getName();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot show presets, error contacting system", e);
                return;
            }
            AndPreset loadedPreset = this.presetManager.load(systemName, user, preset, doGetComponentId(), AndPreset.class);
            if(loadedPreset != null) {
                FxUtils.runLater(() -> loadPresetIntoDisplay(preset, loadedPreset));
            }
        }));
    }

    private void loadPresetIntoDisplay(String name, AndPreset loadedPreset) {
        addItemsFromPreset(loadedPreset, false);
        this.presetName = name;
        setDisplayName(name);
        setDirty(false);
    }

    @FXML
    public void saveButtonClicked(ActionEvent actionEvent) {
        saveCurrentDisplay();
    }

    @FXML
    public void renameButtonClicked(ActionEvent actionEvent) {
        Optional<String> result = DialogUtils.input(getDisplayName(), "Rename AND", "Change name of the AND", "Please provide the name of the AND:");
        result.map(String::trim).filter(s -> !s.isEmpty()).ifPresent(s -> {
            this.presetName = s;
            setDisplayName(s);
        });
    }

    private boolean saveCurrentDisplay() {
        String nameToSave = this.presetName;
        if(nameToSave == null || nameToSave.trim().isEmpty()) {
            Optional<String> result = DialogUtils.input(getDisplayName(), "Save AND", "AND name", "Please provide the name of the AND:");
            if(result.isEmpty()) {
                return false;
            }
            nameToSave = result.get().trim();
            if(nameToSave.isEmpty()) {
                return false;
            }
        }

        IReatmetricSystem currentSystem = getActiveSystem();
        if(currentSystem == null) {
            DialogUtils.alert("Save AND", null, "Cannot save AND: no system connected");
            return false;
        }

        try {
            this.presetManager.save(currentSystem.getName(), user, nameToSave, doGetComponentId(), getParameterDisplayDescription());
            this.presetName = nameToSave;
            setDisplayName(nameToSave);
            setDirty(false);
            return true;
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot save preset, system not responding", e);
            DialogUtils.alert("Save AND", null, "Cannot save AND: system not responding");
            return false;
        }
    }

    @Override
    protected boolean closeRequested() {
        if(!this.dirty) {
            return true;
        }
        DialogUtils.SaveDiscardCancelResult result = DialogUtils.saveDiscardCancel(
                "Close AND",
                "About to close " + getDisplayName(),
                "The AND has unsaved changes."
        );
        switch (result) {
            case SAVE_AND_CLOSE:
                return saveCurrentDisplay();
            case DISCARD_AND_CLOSE:
                return true;
            case CANCEL:
            default:
                return false;
        }
    }

    private String getDisplayName() {
        return this.displayName == null ? "AND" : this.displayName;
    }

    private void setDisplayName(String name) {
        this.displayName = name;
        if(this.mainTab != null) {
            this.mainTab.setText(name);
        }
        if(this.displayTitledPane != null) {
            this.displayTitledPane.setText(name);
        }
        Window window = this.liveTgl != null && this.liveTgl.getScene() != null ? this.liveTgl.getScene().getWindow() : null;
        if(window instanceof Stage && DetachedTabUtil.isDetached((Stage) window)) {
            ((Stage) window).setTitle(name);
        }
    }

    private void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private IReatmetricSystem getActiveSystem() {
        return this.system != null ? this.system : ReatmetricUI.selectedSystem().getSystem();
    }

    private boolean isPristineDisplay() {
        return !this.dirty && this.presetName == null && this.dataItemTableView.getItems().isEmpty();
    }

    private void initialiseParameterDisplayDragAndDrop() {
        this.dataItemTableView.setRowFactory(tv -> {
            TableRow<ParameterDataWrapper> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!row.isEmpty() && row.isSelected()) {
                    this.dragCandidateRows = getSelectedRowsInTableOrder();
                } else {
                    this.dragCandidateRows = Collections.emptyList();
                }
            });
            row.setOnDragDetected(event -> {
                if (row.isEmpty()) {
                    return;
                }
                if (!row.isSelected()) {
                    this.dataItemTableView.getSelectionModel().clearSelection();
                    this.dataItemTableView.getSelectionModel().select(row.getIndex());
                }
                this.draggedRows = this.dragCandidateRows.contains(row.getItem()) ? this.dragCandidateRows : getSelectedRowsInTableOrder();
                if (this.draggedRows.isEmpty()) {
                    return;
                }
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(PARAMETER_DISPLAY_ROW_DRAG_KEY);
                db.setContent(content);
                event.consume();
            });
            row.setOnDragDone(event -> {
                clearDropTarget();
                this.dragCandidateRows = Collections.emptyList();
                this.draggedRows = Collections.emptyList();
                event.consume();
            });
            return row;
        });
    }

    private List<ParameterDataWrapper> getSelectedRowsInTableOrder() {
        return this.dataItemTableView.getSelectionModel().getSelectedItems().stream()
                .sorted(Comparator.comparingInt(this.dataItemTableView.getItems()::indexOf))
                .collect(Collectors.toList());
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
            // We need to do the following: find the parameters with the latest generation time (LGT) among all displayed parameters.
            // We request the retrieval of the state of those parameter linked to the LGT at the time LGT - 1 picosec, i.e. the first change going to the past.
            // But only of these parameter.
            // This retrieval will retrieve the previous state, which overrides the current one for those parameters
            fetchPreviousStateChange();
        }
    }

    protected void fetchNextStateChange() {
        markProgressBusy();
        // For all parameters, check the next one
        List<ParameterData> oms = this.path2wrapper.values().stream().map(ParameterDataWrapper::get).collect(Collectors.toList());
        if (oms.isEmpty()) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            // For each parameter, retrieve the next sample. Render all parameters with the earliest generation time.
            List<ParameterDataBuffer> toPop = new LinkedList<>();
            Instant earliestGenerationTime = null;
            for (ParameterData om : oms) {
                ParameterDataBuffer pdb = path2buffer.get(om.getPath());
                try {
                    // Next call might hit the archive
                    ParameterData nextSample = pdb.evaluateNext(om, RetrievalDirection.TO_FUTURE);
                    // End of stream
                    if(nextSample == null) {
                        continue;
                    }
                    if(earliestGenerationTime == null) {
                        earliestGenerationTime = nextSample.getGenerationTime();
                        toPop.add(pdb);
                    } else {
                        int compareResult = earliestGenerationTime.compareTo(nextSample.getGenerationTime());
                        if (compareResult > 0) {
                            // Reset list and update generation time
                            toPop.clear();
                            earliestGenerationTime = nextSample.getGenerationTime();
                            toPop.add(pdb);
                        } else if(compareResult == 0) {
                            // Same generation time, add to list
                            toPop.add(pdb);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(!toPop.isEmpty()) {
                updateDataItems(toPop.stream().map(ParameterDataBuffer::pop).collect(Collectors.toList()));
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
        // Retrieve the parameters with the latest generation time
        List<ParameterData> oms = findOutLatestParameterSamples();
        // Nothing to do
        if (oms.isEmpty()) {
            markProgressReady();
            return;
        }
        // The parameters you found must be removed from the AND, maybe there is no other sample available
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = new LinkedList<>();
                for(ParameterData om : oms) {
                    ParameterDataBuffer pdb = path2buffer.get(om.getPath());
                    // If there is one parameter, then a sample was found, add it. If not, add an empty ParameterData sample for om
                    if(pdb.evaluateNext(om, RetrievalDirection.TO_PAST) != null) {
                        messages.add(pdb.pop());
                    } else {
                        // Use last internal ID in the archive, so it can be moved again forward
                        messages.add(buildNullParameter(om.getInternalId(), om.getName(), om.getPath(), om.getExternalId()));
                    }
                }
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
                // Reset buffers
                path2buffer.values().forEach(ParameterDataBuffer::reset);
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
            ParameterDataBuffer removed = this.path2buffer.remove(pdw.getPath());
            if(removed != null) {
                removed.reset();
            }
        }
        updateFilter();
        // At this stage, clear selection to avoid false actual selection (the parameters are gone)
        this.dataItemTableView.getSelectionModel().clearSelection();
        if(this.liveTgl.isSelected()) {
            startSubscription();
        }
        setDirty(true);
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
            List<ParameterData> pds = findOutLatestParameterSamples();
            if (pds.isEmpty()) {
                this.selectTimeBtn.setText("---");
            } else {
                // Use the first item
                this.selectTimeBtn.setText(formatTime(pds.get(0).getGenerationTime()));
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

    private List<ParameterData> findOutLatestParameterSamples() {
        List<ParameterData> latest = new LinkedList<>();
        Instant generationTime = null;
        for (ParameterDataWrapper pdw : this.path2wrapper.values()) {
            if(pdw.get() == null) {
                continue;
            }
            if(pdw.get().getGenerationTime() == null) {
                continue;
            }
            if (generationTime == null) {
                generationTime = pdw.get().getGenerationTime();
                latest.add(pdw.get());
            } else {
                if (pdw.get().getGenerationTime() != null) {
                    int compareResult = generationTime.compareTo(pdw.get().getGenerationTime());
                    if (compareResult < 0) {
                        // Reset list and update generation time
                        latest.clear();
                        generationTime = pdw.get().getGenerationTime();
                        latest.add(pdw.get());
                    } else if(compareResult == 0) {
                        // Same generation time, add to list
                        latest.add(pdw.get());
                    }
                }
            }
        }
        // Can be empty, callers must check
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
        if (canAcceptParameterDisplayDrop(event)) {
            if (isInternalRowMove(event)) {
                event.acceptTransferModes(TransferMode.MOVE);
            } else {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            updateDropTarget(event);
        }
        event.consume();
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragExited(DragEvent event) {
        clearDropTarget();
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        int insertionIndex = this.dragDropInsertionIndex >= 0 ? this.dragDropInsertionIndex : resolveDropInsertionIndex(event);
        clearDropTarget();
        if (isInternalRowMove(event)) {
            moveSelectedRows(insertionIndex);
            success = true;
        } else {
            SystemEntityType type = db.hasContent(SystemEntityDataFormats.PARAMETER) ? SystemEntityType.PARAMETER : SystemEntityType.CONTAINER;
            List<SystemEntityDataFormats.EntityReference> entities = SystemEntityDataFormats.read(db, type);
            if (!entities.isEmpty()) {
                addParameters(insertionIndex, entities);
                success = true;
            }
        }

        event.setDropCompleted(success);

        event.consume();
    }

    private boolean canAcceptParameterDisplayDrop(DragEvent event) {
        return isInternalRowMove(event) || event.getGestureSource() != this.dataItemTableView &&
                (
                        event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.PARAMETER)) ||
                                event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))
                );
    }

    private boolean isInternalRowMove(DragEvent event) {
        return event.getDragboard().hasString() &&
                PARAMETER_DISPLAY_ROW_DRAG_KEY.equals(event.getDragboard().getString()) &&
                !this.draggedRows.isEmpty();
    }

    private void updateDropTarget(DragEvent event) {
        int insertionIndex = resolveDropInsertionIndex(event);
        TableRow<ParameterDataWrapper> targetRow = findDropTargetRow(event);
        if (targetRow != null && targetRow.isEmpty()) {
            targetRow = findVisibleBoundaryDropTargetRow(insertionIndex);
        } else if (targetRow == null) {
            targetRow = findVisibleBoundaryDropTargetRow(insertionIndex);
        }

        boolean beforeRow = targetRow != null && insertionIndex == targetRow.getIndex();
        if (this.dragDropTargetRow != targetRow || this.dragDropBeforeRow != beforeRow) {
            clearDropTargetStyle();
            this.dragDropTargetRow = targetRow;
            this.dragDropBeforeRow = beforeRow;
            if (this.dragDropTargetRow != null) {
                this.dragDropTargetRow.getStyleClass().add(beforeRow ? CSS_PARAMETER_DISPLAY_DROP_BEFORE : CSS_PARAMETER_DISPLAY_DROP_AFTER);
            }
        }
        this.dragDropInsertionIndex = insertionIndex;
    }

    private int resolveDropInsertionIndex(DragEvent event) {
        TableRow<ParameterDataWrapper> row = findDropTargetRow(event);
        if (row == null || row.isEmpty()) {
            return resolveDropInsertionIndexOutsideRow(event);
        }
        int rowIndex = Math.max(0, Math.min(row.getIndex(), this.dataItemTableView.getItems().size()));
        Point2D rowPoint = row.sceneToLocal(event.getSceneX(), event.getSceneY());
        return rowPoint.getY() < row.getHeight() / 2 ? rowIndex : Math.min(rowIndex + 1, this.dataItemTableView.getItems().size());
    }

    private TableRow<ParameterDataWrapper> findDropTargetRow(DragEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        while (node != null && node != this.dataItemTableView) {
            if (node instanceof TableRow) {
                return (TableRow<ParameterDataWrapper>) node;
            }
            node = node.getParent();
        }
        return null;
    }

    private int resolveDropInsertionIndexOutsideRow(DragEvent event) {
        List<TableRow<ParameterDataWrapper>> visibleRows = getVisibleDropRows();
        if (visibleRows.isEmpty()) {
            return this.dataItemTableView.getItems().size();
        }
        for (TableRow<ParameterDataWrapper> row : visibleRows) {
            Bounds rowBounds = row.localToScene(row.getBoundsInLocal());
            if (event.getSceneY() < rowBounds.getMinY() + rowBounds.getHeight() / 2) {
                return row.getIndex();
            }
        }
        return this.dataItemTableView.getItems().size();
    }

    private TableRow<ParameterDataWrapper> findVisibleBoundaryDropTargetRow(int insertionIndex) {
        List<TableRow<ParameterDataWrapper>> visibleRows = getVisibleDropRows();
        if (visibleRows.isEmpty()) {
            return null;
        }
        if (insertionIndex <= visibleRows.get(0).getIndex()) {
            return visibleRows.get(0);
        }
        if (insertionIndex >= visibleRows.get(visibleRows.size() - 1).getIndex() + 1) {
            return visibleRows.get(visibleRows.size() - 1);
        }
        return visibleRows.stream()
                .filter(row -> row.getIndex() == insertionIndex)
                .findFirst()
                .orElse(null);
    }

    private List<TableRow<ParameterDataWrapper>> getVisibleDropRows() {
        return this.dataItemTableView.lookupAll(".table-row-cell").stream()
                .filter(node -> node instanceof TableRow)
                .map(node -> (TableRow<ParameterDataWrapper>) node)
                .filter(row -> !row.isEmpty() && row.getIndex() >= 0 && row.getIndex() < this.dataItemTableView.getItems().size())
                .sorted(Comparator.comparingInt(TableRow::getIndex))
                .collect(Collectors.toList());
    }

    private void clearDropTarget() {
        clearDropTargetStyle();
        this.dragDropInsertionIndex = -1;
    }

    private void clearDropTargetStyle() {
        if (this.dragDropTargetRow != null) {
            this.dragDropTargetRow.getStyleClass().remove(CSS_PARAMETER_DISPLAY_DROP_BEFORE);
            this.dragDropTargetRow.getStyleClass().remove(CSS_PARAMETER_DISPLAY_DROP_AFTER);
            this.dragDropTargetRow = null;
        }
        this.dragDropBeforeRow = false;
    }

    private void moveSelectedRows(int insertionIndex) {
        if (this.draggedRows.isEmpty()) {
            return;
        }
        List<ParameterDataWrapper> items = this.dataItemTableView.getItems();
        List<ParameterDataWrapper> rowsToMove = this.draggedRows.stream()
                .filter(items::contains)
                .sorted(Comparator.comparingInt(items::indexOf))
                .collect(Collectors.toList());
        if (rowsToMove.isEmpty()) {
            return;
        }

        int boundedInsertionIndex = Math.max(0, Math.min(insertionIndex, items.size()));
        int rowsBeforeInsertionIndex = (int) rowsToMove.stream()
                .mapToInt(items::indexOf)
                .filter(index -> index >= 0 && index < boundedInsertionIndex)
                .count();
        int effectiveInsertionIndex = boundedInsertionIndex - rowsBeforeInsertionIndex;

        items.removeAll(rowsToMove);
        effectiveInsertionIndex = Math.max(0, Math.min(effectiveInsertionIndex, items.size()));
        items.addAll(effectiveInsertionIndex, rowsToMove);

        this.dataItemTableView.getSelectionModel().clearSelection();
        for (int i = 0; i < rowsToMove.size(); ++i) {
            this.dataItemTableView.getSelectionModel().select(effectiveInsertionIndex + i);
        }
        setDirty(true);
    }

    private void addParameters(int insertionIndex, List<SystemEntityDataFormats.EntityReference> systemEntities) {
        int effectiveInsertionIndex = Math.max(0, Math.min(insertionIndex, this.dataItemTableView.getItems().size()));
        boolean added = false;
        for (SystemEntityDataFormats.EntityReference systemEntity : systemEntities) {
            if (systemEntity.type() == SystemEntityType.PARAMETER) {
                if (!this.path2wrapper.containsKey(systemEntity.path())) {
                    // Add a fake item to initialise the entry
                    ParameterDataWrapper pdw = new ParameterDataWrapper(
                            new ParameterData(
                                    null,
                                    Instant.EPOCH,
                                    systemEntity.externalId(),
                                    systemEntity.path().getLastPathElement(),
                                    systemEntity.path(),
                                    null,
                                    null,
                                    null,
                                    Validity.UNKNOWN, AlarmState.UNKNOWN, null,
                                    null, null),
                            systemEntity.path()
                    );
                    this.path2wrapper.put(systemEntity.path(), pdw);
                    this.path2buffer.put(systemEntity.path(), new ParameterDataBuffer(systemEntity.path(), 10));
                    this.dataItemTableView.getItems().add(effectiveInsertionIndex++, pdw);
                    added = true;
                }
            }
        }
        updateFilter();
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
        if(added) {
            setDirty(true);
        }
    }

    public void addItemsFromPreset(AndPreset p) {
        addItemsFromPreset(p, false);
    }

    private void addItemsFromPreset(AndPreset p, boolean dirtyAfterLoad) {
        this.path2wrapper.clear();
        this.path2buffer.clear();
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
                this.path2buffer.put(sep, new ParameterDataBuffer(sep, 10));
                this.dataItemTableView.getItems().add(pdw);
            }
        }
        updateFilter();
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
        setDirty(dirtyAfterLoad);
    }

    public boolean isLive() {
        return this.live;
    }

    @Override
    public void dispose() {
        stopSubscription();
        path2buffer.values().forEach(ParameterDataBuffer::reset);
        path2buffer.clear();
        path2wrapper.clear();
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
            setDirty(true);
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
    }

    private static ParameterData buildNullParameter(String name) {
        return new ParameterData(null, Instant.EPOCH, -1, name, null, "", "", null, null, null, null, null, null);
    }

    private static ParameterData buildNullParameter(IUniqueId internalId, String name, SystemEntityPath path, int externalId) {
        return new ParameterData(internalId, Instant.EPOCH, externalId, name, path, null, null, null, null, null, null, null, null);
    }
}
