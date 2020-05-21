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

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.ccsds.tmtc.coding.ChannelEncoder;
import eu.dariolucia.ccsds.tmtc.coding.encoder.CltuEncoder;
import eu.dariolucia.ccsds.tmtc.coding.encoder.CltuRandomizerEncoder;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AbstractSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.IVirtualChannelSenderOutput;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.TcSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.CltuServiceInstanceManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TcDataLinkProcessor implements IRawDataSubscriber, IVirtualChannelSenderOutput {

    private final Map<String, CltuServiceInstanceManager> cltuSenders;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final AtomicLong cltuSequencer = new AtomicLong();

    private final TcSenderVirtualChannel[] tcChannels;
    private final ChannelEncoder<TcTransferFrame> encoder;

    private final List<TcTracker> pendingTcPackets = new LinkedList<>();
    private final List<TcTransferFrame> lastGeneratedFrames = new LinkedList<>();

    private volatile boolean useAdMode;
    // TODO introduce command to change mode (AD-BD)

    public TcDataLinkProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, List<CltuServiceInstanceManager> cltuSenders) {
        this.cltuSenders = new TreeMap<>();
        for(CltuServiceInstanceManager m : cltuSenders) {
            this.cltuSenders.put(m.getServiceInstanceIdentifier(), m);
            m.register(this::informTcFrameStatus); // TODO deregister on dispose()
        }
        this.configuration = configuration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        this.useAdMode = configuration.getTcDataLinkConfiguration().isAdModeDefault();
        // Create the CLTU encoder
        this.encoder = ChannelEncoder.create();
        if(configuration.getTcDataLinkConfiguration().isRandomize()) {
            this.encoder.addEncodingFunction(new CltuRandomizerEncoder<>());
        }
        this.encoder.addEncodingFunction(new CltuEncoder<>()).configure();
        // Allocate the TC channels
        this.tcChannels = new TcSenderVirtualChannel[8];
        for(int i = 0; i < tcChannels.length; ++i) {
            // TODO segmentation should be changed per invocation in ccsds.tmtc
            tcChannels[i] = new TcSenderVirtualChannel(configuration.getId(), i, VirtualChannelAccessMode.PACKET, configuration.getTcDataLinkConfiguration().isFecf(), configuration.getTcDataLinkConfiguration().isSegmentation());
            tcChannels[i].register(this); // TODO deregister on dispose()
        }
        // Register for frames to the raw data broker
        this.context.getRawDataBroker().subscribe(this, null,
                new RawDataFilter(true, null, null,
                        Arrays.asList(Constants.T_AOS_FRAME, Constants.T_TM_FRAME),
                        Collections.singletonList(String.valueOf(configuration.getId())),
                        Collections.singletonList(Quality.GOOD)), null); // TODO deregister on dispose()
        // TODO register to the cltu senders
    }

    private void informTcFrameStatus(Long id, CltuServiceInstanceManager.CltuProcessingStatus status) {
        // TODO
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        // TODO: this is needed for frame reception (COP-1)
    }

    public synchronized void sendTcPacket(SpacePacket sp, TcTracker tcTracker) {
        // overridden TC VC ID
        int tcVcId = configuration.getTcDataLinkConfiguration().getTcVc();
        if(tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_OVERRIDE_TCVC_ID)) {
            tcVcId = Integer.parseInt(tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_TCVC_ID));
        }

        // TODO: overridden segmentation - missing support in ccsds.tmtc
        boolean useSegmentation = configuration.getTcDataLinkConfiguration().isSegmentation();
        if(tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_OVERRIDE_TC_SEGMENT)) {
            useSegmentation = Boolean.parseBoolean(tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_TC_SEGMENT));
        }
        int map = tcTracker.getInfo().isMapUsed() ? tcTracker.getInfo().getMap() : -1; // This means segmentation is needed, overridden map already taken into account

        // overriden mode (AD or BD)
        boolean useAd = this.useAdMode;
        if(tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_OVERRIDE_USE_AD_FRAME)) {
            useAd = Boolean.parseBoolean(tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_USE_AD_FRAME));
        }

        // TODO: this approach assumes 1 frame for 1 tc packet, but you have to support more tc packets in a single frame (use group properties) as well as more
        //  frames for a single large tc packet.
        pendingTcPackets.clear();
        pendingTcPackets.add(tcTracker);
        lastGeneratedFrames.clear();
        tcChannels[tcVcId].dispatch(useAd, map, sp);
        // Now you have the generated frames, encode them and send them
        String route = tcTracker.getInvocation().getRoute();
        CltuServiceInstanceManager serviceInstance = this.cltuSenders. get(route);
        for(TcTransferFrame frame : lastGeneratedFrames) {
            byte[] encodedCltu = encoder.apply(frame);
            long frameInTransmissionId = this.cltuSequencer.incrementAndGet();
            // TODO: add to map
            serviceInstance.sendCltu(encodedCltu, frameInTransmissionId, Collections.singletonList(tcTracker));
        }
    }

    public void dispose() {
        // TODO
    }

    @Override
    public void transferFrameGenerated(AbstractSenderVirtualChannel vc, AbstractTransferFrame generatedFrame, int bufferedBytes) {
        lastGeneratedFrames.add((TcTransferFrame) generatedFrame);
    }
}
