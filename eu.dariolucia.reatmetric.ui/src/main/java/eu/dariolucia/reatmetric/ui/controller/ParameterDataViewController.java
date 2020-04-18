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
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.*;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDataViewController extends AbstractDisplayController implements IParameterDataSubscriber {

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

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
    
    // Preset menu
    @FXML
    private Menu loadPresetMenu;

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;
    
    // Temporary object queue
    protected DataProcessingDelegator<ParameterData> delegator;
    
    // Model map
    private final Map<SystemEntityPath, ParameterDataWrapper> path2wrapper = new TreeMap<>();
    
    // Preset manager
    private final PresetStorageManager presetManager = new PresetStorageManager();

    @Override
    protected Window retrieveWindow() {
        return displayTitledPane.getScene().getWindow();
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
            this.dateTimePickerController = (DateTimePickerWidgetController) loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                moveToTime(this.dateTimePickerController.getSelectedTime());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
        
        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getName()));
        this.engValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().get().getEngValue())));
        this.sourceValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().get().getSourceValue())));
        this.validityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getValidity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getGenerationTime()));
        this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getReceptionTime()));
        this.alarmStateCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getAlarmState()));
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getPath().getParent().asString()));

        this.genTimeCol.setCellFactory(new InstantCellFactory<>());
        this.recTimeCol.setCellFactory(new InstantCellFactory<>());

        final ObservableList<ParameterDataWrapper> dataList = FXCollections.observableArrayList(
                new Callback<ParameterDataWrapper, Observable[]>() {
                    @Override
                    public Observable[] call(ParameterDataWrapper param) {
                        return new Observable[]{
                                param.property()
                        };
                    }
                }
        );
        this.dataItemTableView.setItems(dataList);
    }

    protected Consumer<List<ParameterData>> buildIncomingDataDelegatorAction() {
        return this::updateDataItems;
    }
     
    @FXML
    protected void onActionSavePresetMenuItem(ActionEvent e) {
        if(!this.path2wrapper.isEmpty()) {
            Optional<String> result = DialogUtils.input("PresetName", "Save Parameter Preset", "Parameter Preset", "Please provide the name of the preset:");
            if (result.isPresent()){
                Properties props = new OrderedProperties();
                this.dataItemTableView.getItems().forEach((o) -> props.put(o.getPath().asString(), String.valueOf(o.get().getExternalId())));
                this.presetManager.save(system.getName(), user, result.get(), doGetComponentId(), props);
            }
        }
    }
    
    @FXML
    protected void onShowingPresetMenu(Event e) {
        this.loadPresetMenu.getItems().remove(0, this.loadPresetMenu.getItems().size());
        List<String> presets = this.presetManager.getAvailablePresets(system.getName(), user, doGetComponentId());
        for(String preset : presets) {
            final String fpreset = preset;
            MenuItem mi = new MenuItem(preset);
            mi.setOnAction((event) -> {
                Properties p = this.presetManager.load(system.getName(), user, fpreset, doGetComponentId());
                if(p != null) {
                    addItemsFromPreset(p);
                }
            });
            this.loadPresetMenu.getItems().add(mi);
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
            this.dateTimePopup.show(this.displayTitledPane.getScene().getWindow());
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
        if(!isProgressBusy()) {
            moveToTime(Instant.EPOCH);
        }
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if(!isProgressBusy()) {
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
        if(oms.isEmpty()) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            // Keep looking for all parameters: as soon as one is returning a result, stop
            for (ParameterData om : oms) {
                try {
                    System.out.println("Retrieving previous of " + om);
                    List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_FUTURE, new ParameterDataFilter(null, Collections.singletonList(om.getPath()), null, null, null, null));
                    System.out.println("Returned " + messages);
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
        if(!isProgressBusy()) {
            moveToTime(Instant.ofEpochSecond(3600*24*365*1000L));
        }
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if(!isProgressBusy()) {
            fetchNextStateChange();
        }
    }
    
    protected void fetchPreviousStateChange() {
        markProgressBusy();
        // Retrieve the parameter with the latest generation time
        ParameterData om = findOutLatestParameterSample();
        if(om == null) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_PAST, new ParameterDataFilter(null, Collections.singletonList(om.getPath()),null,null,null, null));
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
                List<ParameterData> messages = doRetrieve(selectedTime, new ParameterDataFilter(null, new ArrayList<>(this.path2wrapper.keySet()),null,null,null, null));
                updateDataItems(messages);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }
    
    @FXML
    protected void onActionRemoveMenuItem(ActionEvent e)
    {
    	// Get selected
    	List<ParameterDataWrapper> selected = new ArrayList<>(this.dataItemTableView.getSelectionModel().getSelectedItems());
    	// Remove them
    	removeParameters(selected);
    }

	@FXML
    protected void onActionRemoveAllMenuItem(ActionEvent e)
    {
    	// Get selected
    	List<ParameterDataWrapper> selected = new ArrayList<>(this.dataItemTableView.getItems());
    	// Remove them
    	removeParameters(selected);
    }

	private void removeParameters(List<ParameterDataWrapper> selected) {
		if(selected == null || selected.isEmpty()) {
			return;
		}
		//	
		stopSubscription();
		this.dataItemTableView.getItems().removeAll(selected);
		for(ParameterDataWrapper pdw : selected) {
			this.path2wrapper.remove(pdw.getPath());
		}
		startSubscription();
	}
	
    @FXML
    protected void onActionMoveUpMenuItem(ActionEvent e)
    {
    	int selected = this.dataItemTableView.getSelectionModel().getSelectedIndex();
    	if(selected >= 1) {
    		ParameterDataWrapper pdw = this.dataItemTableView.getItems().get(selected);
    		this.dataItemTableView.getItems().remove(selected);
    		this.dataItemTableView.getItems().add(--selected, pdw);
    	}
    }
    
    @FXML
    protected void onActionMoveDownMenuItem(ActionEvent e)
    {
    	int selected = this.dataItemTableView.getSelectionModel().getSelectedIndex();
    	if(selected >= 0 && selected < this.dataItemTableView.getItems().size() - 1) {
    		ParameterDataWrapper pdw = this.dataItemTableView.getItems().get(selected);
    		this.dataItemTableView.getItems().remove(selected);
    		this.dataItemTableView.getItems().add(++selected, pdw);
    	}
    }
    
    @Override
    public void dataItemsReceived(List<ParameterData> messages) {
        informDataItemsReceived(messages);
    }

    private void restoreColumnConfiguration() {
        if(this.system != null) {
            TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }
    
    private void persistColumnConfiguration() {
        if(this.system != null) {
            TableViewUtil.persistColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }

    protected void informDataItemsReceived(List<ParameterData> objects) {
        this.delegator.delegate(objects);
    }
    
    private void startSubscription() {
        ParameterDataFilter pdf = new ParameterDataFilter(null, new ArrayList<>(this.path2wrapper.keySet()),null,null ,null, null);
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                doServiceSubscribe(pdf);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                doServiceUnsubscribe();
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    protected void updateSelectTime() {
        // Take the latest generation time from the table
        if (this.dataItemTableView.getItems().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            ParameterData pd = findOutLatestParameterSample();
            if(pd == null || pd.getGenerationTime() == null) {
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
        this.displayTitledPane.setDisable(true);
        if(oldStatus) {
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
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    protected void doServiceSubscribe(ParameterDataFilter selectedFilter) throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this, selectedFilter);
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this);
    }

    protected List<ParameterData> doRetrieve(ParameterData om, int n, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(om, n, direction, filter);
    }
    
    protected List<ParameterData> doRetrieve(Instant selectedTime, ParameterDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(selectedTime, filter);
    }

    protected String doGetComponentId() {
        return "ParameterDataView";
    }

    private List<ParameterData> getParameterSamplesSortedByGenerationTimeAscending() {
        List<ParameterData> data = this.path2wrapper.values().stream().map(ParameterDataWrapper::get).collect(Collectors.toCollection(ArrayList::new));
        data.sort((a,b) -> {
            int timeComparison = a.getGenerationTime().compareTo(b.getGenerationTime());
            if(timeComparison == 0) {
                if(a.getInternalId() != null && b.getInternalId() != null) {
                    return (int) (a.getInternalId().asLong() - b.getInternalId().asLong());
                } else if(a.getInternalId() != null) {
                    return 1;
                } else if(b.getInternalId() != null) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return timeComparison;
            }
        });
        return data;
    }

    private ParameterData findOutLatestParameterSample() {
        ParameterData latest = null;
        for(ParameterDataWrapper pdw : this.path2wrapper.values()) {
            if(latest == null) {
                latest = pdw.get();
            } else if(pdw.get() != null) {
                if(latest.getGenerationTime() != null && pdw.get().getGenerationTime() != null) {
                    if(latest.getGenerationTime().isBefore(pdw.get().getGenerationTime()) ||
                            (latest.getInternalId() != null && pdw.get().getInternalId() != null && latest.getGenerationTime().equals(pdw.get().getGenerationTime()) && latest.getInternalId().asLong() < pdw.get().getInternalId().asLong())) {
                        latest = pdw.get();
                    }
                }
            }
        }
        return latest;
    }

    private void updateDataItems(List<ParameterData> messages) {
        Platform.runLater(() -> {
           for(ParameterData pd : messages) {
               ParameterDataWrapper pdw = this.path2wrapper.get(pd.getPath());
               if(pdw != null) {
                   pdw.set(pd);
               }
           }
           this.dataItemTableView.refresh();
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
            addParameters((SystemEntity)db.getContent(SystemEntityDataFormats.PARAMETER));
            success = true;
        } else if(db.hasContent(SystemEntityDataFormats.CONTAINER)) {
            addContainer((List<SystemEntity>)db.getContent(SystemEntityDataFormats.CONTAINER));
            success = true;
        }

        event.setDropCompleted(success);
        
        event.consume();
    }

    private void addParameters(SystemEntity... systemEntities) {
        for(SystemEntity systemEntity : systemEntities) {
            if(systemEntity.getType() == SystemEntityType.PARAMETER) {
                if(!this.path2wrapper.containsKey(systemEntity.getPath())) {
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
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }
    
    private void addContainer(List<SystemEntity> list) {
        addParameters(list.toArray(new SystemEntity[list.size()]));
    }

    private void addItemsFromPreset(Properties p) {
        this.path2wrapper.clear();
        this.dataItemTableView.getItems().clear();
        for(Object systemEntity : p.keySet()) {
            SystemEntityPath sep = SystemEntityPath.fromString(systemEntity.toString());
            int externalId = Integer.parseInt(p.getProperty(sep.toString()));
            if(!this.path2wrapper.containsKey(sep)) {
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
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }
    
    public static class ParameterDataWrapper {
        
        private final SystemEntityPath path;
        private final SimpleObjectProperty<ParameterData> property = new SimpleObjectProperty<>();

        public ParameterDataWrapper(ParameterData data, SystemEntityPath path) {
            property.set(data);
            this.path = path;
        }

        public void set(ParameterData pd) {
            property.set(pd);
        }

        public ParameterData get() {
            return property.getValue();
        }

        public SimpleObjectProperty<ParameterData> property() {
            return property;
        }

        public SystemEntityPath getPath() {
        	return this.path;
        }
    }
}
