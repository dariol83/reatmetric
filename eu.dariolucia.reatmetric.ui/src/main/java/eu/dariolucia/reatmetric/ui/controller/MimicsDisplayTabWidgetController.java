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
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.controlsfx.control.ToggleSwitch;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class MimicsDisplayTabWidgetController extends AbstractDisplayController implements IParameterDataSubscriber {

    // Inner contents
    @FXML
    protected VBox innerBox;

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

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;
    
    // Temporary object queue
    protected DataProcessingDelegator<ParameterData> delegator;

    // Model map
    private final Map<SystemEntityPath, ParameterData> path2wrapper = new TreeMap<>();

    // The mimics manager
    private MimicsSvgViewController mimicsManager;

    @Override
    protected Window retrieveWindow() {
        return innerBox.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
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
        
        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
        // Initialise and add SVG viewer
        try {
            URL svgUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/MimicsSvgView.fxml");
            FXMLLoader loader = new FXMLLoader(svgUrl);
            Parent svgUrlNode = loader.load();
            this.mimicsManager = loader.getController();
            this.innerBox.getChildren().addAll(svgUrlNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Consumer<List<ParameterData>> buildIncomingDataDelegatorAction() {
        return o -> updateDataItems(o, false, false);
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
            this.dateTimePopup.show(this.innerBox.getScene().getWindow());
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
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            // Keep looking for all parameters: as soon as one is returning a result, stop
            for (ParameterData om : oms) {
                try {
                    List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_FUTURE, new ParameterDataFilter(null, Collections.singletonList(om.getPath()), null, null, null, null));
                    if (!messages.isEmpty()) {
                        updateDataItems(messages, false, true);
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
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<ParameterData> messages = doRetrieve(om, 1, RetrievalDirection.TO_PAST, new ParameterDataFilter(null, Collections.singletonList(om.getPath()),null,null,null, null));
                updateDataItems(messages, false, true);
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
                List<ParameterData> messages = doRetrieve(selectedTime, new ParameterDataFilter(null, new ArrayList<>(getCurrentPaths()), null, null, null, null));
                updateDataItems(messages, true, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private Set<SystemEntityPath> getCurrentPaths() {
        synchronized (path2wrapper) {
            return path2wrapper.keySet();
        }
    }

    @Override
    public void dataItemsReceived(List<ParameterData> messages) {
        informDataItemsReceived(messages);
    }

    protected void informDataItemsReceived(List<ParameterData> objects) {
        this.delegator.delegate(objects);
    }
    
    private void startSubscription() {
        ParameterDataFilter pdf = new ParameterDataFilter(null, new ArrayList<>(getCurrentPaths()),null,null ,null, null);
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
        ParameterData pd = findOutLatestParameterSample();
        if(pd == null || pd.getGenerationTime() == null) {
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            this.selectTimeBtn.setText("---");
        } else {
            this.selectTimeBtn.setText(formatTime(pd.getGenerationTime()));
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
    protected Node doBuildNodeForPrinting() {
        return mimicsManager.print();
    }
    
    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(false);
        this.innerBox.setDisable(true);
        stopSubscription();
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(true);
        this.innerBox.setDisable(false);
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
        return "MimicsTabView";
    }

    private List<ParameterData> getParameterSamplesSortedByGenerationTimeAscending() {
        List<ParameterData> data = new ArrayList<>(getCurrentParameterStates());
        // Strip out parameters without internal ID (null parameters)
        data.removeIf(o -> o.getInternalId() == null);
        if(data.isEmpty()) {
            return data;
        } else {
            data.sort((a, b) -> {
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
            });
        }
        return data;
    }

    private List<ParameterData> getCurrentParameterStates() {
        synchronized (path2wrapper) {
            return new ArrayList<>(path2wrapper.values());
        }
    }

    private ParameterData findOutLatestParameterSample() {
        ParameterData latest = null;
        List<ParameterData> currentState = getCurrentParameterStates();
        currentState.removeIf(o -> o == null || o.getInternalId() == null);
        for(ParameterData pdw : currentState) {
            if(latest == null) {
                latest = pdw;
            } else if(pdw != null) {
                if(latest.getGenerationTime() != null && pdw.getGenerationTime() != null) {
                    if(latest.getGenerationTime().isBefore(pdw.getGenerationTime()) ||
                            (latest.getInternalId() != null && pdw.getInternalId() != null && latest.getGenerationTime().equals(pdw.getGenerationTime()) && latest.getInternalId().asLong() < pdw.getInternalId().asLong())) {
                        latest = pdw;
                    }
                }
            }
        }
        return latest;
    }

    // Typically called from outside the UI thread: in normal situations (log displays) this call is immediately
    // delegated to the UI thread. In this situation though, we perform the checks on the separate thread.
    private void updateDataItems(List<ParameterData> messages, boolean resetState, boolean fromArchive) {
       Map<SystemEntityPath, ParameterData> updatedData = new LinkedHashMap<>(messages.size());
       synchronized (path2wrapper) {
           if(resetState) {
               // Set path2wrapper to all nulls
               Set<SystemEntityPath> keys = new TreeSet<>(path2wrapper.keySet());
               for(SystemEntityPath sp : keys) {
                   ParameterData nullData = new ParameterData(null, Instant.EPOCH, 0, sp.getLastPathElement(), sp, null, null, null, Validity.UNKNOWN, AlarmState.UNKNOWN, null, null, null);
                   path2wrapper.put(sp, nullData);
                   updatedData.put(sp, nullData);
               }
               //
           }
           // If from archive and the state is not reset, then raise a warning
           if(messages.isEmpty() && fromArchive && !resetState) {
               Platform.runLater(() -> DialogUtils.alert("No more parameter samples", "No more parameter samples", "The archive does not contain additional parameter samples"));
               return;
           }
           // Just check what is different and what is not different, and inform the mimics manager
           for (ParameterData pd : messages) {
               ParameterData oldState = path2wrapper.get(pd.getPath());
               if (oldState == null || isStateUpdate(oldState, pd)) {
                   updatedData.put(pd.getPath(), pd);
               }
               path2wrapper.put(pd.getPath(), pd);
           }
       }
       // Inform the mimics manager about the changed parameters
       this.mimicsManager.refresh(updatedData);
       // Next shall be run in the UI thread
       Platform.runLater(this::updateSelectTime);
    }

    private boolean isStateUpdate(ParameterData oldState, ParameterData pd) {
        return !Objects.equals(oldState.getEngValue(), pd.getEngValue()) || !Objects.equals(oldState.getSourceValue(), pd.getSourceValue()) || pd.getAlarmState() != oldState.getAlarmState() || pd.getValidity() != oldState.getValidity();
    }

    public void loadPreset(File svgFile) {
        // configure the mimics manager with the svgFile, get back the required parameters
        this.mimicsManager.configure(svgFile, this::updateParameters);
    }

    public void updateParameters(Set<String> parameters) {
        // update the subscription and add them to the path2wrapper map
        synchronized (path2wrapper) {
            for (String p : parameters) {
                SystemEntityPath path = SystemEntityPath.fromString(p);
                this.path2wrapper.put(path, new ParameterData(null, Instant.EPOCH, 0, path.getLastPathElement(), path, null, null, null, Validity.UNKNOWN, AlarmState.UNKNOWN, null, null, null));
            }
        }
        // restart the subscription
        if(this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    public void dispose() {
        this.mimicsManager.dispose();
    }
}
