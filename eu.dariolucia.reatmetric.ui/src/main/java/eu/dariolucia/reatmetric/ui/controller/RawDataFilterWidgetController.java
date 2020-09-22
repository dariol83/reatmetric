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

import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class RawDataFilterWidgetController implements Initializable, IFilterController<RawDataFilter> {

    @FXML
    private CheckBox qualityCheckbox;
    @FXML
    private ListView<Quality> qualityList;
    @FXML
    private CheckBox sourceCheckbox;
    @FXML
    private TextField sourceText;
    @FXML
    private CheckBox routeCheckbox;
    @FXML
    private TextField routeText;
    @FXML
    private CheckBox nameCheckbox;
    @FXML
    private TextField nameText;
    @FXML
    private CheckBox typeCheckbox;
    @FXML
    private TextField typeText;

    @FXML
    private Button selectBtn;

    private Runnable actionAfterSelection;

    // The result of the selection
    private RawDataFilter selectedFilter = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.qualityList.disableProperty().bind(this.qualityCheckbox.selectedProperty().not());
        this.sourceText.disableProperty().bind(this.sourceCheckbox.selectedProperty().not());
        this.nameText.disableProperty().bind(this.nameCheckbox.selectedProperty().not());
        this.typeText.disableProperty().bind(this.typeCheckbox.selectedProperty().not());
        this.routeText.disableProperty().bind(this.routeCheckbox.selectedProperty().not());

        this.qualityList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.qualityList.getItems().addAll(Arrays.asList(Quality.values()));
    }

    @FXML
    private void selectButtonPressed(ActionEvent e) {
        this.selectedFilter = deriveFromWidgets();
        if (this.actionAfterSelection != null) {
            this.actionAfterSelection.run();
        }
    }

    @Override
    public void setActionAfterSelection(Runnable r) {
        this.actionAfterSelection = r;
    }

    @Override
    public void setSelectedFilter(RawDataFilter t) {
        this.selectedFilter = t;
        updateWidgets();
    }

    @Override
    public RawDataFilter getSelectedFilter() {
        return this.selectedFilter;
    }

    private void updateWidgets() {
        if (this.selectedFilter == null) {
            this.qualityCheckbox.setSelected(false);
            this.sourceCheckbox.setSelected(false);
            this.nameCheckbox.setSelected(false);
            this.routeCheckbox.setSelected(false);
            this.typeCheckbox.setSelected(false);
            this.sourceText.setText("");
            this.nameText.setText("");
            this.routeText.setText("");
            this.typeText.setText("");
            this.qualityList.getSelectionModel().clearSelection();
        } else {
            this.qualityCheckbox.setSelected(this.selectedFilter.getQualityList() != null);
            this.sourceCheckbox.setSelected(this.selectedFilter.getSourceList() != null);
            this.nameCheckbox.setSelected(this.selectedFilter.getNameContains() != null);
            this.routeCheckbox.setSelected(this.selectedFilter.getRouteList() != null);
            this.typeCheckbox.setSelected(this.selectedFilter.getTypeList() != null);

            this.sourceText.setText(IFilterController.toStringList(this.selectedFilter.getSourceList()));
            this.routeText.setText(IFilterController.toStringList(this.selectedFilter.getRouteList()));
            this.typeText.setText(IFilterController.toStringList(this.selectedFilter.getTypeList()));
            this.nameText.setText(this.selectedFilter.getNameContains() != null ? this.selectedFilter.getNameContains() : "");
            this.qualityList.getSelectionModel().clearSelection();
            if (this.selectedFilter.getQualityList() != null) {
                this.selectedFilter.getQualityList().stream().forEach((s) -> this.qualityList.getSelectionModel().select(s));
            }
        }
    }

    private RawDataFilter deriveFromWidgets() {
        List<Quality> qList = deriveSelectedQuality();
        List<String> sourceList = deriveSelectedSource();
        List<String> typeList = deriveSelectedType();
        List<String> routeList = deriveSelectedRoute();
        String nameRegExp = deriveRegExpName();
        return new RawDataFilter(false, nameRegExp, routeList, typeList, sourceList, qList);
    }

    private List<String> deriveSelectedSource() {
        if (this.sourceCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.sourceText.getText().split(",")));
            return toReturn;
        } else {
            return null;
        }
    }

    private List<String> deriveSelectedType() {
        if (this.typeCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.typeText.getText().split(",")));
            return toReturn;
        } else {
            return null;
        }
    }

    private List<String> deriveSelectedRoute() {
        if (this.routeCheckbox.isSelected()) {
            List<String> toReturn = new LinkedList<>();
            toReturn.addAll(Arrays.asList(this.routeText.getText().split(",")));
            return toReturn;
        } else {
            return null;
        }
    }

    private List<Quality> deriveSelectedQuality() {
        if (this.qualityCheckbox.isSelected()) {
            List<Quality> toReturn = new LinkedList<>();
            toReturn.addAll(this.qualityList.getSelectionModel().getSelectedItems());
            return toReturn;
        } else {
            return null;
        }
    }

    private String deriveRegExpName() {
        if (this.nameCheckbox.isSelected()) {
            return this.nameText.getText();
        } else {
            return null;
        }
    }
}
