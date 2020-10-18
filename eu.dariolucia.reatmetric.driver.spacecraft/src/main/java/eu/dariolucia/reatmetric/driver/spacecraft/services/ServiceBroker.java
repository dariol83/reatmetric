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

package eu.dariolucia.reatmetric.driver.spacecraft.services;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServiceBroker implements IServiceBroker {

    private static final Logger LOG = Logger.getLogger(ServiceBroker.class.getName());

    private final ExecutorService itemDistributor = Executors.newSingleThreadExecutor(r -> {
       Thread t = new Thread(r, "Service Broker Distributor");
       t.setDaemon(true);
       return t;
    });

    private final List<Pair<IServicePacketSubscriber, IServicePacketFilter>> subscribers = new CopyOnWriteArrayList<>();

    private final Map<Integer, IService> serviceMap = new HashMap<>();
    private final Map<Class, Object> serviceLocator = new HashMap<>();

    @Override
    public void register(IServicePacketSubscriber subscriber, IServicePacketFilter predicateFilter) {
        this.subscribers.add(Pair.of(subscriber, predicateFilter));
    }

    @Override
    public void deregister(IServicePacketSubscriber subscriber) {
        List<Pair<IServicePacketSubscriber, IServicePacketFilter>> itemsToRemove = subscribers.stream().filter(o -> o.getFirst() == subscriber).collect(Collectors.toList());
        subscribers.removeAll(itemsToRemove);
    }

    @Override
    public void distributeTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        final Integer pusType = tmPusHeader != null ? (int) tmPusHeader.getServiceType() : null;
        final Integer pusSubType = tmPusHeader != null ? (int) tmPusHeader.getServiceSubType() : null;
        final Integer destinationId = tmPusHeader != null ? tmPusHeader.getDestinationId() : null;
        final Integer sourceId = null;

        itemDistributor.execute(() -> {
            for(Pair<IServicePacketSubscriber, IServicePacketFilter> s : subscribers) {
                try {
                    if(s.getSecond().filter(packetRawData, spacePacket, pusType, pusSubType, destinationId, sourceId)) {
                        s.getFirst().onTmPacket(packetRawData, spacePacket, tmPusHeader, decoded);
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot notify packet service subscriber " + subscribers + ": " + e.getMessage(), e);
                }
            }
        });
    }

    public void dispose() {
        itemDistributor.shutdownNow();
        subscribers.clear();
        serviceMap.values().forEach(IService::dispose);
        serviceMap.clear();
    }

    @Override
    public void informTcPacket(TcPhase phase, Instant phaseTime, TcTracker trackerBean) {
        itemDistributor.execute(() -> {
            for(Pair<IServicePacketSubscriber, IServicePacketFilter> s : subscribers) {
                try {
                    if(s.getSecond().filter(trackerBean.getRawData(), trackerBean.getPacket(), trackerBean.getInfo().getPusHeader() != null ? Integer.valueOf(trackerBean.getInfo().getPusHeader().getServiceType()) : null,  trackerBean.getInfo().getPusHeader() != null ? Integer.valueOf(trackerBean.getInfo().getPusHeader().getServiceSubType()) : null, null, trackerBean.getInfo().getPusHeader() != null ? trackerBean.getInfo().getPusHeader().getSourceId() : null)) {
                        s.getFirst().onTcPacket(phase, phaseTime, trackerBean);
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot notify packet service subscriber " + s + ": " + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public <T> T locate(Class<T> interfaceClass) {
        T service = (T) serviceLocator.get(interfaceClass);
        if(service == null) {
            for (IService s : serviceMap.values()) {
                if (interfaceClass.isAssignableFrom(s.getClass())) {
                    serviceLocator.put(interfaceClass, s);
                    service = (T) s;
                }
            }
        }
        return service;
    }

    @Override
    public boolean isDirectlyHandled(TcTracker tcTracker) {
        // Ask all services
        return this.serviceMap.values().stream().anyMatch(o -> o.isDirectHandler(tcTracker));
    }

    public <T> void registerServiceInterface(Class<T> locator, T theService) {
        serviceLocator.put(locator, theService);
    }

    public void registerService(IService theService) {
        register(theService, theService.getSubscriptionFilter());
        serviceMap.put(theService.getServiceType(), theService);
    }

    public Map<String, Function<RawData, LinkedHashMap<String, String>>> getServiceRenderers() {
        Map<String, Function<RawData, LinkedHashMap<String, String>>> serviceRenderers = new HashMap<>();
        for(IService s : serviceMap.values()) {
            s.registerRawDataRenderers(serviceRenderers);
        }
        return serviceRenderers;
    }

    public void finaliseServiceLoading() {
        serviceMap.values().forEach(IService::finaliseServiceLoading);
    }
}
