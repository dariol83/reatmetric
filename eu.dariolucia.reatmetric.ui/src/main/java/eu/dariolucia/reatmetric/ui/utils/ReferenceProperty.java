/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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
