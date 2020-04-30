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
import eu.dariolucia.reatmetric.api.processing.input.ActivityArgument;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.utils.ActivityDialogUtil;
import eu.dariolucia.reatmetric.ui.utils.ValueTypeBasedValidator;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.Objects;

public class ActivityInvocationArgumentLine {

    private final ActivityArgumentDescriptor descriptor;
    private final ActivityArgument input;

    private HBox node;

    private final ValidationSupport validationSupport = new ValidationSupport();
    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);
    private Control rawValueControl;
    private Control engValueControl;
    private CheckBox rawEngSelection;

    public ActivityInvocationArgumentLine(ActivityArgumentDescriptor descriptor, ActivityArgument input) {
        this.descriptor = descriptor;
        this.input = input;
        this.valid.bind(this.validationSupport.invalidProperty().not());
        initialiseNode();
    }

    private void initialiseNode() {
        node = new HBox();
        node.setSpacing(8);
        node.setPadding(new Insets(8));
        // Name
        Label nameLbl = new Label(descriptor.getName());
        nameLbl.setPrefWidth(100);
        nameLbl.setTooltip(new Tooltip(descriptor.getDescription()));
        // Unit
        Label unitLbl = new Label(Objects.toString(descriptor.getUnit(), ""));
        unitLbl.setPrefWidth(70);
        // Raw value
        rawValueControl = ActivityDialogUtil.buildValueControl(validationSupport, descriptor.getRawDataType(), input != null ? input.getRawValue() : null, input != null && input.isEngineering(), descriptor.getRawDefaultValue(), descriptor.isFixed());
        rawValueControl.setPrefWidth(100);
        rawValueControl.setTooltip(new Tooltip("Raw value"));
        // Eng. value
        engValueControl = ActivityDialogUtil.buildValueControl(validationSupport, descriptor.getEngineeringDataType(), input != null ? input.getEngValue() : null, input != null && input.isEngineering(), descriptor.getEngineeringDefaultValue(), descriptor.isFixed());
        engValueControl.setPrefWidth(100);
        engValueControl.setTooltip(new Tooltip("Engineering value"));
        // Raw/Eng value selection
        rawEngSelection = new CheckBox();
        rawEngSelection.setText("Use Eng.");
        rawEngSelection.setPrefWidth(90);

        SimpleBooleanProperty fixedProperty = new SimpleBooleanProperty(descriptor.isFixed());
        rawValueControl.disableProperty().bind(rawEngSelection.selectedProperty().or(fixedProperty));
        engValueControl.disableProperty().bind(rawEngSelection.selectedProperty().not().or(fixedProperty));

        if(input != null) {
            rawEngSelection.setSelected(input.isEngineering());
        } else if(descriptor.isDefaultValuePresent()) {
            if(descriptor.getEngineeringDefaultValue() != null) {
                rawEngSelection.setSelected(true);
            } else {
                rawEngSelection.setSelected(false);
            }
        } else {
            rawEngSelection.setSelected(true);
        }

        rawEngSelection.disableProperty().bind(fixedProperty);

        node.getChildren().addAll(nameLbl, rawValueControl, engValueControl, unitLbl, rawEngSelection);
    }

    public HBox getNode() {
        return node;
    }

    public SimpleBooleanProperty validProperty() {
        return valid;
    }

    public ActivityArgument buildArgument() {
        return new ActivityArgument(descriptor.getName(), buildObject(descriptor.getRawDataType(), rawValueControl), buildObject(descriptor.getEngineeringDataType(), engValueControl), rawEngSelection.isSelected());
    }

    private Object buildObject(ValueTypeEnum type, Control control) {
        if(control instanceof TextField) {
            return ValueUtil.parse(type, ((TextField) control).getText());
        } else if(control instanceof ToggleSwitch) {
            return ((ToggleSwitch) control).isSelected();
        } else {
            return null;
        }
    }
}
