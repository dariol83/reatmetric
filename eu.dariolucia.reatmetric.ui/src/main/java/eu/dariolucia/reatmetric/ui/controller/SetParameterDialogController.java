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
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.SetParameterRequest;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.ValueControlUtil;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.validation.ValidationSupport;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class SetParameterDialogController implements Initializable {


    @FXML
    protected Label parameterLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ComboBox<ActivityRouteState> routeChoiceBox;

    private final SimpleBooleanProperty routeChoiceBoxValid = new SimpleBooleanProperty(false);

    @FXML
    protected VBox valueVBox;

    @FXML
    protected Button okButton;
    @FXML
    protected Button cancelButton;

    private ParameterDescriptor descriptor;
    private final ValidationSupport validationSupport = new ValidationSupport();
    private Consumer<SetParameterDialogController> okHandler;
    private Consumer<SetParameterDialogController> cancelHandler;

    private Control rawValueControl;
    private Control engValueControl;
    private CheckBox rawEngSelection;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
                    routeChoiceBox.getSelectionModel().getSelectedItem().getAvailability() != ActivityRouteAvailability.UNAVAILABLE);
        });
    }

    public void initialiseParameterDialog(ParameterDescriptor descriptor, SetParameterRequest currentRequest, List<ActivityRouteState> routesWithAvailability) {
        this.descriptor = descriptor;
        parameterLabel.setText(descriptor.getPath().asString());
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
        } else if(descriptor.getSetterDefaultRoute() != null) {
            Integer position = route2position.getOrDefault(descriptor.getSetterDefaultRoute(), 0);
            if(position != null) {
                routeChoiceBox.getSelectionModel().select(position);
            }
        } else {
            // Check if you can select the first available route
            for(ActivityRouteState ars : routesWithAvailability) {
                if(ars.getAvailability() == ActivityRouteAvailability.AVAILABLE) {
                    Integer position = route2position.getOrDefault(ars.getRoute(), 0);
                    routeChoiceBox.getSelectionModel().select(position);
                }
            }
        }

        initialiseValueTable(currentRequest);

        BooleanBinding allValid = routeChoiceBoxValid.and(validationSupport.invalidProperty().not());
        okButton.disableProperty().bind(allValid.not());
    }

    private void initialiseValueTable(SetParameterRequest currentRequest) {
        HBox node = new HBox();
        node.setSpacing(8);
        // Name
        Label nameLbl = new Label("New value");
        nameLbl.setPrefWidth(120.0);
        // Unit
        Label unitLbl = new Label(Objects.toString(descriptor.getUnit(), ""));
        unitLbl.setPrefWidth(70);
        // Raw value
        rawValueControl = ValueControlUtil.buildValueControl(validationSupport,
                descriptor.getRawDataType(),
                currentRequest != null && !currentRequest.isEngineeringUsed() ? currentRequest.getValue() : null,
                null,
                false,
                descriptor.getExpectedRawValues());
        rawValueControl.setPrefWidth(150);
        // Eng. value
        engValueControl = ValueControlUtil.buildValueControl(validationSupport,
                descriptor.getEngineeringDataType(),
                currentRequest != null && currentRequest.isEngineeringUsed() ? currentRequest.getValue() : null,
                null,
                false,
                descriptor.getExpectedEngineeringValues());
        engValueControl.setPrefWidth(150);
        // Raw/Eng value selection
        rawEngSelection = new CheckBox();
        rawEngSelection.setText("Use Eng.");
        rawEngSelection.setPrefWidth(90);

        SimpleBooleanProperty fixedProperty = new SimpleBooleanProperty(false);
        rawValueControl.disableProperty().bind(rawEngSelection.selectedProperty().or(fixedProperty));
        engValueControl.disableProperty().bind(rawEngSelection.selectedProperty().not().or(fixedProperty));

        if(currentRequest != null) {
            rawEngSelection.setSelected(currentRequest.isEngineeringUsed());
        } else {
            rawEngSelection.setSelected(true);
        }

        rawEngSelection.disableProperty().bind(fixedProperty);

        node.getChildren().addAll(nameLbl, rawValueControl, engValueControl, unitLbl, rawEngSelection);
        validationSupport.initInitialDecoration();

        valueVBox.getChildren().add(node);
    }

    private Object buildValueObject() {
        Control control = rawEngSelection.isSelected() ? engValueControl : rawValueControl;
        ValueTypeEnum type = rawEngSelection.isSelected() ? descriptor.getEngineeringDataType() : descriptor.getRawDataType();
        if(control instanceof TextField) {
            return ValueUtil.parse(type, ((TextField) control).getText());
        } else if(control instanceof ToggleSwitch) {
            return ((ToggleSwitch) control).isSelected();
        } else if(control instanceof ComboBox) {
            return ((ComboBox<?>) control).getSelectionModel().getSelectedItem();
        } else {
            return null;
        }
    }

    public void registerHandlers(Consumer<SetParameterDialogController> okClicked, Consumer<SetParameterDialogController> cancelClicked) {
        this.okHandler = okClicked;
        this.cancelHandler = cancelClicked;
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

    public SetParameterRequest buildRequest() {
        return new SetParameterRequest(descriptor.getExternalId(), rawEngSelection.isSelected(), buildValueObject(), routeChoiceBox.getSelectionModel().getSelectedItem().getRoute(), ReatmetricUI.username());
    }
}
