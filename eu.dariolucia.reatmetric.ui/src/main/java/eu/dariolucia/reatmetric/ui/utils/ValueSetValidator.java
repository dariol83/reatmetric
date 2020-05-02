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

import javafx.beans.property.SimpleBooleanProperty;

import java.util.function.Predicate;

public class ValueSetValidator implements Predicate<Object> {

    private final SimpleBooleanProperty active;
    private final boolean mandatory;

    public ValueSetValidator(boolean mandatory) {
        this.active = new SimpleBooleanProperty(true);
        this.mandatory = mandatory;
    }

    @Override
    public boolean test(Object o) {
        if(active.get()) {
            if(mandatory && (o == null)) {
                return false;
            }
            return !mandatory || (!(o instanceof String) || !o.toString().isBlank());
        } else {
            return true;
        }
    }

    public SimpleBooleanProperty activeProperty() {
        return active;
    }

    public String getErrorMessage() {
        return "Value must be set";
    }
}
