/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Consumer;

import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.OrderedProperties;
import eu.dariolucia.reatmetric.ui.utils.PresetStorageManager;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import eu.dariolucia.reatmetric.ui.utils.TableViewUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Popup;

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
    
    // Additional header fields
    protected List<TableColumn> additionalHeaderFields = new ArrayList<>();

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
        this.engValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().get().getEngValue())));
        this.sourceValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().get().getSourceValue())));
        this.validityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getValidity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getGenerationTime()));
        this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getReceptionTime()));
        this.alarmStateCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getAlarmState()));
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getParent().asString()));
    }

    protected Consumer<List<ParameterData>> buildIncomingDataDelegatorAction() {
        return (List<ParameterData> t) -> {
            updateDataItems(t);
        };
    }
     
    @FXML
    protected void onActionSavePresetMenuItem(ActionEvent e) {
        if(!this.path2wrapper.isEmpty()) {
            TextInputDialog dialog = new TextInputDialog("PresetName");
            dialog.setTitle("Save Parameter Preset");
            dialog.setHeaderText("Parameter Preset");
            dialog.setContentText("Please provide the name of the preset:");

            // Traditional way to get the response value.
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()){
                Properties props = new OrderedProperties();
                this.dataItemTableView.getItems().forEach((o) -> props.put(o.getPath().asString(), ""));
                this.presetManager.save(system, user, result.get(), doGetComponentId(), props);
            }
        }
    }
    
    @FXML
    protected void onShowingPresetMenu(Event e) {
        this.loadPresetMenu.getItems().remove(0, this.loadPresetMenu.getItems().size());
        List<String> presets = this.presetManager.getAvailablePresets(system, user, doGetComponentId());
        for(String preset : presets) {
            final String fpreset = preset;
            MenuItem mi = new MenuItem(preset);
            mi.setOnAction((event) -> {
                Properties p = this.presetManager.load(system, user, fpreset, doGetComponentId());
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
            moveToTime(Instant.MIN);
        }
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if(!isProgressBusy()) {
            // We need to do the following: find the parameter with the latest reception time (LRT) among all displayed parameters.
            // We request the retrieval of the state of the parameter linked to the LRT at the time LRT - 1 picosec, i.e. the first change going to the past.
            // But only of this parameter.
            // This retrieval will retrieve the previous state, which overrides the current one.
            fetchPreviousStateChange();
        }
    }

    protected void fetchNextStateChange() {
        markProgressBusy();
        // Retrieve the parameter with the latest reception time
        ParameterData om = retrieveLatest();
        if(om == null) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_FUTURE, new ParameterDataFilter(new ArrayList<>(this.path2wrapper.keySet()), , ));
                updateDataItems(messages);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }
    
    @FXML
    protected void goToEnd(ActionEvent e) {
        if(!isProgressBusy()) {
            moveToTime(Instant.MAX);
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
        // Retrieve the parameter with the latest reception time
        ParameterData om = retrieveLatest();
        if(om == null) {
            markProgressReady();
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_PAST, new ParameterDataFilter(Collections.singletonList(om.getPath()), , ));
                updateDataItems(messages);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }
    
    protected void moveToTime(Instant selectedTime) {
        this.selectTimeBtn.setText(selectedTime.toString());
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(selectedTime, new ParameterDataFilter(new ArrayList<>(this.path2wrapper.keySet()), , ));
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
        TableViewUtil.restoreColumnConfiguration(this.system, this.user, doGetComponentId(), this.dataItemTableView);
    }
    
    private void persistColumnConfiguration() {
        TableViewUtil.persistColumnConfiguration(this.system, this.user, doGetComponentId(), this.dataItemTableView);
    }

    protected void informDataItemsReceived(List<ParameterData> objects) {
        this.delegator.delegate(objects);
    }
    
    private void startSubscription() {
        ParameterDataFilter pdf = new ParameterDataFilter(new ArrayList<>(this.path2wrapper.keySet()), , );
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
        // Take the latest reception time from the table 
        if (this.dataItemTableView.getItems().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            ParameterData pd = retrieveLatest();
            if(pd == null || pd.getReceptionTime() == null) {
                this.selectTimeBtn.setText("---");
            } else {
                this.selectTimeBtn.setText(pd.getReceptionTime().toString());
            }
        }
    }
    
    private void removeAdditionalHeaderFields() {
        this.dataItemTableView.getColumns().removeAll(this.additionalHeaderFields);
        this.additionalHeaderFields.clear();
        this.dataItemTableView.layout();
        this.dataItemTableView.refresh();
    }

    private void addAdditionalHeaderFields() {
        removeAdditionalHeaderFields();
        try {
            List<FieldDescriptor> items = doGetAdditionalFieldDescriptors();
            int counter = 0;
            for (FieldDescriptor item : items) {
                final int i = counter++;
                TableColumn<ParameterDataWrapper, String> tc = new TableColumn<>();
                tc.setText(item.getName());
                tc.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(String.valueOf(o.getValue().get().getAdditionalFields()[i])));
                tc.setResizable(true);
                tc.setSortable(false);
                this.dataItemTableView.getColumns().add(tc);
                this.additionalHeaderFields.add(tc);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    protected void doUserDisconnected(String system, String user) {
        this.liveTgl.setSelected(false);
        this.displayTitledPane.setDisable(true);
    }

    @Override
    protected void doUserConnected(String system, String user) {
        this.liveTgl.setSelected(true);
        this.displayTitledPane.setDisable(false);
    }

    @Override
    protected void doUserConnectionFailed(String system, String user, String reason) {
        this.liveTgl.setSelected(false);
        this.displayTitledPane.setDisable(true);
    }

    @Override
    protected void doServiceDisconnected(boolean oldState) {
        if(oldState) {
            persistColumnConfiguration();
        }
        // Remove the additional header fields
        removeAdditionalHeaderFields();
    }

    @Override
    protected void doServiceConnected(boolean oldState) {
        // Add the additional header fields
        addAdditionalHeaderFields();
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

    protected List<ParameterData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(selectedTime, n, direction, filter);
    }
    
    protected List<ParameterData> doRetrieve(Instant selectedTime, ParameterDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(selectedTime, filter);
    }

    protected Instant doGetReceptionTime(ParameterData om) {
        return om.getReceptionTime();
    }

    protected List<FieldDescriptor> doGetAdditionalFieldDescriptors() throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().getAdditionalFieldDescriptors();
    }

    protected String doGetComponentId() {
        return "ParameterDataView";
    }

    @Override
    protected ServiceType doGetSupportedService() {
        return ServiceType.PARAMETERS;
    }

    private ParameterData retrieveLatest() {
        ParameterData latest = null;
        for(ParameterDataWrapper pdw : this.path2wrapper.values()) {
            if(latest == null) {
                latest = pdw.get();
            } else if(pdw.get() != null) {
                if(latest.getReceptionTime() != null && pdw.get().getReceptionTime() != null) {
                    if(latest.getReceptionTime().isBefore(pdw.get().getReceptionTime())) {
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
        if (event.getGestureSource() != this.dataItemTableView &&
                (
                    event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.PARAMETER)) ||
                    event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))
                )) {
        }
        event.consume();
    }
    
    @FXML
    private void onDragExited(DragEvent event) {
        if (event.getGestureSource() != this.dataItemTableView &&
                (
                    event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.PARAMETER)) ||
                    event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))
                )) {
        }
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
                    // TODO: this sounds a bit ugly, find a better solution
                    ParameterDataWrapper pdw = new ParameterDataWrapper(
                            new ParameterData(
                            		null,
                                    null, systemEntity.getName(),
                                    systemEntity.getPath(), 
                                    null, 
                                    null, 
                                    null,
                                    Validity.UNKNOWN, AlarmState.UNKNOWN, systemEntity.getPath().getParent(),
                                    null),
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
            if(!this.path2wrapper.containsKey(sep)) {
                // TODO: this sounds a bit ugly, find a better solution
                ParameterDataWrapper pdw = new ParameterDataWrapper(
                        new ParameterData(
                        		null,
                                null, sep.getLastPathElement(),
                                sep, 
                                null, 
                                null, 
                                null,
                                Validity.UNKNOWN, AlarmState.UNKNOWN, sep.getParent(),
                                null),
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
    
    public class ParameterDataWrapper {
        
        private volatile ParameterData data;
        private final SystemEntityPath path;
        
        public ParameterDataWrapper(ParameterData data, SystemEntityPath path) {
            this.data = data;
            this.path = path;
        }
        
        public ParameterData get() {
            return this.data;
        }
        
        public void set(ParameterData pd) {
            this.data = pd;
        }
        
        public SystemEntityPath getPath() {
        	return this.path;
        }
    }
}
