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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteAvailability;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.ActivityArgumentTableManager;
import eu.dariolucia.reatmetric.ui.utils.PropertyBean;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
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
import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ActivitySchedulingDialogController implements Initializable {

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
    protected ToggleSwitch forceToggleSwitch;
    @FXML
    protected Button refreshButton;

    private final SimpleBooleanProperty routeChoiceBoxValid = new SimpleBooleanProperty(false);

    @FXML
    protected VBox argumentVBox;

    private ActivityArgumentTableManager argumentTableManager;

    @FXML
    protected TableView<PropertyBean> propertiesTableView;
    @FXML
    protected TableColumn<PropertyBean, String> keyColumn;
    @FXML
    protected TableColumn<PropertyBean, String> valueColumn;

    private ActivityDescriptor descriptor;
    private Supplier<List<ActivityRouteState>> routeSupplier;

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
        routeChoiceBox.getSelectionModel().selectedItemProperty().addListener(o -> {
            routeChoiceBoxValid.set(routeChoiceBox.getSelectionModel().getSelectedItem() != null &&
                    (forceToggleSwitch.isSelected() || routeChoiceBox.getSelectionModel().getSelectedItem().getAvailability() != ActivityRouteAvailability.UNAVAILABLE));
        });
        forceToggleSwitch.selectedProperty().addListener((obj, oldV, newV) -> {
            routeChoiceBoxValid.set(routeChoiceBox.getSelectionModel().getSelectedItem() != null &&
                    (forceToggleSwitch.isSelected() || routeChoiceBox.getSelectionModel().getSelectedItem().getAvailability() != ActivityRouteAvailability.UNAVAILABLE));
        });
    }

    private void initialiseArgumentTable(ActivityRequest currentRequest) {
        argumentTableManager = new ActivityArgumentTableManager(descriptor, currentRequest);
        TreeTableView<?> table = argumentTableManager.getTable();
        table.setPrefHeight(400);
        argumentVBox.getChildren().add(table);
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

    public void initialiseActivityDialog(ActivityDescriptor descriptor, ActivityRequest currentRequest, Supplier<List<ActivityRouteState>> routesWithAvailabilitySupplier) {
        this.descriptor = descriptor;
        this.routeSupplier = routesWithAvailabilitySupplier;

        activityLabel.setText(descriptor.getPath().asString());
        typeLabel.setText(descriptor.getActivityType());
        descriptionLabel.setText(descriptor.getDescription());
        // Set the routes
        refreshRoutes(descriptor, currentRequest);

        initialiseArgumentTable(currentRequest);

        initialisePropertyTable();

        // Set the properties
        for(Pair<String, String> property : descriptor.getProperties()) {
            if(currentRequest != null && !currentRequest.getProperties().containsKey(property.getFirst())) {
                // This property was removed
                continue;
            }
            PropertyBean pb = new PropertyBean(property);
            propertiesTableView.getItems().add(pb);
            if(currentRequest != null && currentRequest.getProperties().containsKey(property.getFirst())) {
                // This property was updated (potentially)
                pb.valueProperty().set(currentRequest.getProperties().get(property.getFirst()));
            }
        }
        // Add properties that were added before
        addMissingPropertiesFrom(currentRequest);
    }

    private void refreshRoutes(ActivityDescriptor descriptor, ActivityRequest currentRequest) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            final List<ActivityRouteState> routesWithAvailability = this.routeSupplier.get();
            Platform.runLater(() -> {
                initialiseRouteCombo(descriptor, currentRequest, routesWithAvailability);
            });
        });
    }

    private void initialiseRouteCombo(ActivityDescriptor descriptor, ActivityRequest currentRequest, List<ActivityRouteState> routesWithAvailability) {
        // If you have a route already selected, remember it
        ActivityRouteState selected = routeChoiceBox.getSelectionModel().getSelectedItem();

        routeChoiceBox.getItems().remove(0, routeChoiceBox.getItems().size());
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
        } else if(descriptor != null && descriptor.getDefaultRoute() != null) {
            Integer position = route2position.getOrDefault(descriptor.getDefaultRoute(), 0);
            if (position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
        } else if(selected != null) {
            routeChoiceBox.getSelectionModel().select(selected);
        } else {
            // Check if you can select the first available route
            for(ActivityRouteState ars : routesWithAvailability) {
                if(ars.getAvailability() == ActivityRouteAvailability.AVAILABLE) {
                    Integer position = route2position.getOrDefault(ars.getRoute(), 0);
                    routeChoiceBox.getSelectionModel().select(position);
                }
            }
        }
    }

    public void bindOkButton(Button okButton) {
        okButton.disableProperty().bind(Bindings.or(routeChoiceBoxValid.not(), argumentTableManager.argumentTableValidProperty().not()));
    }

    private void addMissingPropertiesFrom(ActivityRequest currentRequest) {
        if(currentRequest == null) {
            return;
        }
        Set<String> propertyKeySet = propertiesTableView.getItems().stream().map(o -> o.valueProperty().get()).collect(Collectors.toUnmodifiableSet());
        for(Map.Entry<String, String> prop : currentRequest.getProperties().entrySet()) {
            if(!propertyKeySet.contains(prop.getKey())) {
                PropertyBean pb = new PropertyBean(Pair.of(prop.getKey(), prop.getValue()));
                propertiesTableView.getItems().add(pb);
            }
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
        return new ActivityRequest(descriptor.getExternalId(), descriptor.getPath(), argumentTableManager.buildArgumentList(), propertyMap, routeChoiceBox.getSelectionModel().getSelectedItem().getRoute(), ReatmetricUI.username());
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

    @FXML
    public void refreshRouteClicked(ActionEvent actionEvent) {
        refreshRoutes(null, null);
    }

}
