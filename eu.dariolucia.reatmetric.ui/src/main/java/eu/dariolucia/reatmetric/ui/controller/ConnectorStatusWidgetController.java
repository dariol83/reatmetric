/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.TransportConnectorInitDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectorStatusWidgetController implements Initializable {

    private static final Logger LOG = Logger.getLogger(ConnectorStatusWidgetController.class.getName());

    private static final Image ACTION_INIT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/menu.png"));
    private static final Image ACTION_START_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/play-circle.png"));
    private static final Image ACTION_STOP_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/stop-circle.png"));
    private static final Image ACTION_ABORT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/close-circle.png"));

    @FXML
    public Circle statusCircle;
    @FXML
    public Label nameLbl;
    @FXML
    public ImageView initImg;
    @FXML
    public Label messageLbl;
    @FXML
    public Label alarmLbl;
    @FXML
    public Label rxLabel;
    @FXML
    public Label txLabel;
    @FXML
    public ImageView startStopImg;
    @FXML
    public ImageView abortImg;

    private ITransportConnector connector;

    private TransportStatus lastStatus;

    public void updateStatus(TransportStatus status) {
        // Assume already in the UI thread
        lastStatus = status;
        nameLbl.setText(status.getName());
        messageLbl.setText(status.getMessage());
        updateAlarmLabel(status.getAlarmState());
        updateStatusCircle(status.getStatus());
        updateRateLabel(rxLabel, status.getRxRate());
        updateRateLabel(txLabel, status.getTxRate());
        updateStatusButton(status.getStatus());
        // Done
    }

    public void setConnector(ITransportConnector connector) {
        this.connector = connector;
        nameLbl.setText(connector.getName());
        messageLbl.setText("");
        updateStatusCircle(connector.getConnectionStatus());
        updateAlarmLabel(AlarmState.UNKNOWN);
        updateRateLabel(txLabel, 0);
        updateRateLabel(rxLabel, 0);
        updateStatusButton(connector.getConnectionStatus());
    }

    private void updateStatusButton(TransportConnectionStatus status) {
        if(status == TransportConnectionStatus.IDLE || status == TransportConnectionStatus.ERROR ||
                status == TransportConnectionStatus.NOT_INIT || status == TransportConnectionStatus.ABORTED) {
            startStopImg.setImage(ACTION_START_IMG);
        } else {
            startStopImg.setImage(ACTION_STOP_IMG);
        }
    }

    private void updateRateLabel(Label label, long rate) {
        String toSet;
        //
        if(rate > 1000000) {
            // Use megabits
            toSet = String.format("%.1f Mbps", rate/1000000.0);
        } else if(rate > 1000) {
            // Use kilobits
            toSet = String.format("%.1f Kbps", rate/1000.0);
        } else {
            toSet = rate + " bps";
        }
        label.setText(toSet);
    }

    private void updateStatusCircle(TransportConnectionStatus status) {
        switch(status) {
            case OPEN: {
                statusCircle.setFill(Paint.valueOf("lime"));
            }
            break;
            case ERROR:
            case ABORTED: {
                statusCircle.setFill(Paint.valueOf("darkred"));
            }
            break;
            case CONNECTING:
            case DISCONNECTING: {
                statusCircle.setFill(Paint.valueOf("dodgerblue"));
            }
            break;
            case IDLE: {
                statusCircle.setFill(Paint.valueOf("gray"));
            }
            break;
            case NOT_INIT: {
                statusCircle.setFill(Paint.valueOf("dimgray"));
            }
            break;
        }
    }

    private void updateAlarmLabel(AlarmState alarmState) {
        switch(alarmState) {
            case ERROR:
            case ALARM: {
                alarmLbl.setText(alarmState.name());
                alarmLbl.setStyle("-fx-background-color: darkred; -fx-text-fill: white; -fx-border-color: white; ");
            }
            break;
            case WARNING: {
                alarmLbl.setText("WARNING");
                alarmLbl.setStyle("-fx-background-color: darkorange; -fx-text-fill: black; -fx-border-color: black; ");
            }
            break;
            case NOT_APPLICABLE:
            case NOMINAL: {
                alarmLbl.setText("OK");
                alarmLbl.setStyle("-fx-background-color: lime; -fx-text-fill: black; -fx-border-color: black; ");
            }
            break;
            case VIOLATED: {
                alarmLbl.setText("VIOLATED");
                alarmLbl.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-border-color: black; ");
            }
            break;
            default: {
                alarmLbl.setText("N/A");
                alarmLbl.setStyle("-fx-background-color: gray; -fx-text-fill: black; -fx-border-color: black; ");
            }
            break;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initImg.setImage(ACTION_INIT_IMG);
        abortImg.setImage(ACTION_ABORT_IMG);
    }

    public void initButtonClicked(MouseEvent mouseEvent) {
        // Init now
        if(!connector.getSupportedProperties().isEmpty()) {
            TransportConnectorInitDialog.openWizard(connector);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(connector.getName() + " Initialisation Properties");
            alert.setHeaderText("No properties present");
            alert.setContentText("Transport connector " + connector.getName() + " does not have any configuration runtime property");
            alert.showAndWait();
        }
    }

    public void startStopButtonClicked(MouseEvent mouseEvent) {
        if(!connector.isInitialised() && !connector.getSupportedProperties().isEmpty()) {
            // Init now
            Optional<Boolean> initialise = TransportConnectorInitDialog.openWizard(connector);
            if(initialise.isPresent() && initialise.get()) {
                // Then start
                ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                    try {
                        connector.connect();
                    } catch (TransportException e) {
                        LOG.log(Level.WARNING, "Cannot open connection from " + connector.getName() + ": " + e.getMessage(), e);
                    }
                });
            }
        } else if(lastStatus == null || lastStatus.getStatus() == TransportConnectionStatus.NOT_INIT
                || lastStatus.getStatus() == TransportConnectionStatus.IDLE
                || lastStatus.getStatus() == TransportConnectionStatus.ERROR
                || lastStatus.getStatus() == TransportConnectionStatus.ABORTED) {
            ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                try {
                    connector.connect();
                } catch (TransportException e) {
                    LOG.log(Level.WARNING, "Cannot open connection from " + connector.getName() + ": " + e.getMessage(), e);
                }
            });
        } else {
            ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                try {
                    connector.disconnect();
                } catch (TransportException e) {
                    LOG.log(Level.WARNING, "Cannot close connection of " + connector.getName() + ": " + e.getMessage(), e);
                }
            });
        }
    }

    public void abortButtonClicked(MouseEvent mouseEvent) {
        ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
            try {
                connector.abort();
            } catch (TransportException e) {
                LOG.log(Level.WARNING, "Cannot abort connection of " + connector.getName() + ": " + e.getMessage(), e);
            }
        });
    }
}
