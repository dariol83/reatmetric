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
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.*;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import eu.dariolucia.reatmetric.ui.widgets.EnhancedTitledPane;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class MimicsDisplayViewController extends AbstractDisplayController implements IParameterDataSubscriber {

    private static final Logger LOG = Logger.getLogger(MimicsDisplayViewController.class.getName());

    public static final String PRESET_STORAGE_LOCATION = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "presets";
    public static final String COMPONENT_ID = "MimicsDisplayView";

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
    protected VBox parentVBox;
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
    // Buffer map
    private final Map<SystemEntityPath, ParameterDataBuffer> path2buffer = new ConcurrentHashMap<>();

    // The mimics manager
    private MimicsSvgViewController mimicsManager;

    private Stage independentStage;
    private Tab mainTab;
    private String displayName;
    private String loadedMimicName;

    @Override
    protected Window retrieveWindow() {
        return displayTitledPane.getScene().getWindow();
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

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());
        // Initialise and add SVG viewer
        try {
            URL svgUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/MimicsSvgView.fxml");
            FXMLLoader loader = new FXMLLoader(svgUrl);
            Parent svgUrlNode = loader.load();
            CssHandler.applyTo(svgUrlNode);
            this.mimicsManager = loader.getController();
            this.innerBox.getChildren().addAll(svgUrlNode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MimicsDisplayCoordinator.instance().register(this);

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
            LOG.log(Level.SEVERE, "Cannot contact system", e);
            return;
        }

        this.loadMenu.getItems().clear();
        List<String> presets = getAvailablePresets(systemName, user);
        if(presets.isEmpty()) {
            MenuItem noPresets = new MenuItem("No Mimics available");
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
        if(isEmptyDisplay()) {
            open(preset);
        } else {
            AbstractDisplayController controller = MainViewController.instance().openPerspective("Mimics");
            if(controller instanceof MimicsDisplayViewController) {
                ((MimicsDisplayViewController) controller).open(preset);
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
            File presetFile = locateFile(systemName, preset);
            if(presetFile != null) {
                FxUtils.runLater(() -> loadPresetIntoDisplay(preset, presetFile));
            }
        }));
    }

    private void loadPresetIntoDisplay(String name, File svgFile) {
        loadPreset(svgFile);
        this.loadedMimicName = name;
        setDisplayName(name);
    }

    private File locateFile(String systemName, String preset) {
        File p = new File(PRESET_STORAGE_LOCATION + File.separator + systemName + File.separator + user + File.separator + doGetComponentId() + File.separator + preset + ".svg");
        if(p.exists()) {
            return p;
        } else {
            return null;
        }
    }

    public List<String> getAvailablePresets(String system, String user) {
        File folder = new File(PRESET_STORAGE_LOCATION + File.separator + system + File.separator + user + File.separator + doGetComponentId());
        if (!folder.exists()) {
            return Collections.emptyList();
        }
        List<String> presets = new LinkedList<>();
        File[] files = folder.listFiles();
        if(files == null) {
            return Collections.emptyList();
        }
        for (File f : files) {
            if (f.getName().endsWith("svg")) {
                presets.add(f.getName().substring(0, f.getName().length() - ".svg".length()));
            }
        }
        return presets;
    }

    @Override
    protected boolean closeRequested() {
        return isEmptyDisplay() || DialogUtils.confirm("Close Mimic", "About to close " + getDisplayName(), "Do you want to close " + getDisplayName() + "?");
    }

    private String getDisplayName() {
        return this.displayName == null ? "Mimic" : this.displayName;
    }

    private void setDisplayName(String name) {
        this.displayName = name;
        if(this.mainTab != null) {
            this.mainTab.setText(name);
        }
        if(this.displayTitledPane != null) {
            this.displayTitledPane.setText(name);
        }
        Window window = this.displayTitledPane != null && this.displayTitledPane.getScene() != null ? this.displayTitledPane.getScene().getWindow() : null;
        if(window instanceof Stage && DetachedTabUtil.isDetached((Stage) window)) {
            ((Stage) window).setTitle(name);
        }
    }

    private IReatmetricSystem getActiveSystem() {
        return this.system != null ? this.system : ReatmetricUI.selectedSystem().getSystem();
    }

    private boolean isEmptyDisplay() {
        return this.loadedMimicName == null;
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
            CssHandler.applyTo(this.dateTimePopup.getScene().getRoot());
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
        // For all parameters, check the next one
        List<ParameterData> oms = getCurrentParameterStates();
        if(oms.isEmpty()) {
            markProgressReady();
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            return;
        }
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            // For each parameter, retrieve the next sample. Render all parameters with the earliest generation time.
            List<ParameterDataBuffer> toPop = new LinkedList<>();
            Instant earliestGenerationTime = null;
            // Keep looking for all parameters: as soon as one is returning a result, stop
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
                updateDataItems(toPop.stream().map(ParameterDataBuffer::pop).collect(Collectors.toList()), false, true);
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
        // Retrieve the parameters with the latest generation time
        List<ParameterData> oms = findOutLatestParameterSamples();
        // Nothing to do
        if (oms.isEmpty()) {
            markProgressReady();
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            return;
        }
        // The parameters you found must be replaced on the mimics, maybe there is no other sample available
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
                synchronized (path2wrapper) {
                    // Reset buffers
                    path2buffer.values().forEach(ParameterDataBuffer::reset);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private Set<SystemEntityPath> getCurrentPaths() {
        synchronized (path2wrapper) {
            return new LinkedHashSet<>(path2wrapper.keySet());
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
        List<ParameterData> pds = findOutLatestParameterSamples();
        if(pds.isEmpty()) {
            DialogUtils.alert("No parameter set", "No parameter sample is present", "The mimics does not contain any parameter sample. Try selecting a specific date.");
            this.selectTimeBtn.setText("---");
        } else {
            this.selectTimeBtn.setText(formatTime(pds.get(0).getGenerationTime()));
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
    protected Node doBuildNodeForPrinting() {
        return mimicsManager.print();
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(false);
        this.innerBox.setDisable(true);
        stopSubscription();
        // If you are detached, close the stage
        if(independentStage != null) {
            independentStage.setOnCloseRequest(null);
            independentStage.close();
        }
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

    public void setIndependentStage(Stage independentStage) {
        this.independentStage = independentStage;
    }

    protected void doServiceSubscribe(ParameterDataFilter selectedFilter) throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this, selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
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
        return COMPONENT_ID;
    }

    private List<ParameterData> getCurrentParameterStates() {
        synchronized (path2wrapper) {
            return new ArrayList<>(path2wrapper.values());
        }
    }

    private List<ParameterData> findOutLatestParameterSamples() {
        List<ParameterData> latest = new LinkedList<>();
        Instant generationTime = null;
        List<ParameterData> currentState = getCurrentParameterStates();
        for(ParameterData pdw : currentState) {
            if(pdw.getGenerationTime() == null) {
                continue;
            }
            if (generationTime == null) {
                generationTime = pdw.getGenerationTime();
                latest.add(pdw);
            } else {
                if (pdw.getGenerationTime() != null) {
                    int compareResult = generationTime.compareTo(pdw.getGenerationTime());
                    if (compareResult < 0) {
                        // Reset list and update generation time
                        latest.clear();
                        generationTime = pdw.getGenerationTime();
                        latest.add(pdw);
                    } else if(compareResult == 0) {
                        // Same generation time, add to list
                        latest.add(pdw);
                    }
                }
            }
        }
        // Can be empty, callers must check
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
               FxUtils.runLater(() -> DialogUtils.alert("No more parameter samples", "No more parameter samples", "The archive does not contain additional parameter samples"));
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
       FxUtils.runLater(this::updateSelectTime);
    }

    private boolean isStateUpdate(ParameterData oldState, ParameterData pd) {
        return !Objects.equals(oldState.getEngValue(), pd.getEngValue()) || !Objects.equals(oldState.getSourceValue(), pd.getSourceValue()) || pd.getAlarmState() != oldState.getAlarmState() || pd.getValidity() != oldState.getValidity();
    }

    public void loadPreset(File svgFile) {
        stopSubscription();
        synchronized (path2wrapper) {
            this.path2wrapper.clear();
            this.path2buffer.values().forEach(ParameterDataBuffer::reset);
            this.path2buffer.clear();
        }
        // configure the mimics manager with the svgFile, get back the required parameters
        this.mimicsManager.configure(svgFile, this::updateParameters);
    }

    public void updateParameters(Set<String> parameters) {
        // update the subscription and add them to the path2wrapper map
        synchronized (path2wrapper) {
            for (String p : parameters) {
                SystemEntityPath path = SystemEntityPath.fromString(p);
                this.path2wrapper.put(path, new ParameterData(null, Instant.EPOCH, 0, path.getLastPathElement(), path, null, null, null, Validity.UNKNOWN, AlarmState.UNKNOWN, null, null, null));
                this.path2buffer.put(path, new ParameterDataBuffer(path, 10));
            }
        }
        // restart the subscription
        if(this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    @Override
    public void dispose() {
        stopSubscription();
        this.delegator.shutdown();
        this.mimicsManager.dispose();
        this.path2wrapper.clear();
        this.path2buffer.values().forEach(ParameterDataBuffer::reset);
        this.path2buffer.clear();
        MimicsDisplayCoordinator.instance().deregister(this);
        super.dispose();
    }

    private static ParameterData buildNullParameter(IUniqueId internalId, String name, SystemEntityPath path, int externalId) {
        return new ParameterData(internalId, Instant.EPOCH, externalId, name, path, null, null, null, null, null, null, null, null);
    }
}
