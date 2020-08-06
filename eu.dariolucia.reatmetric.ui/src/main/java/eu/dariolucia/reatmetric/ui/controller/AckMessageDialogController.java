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

import eu.dariolucia.reatmetric.api.messages.AcknowledgedMessage;
import eu.dariolucia.reatmetric.api.messages.IAcknowledgedMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AckMessageDialogController implements Initializable, IAcknowledgedMessageSubscriber {

    @FXML
    private TableColumn<AcknowledgedMessage, String> idCol;
    @FXML
    private TableColumn<AcknowledgedMessage, Severity> severityCol;
    @FXML
    private TableColumn<AcknowledgedMessage, Instant> genTimeCol;
    @FXML
    private TableColumn<AcknowledgedMessage, String> sourceCol;
    @FXML
    private TableColumn<AcknowledgedMessage, String> messageCol;

    @FXML
    private TableView<AcknowledgedMessage> dataItemTableView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.idCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getId()));
        this.severityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getSeverity()));
        this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getGenerationTime()));
        this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getSource()));
        this.messageCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getMessage().getMessage()));

        this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
        this.severityCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Severity item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case ALARM:
                            setTextFill(Color.RED);
                            break;
                        case WARN:
                            setTextFill(Color.DARKORANGE);
                            break;
                        default:
                            setTextFill(Color.BLACK);
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
    }

    public void activate(Consumer<Boolean> alarmPresentNotifier) {

    }

    @Override
    public void dataItemsReceived(List<AcknowledgedMessage> dataItems) {

    }
}
