/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

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
            this.pathCheckbox.setSelected(false);
            this.routeCheckbox.setSelected(false);
            this.typeCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.pathText.setText("");
            this.routeText.setText("");
            this.typeText.setText("");
            this.severityList.getSelectionModel().clearSelection();
        } else {
            this.severityCheckbox.setSelected(this.selectedFilter.getSeverityList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.pathCheckbox.setSelected(this.selectedFilter.getParentPath() != null);
            this.routeCheckbox.setSelected(this.selectedFilter.getTypeList() != null);
            this.typeCheckbox.setSelected(this.selectedFilter.getRouteList() != null);
             
            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
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
        List<String> typeList = deriveSelectedType();
        List<String> routeList = deriveSelectedRoute();
        SystemEntityPath parentPath = deriveParentPath();
        // TODO: add capability to filter on event path
        return new EventDataFilter(parentPath, null, routeList, typeList, sourceList, qList);
    }

    private List<String> deriveSelectedSource() {
        if(this.sourceCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.sourceText.getText().split(",")));
            return toReturn;
        } else {
            return null;
        }
    }
    
    private List<String> deriveSelectedType() {
        if(this.typeCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.typeText.getText().split(",")));
            return toReturn;
        } else {
            return null;
        }
    }
        
    private List<String> deriveSelectedRoute() {
        if(this.routeCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.routeText.getText().split(",")));
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

    private SystemEntityPath deriveParentPath() {
        if(this.pathCheckbox.isSelected()) {
            return SystemEntityPath.fromString(this.pathText.getText());
        } else {
            return null;
        }
    }
}
