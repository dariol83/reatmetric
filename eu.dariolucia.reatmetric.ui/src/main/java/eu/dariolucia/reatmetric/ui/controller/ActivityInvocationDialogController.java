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
import eu.dariolucia.reatmetric.ui.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ActivityInvocationDialogController implements Initializable {

    private static final String PRESET_NAME = "PropertyKeyList";
    private static final String PRESET_VIEW_ID = "ActivityInvocationDialog";
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

    @FXML
    protected Button addPropButton;
    @FXML
    protected Button removePropButton;

    private ActivityDescriptor descriptor;
    private Supplier<List<ActivityRouteState>> routeSupplier;

    private InvalidationListener registeredRouteListener;

    private final SimpleBooleanProperty entriesValid = new SimpleBooleanProperty(false);

    private final List<String> propertyKeyList = new LinkedList<>();

    // Preset manager
    private final PresetStorageManager presetManager = new PresetStorageManager();

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
        registeredRouteListener = o -> {
            routeChoiceBoxValid.set(routeChoiceBox.getSelectionModel().getSelectedItem() != null &&
                    (forceToggleSwitch.isSelected() || routeChoiceBox.getSelectionModel().getSelectedItem().getAvailability() != ActivityRouteAvailability.UNAVAILABLE));
        };
        routeChoiceBox.getSelectionModel().selectedItemProperty().addListener(registeredRouteListener);
        forceToggleSwitch.selectedProperty().addListener((obj, oldV, newV) -> {
            routeChoiceBoxValid.set(routeChoiceBox.getSelectionModel().getSelectedItem() != null &&
                    (forceToggleSwitch.isSelected() || routeChoiceBox.getSelectionModel().getSelectedItem().getAvailability() != ActivityRouteAvailability.UNAVAILABLE));
        });
        // Load already used property keys
        loadPropertyKeyList();
    }

    public void hideRouteControls() {
        this.forceToggleSwitch.setVisible(false);
        this.refreshButton.setVisible(false);
        this.routeChoiceBox.getSelectionModel().selectedItemProperty().removeListener(registeredRouteListener);
        this.routeChoiceBoxValid.set(true);
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
        keyColumn.setCellFactory(createKeyTableCellFactory());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        keyColumn.setOnEditCommit(event -> {
            if(!keyColumn.isEditable()) {
                return;
            }
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .keyProperty().set(value);
            // Update the propertyKeyList
            updatePropertyKeyList(value);
        });
        valueColumn.setOnEditCommit(event -> {
            if(!valueColumn.isEditable()) {
                return;
            }
            final String value = event.getNewValue() != null ? event.getNewValue()
                    : event.getOldValue();
            event.getTableView().getItems()
                    .get(event.getTablePosition().getRow())
                    .valueProperty().set(value);
        });
        propertiesTableView.setOnKeyPressed(event -> {
            if(!propertiesTableView.isEditable()) {
                return;
            }
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

    private void updatePropertyKeyList(String value) {
        if(value != null && !value.isBlank() && !this.propertyKeyList.contains(value)) {
            this.propertyKeyList.add(value);
            persistPropertyKeyList();
        }
    }

    private void persistPropertyKeyList() {
        PropertyKeyPreset p = new PropertyKeyPreset();
        p.setItems(this.propertyKeyList);
        try {
            this.presetManager.save(ReatmetricUI.selectedSystem().getSystem().getName(), ReatmetricUI.username(), PRESET_NAME, PRESET_VIEW_ID, p);
        } catch (RemoteException e) {
            // List cannot be persisted, ignore
        }
    }

    private void loadPropertyKeyList() {
        try {
            PropertyKeyPreset p = this.presetManager.load(ReatmetricUI.selectedSystem().getSystem().getName(), ReatmetricUI.username(), PRESET_NAME, PRESET_VIEW_ID, PropertyKeyPreset.class);
            if(p != null) {
                this.propertyKeyList.addAll(p.getItems());
            }
        } catch (RemoteException e) {
            // List cannot be persisted, ignore
        }
    }

    private Callback<TableColumn<PropertyBean, String>, TableCell<PropertyBean, String>> createKeyTableCellFactory() {
        // We create a combo box using the list of key values as 'suggestion', make it editable
        return theVar -> {
            ComboBoxTableCell<PropertyBean, String> cell = new ComboBoxTableCell<>(FXCollections.observableList(this.propertyKeyList));
            cell.setComboBoxEditable(true);
            return cell;
        };
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

        entriesValid.bind(Bindings.and(routeChoiceBoxValid, argumentTableManager.argumentTableValidProperty()));
    }

    private void refreshRoutes(ActivityDescriptor descriptor, ActivityRequest currentRequest) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            final List<ActivityRouteState> routesWithAvailability = this.routeSupplier.get();
            FxUtils.runLater(() -> {
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
        okButton.disableProperty().bind(entriesValid.not());
    }

    public SimpleBooleanProperty entriesValidProperty() {
        return entriesValid;
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

    public void makeReadOnly() {
        routeChoiceBox.setEditable(false);
        propertiesTableView.setEditable(false);
        keyColumn.setEditable(false);
        valueColumn.setEditable(false);
        argumentTableManager.getTable().setEditable(false);
        argumentTableManager.setReadOnly();
        addPropButton.setVisible(false);
        removePropButton.setVisible(false);
    }
}
