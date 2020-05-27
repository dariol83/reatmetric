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

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceDataFilter;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
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
public class ActivityDataFilterWidgetController implements Initializable, IFilterController<ActivityOccurrenceDataFilter> {

    @FXML
    private CheckBox stateCheckbox;
    @FXML
    private ListView<ActivityOccurrenceState> stateList;
    @FXML
    private CheckBox sourceCheckbox;
    @FXML
    private TextField sourceText;
    @FXML
    private CheckBox activityPathCheckbox;
    @FXML
    private TextField activityPathText;
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
    private ActivityOccurrenceDataFilter selectedFilter = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.stateList.disableProperty().bind(this.stateCheckbox.selectedProperty().not());
        this.sourceText.disableProperty().bind(this.sourceCheckbox.selectedProperty().not());
        this.activityPathText.disableProperty().bind(this.activityPathCheckbox.selectedProperty().not());
        this.pathText.disableProperty().bind(this.pathCheckbox.selectedProperty().not());
        this.typeText.disableProperty().bind(this.typeCheckbox.selectedProperty().not());
        this.routeText.disableProperty().bind(this.routeCheckbox.selectedProperty().not());
        
        this.stateList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.stateList.getItems().addAll(Arrays.asList(ActivityOccurrenceState.values()));
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
    public void setSelectedFilter(ActivityOccurrenceDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public ActivityOccurrenceDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }
    
    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.stateCheckbox.setSelected(false);
            this.sourceCheckbox.setSelected(false);
            this.activityPathCheckbox.setSelected(false);
            this.pathCheckbox.setSelected(false);
            this.routeCheckbox.setSelected(false);
            this.typeCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.pathText.setText("");
            this.routeText.setText("");
            this.typeText.setText("");
            this.activityPathText.setText("");
            this.stateList.getSelectionModel().clearSelection();
        } else {
            this.stateCheckbox.setSelected(this.selectedFilter.getStateList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.activityPathCheckbox.setSelected(false); // TODO: add to filter
            this.pathCheckbox.setSelected(this.selectedFilter.getParentPath() != null);
            this.routeCheckbox.setSelected(this.selectedFilter.getRouteList() != null);
            this.typeCheckbox.setSelected(this.selectedFilter.getTypeList() != null);
             
            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
            this.activityPathText.setText(IFilterController.toStringList(null)); // TODO: add to filter
            this.routeText.setText(IFilterController.toStringList(this.selectedFilter.getRouteList()));
            this.typeText.setText(IFilterController.toStringList(this.selectedFilter.getTypeList()));
            this.pathText.setText(this.selectedFilter.getParentPath() != null ? this.selectedFilter.getParentPath().asString() : "");
            this.stateList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getStateList() != null) {
            	this.selectedFilter.getStateList().forEach((s) -> this.stateList.getSelectionModel().select(s));
            }
        }
    }

    private ActivityOccurrenceDataFilter deriveFromWidgets() {
        List<ActivityOccurrenceState> stateList = deriveSelectedState();
        List<String> sourceList = deriveSelectedSource();
        // List<SystemEntityPath> eventPathList = deriveSelectedEventPath();
        List<String> typeList = deriveSelectedType();
        List<String> routeList = deriveSelectedRoute();
        SystemEntityPath parentPath = deriveParentPath();
        return new ActivityOccurrenceDataFilter(parentPath, routeList, typeList, stateList, sourceList, null); // TODO: add path list
    }

    private List<String> deriveSelectedSource() {
        if(this.sourceCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.sourceText.getText().split(",")));
        } else {
            return null;
        }
    }

    private List<SystemEntityPath> deriveSelectedEventPath() {
        if(this.activityPathCheckbox.isSelected()) {
            return Arrays.stream(this.activityPathText.getText().split(",")).map(SystemEntityPath::fromString).collect(Collectors.toCollection(LinkedList::new));
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
    
    private List<ActivityOccurrenceState> deriveSelectedState() {
        if(this.stateCheckbox.isSelected()) {
            return new LinkedList<>(this.stateList.getSelectionModel().getSelectedItems());
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
