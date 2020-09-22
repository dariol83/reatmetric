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

import eu.dariolucia.reatmetric.api.activity.ActivityRouteAvailability;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.SetParameterRequest;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.PropertyBean;
import eu.dariolucia.reatmetric.ui.utils.ReatmetricValidationSupport;
import eu.dariolucia.reatmetric.ui.utils.ValueControlUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import org.controlsfx.control.ToggleSwitch;

import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

public class SetParameterDialogController implements Initializable {

    @FXML
    protected Accordion accordion;

    @FXML
    protected Label parameterLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ComboBox<ActivityRouteState> routeChoiceBox;
    @FXML
    protected ToggleSwitch forceToggleSwitch;
    @FXML
    protected Button refreshButton;

    @FXML
    protected TableView<PropertyBean> propertiesTableView;
    @FXML
    protected TableColumn<PropertyBean, String> keyColumn;
    @FXML
    protected TableColumn<PropertyBean, String> valueColumn;

    private final SimpleBooleanProperty routeChoiceBoxValid = new SimpleBooleanProperty(false);

    @FXML
    protected VBox valueVBox;

    private ParameterDescriptor descriptor;

    private Control rawValueControl;
    private Control engValueControl;

    private Supplier<List<ActivityRouteState>> routeSupplier;

    private RadioButton rawSelection;
    private RadioButton engSelection;

    private final ReatmetricValidationSupport validationSupport = new ReatmetricValidationSupport();

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


    public void initialiseParameterDialog(ParameterDescriptor descriptor, SetParameterRequest currentRequest, Supplier<List<ActivityRouteState>> routesWithAvailabilitySupplier) {
        this.descriptor = descriptor;
        this.routeSupplier = routesWithAvailabilitySupplier;
        parameterLabel.setText(descriptor.getPath().asString());
        descriptionLabel.setText(descriptor.getDescription());
        // Set the routes
        refreshRoutes(descriptor, currentRequest);

        initialisePropertyTable();

        initialiseValueTable(currentRequest);
    }

