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

import eu.dariolucia.reatmetric.api.common.Pair;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class RawDataDetailsWidgetController implements Initializable {

    @FXML
    public TitledPane reportingTitledPane;
    @FXML
    public TitledPane hexTitledPane;
    @FXML
    public Accordion accordion;

    @FXML
    public TableView<Pair<String, String>> propertyDetailsTableView;


    @FXML
    private TableView<RawDataEntry> rawDataDetailsTableView;
    
    @FXML
    private Label descriptionText;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ((TableColumn<Pair<String, String>,String>) propertyDetailsTableView.getColumns().get(0)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getFirst()));
        ((TableColumn<Pair<String, String>,String>) propertyDetailsTableView.getColumns().get(1)).setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSecond()));

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
            ((TableColumn<RawDataEntry,String>) rawDataDetailsTableView.getColumns().get(i)).setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setFont(Font.font("Monospaced", 11));
                    setText(item);
                }
            });
        }

        ((TableColumn<Pair<String, String>,String>) propertyDetailsTableView.getColumns().get(1)).setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                TableRow<Pair<String, String>> currentRow = getTableRow();
                if (item != null && !empty && !isEmpty()) {
                    setText(item);
                    currentRow.setStyle("");
                } else if(item == null) {
                    setText("");
                    currentRow.setStyle("-fx-background-color: lightgray; -fx-font-weight: bold;");
                } else {
                    setText("");
                    currentRow.setStyle("");
                }
            }
        });
    }  

    public void setData(String description, byte[] data, LinkedHashMap<String, String> decodedInfo) {
        if(data == null) {
            rawDataDetailsTableView.getItems().clear();
            rawDataDetailsTableView.refresh();
        }
        if(decodedInfo == null || decodedInfo.isEmpty()) {
            propertyDetailsTableView.getItems().clear();
            propertyDetailsTableView.refresh();
        }
        if(description == null) {
            description = "N/A";
        }
        descriptionText.setText(description);
        if(data != null) {
            // Determine how many RawDataEntry entries to create
            int entries = data.length / 16;
            if (data.length % 16 > 0) {
                entries++;
            }
            List<RawDataEntry> dataList = new LinkedList<>();
            for (int i = 0; i < entries; ++i) {
                dataList.add(new RawDataEntry(data, i));
            }
            rawDataDetailsTableView.setItems(FXCollections.observableList(dataList));
            rawDataDetailsTableView.refresh();
        }
        if(decodedInfo != null) {
            List<Pair<String, String>> toAdd = decodedInfo.entrySet().stream().map(o -> Pair.of(o.getKey(), o.getValue())).collect(Collectors.toList());
            propertyDetailsTableView.setItems(FXCollections.observableList(toAdd));
            propertyDetailsTableView.refresh();
        }
        // Expand the property details
        accordion.setExpandedPane(reportingTitledPane);
    }
    
    private static class RawDataEntry {
        
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
