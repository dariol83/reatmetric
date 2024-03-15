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

import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.SetParameterRequest;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.controller.SetParameterDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

public class SetParameterDialogUtil {

    public static Pair<Node, SetParameterDialogController> createParameterSetDialog() throws IOException {
        URL datePickerUrl = SetParameterDialogUtil.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/SetParameterDialog.fxml");
        FXMLLoader loader = new FXMLLoader(datePickerUrl);
        VBox root = loader.load();
        CssHandler.applyTo(root);
        SetParameterDialogController controller = loader.getController();
        return Pair.of(root, controller);
    }

    public static Pair<Node, SetParameterDialogController> createParameterSetDialog(ParameterDescriptor descriptor, SetParameterRequest request, Supplier<List<ActivityRouteState>> routeList) throws IOException {
        Pair<Node, SetParameterDialogController> asBuilt = createParameterSetDialog();
        asBuilt.getSecond().initialiseParameterDialog(descriptor, request, routeList);
        return asBuilt;
    }
}
