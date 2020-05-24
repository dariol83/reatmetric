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

import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class OperationalMessageFilterWidgetController implements Initializable, IFilterController<OperationalMessageFilter> {

    @FXML
    private CheckBox severityCheckbox;
    @FXML
    private ListView<Severity> severityList;
    @FXML
    private CheckBox sourceCheckbox;
    @FXML
    private TextField sourceText;
    @FXML
    private CheckBox idCheckbox;
    @FXML
    private TextField idText;
    @FXML
    private CheckBox messageCheckbox;
    @FXML
    private TextField messageText;
    
    @FXML
    private Button selectBtn;
    
    private Runnable actionAfterSelection;
    
    // The result of the selection
    private OperationalMessageFilter selectedFilter = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.severityList.disableProperty().bind(this.severityCheckbox.selectedProperty().not());
        this.sourceText.disableProperty().bind(this.sourceCheckbox.selectedProperty().not());
        this.idText.disableProperty().bind(this.idCheckbox.selectedProperty().not());
        this.messageText.disableProperty().bind(this.messageCheckbox.selectedProperty().not());
        
        this.severityList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.severityList.getItems().addAll(Arrays.asList(Severity.values()));
    }  
    
    @FXML
    private void selectButtonPressed(ActionEvent e) {
        this.selectedFilter = deriveFromWidgets();
        if(this.actionAfterSelection != null) {
            this.actionAfterSelection.run();
        }
    }
  
    @Override
    public void setActionAfterSelection(Runnable r) {
        this.actionAfterSelection = r;
    }
    
    @Override
    public void setSelectedFilter(OperationalMessageFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public OperationalMessageFilter getSelectedFilter() {
        return this.selectedFilter;
    }
    
    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.severityCheckbox.setSelected(false);
            this.sourceCheckbox.setSelected(false);
            this.idCheckbox.setSelected(false);
            this.messageCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.messageText.setText("");
            this.severityList.getSelectionModel().clearSelection();
        } else {
            this.severityCheckbox.setSelected(this.selectedFilter.getSeverityList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.idCheckbox.setSelected(this.selectedFilter.getIdList() != null);
            this.messageCheckbox.setSelected(this.selectedFilter.getMessageTextContains() != null);

            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
            this.idText.setText(IFilterController.toStringList(this.selectedFilter.getIdList()));
            this.messageText.setText(this.selectedFilter.getMessageTextContains() != null ? this.selectedFilter.getMessageTextContains() : "");
            this.severityList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getSeverityList() != null) {    
            	this.selectedFilter.getSeverityList().forEach((s) -> this.severityList.getSelectionModel().select(s));
            }
        }
    }

    private OperationalMessageFilter deriveFromWidgets() {
        List<Severity> sevList = deriveSelectedSeverity();
        List<String> sourceList = deriveSelectedSource();
        List<String> idList = deriveSelectedId();
        String messageContents = deriveMessageContents();
        return new OperationalMessageFilter(messageContents, idList, sourceList, sevList);
    }

    private List<String> deriveSelectedSource() {
        if(this.sourceCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.sourceText.getText().split(",", -1)));
            return toReturn;
        } else {
            return null;
        }
    }

    private List<String> deriveSelectedId() {
        if(this.idCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.idText.getText().split(",", -1)));
            return toReturn;
        } else {
            return null;
        }
    }
    
    private List<Severity> deriveSelectedSeverity() {
        if(this.severityCheckbox.isSelected()) {
            List<Severity> toReturn = new LinkedList<>();
            toReturn.addAll(this.severityList.getSelectionModel().getSelectedItems());
            return toReturn;
        } else {
            return null;
        }
    }

    private String deriveMessageContents() {
        if(this.messageCheckbox.isSelected()) {
            return this.messageText.getText();
        } else {
            return null;
        }
    }
}
