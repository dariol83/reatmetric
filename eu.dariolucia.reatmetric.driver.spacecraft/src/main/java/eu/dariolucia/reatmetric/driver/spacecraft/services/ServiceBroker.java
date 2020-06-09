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
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.DriverConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.ServiceConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
                    LOG.log(Level.SEVERE, "Cannot notify packet service subscriber " + subscribers + ": " + e.getMessage(), e);
                }
            }
        });
    }

    public void registerService(IService theService) {
        register(theService, theService.getSubscriptionFilter());
        serviceMap.put(theService.getServiceType(), theService);
    }
}
