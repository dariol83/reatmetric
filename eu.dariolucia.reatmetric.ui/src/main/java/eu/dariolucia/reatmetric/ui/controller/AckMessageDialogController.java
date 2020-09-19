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
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AckMessageDialogController implements Initializable, IAcknowledgedMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(AckMessageDialogController.class.getName());

    @FXML
    private TableColumn<AcknowledgedMessage, String> idCol;
    @FXML
    private TableColumn<AcknowledgedMessage, Severity> severityCol;
    @FXML
    private TableColumn<AcknowledgedMessage, Instant> genTimeCol;
    @FXML
    private TableColumn<AcknowledgedMessage, String> sourceCol;
    @FXML
    private TableColumn<AcknowledgedMessage, String> messageCol;

    @FXML
    private TableView<AcknowledgedMessage> ackMessageTableView;
    private volatile IReatmetricSystem system;
    private volatile Consumer<Boolean> handler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.ackMessageTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.ackMessageTableView.setPlaceholder(new Label(""));
        this.idCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getId()));
        this.severityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getSeverity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getGenerationTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getSource()));
        this.messageCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getMessage()));

        this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
        this.severityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Severity item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case ALARM:
                            setTextFill(Color.RED);
                            break;
                        case WARN:
                            setTextFill(Color.DARKORANGE);
                            break;
                        default:
                            setTextFill(Color.BLACK);
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
    }

    public void activate(Consumer<Boolean> alarmPresentNotifier) {
        this.handler = alarmPresentNotifier;
        this.system = ReatmetricUI.selectedSystem().getSystem();
        if(this.system != null) {
            try {
                this.system.getAcknowledgedMessageMonitorService().subscribe(this, new AcknowledgedMessageFilter(null, null));
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Subscription to AcknowledgedMessageMonitorService failed: " + e.getMessage() , e);
            }
        }
    }

    public void deactivate() {
        if(this.system != null) {
            try {
                this.system.getAcknowledgedMessageMonitorService().unsubscribe(this);
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Unsubscription to AcknowledgedMessageMonitorService failed: " + e.getMessage() , e);
            }
        }
        this.ackMessageTableView.getItems().clear();
        this.ackMessageTableView.refresh();
        if(this.handler != null) {
            this.handler.accept(false);
        }
        this.handler = null;
        this.system = null;
    }

    @Override
    public void dataItemsReceived(List<AcknowledgedMessage> dataItems) {
        Set<Long> messagesToRemove = new HashSet<>();
        List<AcknowledgedMessage> messagesToAdd = new LinkedList<>();
        for(AcknowledgedMessage am : dataItems) {
            if(am.getState() == AcknowledgementState.ACKNOWLEDGED) {
                messagesToRemove.add(am.getInternalId().asLong());
            } else {
                messagesToAdd.add(am);
            }
        }
        Platform.runLater(() -> {
            List<AcknowledgedMessage> toRemoveActuals = new LinkedList<>();
            for(int i = 0; i < ackMessageTableView.getItems().size(); ++i) {
                AcknowledgedMessage am = ackMessageTableView.getItems().get(i);
                if(messagesToRemove.contains(am.getInternalId().asLong())) {
                    toRemoveActuals.add(am);
                }
            }
            ackMessageTableView.getItems().removeAll(toRemoveActuals);
            ackMessageTableView.getItems().addAll(messagesToAdd);
            if(this.handler != null) {
                this.handler.accept(!ackMessageTableView.getItems().isEmpty());
            }
        });
    }

    @FXML
    public void ackSelectionButtonSelected(ActionEvent actionEvent) {
        List<AcknowledgedMessage> selected = new ArrayList<>(this.ackMessageTableView.getSelectionModel().getSelectedItems());
        ackMessages(selected);
    }

    private void ackMessages(List<AcknowledgedMessage> selected) {
        if(!selected.isEmpty() && this.system != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    system.getAcknowledgementService().acknowledgeMessages(selected, ReatmetricUI.username());
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Acknowledgement failed: " + e.getMessage() , e);
                }
            });
        }
    }

    @FXML
    public void ackAllButtonSelected(ActionEvent actionEvent) {
        List<AcknowledgedMessage> all = new ArrayList<>(this.ackMessageTableView.getItems());
        ackMessages(all);
    }
}