    private void refreshRoutes(ParameterDescriptor descriptor, SetParameterRequest currentRequest) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            final List<ActivityRouteState> routesWithAvailability = this.routeSupplier.get();
            Platform.runLater(() -> {
                initialiseRouteCombo(descriptor, currentRequest, routesWithAvailability);
            });
        });
    }

    private void initialiseRouteCombo(ParameterDescriptor descriptor, SetParameterRequest currentRequest, List<ActivityRouteState> routesWithAvailability) {
        // If you have a route already selected, remember it
        ActivityRouteState selected = routeChoiceBox.getSelectionModel().getSelectedItem();

        routeChoiceBox.getItems().remove(0, routeChoiceBox.getItems().size());
        Map<String, Integer> route2position = new HashMap<>();
        int i = 0;
        for (ActivityRouteState route : routesWithAvailability) {
            routeChoiceBox.getItems().add(route);
            route2position.put(route.getRoute(), i++);
        }
        // Set the selected route or default
        if (currentRequest != null) {
            Integer position = route2position.getOrDefault(currentRequest.getRoute(), 0);
            if (position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
        } else if (descriptor != null && descriptor.getSetterDefaultRoute() != null) {
            Integer position = route2position.getOrDefault(descriptor.getSetterDefaultRoute(), 0);
            if (position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
        } else if (selected != null) {
            routeChoiceBox.getSelectionModel().select(selected);
        } else {
            // Check if you can select the first available route
            for (ActivityRouteState ars : routesWithAvailability) {
                if (ars.getAvailability() == ActivityRouteAvailability.AVAILABLE) {
                    Integer position = route2position.getOrDefault(ars.getRoute(), 0);
                    routeChoiceBox.getSelectionModel().select(position);
                }
            }
        }
    }

    public void bindOkButton(Button okButton) {
        okButton.disableProperty().bind(Bindings.or(routeChoiceBoxValid.not(), validationSupport.validProperty().not()));
    }

    private void initialiseValueTable(SetParameterRequest input) {
        HBox line1 = new HBox();
        line1.setSpacing(8);

        HBox line2 = new HBox();
        line2.setSpacing(8);

        ToggleGroup rawEngToggleGroup = new ToggleGroup();

        // Raw value
        rawSelection = new RadioButton("Raw Value");
        rawSelection.setPrefWidth(120);
        rawSelection.setToggleGroup(rawEngToggleGroup);
        rawSelection.setTextAlignment(TextAlignment.LEFT);
        line1.getChildren().add(rawSelection);
        rawValueControl = ValueControlUtil.buildValueControl(validationSupport,
                descriptor.getRawDataType(),
                input != null && !input.isEngineeringUsed() ? input.getValue() : null,
                null,
                false,
                descriptor.getExpectedRawValues());
        rawValueControl.setPrefWidth(150);
        line1.getChildren().add(rawValueControl);
        Label emptyLabel = new Label("");
        emptyLabel.setPrefWidth(70);
        line1.getChildren().add(emptyLabel);
        valueVBox.getChildren().add(line1);

        // Eng. value
        engSelection = new RadioButton("Eng. Value");
        engSelection.setPrefWidth(120);
        engSelection.setToggleGroup(rawEngToggleGroup);
        engSelection.setTextAlignment(TextAlignment.LEFT);
        line2.getChildren().add(engSelection);
        engValueControl = ValueControlUtil.buildValueControl(validationSupport,
                descriptor.getEngineeringDataType(),
                input != null && input.isEngineeringUsed() ? input.getValue() : null,
                null,
                false,
                descriptor.getExpectedEngineeringValues());
        engValueControl.setPrefWidth(150);
        line2.getChildren().add(engValueControl);
        // Unit
        Label unitLbl = new Label(Objects.toString(descriptor.getUnit(), ""));
        unitLbl.setPrefWidth(70);
        line2.getChildren().add(unitLbl);
        valueVBox.getChildren().add(line2);

        // Raw/Eng value selection
        rawValueControl.disableProperty().bind(rawSelection.selectedProperty().not());
        engValueControl.disableProperty().bind(engSelection.selectedProperty().not());

        if (input != null) {
            rawSelection.setSelected(!input.isEngineeringUsed());
            engSelection.setSelected(input.isEngineeringUsed());
        } else {
            rawSelection.setSelected(false);
            engSelection.setSelected(true);
        }
    }

    private Object buildValueObject() {
        Control control = engSelection.isSelected() ? engValueControl : rawValueControl;
        ValueTypeEnum type = engSelection.isSelected() ? descriptor.getEngineeringDataType() : descriptor.getRawDataType();
        if (control instanceof TextField) {
            return ValueUtil.parse(type, ((TextField) control).getText());
        } else if (control instanceof ToggleSwitch) {
            return ((ToggleSwitch) control).isSelected();
        } else if (control instanceof ComboBox) {
            return ((ComboBox<?>) control).getSelectionModel().getSelectedItem();
        } else {
            return null;
        }
    }

    public String getPath() {
        return this.descriptor.getPath().asString();
    }

    public SetParameterRequest buildRequest() {
        Map<String, String> propertyMap = new LinkedHashMap<>();
        for (PropertyBean pb : propertiesTableView.getItems()) {
            propertyMap.put(pb.keyProperty().get(), pb.valueProperty().get());
        }
        return new SetParameterRequest(descriptor.getExternalId(), engSelection.isSelected(), buildValueObject(), propertyMap, routeChoiceBox.getSelectionModel().getSelectedItem().getRoute(), ReatmetricUI.username());
    }

    @FXML
    public void addPropertyClicked(ActionEvent actionEvent) {
        propertiesTableView.getItems().add(new PropertyBean(Pair.of("property-key", "property-value")));
    }

    @FXML
    public void removePropertyClicked(ActionEvent actionEvent) {
        if (propertiesTableView.getSelectionModel().getSelectedItem() != null) {
            propertiesTableView.getItems().remove(propertiesTableView.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    public void refreshRouteClicked(ActionEvent actionEvent) {
        refreshRoutes(null, null);
    }
}
