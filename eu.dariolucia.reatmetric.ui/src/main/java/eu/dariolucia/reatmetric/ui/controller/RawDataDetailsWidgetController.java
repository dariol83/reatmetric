/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class RawDataDetailsWidgetController implements Initializable {

    @FXML
    private TableView<RawDataEntry> rawDataDetailsTableView;
    
    @FXML
    private Label descriptionText;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(0)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getOffset()));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(1)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(0)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(2)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(1)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(3)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(2)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(4)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(3)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(5)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(4)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(6)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(5)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(7)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(6)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(8)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(7)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(9)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(8)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(10)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(9)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(11)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(10)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(12)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(11)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(13)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(12)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(14)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(13)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(15)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(14)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(16)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getDataAt(15)));
        ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(17)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getAsciiRepresentation()));
        
        for(int i = 0; i < rawDataDetailsTableView.getColumns().size(); ++i) {
            ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(i)).setCellFactory(column -> {
                return new TableCell<RawDataEntry, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setFont(Font.font("Monospaced", 11));
                        setText(item);
                    }
                };
            });
        }
    }  
    
    public void setData(String description, byte[] data) {
        if(data == null) {
            descriptionText.setText(" ");
            rawDataDetailsTableView.getItems().clear();
            rawDataDetailsTableView.getParent().layout();
            rawDataDetailsTableView.refresh();
            return;
        }
        // Determine how many RawDataEntry entries to create
        int entries = data.length / 16;
        if(data.length % 16 > 0) {
            entries++;
        }
        List<RawDataEntry> dataList = new LinkedList<>();
        for(int i = 0; i < entries; ++i) {
            dataList.add(new RawDataEntry(data, i));
        }
        descriptionText.setText(description);
        rawDataDetailsTableView.setItems(FXCollections.observableList(dataList));
        rawDataDetailsTableView.getParent().layout();
        rawDataDetailsTableView.refresh();
    }
    
    private class RawDataEntry {
        
        private final byte[] data;
        
        private final int offset;

        private String asciiRepresentation = null;
        
        public RawDataEntry(byte[] data, int offset) {
            this.data = data;
            this.offset = offset;
        }
        
        public String getOffset() {
            return String.format("%04X" , offset * 16);
        }
        
        public String getDataAt(int i) {
            int absIndex = offset * 16 + i;
            if(absIndex < data.length) {
                return String.format("%02X", (data[absIndex]));
            } else {
                return " ";
            }
        }
        
        public String getAsciiRepresentation() {
            if(asciiRepresentation == null) {
                StringBuilder sb = new StringBuilder();
                for(int i = offset * 16; i < Math.min(offset * 16 + 16, data.length); ++i) {
                    if(data[i] < 32) {
                        sb.append(".");
                    } else {
                        sb.append((char) data[i]);
                    }
                }
                asciiRepresentation = sb.toString();
            }
            return asciiRepresentation;
        }
    }
}
