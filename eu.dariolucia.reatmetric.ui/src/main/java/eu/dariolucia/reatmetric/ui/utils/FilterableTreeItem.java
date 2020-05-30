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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

import java.util.function.Predicate;

/*
 * Stack Overflow original snippet from: https://stackoverflow.com/questions/15897936/javafx-2-treeview-filtering/34426897#34426897
 *
 * Thanks to kaznovac (https://stackoverflow.com/users/382655/kaznovac)
 *
 * Enhanced to support first-level filtering and correct item list ordering
 */
public class FilterableTreeItem<T> extends TreeItem<T> {
    private final ObservableList<TreeItem<T>> sourceChildren = FXCollections.observableArrayList();
    private final FilteredList<TreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);
    private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>();

    public FilterableTreeItem(T value) {
        this(value, true);
    }

    public FilterableTreeItem(T value, boolean considerChildren) {
        super(value);

        filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            return child -> {
                if (child instanceof FilterableTreeItem) {
                    ((FilterableTreeItem<T>) child).predicateProperty().set(predicate.get());
                }
                if (predicate.get() == null) {
                    return true;
                }
                // This condition prevents filtering if the treeitem has children: can be disabled by constructor
                if(considerChildren && !child.getChildren().isEmpty()) {
                    return true;
                }
                return predicate.get().test(child.getValue());
            };
        } , predicate));

        filteredChildren.addListener((ListChangeListener<TreeItem<T>>) c -> {
            while (c.next()) {
                getChildren().removeAll(c.getRemoved());
                getChildren().addAll(c.getFrom(), c.getAddedSubList());
            }
        });
    }

    public ObservableList<TreeItem<T>> getSourceChildren() {
        return sourceChildren;
    }

    public ObjectProperty<Predicate<T>> predicateProperty() {
        return predicate;
    }
}
