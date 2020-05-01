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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.StageStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class TransportConnectorInitDialog extends Dialog<Boolean> {

	// TODO: make this a PopOver, provide hooking node
	public static Optional<Boolean> openWizard(ITransportConnector connector) {
		return new TransportConnectorInitDialog(connector).showAndWait();
	}

	private BooleanProperty canFinish = new SimpleBooleanProperty(false);
	private ConnectorPropertyPage page;
	private ITransportConnector connector;

	private TransportConnectorInitDialog(ITransportConnector connector) {
		super();
		this.connector = connector;
		initStyle(StageStyle.DECORATED);
		setResizable(false);
		setWidth(400);
		setHeight(500);
		setTitle("Initialisation Properties");
		setHeaderText("Define " + connector.getName() + " properties");

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		createPages();
		setResultConverter((o) -> {
			if(o.equals(ButtonType.OK)) {
				return initialiseConnector();
			} else {
				return false;
			}
		});
	}

	private void createPages() {
		// Connector properties
		page = new ConnectorPropertyPage();
		page.toFront();

		getDialogPane().setContent(page);

		Button finish = (Button) getDialogPane().lookupButton(ButtonType.OK);
		finish.disableProperty().bind(canFinish.not());

		canFinish.bind(page.validProperty());
	}

	private boolean initialiseConnector() {
		if(!canFinish.get()) {
			throw new IllegalStateException("Cannot complete the initialisation for connector " + connector.getName() + ": not valid entries");
		}
		Map<String, Object> configuration = page.buildConfiguration();
		try {
			connector.initialise(configuration);
			return true;
		} catch (TransportException e) {
			return false;
		}
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
				if(currentValue != null) {
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
				if(currentValue != null) {
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
				if(currentValue != null) {
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
