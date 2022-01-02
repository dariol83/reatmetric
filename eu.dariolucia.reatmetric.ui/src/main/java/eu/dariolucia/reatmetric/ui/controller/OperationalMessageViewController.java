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

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.udd.PopoverChartController;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityResolver;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 *
 * @author dario
 */
public class OperationalMessageViewController extends AbstractDataItemLogViewController<OperationalMessage, OperationalMessageFilter> implements IOperationalMessageSubscriber {

    private static final Logger LOG = Logger.getLogger(OperationalMessageViewController.class.getName());

    @FXML
    protected CheckMenuItem toggleShowToolbarItem;
    @FXML
    protected MenuItem detachMenuItem;
    @FXML
    protected ToolBar toolbar;

    @FXML
    private TableColumn<OperationalMessage, String> idCol;
    @FXML
    private TableColumn<OperationalMessage, Severity> severityCol;
    @FXML
    private TableColumn<OperationalMessage, Instant> genTimeCol;
    @FXML
    private TableColumn<OperationalMessage, String> sourceCol;
    @FXML
    private TableColumn<OperationalMessage, String> messageCol;

    @FXML
    private ContextMenu tableContextMenu;
    @FXML
    private MenuItem quickPlotMenuItem;

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        super.doInitialize(url, rb);

        this.idCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getId()));
        this.severityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSeverity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSource()));
        this.messageCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage()));

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

        dataItemTableView.getSelectionModel().selectedItemProperty().addListener(o -> {
            if(dataItemTableView.getSelectionModel().getSelectedItem() != null && dataItemTableView.getSelectionModel().getSelectedItem().getLinkedEntityId() != null) {
                dataItemTableView.setContextMenu(tableContextMenu);
            } else {
                dataItemTableView.setContextMenu(null);
            }
        });
        dataItemTableView.setContextMenu(null);

        initialiseToolbarVisibility(displayTitledPane, toolbar, toggleShowToolbarItem);
    }

    @Override
    public void dataItemsReceived(List<OperationalMessage> messages) {
        informDataItemsReceived(messages);
    }

    @Override
    protected void doServiceSubscribe(OperationalMessageFilter selectedFilter) throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getOperationalMessageMonitorService().subscribe(this, selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getOperationalMessageMonitorService().unsubscribe(this);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<OperationalMessage> doRetrieve(OperationalMessage om, int n, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getOperationalMessageMonitorService().retrieve(om, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<OperationalMessage> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getOperationalMessageMonitorService().retrieve(selectedTime, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected Instant doGetGenerationTime(OperationalMessage om) {
        return om.getGenerationTime();
    }

    @Override
    protected URL doGetFilterWidget() {
        return getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/OperationalMessageFilterWidget.fxml");
    }

    @Override
    protected String doGetComponentId() {
        return "OperationalMessageView";
    }

    @FXML
    public void menuAboutToShow(WindowEvent windowEvent) {
        boolean setVisible = dataItemTableView.getSelectionModel().getSelectedItem() != null && dataItemTableView.getSelectionModel().getSelectedItem().getLinkedEntityId() != null;
        if(setVisible) {
            // Get the source and make sure it can be plotted
            SystemEntity se = SystemEntityResolver.getResolver().getSystemEntity(dataItemTableView.getSelectionModel().getSelectedItem().getLinkedEntityId());
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
        SystemEntity value = SystemEntityResolver.getResolver().getSystemEntity(dataItemTableView.getSelectionModel().getSelectedItem().getLinkedEntityId());
        try {
            PopoverChartController ctrl = new PopoverChartController(value);
            ctrl.show();
        } catch (ReatmetricException e) {
            LOG.log(Level.WARNING, "Cannot plot system entity " + value.getPath() + ": " + e.getMessage(), e);
        }
    }

    @FXML
    private void detachAttachItemAction(ActionEvent actionEvent) {
        if(DetachedTabUtil.isDetached((Stage) displayTitledPane.getScene().getWindow())) {
            DetachedTabUtil.attachTab((Stage) displayTitledPane.getScene().getWindow());
            informDisplayAttached();
        }
    }

    @Override
    protected void informDisplayAttached() {
        detachMenuItem.setDisable(true);
    }

    @Override
    protected void informDisplayDetached() {
        detachMenuItem.setDisable(false);
    }
}
