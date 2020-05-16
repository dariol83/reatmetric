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

import eu.dariolucia.ccsds.encdec.pus.TcPusHeader;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServiceBroker {

    private static final Logger LOG = Logger.getLogger(ServiceBroker.class.getName());

    private final ExecutorService itemDistributor = Executors.newSingleThreadExecutor(r -> {
       Thread t = new Thread(r, "Service Broker Distributor");
       t.setDaemon(true);
       return t;
    });
    private final List<Pair<IServicePacketSubscriber, IServicePacketFilter>> subscribers = new CopyOnWriteArrayList<>();

    public void register(IServicePacketSubscriber subscriber, IServicePacketFilter predicateFilter) {
        this.subscribers.add(Pair.of(subscriber, predicateFilter));
    }

    public void deregister(IServicePacketSubscriber subscriber) {
        List<Pair<IServicePacketSubscriber, IServicePacketFilter>> itemsToRemove = subscribers.stream().filter(o -> o.getFirst() == subscriber).collect(Collectors.toList());
        subscribers.removeAll(itemsToRemove);
    }

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
    }

    public void informTcPacketEncoded(RawData rd, SpacePacket sp, TcPusHeader header, TcTracker trackerBean) {
        itemDistributor.execute(() -> {
            for(Pair<IServicePacketSubscriber, IServicePacketFilter> s : subscribers) {
                try {
                    if(s.getSecond().filter(rd, sp, header != null ? Integer.valueOf(header.getServiceType()) : null,  header != null ? Integer.valueOf(header.getServiceSubType()) : null, null, header != null ? header.getSourceId() : null)) {
                        s.getFirst().onTcPacketEncoded(rd, sp, header, trackerBean);
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot notify packet service subscriber " + subscribers + ": " + e.getMessage(), e);
                }
            }
        });
    }
}
