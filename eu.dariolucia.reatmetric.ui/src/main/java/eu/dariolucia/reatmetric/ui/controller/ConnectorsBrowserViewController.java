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
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ConnectorsBrowserViewController extends AbstractDisplayController implements ITransportSubscriber {

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

    @FXML    
    private ListView<ITransportConnector> modelTree;

    private Map<ITransportConnector, TransportStatus> connector2status = new ConcurrentHashMap<>();
    private Map<ITransportConnector, TransportConnectorCell> connector2cell = new ConcurrentHashMap<>();

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        // Customise the list view
        this.modelTree.setCellFactory(param -> new TransportConnectorCell());
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        // Print function not available in this view
        return null;
    }

    @Override
    protected void doSystemDisconnected(IServiceFactory system, boolean oldStatus) {
        this.displayTitledPane.setDisable(true);
        // Clear the list view
        clearConnectorsModel();
    }

    @Override
    protected void doSystemConnected(IServiceFactory system, boolean oldStatus) {
        this.displayTitledPane.setDisable(false);
        startSubscription();
    }

    private void startSubscription() {
        clearConnectorsModel();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                buildConnectorsModel();
            } catch (ReatmetricException e) {
                // TODO: log
                e.printStackTrace();
            }
        });
    }

    private void clearConnectorsModel() {
        // First lock
        for(ITransportConnector connector : this.modelTree.getItems()) {
            connector.deregister(this);
        }
        this.connector2status.clear();
        this.modelTree.getItems().clear();
        this.modelTree.layout();
        this.modelTree.refresh();
        this.connector2cell.clear();
    }

    private void buildConnectorsModel() throws ReatmetricException {
        final List<ITransportConnector> connectors = ReatmetricUI.selectedSystem().getSystem().getTransportConnectors();
        Platform.runLater(() -> {
            connectors.forEach(o -> this.connector2status.put(o, new TransportStatus(o.getName(), "", TransportConnectionStatus.NOT_INIT, 0, 0, AlarmState.UNKNOWN)));
            this.modelTree.getItems().addAll(connectors);
            connectors.forEach(o -> o.register(this));
            this.modelTree.layout();
            this.modelTree.refresh();
        });
    }

    @Override
    public void status(ITransportConnector connector, TransportStatus status) {
        Platform.runLater(() -> {
          connector2status.put(connector, status);
          modelTree.refresh(); // TODO: might be inefficient, look for a better solution
        });
    }

    private class TransportConnectorCell extends ListCell<ITransportConnector> {
        private HBox content;
        private Text name;
        private Text update;
        private ImageView imageView = new ImageView();
        private Button playStopButton = new Button("Open");
        private Button abortButton = new Button("Abort");
        private Button initialiseButton = new Button("Initialise");
        private ITransportConnector connector = null;
        /**
         *
         */
        public TransportConnectorCell() {
            super();
            this.name = new Text();
            this.name.setSmooth(true);
            this.name.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
            this.update = new Text();
            this.update.setSmooth(true);
            this.playStopButton.setOnAction(this::onPlayStopButtonClicked);
            this.initialiseButton.setOnAction(this::onInitialiseButtonClicked);
            this.abortButton.setOnAction(this::onAbortButtonClicked);
            VBox vBox = new VBox(this.name, this.update);
            this.content = new HBox(playStopButton, abortButton, initialiseButton, this.imageView, vBox);
            this.content.setSpacing(10);
        }

        private void onAbortButtonClicked(ActionEvent actionEvent) {
            if(connector != null) {
                try {
                    connector.abort();
                } catch (TransportException e) {
                    // TODO log
                    e.printStackTrace();
                }
            }
        }

        private void onInitialiseButtonClicked(ActionEvent actionEvent) {
            // TODO: open dialog to populate with properties and initialise after that
            try {
                connector.initialise(new HashMap<>());
            } catch (TransportException e) {
                e.printStackTrace();
            }
        }

        private void onPlayStopButtonClicked(ActionEvent actionEvent) {
            if(connector != null) {
                if(!connector.isInitialised()) {
                    onInitialiseButtonClicked(actionEvent);
                }
                if(connector.isInitialised()) {
                    try {
                        if (playStopButton.getText().equals("Open")) {
                            connector.connect();
                        } else {
                            connector.disconnect();
                        }
                    } catch (ReatmetricException e) {
                        // TODO log
                        e.printStackTrace();
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javafx.scene.control.Cell#updateItem(java.lang.Object, boolean)
         */
        @Override
        protected void updateItem(ITransportConnector item, boolean empty) {
            super.updateItem(item, empty);
            if ((item != null) && !connector2cell.containsKey(item)) {
                connector2cell.put(item, this);
            }
            if (empty || (item == null)) {
                setGraphic(null);
            } else {
                connector = item;
                TransportStatus ts = connector2status.get(item);
                if(ts.getStatus() == TransportConnectionStatus.IDLE || ts.getStatus() == TransportConnectionStatus.ERROR ||
                        ts.getStatus() == TransportConnectionStatus.NOT_INIT || ts.getStatus() == TransportConnectionStatus.ABORTED) {
                    playStopButton.setText("Open");
                } else {
                    playStopButton.setText("Close");
                }
                // this.imageView.setImage(null);
                this.name.setText(item.getName());
                this.update.setText(ts.getMessage() != null ? ts.getMessage() : "");
                setGraphic(this.content);
            }
        }
    }
}
