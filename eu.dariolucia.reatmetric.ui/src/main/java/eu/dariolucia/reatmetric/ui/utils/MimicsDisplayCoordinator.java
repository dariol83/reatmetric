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

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.controller.MimicsDisplayTabWidgetController;
import eu.dariolucia.reatmetric.ui.controller.UserDisplayTabWidgetController;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is used to have a single distribution thread for all the opened chart views.
 */
public class MimicsDisplayCoordinator implements IReatmetricServiceListener {

    private static MimicsDisplayCoordinator INSTANCE;

    public synchronized static MimicsDisplayCoordinator instance() {
        if(INSTANCE == null) {
            INSTANCE = new MimicsDisplayCoordinator();
        }
        return INSTANCE;
    }

    private final List<MimicsDisplayTabWidgetController> registeredViews = new CopyOnWriteArrayList<>();

    public MimicsDisplayCoordinator() {
        ReatmetricUI.selectedSystem().addSubscriber(this);
    }

    @Override
    public void startGlobalOperationProgress() {
        // Nothing
    }

    @Override
    public void stopGlobalOperationProgress() {
        // Nothing
    }

    @Override
    public void systemConnected(IReatmetricSystem system) {
        registeredViews.forEach(o -> o.systemConnected(system));
    }

    @Override
    public void systemDisconnected(IReatmetricSystem system) {
        registeredViews.forEach(o -> o.systemDisconnected(system));
        registeredViews.clear();
    }

    @Override
    public void systemStatusUpdate(SystemStatus status) {
        // Nothing
    }

    public void register(MimicsDisplayTabWidgetController display) {
        this.registeredViews.add(display);
    }

    public void deregister(MimicsDisplayTabWidgetController display) {
        this.registeredViews.remove(display);
    }
}
