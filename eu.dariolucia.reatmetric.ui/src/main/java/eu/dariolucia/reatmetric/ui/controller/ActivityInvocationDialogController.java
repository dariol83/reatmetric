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

import eu.dariolucia.reatmetric.api.activity.ActivityArgumentDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ActivityInvocationDialogController implements Initializable {

    @FXML
    protected Label activityLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ChoiceBox<String> routeChoiceBox;
    @FXML
    protected TableView<ArgumentBean> argumentsTableView;
    @FXML
    protected TableColumn nameColumn;
    @FXML
    protected TableColumn rawValueColumn;
    @FXML
    protected TableColumn engValueColumn;
    @FXML
    protected TableColumn unitColumn;
    @FXML
    protected TableView<PropertyBean> propertiesTableView;
    @FXML
    protected TableColumn keyColumn;
    @FXML
    protected TableColumn valueColumn;
    @FXML
    protected Button okButton;
    @FXML
    protected Button cancelButton;

    private ActivityDescriptor descriptor;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        argumentsTableView.setEditable(true);
        nameColumn.setEditable(false);
    }

    public void initialiseActivityDialog(ActivityDescriptor descriptor, ActivityRequest currentRequest, List<Pair<String, Boolean>> routesWithAvailability) {
        this.descriptor = descriptor;
        activityLabel.setText(descriptor.getPath().asString());
        descriptionLabel.setText(descriptor.getDescription());
        // Set the routes
        for(Pair<String,Boolean> route : routesWithAvailability) {
            routeChoiceBox.getItems().add(route.getFirst());
        }
        // Set the selected route or default
        if(currentRequest != null) {
            routeChoiceBox.getSelectionModel().select(currentRequest.getRoute());
        } else if(descriptor.getDefaultRoute() != null) {
            routeChoiceBox.getSelectionModel().select(descriptor.getDefaultRoute());
        }
        // Set the arguments
        for(ActivityArgumentDescriptor aad : descriptor.getArgumentDescriptors()) {
            ArgumentBean ab = new ArgumentBean(aad);
            argumentsTableView.getItems().add(ab);
            if(currentRequest != null) {
                updateArgument(ab, aad, currentRequest);
            }
        }
        // Set the properties
        for(Pair<String, String> property : descriptor.getProperties()) {
            PropertyBean pb = new PropertyBean(property);
            propertiesTableView.getItems().add(pb);
            if(currentRequest != null) {
                updateProperty(pb, property, currentRequest);
            }
        }
    }

    private void updateProperty(PropertyBean pb, Pair<String, String> property, ActivityRequest currentRequest) {
        // TODO
    }

    private void updateArgument(ArgumentBean ab, ActivityArgumentDescriptor aad, ActivityRequest currentRequest) {
        // TODO
    }

    private static class ArgumentBean {

        private final ActivityArgumentDescriptor descriptor;
        private final ReadOnlyStringProperty name;
        private final ReadOnlyStringProperty unit;
        private final SimpleObjectProperty<Object> rawValue;
        private final SimpleObjectProperty<Object> engValue;

        public ArgumentBean(ActivityArgumentDescriptor aad) {
            descriptor = aad;
            name = new SimpleStringProperty(aad.getName());
            unit = new SimpleStringProperty(aad.getUnit());
            rawValue = new SimpleObjectProperty<>();
            engValue = new SimpleObjectProperty<>();
            if(aad.isDefaultValuePresent()) {
                if(aad.getRawDefaultValue() != null) {
                    rawValue.set(aad.getRawDefaultValue());
                } else if(aad.getEngineeringDefaultValue() != null) {
                    engValue.set(aad.getEngineeringDefaultValue());
                }
            }
        }

        public ActivityArgumentDescriptor getDescriptor() {
            return descriptor;
        }

        public ReadOnlyStringProperty nameProperty() {
            return name;
        }

        public ReadOnlyStringProperty unitProperty() {
            return unit;
        }

        public SimpleObjectProperty<Object> rawValueProperty() {
            return rawValue;
        }

        public SimpleObjectProperty<Object> engValueProperty() {
            return engValue;
        }
    }

    private static class PropertyBean {

        public PropertyBean(Pair<String, String> property) {

        }
    }
}
