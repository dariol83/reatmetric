/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.ui.utils;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantCellFactory<T> implements Callback<TableColumn<T, Instant>, TableCell<T, Instant>> {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));

    @Override
    public TableCell<T, Instant> call(TableColumn<T, Instant> column) {
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
}
