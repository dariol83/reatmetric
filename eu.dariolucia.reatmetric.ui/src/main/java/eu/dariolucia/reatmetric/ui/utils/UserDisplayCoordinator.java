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
import eu.dariolucia.reatmetric.ui.controller.UserDisplayTabWidgetController;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is used to have a single distribution thread for all the opened chart views.
 */
public class UserDisplayCoordinator implements IReatmetricServiceListener {

    private static UserDisplayCoordinator INSTANCE;

    public synchronized static UserDisplayCoordinator instance() {
        if(INSTANCE == null) {
            INSTANCE = new UserDisplayCoordinator();
        }
        return INSTANCE;
    }

    private final DataProcessingDelegator<ParameterData> parameterDelegator;
    private final DataProcessingDelegator<EventData> eventDelegator;

    private IParameterDataSubscriber parameterSubscriber;
    private IEventDataSubscriber eventSubscriber;

    private final List<UserDisplayTabWidgetController> registeredViews = new CopyOnWriteArrayList<>();

    public UserDisplayCoordinator() {
        this.parameterDelegator = new DataProcessingDelegator<>(getClass().getSimpleName(), buildIncomingParameterDataDelegatorAction());
        this.eventDelegator = new DataProcessingDelegator<>(getClass().getSimpleName(), buildIncomingEventDataDelegatorAction());
        this.parameterSubscriber = parameterDelegator::delegate;
        this.eventSubscriber = eventDelegator::delegate;

        ReatmetricUI.selectedSystem().addSubscriber(this);
    }

    private Consumer<List<ParameterData>> buildIncomingParameterDataDelegatorAction() {
        return this::forwardParameterDataItems;
    }

    private Consumer<List<EventData>> buildIncomingEventDataDelegatorAction() {
        return this::forwardEventDataItems;
    }

    private void forwardEventDataItems(List<EventData> t) {
        // Build forward map
        final Map<UserDisplayTabWidgetController, List<EventData>> forwardMap = new LinkedHashMap<>();
        for(UserDisplayTabWidgetController c : registeredViews) {
            if(c.isLive()) {
                EventDataFilter edf = c.getCurrentEventFilter();
                List<EventData> toForward = t.stream().filter(edf).collect(Collectors.toList());
                if(!toForward.isEmpty()) {
                    forwardMap.put(c, toForward);
                }
            }
        }
        // Forward
        Platform.runLater(() -> {
            for(Map.Entry<UserDisplayTabWidgetController, List<EventData>> entry : forwardMap.entrySet()) {
                entry.getKey().updateDataItems(entry.getValue());
            }
        });
    }

    private void forwardParameterDataItems(List<ParameterData> t) {
        // Build forward map
        final Map<UserDisplayTabWidgetController, List<ParameterData>> forwardMap = new LinkedHashMap<>();
        for(UserDisplayTabWidgetController c : registeredViews) {
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
            for(Map.Entry<UserDisplayTabWidgetController, List<ParameterData>> entry : forwardMap.entrySet()) {
                entry.getKey().updateDataItems(entry.getValue());
            }
        });
    }

    private boolean mustSubscribe(ParameterDataFilter globalFilter, EventDataFilter globalEventFilter) {
        return (!globalFilter.getParameterPathList().isEmpty() || !globalEventFilter.getEventPathList().isEmpty()) && this.registeredViews.size() > 0;
    }

    private void startSubscription(ParameterDataFilter currentParameterFilter, EventDataFilter currentEventFilter) {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this.parameterSubscriber, currentParameterFilter);
                ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().subscribe(this.eventSubscriber, currentEventFilter);
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private ParameterDataFilter buildParameterFilter() {
        Set<SystemEntityPath> params = new TreeSet<>();
        for(UserDisplayTabWidgetController c : registeredViews) {
            params.addAll(c.getCurrentParameterFilter().getParameterPathList());
        }
        return new ParameterDataFilter(null, new ArrayList<>(params),null,null,null, null);
    }

    private EventDataFilter buildEventFilter() {
        Set<SystemEntityPath> params = new TreeSet<>();
        for(UserDisplayTabWidgetController c : registeredViews) {
            params.addAll(c.getCurrentEventFilter().getEventPathList());
        }
        return new EventDataFilter(null, new ArrayList<>(params),null, null,null,null, null);
    }

    private void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                IReatmetricSystem service = ReatmetricUI.selectedSystem().getSystem();
                if(service != null && service.getParameterDataMonitorService() != null) {
                    ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this.parameterSubscriber);
                }
                if(service != null && service.getEventDataMonitorService() != null) {
                    ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().unsubscribe(this.eventSubscriber);
                }
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    public void filterUpdated() {
        ParameterDataFilter globalParameterFilter = buildParameterFilter();
        EventDataFilter globalEventFilter = buildEventFilter();
        if(globalParameterFilter.getParameterPathList().isEmpty() && globalEventFilter.getEventPathList().isEmpty()) {
            stopSubscription();
        } else {
            startSubscription(globalParameterFilter, globalEventFilter);
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
        EventDataFilter globalEventFilter = buildEventFilter();
        if(mustSubscribe(globalFilter, globalEventFilter)) {
            startSubscription(globalFilter, globalEventFilter);
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

    public void register(UserDisplayTabWidgetController display) {
        this.registeredViews.add(display);
    }

    public void deregister(UserDisplayTabWidgetController display) {
        this.registeredViews.remove(display);
    }
}
