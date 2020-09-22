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

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.scheduler.ScheduledActivityDataFilter;
import eu.dariolucia.reatmetric.api.scheduler.SchedulingState;
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
public class SchedulerFilterWidgetController implements Initializable, IFilterController<ScheduledActivityDataFilter> {

    @FXML
    private CheckBox stateCheckbox;
    @FXML
    private ListView<SchedulingState> stateList;
    @FXML
    private CheckBox sourceCheckbox;
    @FXML
    private TextField sourceText;
    @FXML
    private CheckBox activityPathCheckbox;
    @FXML
    private TextField activityPathText;
    @FXML
    private CheckBox resourcesCheckbox;
    @FXML
    private TextField resourcesText;

    @FXML
    private Button selectBtn;

    private Runnable actionAfterSelection;

    // The result of the selection
    private ScheduledActivityDataFilter selectedFilter = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.stateList.disableProperty().bind(this.stateCheckbox.selectedProperty().not());
        this.sourceText.disableProperty().bind(this.sourceCheckbox.selectedProperty().not());
        this.activityPathText.disableProperty().bind(this.activityPathCheckbox.selectedProperty().not());
        this.resourcesText.disableProperty().bind(this.resourcesCheckbox.selectedProperty().not());

        this.stateList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.stateList.getItems().addAll(Arrays.asList(SchedulingState.values()));
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
    public void setSelectedFilter(ScheduledActivityDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public ScheduledActivityDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }

    private void updateWidgets() {
        if(this.selectedFilter == null) {
            this.stateCheckbox.setSelected(false);
            this.sourceCheckbox.setSelected(false);
            this.activityPathCheckbox.setSelected(false);
            this.resourcesCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.resourcesText.setText("");
            this.activityPathText.setText("");
            this.stateList.getSelectionModel().clearSelection();
        } else {
            this.stateCheckbox.setSelected(this.selectedFilter.getSchedulingStateList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.activityPathCheckbox.setSelected(this.selectedFilter.getActivityPathList() != null);
            this.resourcesCheckbox.setSelected(this.selectedFilter.getResourceList() != null);

            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
            this.activityPathText.setText(IFilterController.toStringList(this.selectedFilter.getActivityPathList()));
            this.resourcesText.setText(IFilterController.toStringList(this.selectedFilter.getResourceList()));
            this.stateList.getSelectionModel().clearSelection();
            if(this.selectedFilter.getSchedulingStateList() != null) {
            	this.selectedFilter.getSchedulingStateList().forEach((s) -> this.stateList.getSelectionModel().select(s));
            }
        }
    }

    private ScheduledActivityDataFilter deriveFromWidgets() {
        List<SchedulingState> stateList = deriveSelectedState();
        List<String> sourceList = deriveSelectedSource();
        List<SystemEntityPath> activityPathList = deriveSelectedActivityPath();
        List<String> resourcesList = deriveSelectedResources();
        return new ScheduledActivityDataFilter(null, activityPathList, resourcesList, sourceList, stateList, null);
    }

    private List<String> deriveSelectedSource() {
        if(this.sourceCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.sourceText.getText().split(",")));
        } else {
            return null;
        }
    }

    private List<SystemEntityPath> deriveSelectedActivityPath() {
        if(this.activityPathCheckbox.isSelected()) {
            return Arrays.stream(this.activityPathText.getText().split(",")).map(SystemEntityPath::fromString).collect(Collectors.toCollection(LinkedList::new));
        } else {
            return null;
        }
    }

    private List<String> deriveSelectedResources() {
        if(this.resourcesCheckbox.isSelected()) {
            return new LinkedList<>(Arrays.asList(this.resourcesText.getText().split(",")));
        } else {
            return null;
        }
    }

    private List<SchedulingState> deriveSelectedState() {
        if(this.stateCheckbox.isSelected()) {
            return new LinkedList<>(this.stateList.getSelectionModel().getSelectedItems());
        } else {
            return null;
        }
    }
}
