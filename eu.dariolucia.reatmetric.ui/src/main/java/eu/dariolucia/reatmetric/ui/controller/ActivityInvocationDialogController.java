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
import eu.dariolucia.reatmetric.api.processing.input.ActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ActivityInvocationDialogController implements Initializable {



    @FXML
    protected Accordion accordion;
    @FXML
    protected Label activityLabel;
    @FXML
    protected Label typeLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ComboBox<ActivityRouteState> routeChoiceBox;

    @FXML
    protected VBox argumentVBox;

    @FXML
    protected TableView<PropertyBean> propertiesTableView;
    @FXML
    protected TableColumn<PropertyBean, String> keyColumn;
    @FXML
    protected TableColumn<PropertyBean, String> valueColumn;
    @FXML
    protected Button okButton;
    @FXML
    protected Button cancelButton;

    private ActivityDescriptor descriptor;
    private List<ActivityInvocationArgumentLine> arguments = new LinkedList<>();
    private Consumer<ActivityInvocationDialogController> okHandler;
    private Consumer<ActivityInvocationDialogController> cancelHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        accordion.setExpandedPane(accordion.getPanes().get(0));
        routeChoiceBox.setCellFactory(new Callback<>() {
            @Override
            public ListCell<ActivityRouteState> call(ListView<ActivityRouteState> p) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(ActivityRouteState item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item == null ? "" : item.getRoute());
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            Circle c = new Circle();
                            c.setRadius(8);
                            switch (item.getAvailability()) {
                                case AVAILABLE:
                                    c.setFill(Paint.valueOf("#00FF00"));
                                break;
                                case UNAVAILABLE:
                                    c.setFill(Paint.valueOf("#FF0000"));
                                break;
                                case UNKNOWN:
                                    c.setFill(Paint.valueOf("#a9a9a9"));
                                break;
                            }
                            setGraphic(c);
                        }
                    }
                };
            }
        });
    }

    private void initialiseArgumentTable(ActivityRequest currentRequest) {
        for(ActivityArgumentDescriptor d : descriptor.getArgumentDescriptors()) {
            ActivityInvocationArgumentLine line = new ActivityInvocationArgumentLine(d, getInputFor(currentRequest, d));
            argumentVBox.getChildren().add(line.getNode());
            arguments.add(line);
        }
    }

    private ActivityArgument getInputFor(ActivityRequest currentRequest, ActivityArgumentDescriptor d) {
        if(currentRequest == null) {
            return null;
        }
        for(ActivityArgument a : currentRequest.getArguments()) {
            if(a.getName().equals(d.getName())) {
                return a;
            }
        }
        return null;
    }

    private void initialisePropertyTable() {
        propertiesTableView.setEditable(true);
        propertiesTableView.getSelectionModel().cellSelectionEnabledProperty().set(true);
        keyColumn.setEditable(true);
        valueColumn.setEditable(true);
        keyColumn.setCellValueFactory(o -> o.getValue().keyProperty());
        valueColumn.setCellValueFactory(o -> o.getValue().valueProperty());
        keyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        keyColumn.setOnEditCommit(event -> {
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .keyProperty().set(value);
        });
        valueColumn.setOnEditCommit(event -> {
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .valueProperty().set(value);
        });
        propertiesTableView.setOnKeyPressed(event -> {
            if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                propertiesTableView.edit(propertiesTableView.getFocusModel().getFocusedCell().getRow(), (TableColumn<PropertyBean, Object>) propertiesTableView.getFocusModel().getFocusedCell().getTableColumn());
            } else if (event.getCode() == KeyCode.RIGHT
                    || event.getCode() == KeyCode.TAB) {
                propertiesTableView.getSelectionModel().selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                propertiesTableView.getSelectionModel().selectPrevious();
                event.consume();
            }
        });
    }

    public void initialiseActivityDialog(ActivityDescriptor descriptor, ActivityRequest currentRequest, List<ActivityRouteState> routesWithAvailability) {
        this.descriptor = descriptor;
        activityLabel.setText(descriptor.getPath().asString());
        typeLabel.setText(descriptor.getActivityType());
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

        initialiseArgumentTable(currentRequest);

        if(arguments.size() == 1) {
            okButton.disableProperty().bind(arguments.get(0).validProperty().not());
        } else if(arguments.size() > 1) {
            BooleanBinding allValid = arguments.get(0).validProperty().and(arguments.get(1).validProperty());
            for(i = 2; i < arguments.size(); ++i) {
                allValid = allValid.and(arguments.get(i).validProperty());
            }
            okButton.disableProperty().bind(allValid.not());
        }

        initialisePropertyTable();

        // Set the properties
        for(Pair<String, String> property : descriptor.getProperties()) {
            PropertyBean pb = new PropertyBean(property);
            propertiesTableView.getItems().add(pb);
            updateProperty(pb, currentRequest);
        }
    }

    public void registerHandlers(Consumer<ActivityInvocationDialogController> okClicked, Consumer<ActivityInvocationDialogController> cancelClicked) {
        this.okHandler = okClicked;
        this.cancelHandler = cancelClicked;
    }

    private void updateProperty(PropertyBean pb, ActivityRequest currentRequest) {
        if(currentRequest == null) {
            return;
        }
        for(Map.Entry<String, String> prop : currentRequest.getProperties().entrySet()) {
            if(prop.getKey().equals(pb.keyProperty().get())) {
                pb.valueProperty().set(prop.getValue());
                return;
            }
        }
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
        Map<String, String> propertyMap = new LinkedHashMap<>();
        for(PropertyBean pb : propertiesTableView.getItems()) {
            propertyMap.put(pb.keyProperty().get(), pb.valueProperty().get());
        }
        return new ActivityRequest(descriptor.getExternalId(), arguments.stream().filter(o -> !o.isFixed()).map(ActivityInvocationArgumentLine::buildArgument).collect(Collectors.toList()), propertyMap, routeChoiceBox.getSelectionModel().getSelectedItem().getRoute(), "TODO Source");
    }

    @FXML
    public void addPropertyClicked(ActionEvent actionEvent) {
        propertiesTableView.getItems().add(new PropertyBean(Pair.of("property-key","property-value")));
    }

    @FXML
    public void removePropertyClicked(ActionEvent actionEvent) {
        if(propertiesTableView.getSelectionModel().getSelectedItem() != null) {
            propertiesTableView.getItems().remove(propertiesTableView.getSelectionModel().getSelectedItem());
        }
    }

    private static class PropertyBean {

        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public PropertyBean(Pair<String, String> property) {
            key = new SimpleStringProperty(property.getFirst());
            value = new SimpleStringProperty(property.getSecond());
        }

        public SimpleStringProperty keyProperty() {
            return key;
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }
    }
}
