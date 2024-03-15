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
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class EventDataViewController extends AbstractDataItemLogViewController<EventData, EventDataFilter>
        implements IEventDataSubscriber {

    @FXML
    protected CheckMenuItem toggleShowToolbarItem;
    @FXML
    protected MenuItem detachMenuItem;
    @FXML
    protected ToolBar toolbar;

    @FXML
    private TableColumn<EventData, String> nameCol;
    @FXML
    private TableColumn<EventData, String> typeCol;
    @FXML
    private TableColumn<EventData, String> routeCol;
    @FXML
    private TableColumn<EventData, Severity> severityCol;
    @FXML
    private TableColumn<EventData, Instant> genTimeCol;
    @FXML
    private TableColumn<EventData, Instant> recTimeCol;
    @FXML
    private TableColumn<EventData, String> sourceCol;
    @FXML
    private TableColumn<EventData, String> qualifierCol;
    @FXML
    private TableColumn<EventData, String> parentCol;

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        super.doInitialize(url, rb);
        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getName()));
        this.typeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getType()));
        this.routeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getRoute()));
        this.severityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSeverity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
        this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSource()));
        this.qualifierCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getQualifier()));
        this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

        this.genTimeCol.setCellFactory(getInstantCellCallback());
        this.recTimeCol.setCellFactory(getInstantCellCallback());
        this.nameCol.setCellFactory(getNormalTextCellCallback());
        this.typeCol.setCellFactory(getNormalTextCellCallback());
        this.routeCol.setCellFactory(getNormalTextCellCallback());
        this.parentCol.setCellFactory(getNormalTextCellCallback());
        this.sourceCol.setCellFactory(getNormalTextCellCallback());
        this.qualifierCol.setCellFactory(getNormalTextCellCallback());

        this.severityCol.setCellFactory(zoomEnabledWrapper(column -> new TableCell<>() {
            @Override
            protected void updateItem(Severity item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case ALARM:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ALARM);
                            break;
                        case ERROR:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ERROR);
                            break;
                        case WARN:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_WARNING);
                            break;
                        default:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_NOMINAL);
                            break;
                    }
                } else {
                    CssHandler.updateStyleClass(this, null);
                    setText("");
                    setGraphic(null);
                }
            }
        }));

        initialiseToolbarVisibility(displayTitledPane, toolbar, toggleShowToolbarItem);
    }

    @FXML
    protected void onDragOver(DragEvent event) {
        if (event.getGestureSource() != this.dataItemTableView
                && event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        if (event.getGestureSource() != this.dataItemTableView
                && event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
        }
        event.consume();
    }

    @FXML
    private void onDragExited(DragEvent event) {
        if (event.getGestureSource() != this.dataItemTableView
                && event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
        }
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasContent(SystemEntityDataFormats.CONTAINER)) {
            List<SystemEntity> entities = (List<SystemEntity>) db.getContent(SystemEntityDataFormats.CONTAINER);
            // Get the first
            EventDataFilter filter = getCurrentFilter();
            if (filter == null) {
                filter = new EventDataFilter(null, null, null, null, null, null, null);
            }
            EventDataFilter newFilter = new EventDataFilter(entities.get(0).getPath(), filter.getEventPathList(), filter.getRouteList(),
                    filter.getTypeList(), filter.getSourceList(), filter.getSeverityList(), null);
            applyFilter(newFilter);
            success = true;
        }

        event.setDropCompleted(success);

        event.consume();
    }

    @Override
    public void dataItemsReceived(List<EventData> messages) {
        informDataItemsReceived(messages);
    }

    @Override
    protected void doServiceSubscribe(EventDataFilter selectedFilter) throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().subscribe(this, selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().unsubscribe(this);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<EventData> doRetrieve(EventData om, int n, RetrievalDirection direction, EventDataFilter filter)
            throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().retrieve(om, n, direction,
                    filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<EventData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction,
                                         EventDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().retrieve(selectedTime, n,
                    direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected Instant doGetGenerationTime(EventData om) {
        return om.getGenerationTime();
    }

    @Override
    protected URL doGetFilterWidget() {
        return getClass().getClassLoader()
                .getResource("eu/dariolucia/reatmetric/ui/fxml/EventDataFilterWidget.fxml");
    }

    @Override
    protected String doGetComponentId() {
        return "EventDataView";
    }

    @FXML
    private void locateItemAction(ActionEvent actionEvent) {
        EventData ed = this.dataItemTableView.getSelectionModel().getSelectedItem();
        if(ed != null) {
            MainViewController.instance().getModelController().locate(ed.getPath());
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
