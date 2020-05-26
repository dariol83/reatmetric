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

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantCellFactory<T> {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));

    public static <T>  Callback<TableColumn<T, Instant>, TableCell<T, Instant>> instantCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<T, Instant> call(TableColumn<T, Instant> tInstantTableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Instant item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty && !isEmpty()) {
                            setText(DATE_TIME_FORMATTER.format(item));
                        } else {
                            setText("");
                        }
                    }
                };
            }
        };
    }

    public static <T>  Callback<TreeTableColumn<T, Instant>, TreeTableCell<T, Instant>> instantTreeCellFactory() {
        return new Callback<>() {
            @Override
            public TreeTableCell<T, Instant> call(TreeTableColumn<T, Instant> tInstantTableColumn) {
                return new TreeTableCell<>() {
                    @Override
                    protected void updateItem(Instant item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty && !isEmpty()) {
                            setText(DATE_TIME_FORMATTER.format(item));
                        } else {
                            setText("");
                        }
                    }
                };
            }
        };
    }
}
