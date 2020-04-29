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
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ActivityInvocationDialogController implements Initializable {

    @FXML
    protected Accordion accordion;
    @FXML
    protected Label activityLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ChoiceBox<ActivityRouteState> routeChoiceBox;
    @FXML
    protected TableView<ArgumentBean> argumentsTableView;
    @FXML
    protected TableColumn<ArgumentBean, String> nameColumn;
    @FXML
    protected TableColumn<ArgumentBean, String> rawValueColumn;
    @FXML
    protected TableColumn<ArgumentBean, String> engValueColumn;
    @FXML
    protected TableColumn<ArgumentBean, String> unitColumn;
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
    private Consumer<ActivityInvocationDialogController> okHandler;
    private Consumer<ActivityInvocationDialogController> cancelHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        accordion.setExpandedPane(accordion.getPanes().get(0));

        argumentsTableView.setEditable(true);
        argumentsTableView.getSelectionModel().cellSelectionEnabledProperty().set(true);

        nameColumn.setEditable(false);
        unitColumn.setEditable(false);
        rawValueColumn.setEditable(true);
        engValueColumn.setEditable(true);

        rawValueColumn.setCellValueFactory(o -> o.getValue().rawValueStringProperty());
        engValueColumn.setCellValueFactory(o -> o.getValue().engValueStringProperty());
        nameColumn.setCellValueFactory(o -> o.getValue().nameProperty());
        unitColumn.setCellValueFactory(o -> o.getValue().unitProperty());

        rawValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        engValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        engValueColumn.setOnEditStart(event -> {
            if(event.getRowValue().descriptor.isFixed()) {
                // Cancel editing
                argumentsTableView.edit(-1, null);
            }
        });
        rawValueColumn.setOnEditStart(event -> {
            if(event.getRowValue().descriptor.isFixed()) {
                // Cancel editing
                argumentsTableView.edit(-1, null);
            }
        });
        engValueColumn.setOnEditCommit(event -> {
            if(event.getRowValue().descriptor.isFixed()) {
                return;
            }
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            // TODO: validation
            Object engVal = ValueUtil.parse(event.getRowValue().descriptor.getEngineeringDataType(), value);
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .engValueProperty().set(engVal);
        });
        rawValueColumn.setOnEditCommit(event -> {
            if(event.getRowValue().descriptor.isFixed()) {
                return;
            }
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            // TODO: validation
            Object rawVal = ValueUtil.parse(event.getRowValue().descriptor.getRawDataType(), value);
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .rawValueProperty().set(rawVal);
        });

        argumentsTableView.setOnKeyPressed(event -> {
            if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                argumentsTableView.edit(argumentsTableView.getFocusModel().getFocusedCell().getRow(), (TableColumn<ArgumentBean, Object>) argumentsTableView.getFocusModel().getFocusedCell().getTableColumn());
            } else if (event.getCode() == KeyCode.RIGHT
                    || event.getCode() == KeyCode.TAB) {
                argumentsTableView.getSelectionModel().selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                argumentsTableView.getSelectionModel().selectPrevious();
                event.consume();
            }
        });
    }

    public void initialiseActivityDialog(ActivityDescriptor descriptor, ActivityRequest currentRequest, List<ActivityRouteState> routesWithAvailability) {
        this.descriptor = descriptor;
        activityLabel.setText(descriptor.getPath().asString());
        descriptionLabel.setText(descriptor.getDescription());
        // Set the routes
        Map<String, Integer> route2position = new HashMap<>();
        int i = 0;
        for(ActivityRouteState route : routesWithAvailability) {
            routeChoiceBox.getItems().add(route);
            route2position.put(route.getRoute(), i++);
        }
        // Set the selected route or default
        if(currentRequest != null) {
            Integer position = route2position.getOrDefault(currentRequest.getRoute(), 0);
            if(position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
        } else if(descriptor.getDefaultRoute() != null) {
            Integer position = route2position.getOrDefault(descriptor.getDefaultRoute(), 0);
            if(position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
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

    public void registerHandlers(Consumer<ActivityInvocationDialogController> okClicked, Consumer<ActivityInvocationDialogController> cancelClicked) {
        this.okHandler = okClicked;
        this.cancelHandler = cancelClicked;
    }

    private void updateProperty(PropertyBean pb, Pair<String, String> property, ActivityRequest currentRequest) {
        // TODO
    }

    private void updateArgument(ArgumentBean ab, ActivityArgumentDescriptor aad, ActivityRequest currentRequest) {
        // TODO
    }

    public void okButtonClicked(ActionEvent actionEvent) {
        if(okHandler != null) {
            okHandler.accept(this);
        }
    }

    public void cancelButtonClicked(ActionEvent actionEvent) {
        if(cancelHandler != null) {
            cancelHandler.accept(this);
        }
    }

    public String getPath() {
        return this.descriptor.getPath().asString();
    }

    public ActivityRequest buildRequest() {
        // TODO
        return null;
    }

    private static class ArgumentBean {

        private final ActivityArgumentDescriptor descriptor;
        private final ReadOnlyStringProperty name;
        private final ReadOnlyStringProperty unit;
        private final SimpleObjectProperty<Object> rawValue;
        private final SimpleObjectProperty<Object> engValue;
        private final SimpleStringProperty rawValueString;
        private final SimpleStringProperty engValueString;

        public ArgumentBean(ActivityArgumentDescriptor aad) {
            descriptor = aad;
            name = new SimpleStringProperty(aad.getName());
            unit = new SimpleStringProperty(aad.getUnit());
            rawValue = new SimpleObjectProperty<>();
            engValue = new SimpleObjectProperty<>();
            rawValueString = new SimpleStringProperty();
            engValueString = new SimpleStringProperty();
            rawValue.addListener(o -> rawValueString.set(ValueUtil.toString(descriptor.getRawDataType(), rawValue.get())));
            engValue.addListener(o -> engValueString.set(ValueUtil.toString(descriptor.getEngineeringDataType(), engValue.get())));
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

        public SimpleStringProperty rawValueStringProperty() {
            return rawValueString;
        }

        public SimpleStringProperty engValueStringProperty() {
            return engValueString;
        }
    }

    private static class PropertyBean {

        public PropertyBean(Pair<String, String> property) {

        }
    }
}
