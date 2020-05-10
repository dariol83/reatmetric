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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.ui.controller.ActivityInvocationDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

public class ActivityInvocationDialogUtil {

    public static Pair<Node, ActivityInvocationDialogController> createActivityInvocationDialog() throws IOException {
        URL datePickerUrl = ActivityInvocationDialogUtil.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/ActivityInvocationDialog.fxml");
        FXMLLoader loader = new FXMLLoader(datePickerUrl);
        VBox root = loader.load();
        ActivityInvocationDialogController controller = loader.getController();
        return Pair.of(root, controller);
    }

    public static Pair<Node, ActivityInvocationDialogController> createActivityInvocationDialog(ActivityDescriptor descriptor, ActivityRequest request, Supplier<List<ActivityRouteState>> routeList) throws IOException {
        Pair<Node, ActivityInvocationDialogController> asBuilt = createActivityInvocationDialog();
        asBuilt.getSecond().initialiseActivityDialog(descriptor, request, routeList);
        return asBuilt;
    }
}
