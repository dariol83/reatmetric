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

package eu.dariolucia.reatmetric.api.common;

import java.io.Serializable;

/**
 * This class provides debug information of a specific aspect of the component/driver/system.
 */
public final class DebugInformation implements Serializable {

    public static DebugInformation of(String element, String name, Object measure, Object maximum, String unit) {
        return new DebugInformation(element, name, measure, maximum, unit);
    }

    private final String element;

    private final String name;

    private final Object measure;

    private final Object maximum;

    private final String unit;

    private DebugInformation(String element, String name, Object measure, Object maximum, String unit) {
        if(measure != null) {
            if(!(measure instanceof Number || measure instanceof String || measure instanceof Enum<?>)) {
                throw new IllegalArgumentException("Measure can only be of type Number, String or Enum, found " + measure.getClass().getName());
            }
        }
        if(maximum != null) {
            if(!(maximum instanceof Number)) {
                throw new IllegalArgumentException("Maximum can only be of type Number, found " + maximum.getClass().getName());
            }
        }
        this.element = element;
        this.name = name;
        this.measure = measure;
        this.maximum = maximum;
        this.unit = unit;
    }

    public String getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    public Object getMeasure() {
        return measure;
    }

    public Object getMaximum() {
        return maximum;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "[" +
                "element='" + element + '\'' +
                ", name='" + name + '\'' +
                ", measure=" + measure +
                (maximum != null ? ", maximum=" + maximum : "") +
                (unit != null ? ", unit=" + unit : "") +
                ']';
    }
}
