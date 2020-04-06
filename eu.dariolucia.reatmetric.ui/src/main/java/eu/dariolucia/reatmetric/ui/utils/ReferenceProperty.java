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

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;

import java.util.Objects;

public class ReferenceProperty<T extends AbstractDataItem> {

    public static Callback<ReferenceProperty<?>, Observable[]> extractor() {
        return (ReferenceProperty<?> p) -> new Observable[]{p.referenceProperty()};
    }

    private final SimpleObjectProperty<T> reference = new SimpleObjectProperty<>();

    public ReferenceProperty() {
    }

    public ReferenceProperty(T initialValue) {
        reference.set(initialValue);
    }

    public void set(T newValue) {
        reference.set(newValue);
    }

    public SimpleObjectProperty<T> referenceProperty() {
        return reference;
    }

    public T get() {
        return reference.get();
    }

    @Override
    public String toString() {
        return Objects.toString(get());
    }
}
