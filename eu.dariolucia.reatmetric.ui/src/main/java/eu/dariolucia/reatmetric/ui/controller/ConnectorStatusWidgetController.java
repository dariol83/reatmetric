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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.TransportConnectorInitDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectorStatusWidgetController implements Initializable {

    private static final Logger LOG = Logger.getLogger(ConnectorStatusWidgetController.class.getName());

    private static final Image ACTION_INIT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/cog.svg.png"));
    private static final Image ACTION_ABORT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/close-circle.svg.png"));

    @FXML
    public Circle statusCircle;
    @FXML
    public Label nameLbl;
    @FXML
    public ImageView initImg;
    @FXML
    public Label alarmLbl;
    @FXML
    public Label rxLabel;
    @FXML
    public Label txLabel;
    @FXML
    public ToggleSwitch startStopSwitch;
    @FXML
    public ImageView abortImg;

    private ITransportConnector connector;

    private TransportStatus lastStatus;

    public void updateStatus(TransportStatus status) {
        // Assume already in the UI thread
        lastStatus = status;
        nameLbl.setText(status.getName());
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
        updateStatusCircle(connector.getConnectionStatus());
        updateAlarmLabel(AlarmState.UNKNOWN);
        updateRateLabel(txLabel, 0);
        updateRateLabel(rxLabel, 0);
        updateStatusButton(connector.getConnectionStatus());
    }

    private void updateStatusButton(TransportConnectionStatus status) {
        if(status == TransportConnectionStatus.IDLE || status == TransportConnectionStatus.ERROR ||
                status == TransportConnectionStatus.NOT_INIT || status == TransportConnectionStatus.ABORTED) {
            startStopSwitch.setSelected(false);
        } else {
            startStopSwitch.setSelected(true);
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
            // Only init
            TransportConnectorInitDialog.openWizard(connector, initImg, false);
        } else {
            TransportConnectorInitDialog.openWizardNoElements(connector, initImg);
        }
    }

    public void startStopButtonClicked(MouseEvent mouseEvent) {
        if(!connector.isInitialised() && !connector.getSupportedProperties().isEmpty()) {
            // Init and connect now
            startStopSwitch.setSelected(false);
            TransportConnectorInitDialog.openWizard(connector, startStopSwitch, true);
        } else if(lastStatus == null || lastStatus.getStatus() == TransportConnectionStatus.NOT_INIT
                || lastStatus.getStatus() == TransportConnectionStatus.IDLE
                || lastStatus.getStatus() == TransportConnectionStatus.ERROR
                || lastStatus.getStatus() == TransportConnectionStatus.ABORTED) {
            ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                try {
                    if(!connector.isInitialised()) {
                        // If you are here, it means there are really no properties, so initialise first
                        connector.initialise(connector.getCurrentProperties());
                    }
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
