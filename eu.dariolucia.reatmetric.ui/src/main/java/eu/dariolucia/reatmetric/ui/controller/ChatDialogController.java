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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.messages.input.OperationalMessageRequest;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatDialogController implements Initializable, IOperationalMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(ChatDialogController.class.getName());

    @FXML
    private Button sendButton;
    @FXML
    private TableColumn<OperationalMessage, Instant> genTimeCol;
    @FXML
    private TableColumn<OperationalMessage, String> sourceCol;
    @FXML
    private TableColumn<OperationalMessage, String> messageCol;
    @FXML
    private TableView<OperationalMessage> chatTableView;
    @FXML
    private TextField messageText;

    private volatile IReatmetricSystem system;
    private volatile Consumer<Boolean> handler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.chatTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.chatTableView.setPlaceholder(new Label("No new messages"));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSource()));
        this.messageCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage()));

        this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());

        this.sendButton.disableProperty().bind(messageText.textProperty().isEmpty());
    }

    public void activate(Consumer<Boolean> newMessageNotifier) {
        this.handler = newMessageNotifier;
        this.system = ReatmetricUI.selectedSystem().getSystem();
        if (this.system != null) {
            try {
                this.system.getOperationalMessageMonitorService().subscribe(this, new OperationalMessageFilter(null, null, null, Collections.singletonList(Severity.CHAT)));
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Subscription to OperationalMessageMonitorService failed: " + e.getMessage(), e);
            }
        }
    }

    public void setFocus() {
        this.messageText.requestFocus();
    }

    public void deactivate() {
        if (this.system != null) {
            try {
                this.system.getOperationalMessageMonitorService().unsubscribe(this);
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Unsubscription to OperationalMessageMonitorService failed: " + e.getMessage(), e);
            }
        }
        this.chatTableView.getItems().clear();
        this.chatTableView.refresh();
        if (this.handler != null) {
            this.handler.accept(false);
        }
        this.handler = null;
        this.system = null;
    }

    @Override
    public void dataItemsReceived(List<OperationalMessage> dataItems) {
        FxUtils.runLater(() -> {
            // Update the table
            chatTableView.getItems().addAll(dataItems);
            if (this.handler != null) {
                // Signal new messages
                this.handler.accept(true);
            }
        });
    }

    @FXML
    private void sendMessageAction(ActionEvent actionEvent) {
        final String messageToSend = messageText.getText().trim();
        if(!messageToSend.isBlank()) {
            messageText.setText("");
            messageText.requestFocus();
            ReatmetricUI.threadPool(getClass()).submit(() -> {
                try {
                    if(system != null) {
                        system.getOperationalMessageCollectorService().logMessage(
                                OperationalMessageRequest.of("ReatMetric",
                                        messageToSend,
                                        ReatmetricUI.username(),
                                        Severity.CHAT,
                                        null,
                                        null
                                ));
                    }
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Cannot log message '" + messageToSend + "' due to error: " + e.getMessage(), e);
                }
            });
        }
    }
}
