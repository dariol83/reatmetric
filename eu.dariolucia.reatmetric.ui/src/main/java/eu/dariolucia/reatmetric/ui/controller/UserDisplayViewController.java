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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.UserDisplayStorageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class UserDisplayViewController extends AbstractDisplayController
		implements IParameterDataSubscriber {

	// TODO: subscribe also to events and propagate
	
	// Pane control
	@FXML
	protected TitledPane displayTitledPane;

	@FXML
	protected TabPane tabPane;

	private final UserDisplayStorageManager uddManager = new UserDisplayStorageManager();

	private final Map<Tab, UserDisplayTabWidgetController> tab2contents = new ConcurrentHashMap<>();
	
	// Temporary object queues
    protected DataProcessingDelegator<ParameterData> parameterDelegator;
    protected DataProcessingDelegator<EventData> eventDelegator;
    
	
	@Override
	public final void doInitialize(URL url, ResourceBundle rb) {
		this.parameterDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingParameterDataDelegatorAction());
		this.eventDelegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingEventDataDelegatorAction());
	}
	
	protected Consumer<List<ParameterData>> buildIncomingParameterDataDelegatorAction() {
        return (List<ParameterData> t) -> {
            forwardParameterDataItems(t);
        };
    }
	
	protected Consumer<List<EventData>> buildIncomingEventDataDelegatorAction() {
        return (List<EventData> t) -> {
        	forwardEventDataItems(t);
        };
    }

    private void forwardEventDataItems(List<EventData> t) {
    	if(this.displayTitledPane.isDisabled()) {
    		return;
    	}
    	// TODO
	}
	
    private void forwardParameterDataItems(List<ParameterData> t) {
    	if(this.displayTitledPane.isDisabled()) {
    		return;
    	}
		// Build forward map
    	final Map<UserDisplayTabWidgetController, List<ParameterData>> forwardMap = new LinkedHashMap<>();
    	for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
    		if(c.isLive()) {
    			ParameterDataFilter pdf = c.getCurrentFilter();
    			List<ParameterData> toForward = t.stream().filter(p -> match(pdf, p)).collect(Collectors.toList());
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

	private boolean match(ParameterDataFilter pdf, ParameterData p) {
		return pdf.isClear() || pdf.getParameterPathList().contains(p.getPath());
	}

	protected String doGetComponentId() {
        return "UserDisplayView";
    }
    
	@FXML
	protected void newButtonSelected(ActionEvent e) throws IOException {
		Tab t = new Tab("Display");
		// TODO: add context menu on tab to rename tab
		URL userDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/UserDisplayTabWidget.fxml");
        FXMLLoader loader = new FXMLLoader(userDisplayWidgetUrl);
        VBox userDisplayWidget = (VBox) loader.load();	
		UserDisplayTabWidgetController ctrl = (UserDisplayTabWidgetController) loader.getController();
		ctrl.setParentController(this);
		
		userDisplayWidget.prefWidthProperty().bind(this.tabPane.widthProperty());
		// userDisplayWidget.prefHeightProperty().bind(t.heightProperty()); // this creates problems with the height
		t.setContent(userDisplayWidget);
		this.tabPane.getTabs().add(t);
		this.tabPane.getParent().layout();
		this.tab2contents.put(t, ctrl);
		
		ctrl.startSubscription();
	}

	@FXML
	protected void closeButtonSelected(ActionEvent e) {
		Tab t = this.tabPane.getSelectionModel().getSelectedItem();
		if(t != null) {
			this.tabPane.getTabs().remove(t);
			this.tab2contents.remove(t);
		}
	}
	
	@Override
	protected Control doBuildNodeForPrinting() {
		return null;
	}

	@Override
	protected void doUserDisconnected(String system, String user) {
		this.tab2contents.values().stream().forEach(c -> c.doUserDisconnected(system, user));
		this.displayTitledPane.setDisable(true);
	}

	@Override
	protected void doUserConnected(String system, String user) {
		this.tab2contents.values().stream().forEach(c -> c.doUserConnected(system, user));
		this.displayTitledPane.setDisable(false);
	}

	@Override
	protected void doUserConnectionFailed(String system, String user, String reason) {
		this.tab2contents.values().stream().forEach(c -> c.doUserConnectionFailed(system, user, reason));
		this.displayTitledPane.setDisable(true);
	}

	@Override
	protected void doServiceDisconnected(boolean previousConnectionStatus) {
		this.tab2contents.values().stream().forEach(c -> c.doServiceDisconnected(previousConnectionStatus));
		stopSubscription();
	}

	@Override
	protected void doServiceConnected(boolean previousConnectionStatus) {
		this.tab2contents.values().stream().forEach(c -> c.doServiceConnected(previousConnectionStatus));
		ParameterDataFilter globalFilter = buildFilter();
		if(mustSubscribe(globalFilter)) {
			startSubscription(globalFilter);
		}
	}

    private boolean mustSubscribe(ParameterDataFilter globalFilter) {
		return !globalFilter.getParameterPathList().isEmpty() && this.tab2contents.size() > 0;
	}

	private void startSubscription(ParameterDataFilter currentFilter) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
            	ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this, currentFilter);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private ParameterDataFilter buildFilter() {
    	Set<SystemEntityPath> params = new TreeSet<>();
		for(UserDisplayTabWidgetController c : this.tab2contents.values()) {
			params.addAll(c.getCurrentFilter().getParameterPathList());
		}
		return new ParameterDataFilter(null, new ArrayList<>(params),null,null,null);
	}

	private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
            	ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }
	
    @Override
    protected ServiceType doGetSupportedService() {
        return ServiceType.PARAMETERS;
    }

	@Override
	public void dataItemsReceived(List<ParameterData> messages) {
		this.parameterDelegator.delegate(messages);
	}

	public void filterUpdated(UserDisplayTabWidgetController userDisplayTabWidgetController,
			ParameterDataFilter currentFilter) {
		ParameterDataFilter globalFilter = buildFilter();
		if(globalFilter.getParameterPathList().isEmpty()) {
			stopSubscription();
		} else {
			startSubscription(globalFilter);
		}
	}
}
