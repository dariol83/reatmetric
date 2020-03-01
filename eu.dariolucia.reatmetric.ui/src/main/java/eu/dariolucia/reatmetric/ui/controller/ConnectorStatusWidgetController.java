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
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class ConnectorStatusWidgetController implements Initializable {

    private static final Image ACTION_INIT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/init.png"));
    private static final Image ACTION_START_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/start.png"));
    private static final Image ACTION_STOP_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/stop.png"));
    private static final Image ACTION_ABORT_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/abort.png"));

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
        if(status.getStatus() == TransportConnectionStatus.IDLE || status.getStatus() == TransportConnectionStatus.ERROR ||
                status.getStatus() == TransportConnectionStatus.NOT_INIT || status.getStatus() == TransportConnectionStatus.ABORTED) {
            startStopImg.setImage(ACTION_START_IMG);
        } else {
            startStopImg.setImage(ACTION_STOP_IMG);
        }
        // Done
    }

    public void setConnector(ITransportConnector connector) {
        this.connector = connector;
    }

    private void updateRateLabel(Label rxLabel, long rxRate) {
    }

    private void updateStatusCircle(TransportConnectionStatus status) {
    }

    private void updateAlarmLabel(AlarmState alarmState) {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initImg.setImage(ACTION_INIT_IMG);
        abortImg.setImage(ACTION_ABORT_IMG);
    }

    public void initButtonClicked(MouseEvent mouseEvent) {
        // Init now
        // TODO
    }

    public void startStopButtonClicked(MouseEvent mouseEvent) {
        if(lastStatus != null) {
            if(lastStatus.getStatus() == TransportConnectionStatus.NOT_INIT) {
                // Init now
                // TODO
                // Then start
                ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                    connector.connect();
                });
            } else if(lastStatus.getStatus() == TransportConnectionStatus.IDLE || lastStatus.getStatus() == TransportConnectionStatus.ERROR ||
                    lastStatus.getStatus() == TransportConnectionStatus.ABORTED) {
                ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                    connector.connect();
                });
            } else {
                ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
                    connector.disconnect();
                });
            }
        }
    }

    public void abortButtonClicked(MouseEvent mouseEvent) {
        ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
            connector.abort();
        });
    }
}
