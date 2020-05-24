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
import java.util.stream.Collectors;

import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
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
public class EventDataFilterWidgetController implements Initializable, IFilterController<EventDataFilter> {

    @FXML
    private CheckBox severityCheckbox;
    @FXML
    private ListView<Severity> severityList;
    @FXML
    private CheckBox sourceCheckbox;
    @FXML
    private TextField sourceText;
    @FXML
    private CheckBox eventPathCheckbox;
    @FXML
    private TextField eventPathText;
    @FXML
    private CheckBox routeCheckbox;
    @FXML
    private TextField routeText;
    @FXML
    private CheckBox pathCheckbox;
    @FXML
    private TextField pathText;
    @FXML
    private CheckBox typeCheckbox;
    @FXML
    private TextField typeText;
    
    @FXML
    private Button selectBtn;
    
    private Runnable actionAfterSelection;
    
    // The result of the selection
    private EventDataFilter selectedFilter = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.severityList.disableProperty().bind(this.severityCheckbox.selectedProperty().not());
        this.sourceText.disableProperty().bind(this.sourceCheckbox.selectedProperty().not());
        this.eventPathText.disableProperty().bind(this.eventPathCheckbox.selectedProperty().not());
        this.pathText.disableProperty().bind(this.pathCheckbox.selectedProperty().not());
        this.typeText.disableProperty().bind(this.typeCheckbox.selectedProperty().not());
        this.routeText.disableProperty().bind(this.routeCheckbox.selectedProperty().not());
        
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
    public void setSelectedFilter(EventDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public EventDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }
    
    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.severityCheckbox.setSelected(false);
            this.sourceCheckbox.setSelected(false);
            this.eventPathCheckbox.setSelected(false);
            this.pathCheckbox.setSelected(false);
            this.routeCheckbox.setSelected(false);
            this.typeCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.pathText.setText("");
            this.routeText.setText("");
            this.typeText.setText("");
            this.eventPathText.setText("");
            this.severityList.getSelectionModel().clearSelection();
        } else {
            this.severityCheckbox.setSelected(this.selectedFilter.getSeverityList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.eventPathCheckbox.setSelected(this.selectedFilter.getEventPathList() != null);
            this.pathCheckbox.setSelected(this.selectedFilter.getParentPath() != null);
            this.routeCheckbox.setSelected(this.selectedFilter.getRouteList() != null);
            this.typeCheckbox.setSelected(this.selectedFilter.getTypeList() != null);
             
            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
            this.eventPathText.setText(IFilterController.toStringList(this.selectedFilter.getEventPathList()));
            this.routeText.setText(IFilterController.toStringList(this.selectedFilter.getRouteList()));
            this.typeText.setText(IFilterController.toStringList(this.selectedFilter.getTypeList()));
            this.pathText.setText(this.selectedFilter.getParentPath() != null ? this.selectedFilter.getParentPath().asString() : "");
            this.severityList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getSeverityList() != null) {
            	this.selectedFilter.getSeverityList().stream().forEach((s) -> this.severityList.getSelectionModel().select(s));
            }
        }
    }

    private EventDataFilter deriveFromWidgets() {
        List<Severity> qList = deriveSelectedSeverity();
        List<String> sourceList = deriveSelectedSource();
        List<SystemEntityPath> eventPathList = deriveSelectedEventPath();
        List<String> typeList = deriveSelectedType();
        List<String> routeList = deriveSelectedRoute();
        SystemEntityPath parentPath = deriveParentPath();
        return new EventDataFilter(parentPath, eventPathList, routeList, typeList, sourceList, qList, null);
    }

    private List<String> deriveSelectedSource() {
        if(this.sourceCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.sourceText.getText().split(",")));
        } else {
            return null;
        }
    }

    private List<SystemEntityPath> deriveSelectedEventPath() {
        if(this.eventPathCheckbox.isSelected()) {
            return Arrays.stream(this.eventPathText.getText().split(",")).map(SystemEntityPath::fromString).collect(Collectors.toCollection(LinkedList::new));
        } else {
            return null;
        }
    }
    
    private List<String> deriveSelectedType() {
        if(this.typeCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.typeText.getText().split(",")));
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
    
    private List<Severity> deriveSelectedSeverity() {
        if(this.severityCheckbox.isSelected()) {
            return new LinkedList<>(this.severityList.getSelectionModel().getSelectedItems());
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
