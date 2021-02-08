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
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SystemEntityDescriptorPanelController implements Initializable {

    public VBox vboxParent;
    public Label pathLabel;
    public Label externalIdLabel;
    public StackPane stackPane;
    public VBox parameterVBox;
    public Label parameterDescriptionLabel;
    public Label rawDataTypeLabel;
    public Label engDataTypeLabel;
    public Label unitLabel;
    public Label parameterTypeLabel;
    public VBox eventVBox;
    public Label eventDescriptionLabel;
    public Label severityLabel;
    public Label eventTypeLabel;
    public Label conditionLabel;
    public VBox activityVBox;
    public Label activityDescriptionLabel;
    public Label routeLabel;
    public Label activityTypeLabel;
    public Label expectedDurationLabel;

    private final Map<Class<? extends AbstractSystemEntityDescriptor>, Consumer<AbstractSystemEntityDescriptor>> handlers = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        handlers.put(ParameterDescriptor.class, this::handleParameterDescriptor);
        handlers.put(EventDescriptor.class, this::handleEventDescriptor);
        handlers.put(ActivityDescriptor.class, this::handleActivityDescriptor);
    }

    public void handle(SystemEntityPath path, AbstractSystemEntityDescriptor descriptor) {
        if(descriptor != null) {
            Consumer<AbstractSystemEntityDescriptor> handler = handlers.get(descriptor.getClass());
            handler.accept(descriptor);
        } else {
            handleContainer(path);
        }
    }

    private void handleContainer(SystemEntityPath path) {
        parameterVBox.setVisible(false);
        eventVBox.setVisible(false);
        activityVBox.setVisible(false);
        pathLabel.setText(path.asString());
        externalIdLabel.setText("---");
    }

    private void handleParameterDescriptor(AbstractSystemEntityDescriptor descriptor) {
        ParameterDescriptor pd = (ParameterDescriptor) descriptor;
        parameterVBox.setVisible(true);
        eventVBox.setVisible(false);
        activityVBox.setVisible(false);
        pathLabel.setText(descriptor.getPath().asString());
        externalIdLabel.setText(String.valueOf(pd.getExternalId()));
        parameterDescriptionLabel.setText(pd.getDescription());
        rawDataTypeLabel.setText(pd.getRawDataType().asBeautyString());
        engDataTypeLabel.setText(pd.getEngineeringDataType().asBeautyString());
        unitLabel.setText(pd.getUnit());
        parameterTypeLabel.setText(pd.isSynthetic() ? "Synthetic" : "Reported");
    }

    private void handleEventDescriptor(AbstractSystemEntityDescriptor descriptor) {
        EventDescriptor pd = (EventDescriptor) descriptor;
        parameterVBox.setVisible(false);
        eventVBox.setVisible(true);
        activityVBox.setVisible(false);
        pathLabel.setText(descriptor.getPath().asString());
        externalIdLabel.setText(String.valueOf(pd.getExternalId()));
        eventDescriptionLabel.setText(pd.getDescription());
        severityLabel.setText(pd.getSeverity().name());
        eventTypeLabel.setText(pd.getEventType());
        conditionLabel.setText(pd.isConditionDriven() ? "Yes" : "No");
    }

    private void handleActivityDescriptor(AbstractSystemEntityDescriptor descriptor) {
        ActivityDescriptor pd = (ActivityDescriptor) descriptor;
        parameterVBox.setVisible(false);
        eventVBox.setVisible(false);
        activityVBox.setVisible(true);
        pathLabel.setText(descriptor.getPath().asString());
        externalIdLabel.setText(String.valueOf(pd.getExternalId()));
        activityDescriptionLabel.setText(pd.getDescription());
        routeLabel.setText(pd.getDefaultRoute());
        activityTypeLabel.setText(pd.getActivityType());
        expectedDurationLabel.setText(String.format("%d:%02d:%02d", pd.getExpectedDuration().getSeconds()/3600,
                (pd.getExpectedDuration().getSeconds() % 3600) / 60,
                pd.getExpectedDuration().getSeconds() % 60));
    }

    public void reset() {
        parameterVBox.setVisible(false);
        eventVBox.setVisible(false);
        activityVBox.setVisible(false);
        pathLabel.setText("---");
        externalIdLabel.setText("---");
    }
}
