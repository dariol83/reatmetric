/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IServiceFactory;
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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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

	@Override
	public final void doInitialize(URL url, ResourceBundle rb) {
		this.parameterDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingParameterDataDelegatorAction());
		this.eventDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingEventDataDelegatorAction());
		this.parameterSubscriber = items -> parameterDelegator.delegate(items);
		this.eventSubscriber = items -> eventDelegator.delegate(items);
		this.loadBtn.setOnShowing(this::onShowingPresetMenu);
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
        return "UserDisplayView";
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
				this.presetManager.save(system.getSystem(), user, result.get(), doGetComponentId(), controller.getChartDescription());
			}
		});

		URL userDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/UserDisplayTabWidget.fxml");
		FXMLLoader loader = new FXMLLoader(userDisplayWidgetUrl);
		VBox userDisplayWidget = loader.load();
		UserDisplayTabWidgetController ctrl = loader.getController();
		ctrl.setParentController(this);

		userDisplayWidget.prefWidthProperty().bind(this.tabPane.widthProperty());
		// userDisplayWidget.prefHeightProperty().bind(t.heightProperty()); // this creates problems with the height
		t.setContent(userDisplayWidget);
		this.tabPane.getTabs().add(t);
		this.tabPane.getParent().layout();
		this.tabPane.getSelectionModel().select(t);
		this.tab2contents.put(t, ctrl);
		ctrl.startSubscription();
		return t;
	}

	@FXML
	protected void closeButtonSelected(ActionEvent e) {
		Tab t = this.tabPane.getSelectionModel().getSelectedItem();
		if(t != null && DialogUtils.confirm("Close chart tab", "About to close chart tab " + t.getText(), "Do you want to close chart tab " + t.getText() + "? Unsaved chart updates will be lost!")) {
			this.tabPane.getTabs().remove(t);
			this.tab2contents.remove(t);
		}
	}
	
	@Override
	protected Control doBuildNodeForPrinting() {
		return null;
	}

	@Override
	protected void doSystemDisconnected(IServiceFactory system, boolean oldStatus) {
		stopSubscription();
		this.tab2contents.values().forEach(UserDisplayTabWidgetController::doSystemDisconnected);
		this.displayTitledPane.setDisable(true);
	}

	@Override
	protected void doSystemConnected(IServiceFactory system, boolean oldStatus) {
		this.tab2contents.values().forEach(UserDisplayTabWidgetController::doSystemConnected);
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
		return new ParameterDataFilter(null, new ArrayList<>(params),null,null,null);
	}

	private EventDataFilter buildEventFilter() {
		Set<SystemEntityPath> params = new TreeSet<>();
		for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
			params.addAll(c.getCurrentEventFilter().getEventPathList());
		}
		return new EventDataFilter(null, new ArrayList<>(params),null, null,null,null);
	}

	private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
				IServiceFactory service = ReatmetricUI.selectedSystem().getSystem();
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
		List<String> presets = this.presetManager.getAvailablePresets(system.getSystem(), user, doGetComponentId());
		for(String preset : presets) {
			final String fpreset = preset;
			MenuItem mi = new MenuItem(preset);
			mi.setOnAction((event) -> {
				Properties p = this.presetManager.load(system.getSystem(), user, fpreset, doGetComponentId());
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
}
