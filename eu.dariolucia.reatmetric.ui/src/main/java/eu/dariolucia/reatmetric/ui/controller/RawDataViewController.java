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

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class RawDataViewController extends AbstractDataItemLogViewController<RawData, RawDataFilter> implements IRawDataSubscriber {

    @FXML
    protected CheckMenuItem toggleShowToolbarItem;
    @FXML
    protected MenuItem detachMenuItem;
    @FXML
    protected ToolBar toolbar;

    @FXML
    private TableColumn<RawData, String> nameCol;
    @FXML
    private TableColumn<RawData, String> typeCol;
    @FXML
    private TableColumn<RawData, String> routeCol;
    @FXML
    private TableColumn<RawData, Quality> qualityCol;
    @FXML
    private TableColumn<RawData, Instant> genTimeCol;
    @FXML
    private TableColumn<RawData, Instant> recTimeCol;
    @FXML
    private TableColumn<RawData, String> sourceCol;

    // Inspector controller
    private RawDataDetailsWidgetController dataInspectionController;
    private Parent dataInspectionWidget;

    // Report cache
    private final LRUCache cache = new LRUCache();

    @FXML
    private void inspectItemAction(ActionEvent event) {
        final RawData selectedRawData = this.dataItemTableView.getSelectionModel().getSelectedItem();
        if (selectedRawData != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    RawData rd = selectedRawData;
                    // Data
                    byte[] data = null;
                    if (rd.isContentsSet()) {
                        data = rd.getContents();
                    } else {
                        rd = ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().getRawDataContents(rd.getInternalId());
                        if (rd != null) {
                            data = rd.getContents();
                        }
                    }
                    final byte[] fdata = data;
                    // Rendered item
                    LinkedHashMap<String, String> formatData = cache.get(selectedRawData.getInternalId());
                    if (formatData == null) {
                        formatData = ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().getRenderedInformation(rd);
                        cache.put(selectedRawData.getInternalId(), formatData);
                    }
                    String name = selectedRawData.getName();
                    String genTime = InstantCellFactory.DATE_TIME_FORMATTER.format(selectedRawData.getGenerationTime());
                    final LinkedHashMap<String, String> fformatData = formatData;
                    FxUtils.runLater(() -> {
                        this.dataInspectionController.setData(name + " - Gen. Time " + genTime, fdata, fformatData);
                        DialogUtils.customInfoDialog(this.displayTitledPane.getScene().getWindow(), this.dataInspectionWidget, name + " - " + genTime);
                    });
                } catch (ReatmetricException | RemoteException e) {
                    ReatmetricUI.setStatusLabel("Retrieve of raw data contents failed: " + selectedRawData.getName());
                }
            });
        }
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        super.doInitialize(url, rb);
        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getName()));
        this.typeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getType()));
        this.routeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getRoute()));
        this.qualityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getQuality()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
        this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSource()));

        this.genTimeCol.setCellFactory(getInstantCellCallback());
        this.recTimeCol.setCellFactory(getInstantCellCallback());
        Callback<TableColumn<RawData, Quality>, TableCell<RawData, Quality>> qualityColFactory = column -> new TableCell<>() {
            @Override
            protected void updateItem(Quality item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case BAD:
                            CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ERROR);
                            break;
                        case UNKNOWN:
                            CssHandler.updateStyleClass(this, null);
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
        };
        this.qualityCol.setCellFactory(zoomEnabledWrapper(qualityColFactory));

        this.nameCol.setCellFactory(getNormalTextCellCallback());
        this.typeCol.setCellFactory(getNormalTextCellCallback());
        this.routeCol.setCellFactory(getNormalTextCellCallback());
        this.sourceCol.setCellFactory(getNormalTextCellCallback());

        try {
            URL rawDataDetailsUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/RawDataDetailsWidget.fxml");
            FXMLLoader loader = new FXMLLoader(rawDataDetailsUrl);
            this.dataInspectionWidget = loader.load();
            this.dataInspectionController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }

        initialiseToolbarVisibility(displayTitledPane, toolbar, toggleShowToolbarItem);
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

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        informDataItemsReceived(messages);
    }

    @Override
    protected void doServiceSubscribe(RawDataFilter selectedFilter) throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().subscribe(this, selectedFilter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected void doServiceUnsubscribe() throws ReatmetricException {
        try {
            ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().unsubscribe(this);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<RawData> doRetrieve(RawData om, int n, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().retrieve(om, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected List<RawData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().retrieve(selectedTime, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    protected Instant doGetGenerationTime(RawData om) {
        return om.getGenerationTime();
    }

    @Override
    protected URL doGetFilterWidget() {
        return getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/RawDataFilterWidget.fxml");
    }

    @Override
    protected String doGetComponentId() {
        return "RawDataView";
    }

    private static class LRUCache extends LinkedHashMap<IUniqueId, LinkedHashMap<String, String>> {

        private static final int MAX_CACHE_SIZE = 20;

        public LRUCache() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<IUniqueId, LinkedHashMap<String, String>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    }
}
