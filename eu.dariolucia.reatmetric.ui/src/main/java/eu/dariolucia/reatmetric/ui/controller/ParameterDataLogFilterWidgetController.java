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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDataLogFilterWidgetController implements Initializable, IFilterController<ParameterDataFilter> {

    @FXML
    private CheckBox validityCheckbox;
    @FXML
    private ListView<Validity> validityList;
    @FXML
    private CheckBox alarmCheckbox;
    @FXML
    private ListView<AlarmState> alarmList;
    @FXML
    private CheckBox routeCheckbox;
    @FXML
    private TextField routeText;
    @FXML
    private CheckBox pathCheckbox;
    @FXML
    private TextField pathText;
    @FXML
    private CheckBox parameterPathCheckbox;
    @FXML
    private TextField parameterPathText;
    @FXML
    private Button selectBtn;
    
    private Runnable actionAfterSelection;
    
    // The result of the selection
    private ParameterDataFilter selectedFilter = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.validityList.disableProperty().bind(this.validityCheckbox.selectedProperty().not());
        this.alarmList.disableProperty().bind(this.alarmCheckbox.selectedProperty().not());
        this.pathText.disableProperty().bind(this.pathCheckbox.selectedProperty().not());
        this.routeText.disableProperty().bind(this.routeCheckbox.selectedProperty().not());
        this.parameterPathText.disableProperty().bind(this.parameterPathCheckbox.selectedProperty().not());
        
        this.validityList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.validityList.getItems().addAll(Arrays.asList(Validity.values()));

        this.alarmList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.alarmList.getItems().addAll(Arrays.asList(AlarmState.values()));
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
    public void setSelectedFilter(ParameterDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public ParameterDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }
    
    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.validityCheckbox.setSelected(false);
            this.alarmCheckbox.setSelected(false);
            this.pathCheckbox.setSelected(false);
            this.routeCheckbox.setSelected(false);
            this.pathText.setText("");
            this.routeText.setText("");
            this.parameterPathText.setText("");
            this.validityList.getSelectionModel().clearSelection();
            this.alarmList.getSelectionModel().clearSelection();
        } else {
            this.validityCheckbox.setSelected(this.selectedFilter.getValidityList() != null);
            this.alarmCheckbox.setSelected(this.selectedFilter.getAlarmStateList() != null);
            this.pathCheckbox.setSelected(this.selectedFilter.getParentPath() != null);
            this.routeCheckbox.setSelected(this.selectedFilter.getRouteList() != null);
            this.parameterPathCheckbox.setSelected(this.selectedFilter.getParameterPathList() != null);

            this.routeText.setText(IFilterController.toStringList(this.selectedFilter.getRouteList()));
            this.parameterPathText.setText(IFilterController.toStringList(this.selectedFilter.getParameterPathList()));
            this.pathText.setText(this.selectedFilter.getParentPath() != null ? this.selectedFilter.getParentPath().asString() : "");
            this.validityList.getSelectionModel().clearSelection();
            this.alarmList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getValidityList() != null) {
            	this.selectedFilter.getValidityList().stream().forEach((s) -> this.validityList.getSelectionModel().select(s));
            }
            if(this.selectedFilter.getAlarmStateList() != null) {
                this.selectedFilter.getAlarmStateList().stream().forEach((s) -> this.alarmList.getSelectionModel().select(s));
            }
        }
    }

    private ParameterDataFilter deriveFromWidgets() {
        List<Validity> qList = deriveSelectedValidity();
        List<AlarmState> aList = deriveSelectedAlarm();
        List<SystemEntityPath> parameterPathList = deriveSelectedParameterPath();
        List<String> routeList = deriveSelectedRoute();
        SystemEntityPath parentPath = deriveParentPath();
        return new ParameterDataFilter(parentPath, parameterPathList, routeList, qList, aList, null);
    }


    private List<SystemEntityPath> deriveSelectedParameterPath() {
        if(this.parameterPathCheckbox.isSelected()) {
            return Arrays.stream(this.parameterPathText.getText().split(",")).map(SystemEntityPath::fromString).collect(Collectors.toCollection(LinkedList::new));
        } else {
            return null;
        }
    }

    private List<String> deriveSelectedRoute() {
        if(this.routeCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.routeText.getText().split(",")));
        } else {
            return null;
        }
    }
    
    private List<Validity> deriveSelectedValidity() {
        if(this.validityCheckbox.isSelected()) {
            return new LinkedList<>(this.validityList.getSelectionModel().getSelectedItems());
        } else {
            return null;
        }
    }

    private List<AlarmState> deriveSelectedAlarm() {
        if(this.alarmCheckbox.isSelected()) {
            return new LinkedList<>(this.alarmList.getSelectionModel().getSelectedItems());
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
