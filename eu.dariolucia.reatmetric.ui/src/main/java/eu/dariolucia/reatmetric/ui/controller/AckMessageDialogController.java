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
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.udd.PopoverChartController;
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityResolver;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class AckMessageDialogController implements Initializable, IAcknowledgedMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(AckMessageDialogController.class.getName());

    private static final int MAX_TABLE_ENTRIES = 5000;
    private static final int TABLE_ENTRIES_REMOVE_ON_FULL = MAX_TABLE_ENTRIES/10;

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

    @FXML
    private ContextMenu tableContextMenu;
    @FXML
    private MenuItem quickPlotMenuItem;

    private volatile IReatmetricSystem system;
    private volatile Consumer<Boolean> handler;

    private final Set<Integer> pendingAcknowledgementSet = new HashSet<>();

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

        ackMessageTableView.getSelectionModel().selectedItemProperty().addListener(o -> {
            if(ackMessageTableView.getSelectionModel().getSelectedItem() != null && ackMessageTableView.getSelectionModel().getSelectedItem().getMessage().getLinkedEntityId() != null) {
                ackMessageTableView.setContextMenu(tableContextMenu);
            } else {
                ackMessageTableView.setContextMenu(null);
            }
        });
        ackMessageTableView.setContextMenu(null);
    }

    public void activate(Consumer<Boolean> alarmPresentNotifier) {
        this.handler = alarmPresentNotifier;
        this.system = ReatmetricUI.selectedSystem().getSystem();
        if (this.system != null) {
            try {
                this.system.getAcknowledgedMessageMonitorService().subscribe(this, new AcknowledgedMessageFilter(null, null));
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Subscription to AcknowledgedMessageMonitorService failed: " + e.getMessage(), e);
            }
        }
    }

    public void deactivate() {
        if (this.system != null) {
            try {
                this.system.getAcknowledgedMessageMonitorService().unsubscribe(this);
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Unsubscription to AcknowledgedMessageMonitorService failed: " + e.getMessage(), e);
            }
        }
        this.ackMessageTableView.getItems().clear();
        this.ackMessageTableView.refresh();
        if (this.handler != null) {
            this.handler.accept(false);
        }
        this.handler = null;
        this.system = null;
    }

    @Override
    public void dataItemsReceived(List<AcknowledgedMessage> dataItems) {
        Set<Long> messagesToRemove = new HashSet<>();
        List<AcknowledgedMessage> messagesToAdd = new LinkedList<>();
        for (AcknowledgedMessage am : dataItems) {
            if (am.getState() == AcknowledgementState.ACKNOWLEDGED) {
                messagesToRemove.add(am.getInternalId().asLong());
            } else {
                // New message for acknowledgement
                messagesToAdd.add(am);

            }
        }
        FxUtils.runLater(() -> {
            List<AcknowledgedMessage> toRemoveActuals = new LinkedList<>();
            // I iterate on the whole list, so here I compute the messages pending ack
            pendingAcknowledgementSet.clear();
            for (int i = 0; i < ackMessageTableView.getItems().size(); ++i) {
                AcknowledgedMessage am = ackMessageTableView.getItems().get(i);
                if (messagesToRemove.contains(am.getInternalId().asLong())) {
                    toRemoveActuals.add(am);
                } else {
                    // Not to be removed, remember the ID
                    if (am.getMessage().getLinkedEntityId() != null) {
                        pendingAcknowledgementSet.add(am.getMessage().getLinkedEntityId());
                    }
                }
            }
            // Add all the new ones
            Set<Integer> finallyRemoved = toRemoveActuals.stream().map(o -> o.getMessage().getLinkedEntityId()).filter(Objects::nonNull).collect(Collectors.toSet());
            pendingAcknowledgementSet.addAll(messagesToAdd.stream().map(o -> o.getMessage().getLinkedEntityId()).filter(Objects::nonNull).collect(Collectors.toSet()));
            finallyRemoved.removeAll(pendingAcknowledgementSet);
            // Inform the model browser of the current status
            MainViewController.instance().getModelController().informAcknowledgementStatus(finallyRemoved, Collections.unmodifiableSet(pendingAcknowledgementSet));
            // Finally update the table
            ackMessageTableView.getItems().removeAll(toRemoveActuals);
            ackMessageTableView.getItems().addAll(messagesToAdd);
            if(ackMessageTableView.getItems().size() > MAX_TABLE_ENTRIES) {
                int toRemove = (ackMessageTableView.getItems().size() - MAX_TABLE_ENTRIES) + TABLE_ENTRIES_REMOVE_ON_FULL;
                ackMessageTableView.getItems().remove(0, toRemove);
            }
            if (this.handler != null) {
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
        if (!selected.isEmpty() && this.system != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    system.getAcknowledgementService().acknowledgeMessages(selected, ReatmetricUI.username());
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Acknowledgement failed: " + e.getMessage(), e);
                }
            });
            this.ackMessageTableView.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void ackAllButtonSelected(ActionEvent actionEvent) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                system.getAcknowledgementService().acknowledgeAllMessages(ReatmetricUI.username());
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Acknowledgement failed: " + e.getMessage(), e);
            }
        });
        this.ackMessageTableView.getSelectionModel().clearSelection();
    }

    public boolean isPendingAcknowledgement(int externalId) {
        return pendingAcknowledgementSet.contains(externalId);
    }

    public void ackRecursive(Set<Integer> externalIdsToAcknowledge) {
        List<AcknowledgedMessage> toAckItems = new LinkedList<>();
        // Iterate on the whole list
        for (int i = 0; i < ackMessageTableView.getItems().size(); ++i) {
            AcknowledgedMessage am = ackMessageTableView.getItems().get(i);
            if (am.getMessage().getLinkedEntityId() != null && externalIdsToAcknowledge.contains(am.getMessage().getLinkedEntityId())) {
                toAckItems.add(am);
            }
        }
        if(!toAckItems.isEmpty()) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    system.getAcknowledgementService().acknowledgeMessages(toAckItems, ReatmetricUI.username());
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Acknowledgement failed: " + e.getMessage(), e);
                }
            });
        }
    }

    @FXML
    public void menuAboutToShow(WindowEvent windowEvent) {
        boolean setVisible = ackMessageTableView.getSelectionModel().getSelectedItem() != null && ackMessageTableView.getSelectionModel().getSelectedItem().getMessage().getLinkedEntityId() != null;
        if(setVisible) {
            // Get the source and make sure it can be plotted
            SystemEntity se = SystemEntityResolver.getResolver().getSystemEntity(ackMessageTableView.getSelectionModel().getSelectedItem().getMessage().getLinkedEntityId());
            setVisible = se.getType() == SystemEntityType.PARAMETER || se.getType() == SystemEntityType.EVENT;
            if(setVisible && se.getType() == SystemEntityType.PARAMETER) {
                // Get the descriptor and make sure it can be plotted
                try {
                    ParameterDescriptor pd = (ParameterDescriptor) SystemEntityResolver.getResolver().getDescriptorOf(se.getExternalId());
                    if (pd == null) {
                        // Weird ...
                        setVisible = false;
                    } else if (pd.getEngineeringDataType() != ValueTypeEnum.REAL &&
                            pd.getEngineeringDataType() != ValueTypeEnum.SIGNED_INTEGER &&
                            pd.getEngineeringDataType() != ValueTypeEnum.UNSIGNED_INTEGER &&
                            pd.getEngineeringDataType() != ValueTypeEnum.ENUMERATED) {
                        LOG.log(Level.FINE, "Cannot plot system entity " + se.getPath() + ": unsupported parameter type " + pd.getEngineeringDataType());
                        setVisible = false;
                    }
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.WARNING, "Cannot plot system entity " + se.getPath() + " (descriptor error): " + e.getMessage(), e);
                    setVisible = false;
                }
            }
        }
        quickPlotMenuItem.setVisible(setVisible);
    }

    @FXML
    public void onQuickPlotMenuItem(ActionEvent actionEvent) {
        SystemEntity value = SystemEntityResolver.getResolver().getSystemEntity(ackMessageTableView.getSelectionModel().getSelectedItem().getMessage().getLinkedEntityId());
        try {
            PopoverChartController ctrl = new PopoverChartController(value);
            ctrl.show();
        } catch (ReatmetricException e) {
            LOG.log(Level.WARNING, "Cannot plot system entity " + value.getPath() + ": " + e.getMessage(), e);
        }
    }
}
