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
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.io.IOException;
import java.net.URL;
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

    // Popup selector for date/time
    private final Popup dataInspectionPopup = new Popup();
    private RawDataDetailsWidgetController dataInspectionController;

    // Report cache
    private final LRUCache cache = new LRUCache();

    @FXML
    private void inspectItemAction(ActionEvent event) {
        final RawData selectedRawData = this.dataItemTableView.getSelectionModel().getSelectedItem();
        if(selectedRawData != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    RawData rd = selectedRawData;
                    // Data
                    byte[] data = null;
                    if(rd.isContentsSet()) {
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
                    if(formatData == null) {
                        formatData = ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().getRenderedInformation(rd);
                        cache.put(selectedRawData.getInternalId(), formatData);
                    }
                    String name = selectedRawData.getName();
                    String genTime = InstantCellFactory.DATE_TIME_FORMATTER.format(selectedRawData.getGenerationTime());
                    final LinkedHashMap<String, String> fformatData = formatData;
                    Platform.runLater(() -> {
                        this.dataInspectionController.setData(name + " - Gen. Time " + genTime, fdata, fformatData);
                        // Bounds b = this.dataItemTableView.localToScreen(this.dataItemTableView.getBoundsInLocal());
                        this.dataInspectionPopup.setX(((MenuItem)event.getSource()).getParentPopup().getAnchorX());
                        this.dataInspectionPopup.setY(((MenuItem)event.getSource()).getParentPopup().getAnchorY());
                        this.dataInspectionPopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
                        this.dataInspectionPopup.show(this.displayTitledPane.getScene().getWindow());
                    });
                } catch(ReatmetricException e) {
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

        this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
        this.recTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
        this.qualityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Quality item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case BAD:
                            setTextFill(Color.DARKRED);
                            break;
                        case UNKNOWN:
                            setTextFill(Color.BLACK);
                            break;
                        default:
                            setTextFill(Color.DARKGREEN);
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
        
        this.dataInspectionPopup.setAutoHide(true);
        this.dataInspectionPopup.setHideOnEscape(true);
        
        try {
            URL rawDataDetailsUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/RawDataDetailsWidget.fxml");
            FXMLLoader loader = new FXMLLoader(rawDataDetailsUrl);
            Parent rawDataDetailsWidget = loader.load();
            this.dataInspectionController = loader.getController();
            this.dataInspectionPopup.getContent().addAll(rawDataDetailsWidget);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        informDataItemsReceived(messages);
    }

    @Override
    protected void doServiceSubscribe(RawDataFilter selectedFilter) throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().subscribe(this, selectedFilter);
    }

    @Override
    protected void doServiceUnsubscribe() throws ReatmetricException {
        ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().unsubscribe(this);
    }

    @Override
    protected List<RawData> doRetrieve(RawData om, int n, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().retrieve(om, n, direction, filter);
    }

    @Override
    protected List<RawData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().retrieve(selectedTime, n, direction, filter);
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
