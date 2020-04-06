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

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
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
public class AlarmParameterDataFilterWidgetController implements Initializable, IFilterController<AlarmParameterDataFilter> {

    @FXML
    private CheckBox alarmStateCheckbox;
    @FXML
    private ListView<AlarmState> alarmStateList;
    
    @FXML
    private CheckBox pathCheckbox;
    @FXML
    private TextField pathText;

    @FXML
    private Button selectBtn;
    
    private Runnable actionAfterSelection;
    
    // The result of the selection
    private AlarmParameterDataFilter selectedFilter = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.alarmStateList.disableProperty().bind(this.alarmStateCheckbox.selectedProperty().not());
        this.pathText.disableProperty().bind(this.pathCheckbox.selectedProperty().not());
        
        this.alarmStateList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.alarmStateList.getItems().addAll(Arrays.asList(AlarmState.values()));
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
    public void setSelectedFilter(AlarmParameterDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public AlarmParameterDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }
    
    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.alarmStateCheckbox.setSelected(false);
            this.pathCheckbox.setSelected(false);
            this.pathText.setText("");
            this.alarmStateList.getSelectionModel().clearSelection();
        } else {
            this.alarmStateCheckbox.setSelected(this.selectedFilter.getAlarmStateList() != null);
            this.pathCheckbox.setSelected(this.selectedFilter.getParentPath() != null);
            this.pathText.setText(this.selectedFilter.getParentPath() != null ? this.selectedFilter.getParentPath().asString() : "");
            this.alarmStateList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getAlarmStateList() != null) {
            	this.selectedFilter.getAlarmStateList().stream().forEach((s) -> this.alarmStateList.getSelectionModel().select(s));
            }
        }
    }

    private AlarmParameterDataFilter deriveFromWidgets() {
        List<AlarmState> qList = deriveSelectedAlarmState();
        SystemEntityPath parentPath = deriveParentPath();
        return new AlarmParameterDataFilter(parentPath, null, qList, null);
    }

    private List<AlarmState> deriveSelectedAlarmState() {
        if(this.alarmStateCheckbox.isSelected()) {
            List<AlarmState> toReturn = new LinkedList<>();
            toReturn.addAll(this.alarmStateList.getSelectionModel().getSelectedItems());
            return toReturn;
        } else {
            return null;
        }
    }

    private SystemEntityPath deriveParentPath() {
        if(this.pathCheckbox.isSelected()) {
            return SystemEntityPath.fromString(this.pathText.getText());
        } else {
            return null;
        }
    }
}
