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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class ReatmetricValidationSupport {

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(true);

    private final List<ValidationController> registeredControls = new LinkedList<>();

    public void registerValidator(Control control, Predicate<?> checker, String errorMessage) {
        ValidationController vc = new ValidationController(control, checker, errorMessage);
        registeredControls.add(vc);
        if(registeredControls.size() == 1) {
            valid.bind(vc.validProperty());
        } else {
            BooleanBinding bb = Bindings.and(registeredControls.get(0).validProperty(), registeredControls.get(1).validProperty());
            for(int i = 2; i < registeredControls.size(); ++i) {
                bb = bb.and(registeredControls.get(i).validProperty());
            }
            valid.bind(bb);
        }

        recheck();
    }

    public void recheck() {
        for(ValidationController vc : registeredControls) {
            vc.recheck();
        }
    }

    public SimpleBooleanProperty validProperty() {
        return valid;
    }

    private static class ValidationController implements InvalidationListener {

        private final Control control;
        private final Predicate predicate;
        private final String error;

        private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

        public ValidationController(Control control, Predicate predicate, String error) {
            this.control = control;
            this.predicate = predicate;
            this.error = error;
            if(control instanceof ComboBox<?>) {
                ((ComboBox<?>) control).getSelectionModel().selectedItemProperty().addListener(this);
            } else if(control instanceof TextField) {
                ((TextField) control).textProperty().addListener(this);
            } else if(control instanceof CheckBox) {
                ((CheckBox) control).selectedProperty().addListener(this);
            } else {
                throw new IllegalStateException("Control " + control.getClass().getName() + " not supported");
            }
        }

        public SimpleBooleanProperty validProperty() {
            return valid;
        }

        public void recheck() {
            if(check()) {
                markAsGood();
            } else {
                markAsFailure();
            }
        }

        private void markAsGood() {
            control.setTooltip(null);
            control.setStyle("");
            valid.set(true);
        }

        private void markAsFailure() {
            control.setTooltip(new Tooltip(error));
            control.setStyle("-fx-border-color: #FF0000; -fx-border-width: 2; -fx-border-insets: -2;");
            valid.set(false);
        }

        private boolean check() {
            if(control.isDisabled()) {
                return true; // No check if control is disabled
            }
            if(control instanceof ComboBox<?>) {
                return predicate.test(((ComboBox<?>) control).getSelectionModel().getSelectedItem());
            }
            if(control instanceof TextField) {
                return predicate.test(((TextField) control).getText());
            }
            if(control instanceof CheckBox) {
                return predicate.test(((CheckBox) control).isSelected());
            }
            throw new IllegalStateException("Control " + control.getClass().getName() + " not supported");
        }

        @Override
        public void invalidated(Observable observable) {
            recheck();
        }
    }
}
