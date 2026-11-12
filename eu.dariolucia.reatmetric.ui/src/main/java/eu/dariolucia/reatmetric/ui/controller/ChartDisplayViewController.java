/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.udd.*;
import eu.dariolucia.reatmetric.ui.utils.ChartPreset;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.PresetStorageManager;
import eu.dariolucia.reatmetric.ui.utils.UserDisplayCoordinator;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import eu.dariolucia.reatmetric.ui.widgets.EnhancedTitledPane;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ChartDisplayViewController extends AbstractDisplayController implements IChartDisplayController {

    private static final Logger LOG = Logger.getLogger(ChartDisplayViewController.class.getName());

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
    protected ToggleButton liveTimeTgl;

    @FXML
    protected VBox parentVBox;

    @FXML
    protected VBox innerBox;

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    //
    protected final List<AbstractChartManager> charts = new CopyOnWriteArrayList<>();

    protected volatile ParameterDataFilter currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(), null, null, null, null);

    protected volatile EventDataFilter currentEventFilter = new EventDataFilter(null, new LinkedList<>(), null, null, null, null, null);

    protected final int timeWindowSize = 60000;

    protected final int timeUnit = 5000;

    private volatile Instant currentMin = null;
    private volatile Instant currentMax = null;

    private volatile Instant lastDataMaxGenerationTime; // Lastest generation time of data

    private Timer timer = null;
    private volatile boolean liveTimeTracker = true;
    private volatile boolean live = false;

    private final PresetStorageManager presetManager = new PresetStorageManager();
    private Stage independentStage;
    private Tab mainTab;
    private String displayName;
    private String presetName;
    private boolean dirty;
    private boolean loadingPreset;

    @Override
    public void doInitialize(URL url, ResourceBundle rb) {
        if (this.liveTgl != null) {
            this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
            this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        }

        this.dateTimePopup.setAutoHide(true);
        this.dateTimePopup.setHideOnEscape(true);

        try {
            URL datePickerUrl = getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            CssHandler.applyTo(dateTimePopup.getScene().getRoot());
            CssHandler.applyTo(dateTimePicker);
            this.dateTimePickerController = loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                Instant theTime = this.dateTimePickerController.getSelectedTime();
                fetchRecords(theTime.minusMillis(getTimeWindowSize()), theTime, true);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.live = true;
        // Register to the coordinator
        UserDisplayCoordinator.instance().register(this);

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

    protected String doGetComponentId() {
        return "ChartDisplayView";
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
            MenuItem noPresets = new MenuItem("No Charts available");
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
            AbstractDisplayController controller = MainViewController.instance().openPerspective("Charts");
            if(controller instanceof ChartDisplayViewController) {
                ((ChartDisplayViewController) controller).open(preset);
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
            ChartPreset loadedPreset = this.presetManager.load(systemName, user, preset, doGetComponentId(), ChartPreset.class);
            if(loadedPreset != null) {
                FxUtils.runLater(() -> loadPresetIntoDisplay(preset, loadedPreset));
            }
        }));
    }

    private void loadPresetIntoDisplay(String name, ChartPreset loadedPreset) {
        loadPreset(loadedPreset);
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
        Optional<String> result = DialogUtils.input(getDisplayName(), "Rename Chart", "Change name of the Chart", "Please provide the name of the Chart:");
        result.map(String::trim).filter(s -> !s.isEmpty()).ifPresent(s -> {
            this.presetName = s;
            setDisplayName(s);
        });
    }

    private boolean saveCurrentDisplay() {
        String nameToSave = this.presetName;
        if(nameToSave == null || nameToSave.trim().isEmpty()) {
            Optional<String> result = DialogUtils.input(getDisplayName(), "Save Chart", "Chart name", "Please provide the name of the Chart:");
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
            DialogUtils.alert("Save Chart", null, "Cannot save Chart: no system connected");
            return false;
        }

        try {
            this.presetManager.save(currentSystem.getName(), user, nameToSave, doGetComponentId(), getChartDescription());
            this.presetName = nameToSave;
            setDisplayName(nameToSave);
            setDirty(false);
            return true;
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot save preset, system not responding", e);
            DialogUtils.alert("Save Chart", null, "Cannot save Chart: system not responding");
            return false;
        }
    }

    @Override
    protected boolean closeRequested() {
        if(!this.dirty) {
            return true;
        }
        DialogUtils.SaveDiscardCancelResult result = DialogUtils.saveDiscardCancel(
                "Close Chart",
                "About to close " + getDisplayName(),
                "The Chart has unsaved changes."
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
        return this.displayName == null ? "Chart" : this.displayName;
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
        this.charts.removeIf(AbstractChartManager::isDeleted);
        return !this.dirty && this.presetName == null && this.charts.isEmpty();
    }

    @FXML
    protected void addLineChartMenuItemSelected(ActionEvent e) {
        addLineChart();
        setDirty(true);
    }

    @FXML
    protected void addAreaChartMenuItemSelected(ActionEvent e) {
        addAreaChart();
        setDirty(true);
    }

    @FXML
    protected void addBarChartMenuItemSelected(ActionEvent e) {
        addBarChart();
        setDirty(true);
    }

    @FXML
    protected void addScatterChartMenuItemSelected(ActionEvent e) {
        addScatterChart();
        setDirty(true);
    }

    private void fetchRecords(final Instant minTime, final Instant maxTime, final boolean clear) {
        final ParameterDataFilter pdf = getCurrentParameterFilter();
        final EventDataFilter edf = getCurrentEventFilter();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {

                // Retrieve the parameters
                List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
                        .getParameterDataMonitorService().retrieve(minTime, maxTime, pdf);
                List<AbstractDataItem> messages = new LinkedList<>(pmessages);

                // Retrieve the events
                List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
                        .getEventDataMonitorService().retrieve(minTime, maxTime, edf);
                messages.addAll(emessages);

                setData(minTime, maxTime, messages, clear);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    protected void setData(Instant minTime, Instant maxTime, List<AbstractDataItem> messages, boolean clear) {
        // As this data comes from the archive, set the last data max time
        lastDataMaxGenerationTime = lastDataMaxGenerationTime == null ? maxTime : (lastDataMaxGenerationTime.isBefore(maxTime) ? maxTime : lastDataMaxGenerationTime);
        FxUtils.runLater(() -> {
            if (clear) {
                clearCharts();
            }
            this.currentMin = minTime;
            this.currentMax = maxTime;
            this.charts.forEach(a -> a.setBoundaries(minTime, maxTime));
            this.charts.forEach(a -> a.plot(messages));
            this.selectTimeBtn.setText(formatTime(maxTime));
        });
    }

    @Override
    public void updateDataItems(List<? extends AbstractDataItem> items) {
        if (this.live) {
            // As this data comes from the live stream, set the last data max time
            Optional<Instant> maxGenerationTime = items.stream().map(AbstractDataItem::getGenerationTime).max((Comparator.naturalOrder()));
            if (maxGenerationTime.isPresent()) {
                lastDataMaxGenerationTime = lastDataMaxGenerationTime == null ? maxGenerationTime.get() : (lastDataMaxGenerationTime.isBefore(maxGenerationTime.get()) ? maxGenerationTime.get() : lastDataMaxGenerationTime);
            }
            this.charts.forEach(a -> a.plot((List<AbstractDataItem>) items));
        }
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
            Instant now = Instant.now();
            fetchRecords(now.minusMillis(getTimeWindowSize()), now, true);
            startSubscription();
        } else {
            stopSubscription();
        }
    }

    @FXML
    protected void liveTimeToggleSelected(ActionEvent e) {
        this.liveTimeTracker = this.liveTimeTgl.isSelected();
    }

    private void clearCharts() {
        this.charts.forEach(AbstractChartManager::clear);
    }

    private int getTimeWindowSize() {
        return this.timeWindowSize;
    }

    public void setIndependentStage(Stage independentStage) {
        this.independentStage = independentStage;
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTimeBeginning();
        }
    }

    private void moveToTimeEnd() {
        final ParameterDataFilter pdf = getCurrentParameterFilter();
        final EventDataFilter edf = getCurrentEventFilter();
        final int timeWindow = getTimeWindowSize();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<AbstractDataItem> messages = new LinkedList<>();

                // Retrieve the parameters
                List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
                        .getParameterDataMonitorService().retrieve(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), 100, RetrievalDirection.TO_PAST, pdf);
                messages.addAll(pmessages);
                // Repeat until endTime is reached
                Instant minTime = null;
                if (pmessages.size() > 0) {
                    minTime = pmessages.get(0).getGenerationTime().minusMillis(timeWindow);

                    if (pmessages.get(pmessages.size() - 1).getGenerationTime().isAfter(minTime)) {
                        List<ParameterData> newMessages = ReatmetricUI.selectedSystem().getSystem()
                                .getParameterDataMonitorService()
                                .retrieve(pmessages.get(pmessages.size() - 1), 100, RetrievalDirection.TO_PAST, pdf);
                        messages.addAll(newMessages);
                    }
                }
                // Retrieve the events
                List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
                        .getEventDataMonitorService().retrieve(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), 100, RetrievalDirection.TO_PAST, edf);
                messages.addAll(emessages);
                // Repeat until endTime is reached
                if (emessages.size() > 0) {
                    if (minTime == null) {
                        minTime = emessages.get(0).getGenerationTime().minusMillis(timeWindow);
                    } else {
                        minTime = emessages.get(0).getGenerationTime().minusMillis(timeWindow).isBefore(minTime) ? emessages.get(0).getGenerationTime().minusMillis(timeWindow) : minTime;
                    }

                    if (emessages.get(emessages.size() - 1).getGenerationTime().isAfter(minTime)) {
                        List<EventData> newMessages = ReatmetricUI.selectedSystem().getSystem()
                                .getEventDataMonitorService()
                                .retrieve(emessages.get(emessages.size() - 1), 100, RetrievalDirection.TO_PAST, edf);
                        messages.addAll(newMessages);
                    }
                }
                // Remove too old items
                if (minTime != null) {
                    for (Iterator<AbstractDataItem> it = messages.iterator(); it.hasNext(); ) {
                        if (it.next().getGenerationTime().isBefore(minTime)) {
                            it.remove();
                        }
                    }
                }
                setData(minTime, messages.get(0).getGenerationTime(), messages, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void moveToTimeBeginning() {
        final ParameterDataFilter pdf = getCurrentParameterFilter();
        final EventDataFilter edf = getCurrentEventFilter();
        final int timeWindow = getTimeWindowSize();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<AbstractDataItem> messages = new LinkedList<>();

                List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
                        .getParameterDataMonitorService().retrieve(Instant.EPOCH, 100, RetrievalDirection.TO_FUTURE, pdf);
                messages.addAll(pmessages);

                // Repeat until endTime is reached
                Instant maxTime = null;
                if (pmessages.size() > 0) {
                    maxTime = pmessages.get(0).getGenerationTime().plusMillis(timeWindow);

                    if (pmessages.get(pmessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
                        List<ParameterData> newMessages = ReatmetricUI.selectedSystem().getSystem()
                                .getParameterDataMonitorService()
                                .retrieve(pmessages.get(pmessages.size() - 1), 100, RetrievalDirection.TO_FUTURE, pdf);
                        messages.addAll(newMessages);
                    }
                }
                // Retrieve the events
                List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
                        .getEventDataMonitorService().retrieve(Instant.EPOCH, 100, RetrievalDirection.TO_FUTURE, edf);
                messages.addAll(emessages);
                // Repeat until endTime is reached
                if (emessages.size() > 0) {
                    if (maxTime == null) {
                        maxTime = emessages.get(0).getGenerationTime().plusMillis(timeWindow);
                    } else {
                        maxTime = emessages.get(0).getGenerationTime().plusMillis(timeWindow).isAfter(maxTime) ? emessages.get(0).getGenerationTime().plusMillis(timeWindow) : maxTime;
                    }

                    if (emessages.get(emessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
                        List<EventData> newMessages = ReatmetricUI.selectedSystem().getSystem()
                                .getEventDataMonitorService()
                                .retrieve(emessages.get(emessages.size() - 1), 100, RetrievalDirection.TO_FUTURE, edf);
                        messages.addAll(newMessages);
                    }
                }

                // Remove too late items
                if (maxTime != null) {
                    for (Iterator<AbstractDataItem> it = messages.iterator(); it.hasNext(); ) {
                        if (it.next().getGenerationTime().isAfter(maxTime)) {
                            it.remove();
                        }
                    }
                }
                setData(messages.get(0).getGenerationTime(), maxTime, messages, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if (!isProgressBusy()) {
            fetchRecords(getCurrentMinTime().minusMillis(getTimeWindowSize()), getCurrentMinTime(), false);
        }
    }

    private Instant getCurrentMinTime() {
        return this.currentMin;
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if (!isProgressBusy()) {
            fetchRecords(getCurrentMinTime().minusMillis(getTimeUnit()), getCurrentMaxTime().minusMillis(getTimeUnit()),
                    false);
        }
    }

    private Instant getCurrentMaxTime() {
        return this.currentMax;
    }

    private int getTimeUnit() {
        return this.timeUnit;
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTimeEnd();
        }
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if (!isProgressBusy()) {
            fetchRecords(getCurrentMaxTime(), getCurrentMaxTime().plusMillis(getTimeWindowSize()), false);
        }
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if (!isProgressBusy()) {
            fetchRecords(getCurrentMinTime().plusMillis(getTimeUnit()), getCurrentMaxTime().plusMillis(getTimeUnit()),
                    false);
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
            this.dateTimePopup.show(this.innerBox.getScene().getWindow());
        }
    }

    @Override
    public boolean isLive() {
        return this.live;
    }

    @Override
    public void dispose() {
        stopSubscription();
        UserDisplayCoordinator.instance().deregister(this);
        super.dispose();
    }

    @Override
    public void doSystemDisconnected(IReatmetricSystem s, boolean oldStatus) {
        if (this.liveTgl != null) {
            this.liveTgl.setSelected(false);
        }
        this.innerBox.getParent().getParent().setDisable(true);
        stopSubscription();
        clearCharts();
        // If you are detached, close the stage
        if (independentStage != null) {
            independentStage.setOnCloseRequest(null);
            independentStage.close();
        }
    }

    @Override
    public void doSystemConnected(IReatmetricSystem s, boolean oldStatus) {
        // Start subscription if there
        if (this.liveTgl == null || this.liveTgl.isSelected()) {
            clearCharts();
            startSubscription();
        }
        if (this.liveTgl != null) {
            this.liveTgl.setSelected(true);
        }
        this.innerBox.getParent().getParent().setDisable(false);
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

    protected final void startSubscription() {
        this.live = true;
        this.charts.forEach(a -> a.switchToLive(true));
        if (this.currentMin == null) {
            this.currentMax = getMaxTimeReference();
            this.currentMin = this.currentMax.minusMillis(getTimeWindowSize());
            this.charts.forEach(a -> a.setBoundaries(this.currentMin, this.currentMax));
        }
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = new Timer(true);
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                FxUtils.runLater(() -> {
                    currentMax = getMaxTimeReference();
                    currentMin = currentMax.minusMillis(getTimeWindowSize());
                    charts.forEach(a -> a.setBoundaries(currentMin, currentMax));
                    selectTimeBtn.setText(formatTime(currentMax.minusMillis(getTimeUnit())));
                });
            }
        }, getTimeUnit(), getTimeUnit());
    }

    private Instant getMaxTimeReference() {
        if (this.liveTimeTracker) {
            return Instant.now().plusMillis(getTimeUnit());
        } else {
            // last retrieve data generation time
            Instant lastRetriever = lastDataMaxGenerationTime;
            if (lastRetriever != null) {
                return lastRetriever.plusMillis(getTimeUnit());
            } else {
                return Instant.now().plusMillis(getTimeUnit());
            }
        }
    }

    @Override
    public ParameterDataFilter getCurrentParameterFilter() {
        return this.currentParameterFilter;
    }

    @Override
    public EventDataFilter getCurrentEventFilter() {
        return this.currentEventFilter;
    }

    protected final void stopSubscription() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = null;
        this.charts.forEach(a -> a.switchToLive(false));

        this.live = false;
    }

    @Override
    protected Window retrieveWindow() {
        return this.liveTgl.getScene().getWindow();
    }

    @Override
    protected Node doBuildNodeForPrinting() {
        WritableImage image = innerBox.snapshot(null, null);
        ImageView v = new ImageView(image);
        v.setFitWidth(innerBox.getWidth());
        v.setFitHeight(innerBox.getHeight());
        return v;
    }

    private AbstractChartManager addLineChart() {
        LineChart<Instant, Number> l = new LineChart<>(new InstantAxis(), new NumberAxis());
        return initialiseTimeChart(l);
    }

    private AbstractChartManager addAreaChart() {
        AreaChart<Instant, Number> l = new AreaChart<>(new InstantAxis(), new NumberAxis());
        return initialiseTimeChart(l);
    }

    private AbstractChartManager addBarChart() {
        BarChart<String, Number> l = new BarChart<>(new CategoryAxis(), new NumberAxis());
        return initialiseBarChart(l);
    }

    private AbstractChartManager addScatterChart() {
        ScatterChart<Instant, Number> l = new ScatterChart<>(new InstantAxis(), new NumberAxis());
        return initialiseScatterChart(l);
    }

    private AbstractChartManager initialiseBarChart(BarChart<String, Number> l) {
        l.setAnimated(false);
        l.getXAxis().setTickLabelsVisible(true);

        // Add to list
        AbstractChartManager udd = new XYBarChartManager(o -> updateFilter(), l);
        addToPane(l);
        this.charts.add(udd);
        this.innerBox.getParent().layout();
        return udd;
    }

    private void addToPane(XYChart<?, ?> l) {
        l.setPrefHeight(300);
        l.setMinHeight(300);
        l.setPadding(new Insets(10, 10, 10, 10));
        l.prefWidthProperty().bind(this.innerBox.widthProperty());
        this.innerBox.getChildren().add(l);
        VBox.setVgrow(l, Priority.NEVER);
    }

    private AbstractChartManager initialiseTimeChart(XYChart<Instant, Number> l) {
        l.setAnimated(false);
        l.getXAxis().setTickLabelsVisible(true);

        l.getXAxis().setAutoRanging(false);
        ((InstantAxis) l.getXAxis()).setLowerBound(Instant.now().minusMillis(getTimeWindowSize() - getTimeUnit()));
        ((InstantAxis) l.getXAxis()).setUpperBound(Instant.now().plusMillis(getTimeUnit()));

        // Add to list
        XYTimeChartManager udd = new XYTimeChartManager(o -> updateFilter(), l);

        addToPane(l);

        this.charts.add(udd);
        this.innerBox.getParent().layout();
        return udd;
    }

    private AbstractChartManager initialiseScatterChart(ScatterChart<Instant, Number> l) {
        l.setAnimated(false);
        l.getXAxis().setTickLabelsVisible(true);

        l.getXAxis().setAutoRanging(false);
        ((InstantAxis) l.getXAxis()).setLowerBound(Instant.now().minusMillis(getTimeWindowSize() - getTimeUnit()));
        ((InstantAxis) l.getXAxis()).setUpperBound(Instant.now().plusMillis(getTimeUnit()));

        ((NumberAxis) l.getYAxis()).setTickUnit(1.0);
        l.getYAxis().setTickLabelsVisible(false);

        // Add to list
        XYScatterChartManager udd = new XYScatterChartManager(o -> updateFilter(), l);

        addToPane(l);

        this.charts.add(udd);
        this.innerBox.getParent().layout();
        return udd;
    }

    protected void updateFilter() {
        // Remove all deleted charts if any
        this.charts.removeIf(AbstractChartManager::isDeleted);
        if(!this.loadingPreset) {
            setDirty(true);
        }
        Set<SystemEntityPath> selectedParameters = new LinkedHashSet<>();
        Set<SystemEntityPath> selectedEvents = new LinkedHashSet<>();
        for (AbstractChartManager acm : this.charts) {
            if(acm.getSystemElementType() == SystemEntityType.PARAMETER) {
                selectedParameters.addAll(acm.getPlottedSystemEntities());
            } else if(acm.getSystemElementType() == SystemEntityType.EVENT) {
                selectedEvents.addAll(acm.getPlottedSystemEntities());
            }
        }
        this.currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(selectedParameters), null, null, null, null);
        this.currentEventFilter = new EventDataFilter(null, new LinkedList<>(selectedEvents), null, null, null, null, null);
        // Update the subscriptions
        UserDisplayCoordinator.instance().filterUpdated();
        // If not live, fetch the data to refresh the dataset
        if(!live) {
            Platform.runLater(() -> fetchRecords(this.currentMin, this.currentMax, true));
        }
    }

    public ChartPreset getChartDescription() {
        // Remove all deleted charts if any
        this.charts.removeIf(AbstractChartManager::isDeleted);
        // Iterate on the chart managers
        ChartPreset props = new ChartPreset();
        for (AbstractChartManager acm : charts) {
            String chartType = acm.getChartType();
            List<String> items = acm.getCurrentEntityPaths();
            // Add the type, and value is the list of paths
            props.addElement(chartType, items.toArray(new String[0]));
        }
        return props;
    }

    public void loadPreset(ChartPreset p) {
        this.loadingPreset = true;
        try {
            clearChartDisplay();
            for (ChartPreset.Element chartSet : p.getElements()) {
                String chartType = chartSet.getType();
                AbstractChartManager chartManager = null;
                List<String> items = Arrays.asList(chartSet.getNames());
                switch (chartType) {
                    case "line": {
                        chartManager = addLineChart();
                    }
                    break;
                    case "area": {
                        chartManager = addAreaChart();
                    }
                    break;
                    case "bar": {
                        chartManager = addBarChart();
                    }
                    break;
                    case "scatter": {
                        chartManager = addScatterChart();
                    }
                    break;
                }
                if (chartManager != null) {
                    chartManager.addItems(items);
                }
            }
        } finally {
            this.loadingPreset = false;
        }
    }

    private void clearChartDisplay() {
        for(AbstractChartManager chart : this.charts) {
            if(!chart.isDeleted()) {
                chart.clear();
            }
        }
        this.innerBox.getChildren().clear();
        this.charts.clear();
        this.currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(), null, null, null, null);
        this.currentEventFilter = new EventDataFilter(null, new LinkedList<>(), null, null, null, null, null);
        UserDisplayCoordinator.instance().filterUpdated();
    }
}
