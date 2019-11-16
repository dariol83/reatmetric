/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
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
    
    @FXML
    private void inspectItemAction(ActionEvent event) {
        final RawData selectedRawData = this.dataItemTableView.getSelectionModel().getSelectedItem();
        if(selectedRawData != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    byte[] data = null;
                    RawData rd = ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().getRawDataContents(selectedRawData.getInternalId());
                    if(rd != null) {
                        data = rd.getContents();
                    }
                    final byte[] fdata = data;
                    Platform.runLater(() -> {
                        this.dataInspectionController.setData(selectedRawData.getName() + " - Gen. Time " + selectedRawData.getGenerationTime(),fdata);
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
        
        this.qualityCol.setCellFactory(column -> {
            return new TableCell<RawData, Quality>() {
                @Override
                protected void updateItem(Quality item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && !isEmpty()) {
                        setText(item.name());
                        switch (item) {
                            case BAD:
                                setTextFill(Color.DARKRED);
                                setStyle("-fx-font-weight: bold");
                                break;
                            case UNKNOWN:
                                setTextFill(Color.BLACK);
                                setStyle("-fx-font-weight: bold");
                                break;
                            default:
                                setTextFill(Color.DARKGREEN);
                                setStyle("-fx-font-weight: bold");
                                break;
                        }
                    } else {
                        setText("");
                        setGraphic(null);
                    }
                }
            };
        });
        
        this.dataInspectionPopup.setAutoHide(true);
        this.dataInspectionPopup.setHideOnEscape(true);
        
        try {
            URL rawDataDetailsUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/RawDataDetailsWidget.fxml");
            FXMLLoader loader = new FXMLLoader(rawDataDetailsUrl);
            Parent rawDataDetailsWidget = loader.load();
            this.dataInspectionController = (RawDataDetailsWidgetController) loader.getController();
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
    protected List<FieldDescriptor> doGetAdditionalFieldDescriptors() throws ReatmetricException {
        return ReatmetricUI.selectedSystem().getSystem().getRawDataMonitorService().getAdditionalFieldDescriptors();
    }

    @Override
    protected URL doGetFilterWidget() {
        return getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/RawDataFilterWidget.fxml");
    }

    @Override
    protected String doGetComponentId() {
        return "RawDataView";
    }

    @Override
    protected ServiceType doGetSupportedService() {
        return ServiceType.RAW_DATA;
    }
}
