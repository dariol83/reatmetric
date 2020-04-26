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
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.controller.ParameterDisplayTabWidgetController;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is used to have a single distribution thread for all the opened AND views.
 */
public class ParameterDisplayCoordinator implements IReatmetricServiceListener {

    private static ParameterDisplayCoordinator INSTANCE;

    public synchronized static ParameterDisplayCoordinator instance() {
        if(INSTANCE == null) {
            INSTANCE = new ParameterDisplayCoordinator();
        }
        return INSTANCE;
    }

    private final DataProcessingDelegator<ParameterData> parameterDelegator;

    private IParameterDataSubscriber parameterSubscriber;

    private final List<ParameterDisplayTabWidgetController> registeredViews = new CopyOnWriteArrayList<>();

    public ParameterDisplayCoordinator() {
        this.parameterDelegator = new DataProcessingDelegator<>(getClass().getSimpleName(), buildIncomingParameterDataDelegatorAction());
        this.parameterSubscriber = parameterDelegator::delegate;

        ReatmetricUI.selectedSystem().addSubscriber(this);
    }

    private Consumer<List<ParameterData>> buildIncomingParameterDataDelegatorAction() {
        return this::forwardParameterDataItems;
    }

    private void forwardParameterDataItems(List<ParameterData> t) {
        // Build forward map
        final Map<ParameterDisplayTabWidgetController, List<ParameterData>> forwardMap = new LinkedHashMap<>();
        for(ParameterDisplayTabWidgetController c : registeredViews) {
            if(c.isLive()) {
                ParameterDataFilter pdf = c.getCurrentParameterFilter();
                List<ParameterData> toForward = t.stream().filter(pdf).collect(Collectors.toList());
                if(!toForward.isEmpty()) {
                    forwardMap.put(c, toForward);
                }
            }
        }
        // Forward
        Platform.runLater(() -> {
            for(Map.Entry<ParameterDisplayTabWidgetController, List<ParameterData>> entry : forwardMap.entrySet()) {
                entry.getKey().updateDataItems(entry.getValue());
            }
        });
    }

    private boolean mustSubscribe(ParameterDataFilter globalFilter) {
        return !globalFilter.getParameterPathList().isEmpty() && this.registeredViews.size() > 0;
    }

    private void startSubscription(ParameterDataFilter currentParameterFilter) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this.parameterSubscriber, currentParameterFilter);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private ParameterDataFilter buildParameterFilter() {
        Set<SystemEntityPath> params = new TreeSet<>();
        for(ParameterDisplayTabWidgetController c : registeredViews) {
            params.addAll(c.getCurrentParameterFilter().getParameterPathList());
        }
        return new ParameterDataFilter(null, new ArrayList<>(params),null,null,null, null);
    }

    private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                IReatmetricSystem service = ReatmetricUI.selectedSystem().getSystem();
                if(service != null && service.getParameterDataMonitorService() != null) {
                    ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this.parameterSubscriber);
                }
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    public void filterUpdated() {
        ParameterDataFilter globalParameterFilter = buildParameterFilter();
        if(globalParameterFilter.getParameterPathList().isEmpty()) {
            stopSubscription();
        } else {
            startSubscription(globalParameterFilter);
        }
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
        ParameterDataFilter globalFilter = buildParameterFilter();
        if(mustSubscribe(globalFilter)) {
            startSubscription(globalFilter);
        }
    }

    @Override
    public void systemDisconnected(IReatmetricSystem system) {
        stopSubscription();
        registeredViews.forEach(o -> o.systemDisconnected(system));
        registeredViews.clear();
    }

    @Override
    public void systemStatusUpdate(SystemStatus status) {
        // Nothing
    }

    public void register(ParameterDisplayTabWidgetController display) {
        this.registeredViews.add(display);
    }

    public void deregister(ParameterDisplayTabWidgetController display) {
        this.registeredViews.remove(display);
    }
}
