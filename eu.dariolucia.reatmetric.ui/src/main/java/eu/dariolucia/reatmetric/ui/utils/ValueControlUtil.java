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

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.List;

public class ValueControlUtil {

    public static Control buildValueControl(ValidationSupport validationSupport, ValueTypeEnum type, Object inputValue, Object defaultValue, boolean isFixed, List<Object> acceptableValues) {
        boolean mandatory = true;
        if(isFixed) {
            mandatory = false;
        }

        Control toReturn;
        if(acceptableValues != null) {
            ValueSetValidator typeValidator = new ValueSetValidator(mandatory);
            // Combo box
            ComboBox<Object> t = new ComboBox<>();
            t.setPrefHeight(24);
            t.setCellFactory(new Callback<>() {
                @Override
                public ListCell<Object> call(ListView<Object> p) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(item == null ? "" : ValueUtil.toString(type, item));
                        }
                    };
                }
            });
            // Add items
            t.getItems().addAll(acceptableValues);
            // Set verification on change
            if(!isFixed && validationSupport != null) {
                typeValidator.activeProperty().bind(t.disableProperty().not());
                validationSupport.registerValidator(t, Validator.createPredicateValidator(typeValidator, typeValidator.getErrorMessage(), Severity.ERROR));
            }
            // Set current value if any
            if(inputValue != null) {
                t.getSelectionModel().select(inputValue);
            } else if(defaultValue != null) {
                t.getSelectionModel().select(defaultValue);
            } else {
                // Select the first
                t.getSelectionModel().select(0);
            }
            // Workaround for validation triggering on disabled
            if(!isFixed) {
                t.disableProperty().addListener((a,b,c) -> {
                    if(t.isDisabled()) {
                        t.setUserData(t.getSelectionModel().getSelectedItem());
                        // Force trigger
                        t.getSelectionModel().select(0);
                        t.getSelectionModel().clearSelection();
                    } else {
                        if(t.getUserData() != null) {
                            // Force trigger
                            t.getSelectionModel().select(t.getUserData());
                        } else {
                            // Force trigger
                            t.getSelectionModel().select(0);
                            t.getSelectionModel().clearSelection();
                        }
                    }
                });
            }
            toReturn = t;
        } else if (type == ValueTypeEnum.ENUMERATED
                || type == ValueTypeEnum.SIGNED_INTEGER
                || type == ValueTypeEnum.UNSIGNED_INTEGER
                || type == ValueTypeEnum.REAL
                || type == ValueTypeEnum.CHARACTER_STRING
                || type == ValueTypeEnum.ABSOLUTE_TIME
                || type == ValueTypeEnum.RELATIVE_TIME
                || type == ValueTypeEnum.OCTET_STRING
                || type == ValueTypeEnum.BIT_STRING) {
            ValueTypeBasedStringValidator typeValidator = new ValueTypeBasedStringValidator(type, mandatory);
            TextField t = new TextField();
            t.setPrefHeight(24);
            t.setTooltip(new Tooltip(typeValidator.getErrorMessage()));
            // Set verification on change
            if(!isFixed && validationSupport != null) {
                typeValidator.activeProperty().bind(t.disableProperty().not());
                validationSupport.registerValidator(t, Validator.createPredicateValidator(typeValidator, typeValidator.getErrorMessage(), Severity.ERROR));
            }
            // Set current value if any
            if(inputValue != null) {
                t.setText(ValueUtil.toString(type, inputValue));
            } else if(defaultValue != null) {
                t.setText(ValueUtil.toString(type, defaultValue));
            } else {
                t.setText("");
            }
            t.setPromptText("");
            // Workaround for validation triggering on disabled
            if(!isFixed) {
                t.disableProperty().addListener((a,b,c) -> {
                    if(t.isDisabled()) {
                        t.setUserData(t.getText());
                        // Force trigger
                        t.setText("_");
                        t.setText("");
                    } else {
                        if(t.getUserData() != null) {
                            // Force trigger
                            t.setText("_");
                            t.setText(t.getUserData().toString());
                        } else {
                            // Force trigger
                            t.setText("_");
                            t.setText("");
                        }
                    }
                });
            }
            // Set the outer object
            toReturn = t;
        } else if (type == ValueTypeEnum.BOOLEAN) {
            ToggleSwitch t = new ToggleSwitch();
            t.setPrefHeight(24);
            // Set current value if any
            if(inputValue != null) {
                t.setSelected((Boolean) inputValue);
            } else if(defaultValue != null) {
                t.setSelected((Boolean) defaultValue);
            }
            // Set the outer object
            toReturn = t;
        } else {
            // Not supported type, use generic text field
            TextField t = new TextField();
            t.setPrefHeight(24);
            t.setPromptText("");
            t.setText("");
            // Set the outer object
            toReturn = t;
        }
        return toReturn;
    }
}
