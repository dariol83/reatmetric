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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ActivityInvocationDialogController implements Initializable {

    @FXML
    protected Label activityLabel;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected ChoiceBox<String> routeChoiceBox;
    @FXML
    protected TableView argumentsTableView;
    @FXML
    protected TableColumn nameColumn;
    @FXML
    protected TableColumn rawValueColumn;
    @FXML
    protected TableColumn engValueColumn;
    @FXML
    protected TableColumn unitColumn;
    @FXML
    protected TableView propertiesTableView;
    @FXML
    protected TableColumn keyColumn;
    @FXML
    protected TableColumn valueColumn;
    @FXML
    protected Button okButton;
    @FXML
    protected Button cancelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void initialiseActivityDialog(ActivityDescriptor descriptor, ActivityRequest currentRequest, List<Pair<String, Boolean>> routesWithAvailability) {

    }

    private static class ArgumentBean {

    }

    private static class PropertyBean {

    }
}
