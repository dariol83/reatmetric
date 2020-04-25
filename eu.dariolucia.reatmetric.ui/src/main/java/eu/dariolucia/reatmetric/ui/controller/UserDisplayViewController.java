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
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.PresetStorageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
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
public class UserDisplayViewController extends AbstractDisplayController {

	private static final Logger LOG = Logger.getLogger(UserDisplayViewController.class.getName());

	private URL cssUrl;

	// Pane control
	@FXML
	protected TitledPane displayTitledPane;

	@FXML
	protected TabPane tabPane;

	@FXML
	protected MenuButton loadBtn;

	private final Map<Tab, UserDisplayTabWidgetController> tab2contents = new ConcurrentHashMap<>();
	
	// Temporary object queues
    protected DataProcessingDelegator<ParameterData> parameterDelegator;
    protected DataProcessingDelegator<EventData> eventDelegator;

    private IParameterDataSubscriber parameterSubscriber;
    private IEventDataSubscriber eventSubscriber;

	// Preset manager
	private final PresetStorageManager presetManager = new PresetStorageManager();

	private final List<Stage> detachedTabs = new LinkedList<>();

	@Override
	protected Window retrieveWindow() {
		return displayTitledPane.getScene().getWindow();
	}

	@Override
	public final void doInitialize(URL url, ResourceBundle rb) {
		this.parameterDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingParameterDataDelegatorAction());
		this.eventDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingEventDataDelegatorAction());
		this.parameterSubscriber = items -> parameterDelegator.delegate(items);
		this.eventSubscriber = items -> eventDelegator.delegate(items);
		this.loadBtn.setOnShowing(this::onShowingPresetMenu);

		this.cssUrl = getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css");
	}
	
	protected Consumer<List<ParameterData>> buildIncomingParameterDataDelegatorAction() {
        return this::forwardParameterDataItems;
    }
	
	protected Consumer<List<EventData>> buildIncomingEventDataDelegatorAction() {
        return this::forwardEventDataItems;
    }

    private void forwardEventDataItems(List<EventData> t) {
    	if(this.displayTitledPane.isDisabled()) {
    		return;
    	}
		// Build forward map
		final Map<UserDisplayTabWidgetController, List<EventData>> forwardMap = new LinkedHashMap<>();
		for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
			if(c.isLive()) {
				EventDataFilter edf = c.getCurrentEventFilter();
				List<EventData> toForward = t.stream().filter(edf).collect(Collectors.toList());
				if(!toForward.isEmpty()) {
					forwardMap.put(c, toForward);
				}
			}
		}
		// Forward
		Platform.runLater(() -> {
			for(Map.Entry<UserDisplayTabWidgetController, List<EventData>> entry : forwardMap.entrySet()) {
				entry.getKey().updateDataItems(entry.getValue());
			}
		});
	}
	
    private void forwardParameterDataItems(List<ParameterData> t) {
    	if(this.displayTitledPane.isDisabled()) {
    		return;
    	}
		// Build forward map
    	final Map<UserDisplayTabWidgetController, List<ParameterData>> forwardMap = new LinkedHashMap<>();
    	for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
    		if(c.isLive()) {
    			ParameterDataFilter pdf = c.getCurrentParameterFilter();
    			List<ParameterData> toForward = t.stream().filter(pdf).collect(Collectors.toList());
    			if(!toForward.isEmpty()) {
    				forwardMap.put(c, toForward);
    			}
    		}
    	}
    	// Forward
    	Platform.runLater(() -> {
	    	for(Map.Entry<UserDisplayTabWidgetController, List<ParameterData>> entry : forwardMap.entrySet()) {
	    		entry.getKey().updateDataItems(entry.getValue());
	    	}
    	});
	} 

	protected String doGetComponentId() {
        return "ChartDisplayView";
    }
    
	@FXML
	protected void newButtonSelected(ActionEvent e) throws IOException {
		createNewTab("Display");
	}

	private Tab createNewTab(String tabText) throws IOException {
		Tab t = new Tab(tabText);
		t.setContextMenu(new ContextMenu());
		MenuItem renameTabMenuItem = new MenuItem("Rename...");
		t.getContextMenu().getItems().add(renameTabMenuItem);
		renameTabMenuItem.setOnAction(event -> {
			// Traditional way to get the response value.
			Optional<String> result = DialogUtils.input(t.getText(), "Rename Tab", "Change name of the chart tab", "Please provide the name of the chart tab:");
			result.ifPresent(t::setText);
		});
		MenuItem saveTabMenuItem = new MenuItem("Save chart preset...");
		t.getContextMenu().getItems().add(saveTabMenuItem);
		saveTabMenuItem.setOnAction(event -> {
			// Traditional way to get the response value.
			Optional<String> result = DialogUtils.input(t.getText(), "Save Chart Preset", "Chart Preset", "Please provide the name of the preset:");
			if(result.isPresent()) {
				UserDisplayTabWidgetController controller = tab2contents.get(t);
				this.presetManager.save(system.getName(), user, result.get(), doGetComponentId(), controller.getChartDescription());
			}
		});

		URL userDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/UserDisplayTabWidget.fxml");
		FXMLLoader loader = new FXMLLoader(userDisplayWidgetUrl);
		final VBox userDisplayWidget = loader.load();
		final UserDisplayTabWidgetController ctrl = loader.getController();
		ctrl.setParentController(this);

		userDisplayWidget.prefWidthProperty().bind(this.tabPane.widthProperty());
		// userDisplayWidget.prefHeightProperty().bind(t.heightProperty()); // this creates problems with the height
		t.setContent(userDisplayWidget);
		t.setClosable(true);
		t.setOnCloseRequest(event -> {
			// TODO: what about detached tabs?
			if(DialogUtils.confirm("Close chart tab", "About to close chart tab " + t.getText(), "Do you want to close chart tab " + t.getText() + "? Unsaved chart updates will be lost!")) {
				this.tabPane.getTabs().remove(t);
				this.tab2contents.remove(t);
				ctrl.dispose();
			} else {
				event.consume();
			}
		});
		this.tabPane.getTabs().add(t);
		this.tabPane.getParent().layout();
		this.tabPane.getSelectionModel().select(t);
		this.tab2contents.put(t, ctrl);
		ctrl.startSubscription();

		// Tab detaching
		SeparatorMenuItem sep = new SeparatorMenuItem();
		t.getContextMenu().getItems().add(sep);
		MenuItem detachMenuItem = new MenuItem("Detach");
		t.getContextMenu().getItems().add(detachMenuItem);
		detachMenuItem.setOnAction(event -> {
			// Create a detached scene parent
			Stage stage = new Stage();
			t.setContent(null);
			t.setOnCloseRequest(null);
			this.tabPane.getTabs().remove(t);
			// this.tab2contents.remove(t); // if removed, there will be no forwards of system status change
			Scene scene = new Scene(userDisplayWidget);
			scene.getStylesheets().add(cssUrl.toExternalForm());

			stage.setScene(scene);
			stage.setTitle(t.getText());

			Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-32px.png"));
			stage.getIcons().add(icon);
			detachedTabs.add(stage);

			stage.setOnCloseRequest(ev -> {
				// TODO: what about detached tabs?
				if(DialogUtils.confirm("Close chart", "About to close chart " + stage.getTitle(), "Do you want to close chart " + stage.getTitle() + "? Unsaved chart updates will be lost!")) {
					detachedTabs.remove(stage);
					ctrl.dispose();
					stage.close();
				} else {
					event.consume();
				}
			});

			stage.show();
		});

		return t;
	}

	@Override
	protected Control doBuildNodeForPrinting() {
		return null;
	}

	@Override
	protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
		stopSubscription();
		this.tab2contents.values().forEach(o -> o.doSystemDisconnected(system, oldStatus));
		this.displayTitledPane.setDisable(true);
	}

	@Override
	protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
		this.tab2contents.values().forEach(o -> o.doSystemConnected(system, oldStatus));
		ParameterDataFilter globalFilter = buildParameterFilter();
		EventDataFilter globalEventFilter = buildEventFilter();
		if(mustSubscribe(globalFilter, globalEventFilter)) {
			startSubscription(globalFilter, globalEventFilter);
		}
		this.displayTitledPane.setDisable(false);
	}

    private boolean mustSubscribe(ParameterDataFilter globalFilter, EventDataFilter globalEventFilter) {
		return (!globalFilter.getParameterPathList().isEmpty() || !globalEventFilter.getEventPathList().isEmpty()) && this.tab2contents.size() > 0;
	}

	private void startSubscription(ParameterDataFilter currentParameterFilter, EventDataFilter currentEventFilter) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
            	ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this.parameterSubscriber, currentParameterFilter);
            	ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().subscribe(this.eventSubscriber, currentEventFilter);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private ParameterDataFilter buildParameterFilter() {
    	Set<SystemEntityPath> params = new TreeSet<>();
		for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
			params.addAll(c.getCurrentParameterFilter().getParameterPathList());
		}
		return new ParameterDataFilter(null, new ArrayList<>(params),null,null,null, null);
	}

	private EventDataFilter buildEventFilter() {
		Set<SystemEntityPath> params = new TreeSet<>();
		for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
			params.addAll(c.getCurrentEventFilter().getEventPathList());
		}
		return new EventDataFilter(null, new ArrayList<>(params),null, null,null,null, null);
	}

	private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
				IReatmetricSystem service = ReatmetricUI.selectedSystem().getSystem();
				if(service != null && service.getParameterDataMonitorService() != null) {
					ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this.parameterSubscriber);
				}
				if(service != null && service.getEventDataMonitorService() != null) {
					ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().unsubscribe(this.eventSubscriber);
				}
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

	public void filterUpdated() {
		ParameterDataFilter globalParameterFilter = buildParameterFilter();
		EventDataFilter globalEventFilter = buildEventFilter();
		if(globalParameterFilter.getParameterPathList().isEmpty() && globalEventFilter.getEventPathList().isEmpty()) {
			stopSubscription();
		} else {
			startSubscription(globalParameterFilter, globalEventFilter);
		}
	}

	private void onShowingPresetMenu(Event contextMenuEvent) {
		this.loadBtn.getItems().remove(0, this.loadBtn.getItems().size());
		List<String> presets = this.presetManager.getAvailablePresets(system.getName(), user, doGetComponentId());
		for(String preset : presets) {
			final String fpreset = preset;
			MenuItem mi = new MenuItem(preset);
			mi.setOnAction((event) -> {
				Properties p = this.presetManager.load(system.getName(), user, fpreset, doGetComponentId());
				if(p != null) {
					try {
						addChartTabFromPreset(fpreset, p);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Cannot initialise chart tab preset " + preset + ": " + e.getMessage(), e);
					}
				}
			});
			this.loadBtn.getItems().add(mi);
		}
	}

	private void addChartTabFromPreset(String tabName, Properties p) throws IOException {
		Tab t = createNewTab(tabName);
		// After adding the tab, you need to initialise it in a new round of the UI thread,
		// to allow the layouting of the tab and the correct definition of the parent elements,
		// hence avoiding a null pointer exception
		Platform.runLater(() -> {
			UserDisplayTabWidgetController tabController = this.tab2contents.get(t);
			tabController.loadPreset(p);
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		// Close tabs and detached stages
		for(Stage s : detachedTabs) {
			s.close();
		}
	}
}
