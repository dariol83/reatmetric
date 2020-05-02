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

package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.controller.ConnectorStatusWidgetController;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransportConnectorInitDialog {

    private static final Logger LOG = Logger.getLogger(TransportConnectorInitDialog.class.getName());

    private static final PopOver popOver = new PopOver();

    public static void openWizard(ITransportConnector connector, Node hook, boolean connectOnPositiveInitialise) {
        popOver.setAutoHide(true);
        popOver.setDetachable(false);
        popOver.setHideOnEscape(true);
        popOver.hide();
        TransportConnectorInitDialog dialog = new TransportConnectorInitDialog(connector, hook, connectOnPositiveInitialise);
        dialog.show();
    }

    private BooleanProperty canFinish = new SimpleBooleanProperty(false);
    private ConnectorPropertyPage page;
    private final ITransportConnector connector;
    private final boolean connectOnPositiveInitialise;
    private final Node hook;

    private TransportConnectorInitDialog(ITransportConnector connector, Node hook, boolean connectOnPositiveInitialise) {
        this.connector = connector;
        this.hook = hook;
        this.connectOnPositiveInitialise = connectOnPositiveInitialise;
        // Connector properties
        VBox box = new VBox();
        box.setPadding(new Insets(8));
        box.setSpacing(8.0);
        page = new ConnectorPropertyPage();
        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        HBox hbox = new HBox();
        hbox.setSpacing(8);
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        Button finish = new Button(connectOnPositiveInitialise ? "Initialise and Connect" : "OK");
        finish.setMinWidth(50);
        finish.disableProperty().bind(canFinish.not());
        finish.setOnAction(this::initialiseConnector);
        hbox.getChildren().addAll(r, finish);
        canFinish.bind(page.validProperty());
        box.getChildren().addAll(page, separator, hbox);
        popOver.setContentNode(box);
    }

    public static void openWizardNoElements(ITransportConnector connector, Node hook) {
        popOver.setAutoHide(true);
        popOver.setDetachable(false);
        popOver.setHideOnEscape(true);
        popOver.hide();
        VBox box = new VBox();
        box.setPadding(new Insets(8));
        Label l = new Label("Transport connector " + connector.getName() + " does not have any configuration runtime property");
        l.setPrefWidth(300);
        l.setWrapText(true);
        l.setPrefHeight(50);
        box.getChildren().add(l);
        popOver.setContentNode(box);
        popOver.show(hook);
        popOver.setOnCloseRequest((e) -> {
        	if(hook instanceof ToggleSwitch) {
        		((ToggleSwitch) hook).setSelected(false);
			}
		});
    }

    private void initialiseConnector(ActionEvent event) {
        if (!canFinish.get()) {
            throw new IllegalStateException("Cannot complete the initialisation for connector " + connector.getName() + ": not valid entries");
        }
        Map<String, Object> configuration = page.buildConfiguration();
        ReatmetricUI.threadPool(ConnectorStatusWidgetController.class).execute(() -> {
            try {
                connector.initialise(configuration);
                if (connectOnPositiveInitialise) {
                    connector.connect();
                }
				Platform.runLater(() -> {
					popOver.setOnCloseRequest(null);
					popOver.hide();
				});
            } catch (TransportException e) {
                LOG.log(Level.SEVERE, "Failing to initialise " + (connectOnPositiveInitialise ? "and activate " : "") + "connector " + connector.getName() + ": " + e.getMessage(), e);
                // Nasty workaround on this design - deselect if you fail
				if(connectOnPositiveInitialise) {
					Platform.runLater(() -> {
						if (hook instanceof ToggleSwitch) {
							((ToggleSwitch) hook).setSelected(false);
						}
					});
				}
            }
        });
    }

    private void show() {
        popOver.show(hook);
    }

    protected class ConnectorPropertyPage extends GridPane {

        private BooleanProperty valid = new SimpleBooleanProperty();
        private Map<Control, Function<Control, Object>> mappers = new HashMap<>();
        private Map<Control, Function<Control, String>> validators = new HashMap<>();
        private int nextRow = 0;

        protected ConnectorPropertyPage() {
            super();
            setHgap(8);
            setVgap(8);
            setMargin(this, new Insets(5, 5, 5, 5));
            setPadding(new Insets(5, 5, 5, 5));
            setStyle("-fx-background-color: derive(-fx-base,26.4%);");
            initPageWith(connector);
            validate();
        }

        public BooleanProperty validProperty() {
            return valid;
        }

        private void initPageWith(ITransportConnector connector) {
            if (connector != null) {
                Map<String, Pair<String, ValueTypeEnum>> currentDescriptor = connector.getSupportedProperties();
                Map<String, Object> values = connector.getCurrentProperties();
                for (Map.Entry<String, Pair<String, ValueTypeEnum>> cpd : currentDescriptor.entrySet()) {
                    addPropertyItem(cpd.getKey(), cpd.getValue().getFirst(), cpd.getValue().getSecond(), values.get(cpd.getKey()));
                }
            }
            validate();
            layout();
        }

        private void addPropertyItem(String key, String fieldName, ValueTypeEnum value, Object currentValue) {
            Control toReturn;
            Label propertyName = new Label(fieldName);
            propertyName.setUserData(key);
            propertyName.setPrefHeight(24);
            propertyName.setPrefWidth(200);
            if (value == ValueTypeEnum.ENUMERATED
                    || value == ValueTypeEnum.SIGNED_INTEGER
                    || value == ValueTypeEnum.UNSIGNED_INTEGER
                    || value == ValueTypeEnum.REAL
                    || value == ValueTypeEnum.CHARACTER_STRING
                    || value == ValueTypeEnum.ABSOLUTE_TIME
                    || value == ValueTypeEnum.RELATIVE_TIME
                    || value == ValueTypeEnum.OCTET_STRING
                    || value == ValueTypeEnum.BIT_STRING) {
                TextField t = new TextField();
                t.setPromptText("");
                t.setText("");
                t.setPrefHeight(24);
                // Set the mapper
                this.mappers.put(t, ctrl -> ValueUtil.parse(value, t.getText()));
                // Set the validator
                this.validators.put(t, ctrl -> {
                    try {
                        ValueUtil.parse(value, t.getText());
                        return null;
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                });
                // Set verification on change
                t.textProperty().addListener(change -> validate());
                // Set current value if any
                if (currentValue != null) {
                    t.setText(ValueUtil.toString(value, currentValue));
                }
                // Set the outer object
                toReturn = t;
            } else if (value == ValueTypeEnum.BOOLEAN) {
                CheckBox t = new CheckBox();
                t.setPrefHeight(24);
                // Set the mapper
                this.mappers.put(t, ctrl -> t.isSelected());
                // Set the validator
                this.validators.put(t, ctrl -> null);
                // Set verification on change
                t.selectedProperty().addListener(change -> validate());
                // Set current value if any
                if (currentValue != null) {
                    t.setSelected((Boolean) currentValue);
                }
                // Set the outer object
                toReturn = t;
            } else {
                // Not supported type, use generic text field
                TextField t = new TextField();
                t.setPrefHeight(24);
                t.setPromptText("");
                t.setText("");
                // Set the mapper
                this.mappers.put(t, ctrl -> t.getText());
                // Set the validator
                this.validators.put(t, ctrl -> null);
                // Set verification on change
                t.textProperty().addListener(change -> validate());
                // Set current value if any
                if (currentValue != null) {
                    t.setText(Objects.toString(currentValue));
                }
                // Set the outer object
                toReturn = t;
            }

            toReturn.setTooltip(null);
            toReturn.setUserData(key);

            add(propertyName, 0, this.nextRow);
            add(toReturn, 1, this.nextRow);
            add(new Label(), 2, this.nextRow++);

            GridPane.setHgrow(toReturn, Priority.ALWAYS);
        }

        private void validate() {
            boolean finalResult = true;
            for (Control c : this.validators.keySet()) {
                String validity = this.validators.get(c).apply(c);
                if (validity != null) {
                    finalResult = false;
                    c.setTooltip(new Tooltip(validity));
                    c.setStyle("-fx-background-color: red");
                } else {
                    c.setTooltip(null);
                    c.setStyle("");
                }
            }
            valid.set(finalResult);
        }

        public Map<String, Object> buildConfiguration() {
            if (!valid.get()) {
                throw new IllegalStateException("Cannot build the connector configuration: not valid");
            }
            Map<String, Object> toReturn = new HashMap<>();
            for (Control c : this.mappers.keySet()) {
                Function<Control, Object> mapper = this.mappers.get(c);
                toReturn.put((String) c.getUserData(), mapper.apply(c));
            }
            return toReturn;
        }
    }
}
