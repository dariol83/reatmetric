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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.controller.ActivityInvocationDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ActivityDialogUtil {

    public static Pair<Node, ActivityInvocationDialogController> createActivityInvocationDialog() throws IOException {
        URL datePickerUrl = ActivityDialogUtil.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/ActivityInvocationDialog.fxml");
        FXMLLoader loader = new FXMLLoader(datePickerUrl);
        VBox root = loader.load();
        ActivityInvocationDialogController controller = loader.getController();
        return Pair.of(root, controller);
    }

    public static Pair<Node, ActivityInvocationDialogController> createActivityInvocationDialog(ActivityDescriptor descriptor, List<ActivityRouteState> routeList) throws IOException {
        Pair<Node, ActivityInvocationDialogController> asBuilt = createActivityInvocationDialog();
        asBuilt.getSecond().initialiseActivityDialog(descriptor, null, routeList);
        return asBuilt;
    }

    public static Pair<Node, ActivityInvocationDialogController> createActivityInvocationDialog(ActivityDescriptor descriptor, ActivityRequest request, List<ActivityRouteState> routeList) throws IOException {
        Pair<Node, ActivityInvocationDialogController> asBuilt = createActivityInvocationDialog();
        asBuilt.getSecond().initialiseActivityDialog(descriptor, request, routeList);
        return asBuilt;
    }


    public static Control buildValueControl(ValidationSupport validationSupport, ValueTypeEnum value, Object inputRawValue, boolean inputIsEngineering, Object defaultRawValue, boolean isFixed) {
        ValueTypeBasedValidator typeValidator = new ValueTypeBasedValidator(value);
        Control toReturn;
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
            t.setPrefHeight(24);
            // Set verification on change
            if(!isFixed && validationSupport != null) {
                validationSupport.registerValidator(t, Validator.createPredicateValidator(typeValidator, typeValidator.getErrorMessage(), Severity.ERROR));
            }
            // Set current value if any
            if(inputRawValue != null && !inputIsEngineering) {
                t.setText(ValueUtil.toString(value, inputRawValue));
            } else if(defaultRawValue != null) {
                t.setText(ValueUtil.toString(value, defaultRawValue));
            } else {
                t.setText("");
            }
            t.setPromptText("");
            // Set the outer object
            toReturn = t;
        } else if (value == ValueTypeEnum.BOOLEAN) {
            ToggleSwitch t = new ToggleSwitch();
            t.setPrefHeight(24);
            // Set current value if any
            if(inputRawValue != null && !inputIsEngineering) {
                t.setSelected((Boolean) inputRawValue);
            } else if(defaultRawValue != null) {
                t.setSelected((Boolean) defaultRawValue);
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
