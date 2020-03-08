/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ConnectorsBrowserViewController extends AbstractDisplayController implements ITransportSubscriber {

    private static final Logger LOG = Logger.getLogger(ConnectorsBrowserViewController.class.getName());

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

    @FXML    
    private VBox vbox;
    @FXML
    private ScrollPane scrollpane;

    private Map<ITransportConnector, ConnectorStatusWidgetController> connector2controller = new ConcurrentHashMap<>();

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        // Nothing
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        // Print function not available in this view
        return null;
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(true);
        // Clear the list view
        clearConnectorsModel();
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(false);
        startSubscription();
    }

    private void startSubscription() {
        clearConnectorsModel();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                buildConnectorsModel();
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot retrieve sle connectors: " + e.getMessage(), e);
            }
        });
    }

    private void clearConnectorsModel() {
        // First lock
        for(ITransportConnector connector : this.connector2controller.keySet()) {
            connector.deregister(this);
        }
        this.connector2controller.clear();
        this.vbox.getChildren().removeAll(this.vbox.getChildren());
        this.vbox.getChildren().clear();
        this.vbox.layout();
    }

    private void buildConnectorsModel() throws ReatmetricException {
        final List<ITransportConnector> connectors = ReatmetricUI.selectedSystem().getSystem().getTransportConnectors();
        Platform.runLater(() -> {
            for(ITransportConnector tc : connectors) {
                ConnectorStatusWidgetController controller = buildConnectorController(tc);
                if(controller != null) {
                    connector2controller.put(tc, controller);
                    controller.setConnector(tc);
                    tc.register(this);
                }
            }
            vbox.layout();
        });
    }

    private ConnectorStatusWidgetController buildConnectorController(ITransportConnector tc) {
        try {
            URL paneUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/ConnectorStatusWidget.fxml");
            FXMLLoader loader = new FXMLLoader(paneUrl);
            VBox pp = loader.load();
            HBox.setHgrow(pp, Priority.ALWAYS);
            vbox.getChildren().add(pp);
            return loader.getController();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load FXML file", e);
            return null;
        }
    }

    @Override
    public void status(ITransportConnector connector, TransportStatus status) {
        Platform.runLater(() -> {
            ConnectorStatusWidgetController controller = connector2controller.get(connector);
            if(controller != null) {
                controller.updateStatus(status);
            }
        });
    }
}
