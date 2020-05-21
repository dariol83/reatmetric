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
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPacketPhase;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.CltuServiceInstanceManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcDataLinkProcessor implements IRawDataSubscriber, IVirtualChannelSenderOutput<TcTransferFrame>, CltuServiceInstanceManager.ICltuStatusSubscriber {

    private static final Logger LOG = Logger.getLogger(TcDataLinkProcessor.class.getName());

    private final Map<String, CltuServiceInstanceManager> cltuSenders;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final AtomicLong cltuSequencer = new AtomicLong();

    private final TcSenderVirtualChannel[] tcChannels;
    private final ChannelEncoder<TcTransferFrame> encoder;

    private final List<TcTracker> pendingTcPackets = new LinkedList<>();
    private final List<TcTransferFrame> lastGeneratedFrames = new LinkedList<>();

    private final Map<Long, RequestTracker> cltuId2requestTracker = new ConcurrentHashMap<>();

    private final Map<String, List<TcTracker>> pendingGroupTcs = new HashMap<>();

    private volatile boolean useAdMode;

    public TcDataLinkProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, List<CltuServiceInstanceManager> cltuSenders) {
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
            tcChannels[i].register(this);
        }
        // Register for frames to the raw data broker
        this.context.getRawDataBroker().subscribe(this, null,
                new RawDataFilter(true, null, null,
                        Arrays.asList(Constants.T_AOS_FRAME, Constants.T_TM_FRAME),
                        Collections.singletonList(String.valueOf(configuration.getId())),
                        Collections.singletonList(Quality.GOOD)), null);
        // Register to the cltu senders
        this.cltuSenders = new TreeMap<>();
        for(CltuServiceInstanceManager m : cltuSenders) {
            this.cltuSenders.put(m.getServiceInstanceIdentifier(), m);
            m.register(this);
        }
    }

    public void setAdMode(boolean useAdMode) {
        this.useAdMode = useAdMode;
    }

    @Override
    public void informStatusUpdate(long id, CltuServiceInstanceManager.CltuProcessingStatus status, Instant time) {
        RequestTracker tracker = this.cltuId2requestTracker.get(id);
        if(tracker != null) {
            tracker.trackCltuStatus(id, status, time);
            if(tracker.isLifecycleCompleted()) {
                this.cltuId2requestTracker.remove(id);
            }
        } else {
            LOG.log(Level.WARNING, "Reported CLTU ID " + id + " not found in the CLTU tracking map");
        }
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        // TODO: this is needed for frame reception (COP-1)
    }

    public void sendTcPacket(SpacePacket sp, TcTracker tcTracker) {
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

        // TODO: this approach assumes 1 frame for 1 tc packet, but you have to support more tc packets in a single frame (use group properties -> multiple different TcTrackers) as well as more
        //  frames for a single large tc packet.
        pendingTcPackets.clear();
        lastGeneratedFrames.clear();

        pendingTcPackets.add(tcTracker);
        tcChannels[tcVcId].dispatch(useAd, map, sp);
        // Now you have the generated frames, prepare for tracking them, encode them and send them

        // Retrieve the route and hence the service instance to use
        String route = tcTracker.getInvocation().getRoute();
        CltuServiceInstanceManager serviceInstance = this.cltuSenders.get(route);
        // Create the request tracker
        RequestTracker tracker = new RequestTracker(useAd);
        // Encode the TC frames and remember them
        List<Pair<Long, byte[]>> toSend = new LinkedList<>();
        for(TcTransferFrame frame : lastGeneratedFrames) {
            byte[] encodedCltu = encoder.apply(frame);
            long frameInTransmissionId = this.cltuSequencer.incrementAndGet();
            cltuId2requestTracker.put(frameInTransmissionId, tracker);
            toSend.add(Pair.of(frameInTransmissionId, encodedCltu));
        }
        // Initialise the tracker
        tracker.initialise(pendingTcPackets, toSend);
        // Send the CLTUs
        for(Pair<Long, byte[]> p : toSend) {
            serviceInstance.sendCltu(p.getSecond(), p.getFirst());
        }
    }

    public void dispose() {
        for(CltuServiceInstanceManager m : cltuSenders.values()) {
            m.deregister(this);
        }
        this.context.getRawDataBroker().unsubscribe(this);
        for (TcSenderVirtualChannel tcChannel : tcChannels) {
            tcChannel.deregister(this);
        }
    }

    @Override
    public void transferFrameGenerated(AbstractSenderVirtualChannel vc, TcTransferFrame generatedFrame, int bufferedBytes) {
        lastGeneratedFrames.add(generatedFrame);
    }

    private void informServiceBroker(TcPacketPhase phase, Instant time, List<TcTracker> trackers) {
        for (TcTracker tracker : trackers) {
            serviceBroker.informTcPacket(phase, time, tracker);
        }
    }

    private void reportActivityState(List<TcTracker> trackers, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        for (TcTracker tracker : trackers) {
            context.getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, null, status, nextState, null));
        }
    }

    private class RequestTracker {
        private final List<TcTracker> tcTrackers = new LinkedList<>();
        private final Map<Long, byte[]> cltus = new HashMap<>();
        private final boolean useAd;
        private volatile boolean lifecycleCompleted;

        private final Set<Long> released = new HashSet<>();
        private final Set<Long> accepted = new HashSet<>();
        private final Set<Long> uplinked = new HashSet<>();

        public RequestTracker(boolean useAd) {
           this.useAd = useAd;
        }

        public void initialise(List<TcTracker> tcTrackers, List<Pair<Long, byte[]>> cltus) {
            this.tcTrackers.addAll(tcTrackers);
            for(Pair<Long, byte[]> cltu : cltus) {
                this.cltus.put(cltu.getFirst(), cltu.getSecond());
            }
        }

        public void trackCltuStatus(Long id, CltuServiceInstanceManager.CltuProcessingStatus status, Instant time) {
            //
            switch (status) {
                case RELEASED: { // The CLTU was sent to the ground station
                    released.add(id);
                    if(released.size() == cltus.size()) { // All released
                        informServiceBroker(TcPacketPhase.RELEASED, time, tcTrackers);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                    }
                }
                break;
                case RELEASED_FAILED: { // Release problem
                    informServiceBroker(TcPacketPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE);
                    lifecycleCompleted = true;
                }
                break;
                case ACCEPTED: { // The CLTU was accepted by the ground station
                    accepted.add(id);
                    if(accepted.size() == cltus.size()) { // All CLTUs accepted, so command is all at the ground station
                        // Nothing to be done here with the service broker
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                    }
                }
                break;
                case REJECTED: { // The CLTU was rejected by the ground station or discarded -> all related TC requests to be marked as failed in ground station reception
                    informServiceBroker(TcPacketPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
                    lifecycleCompleted = true;
                }
                break;
                case UPLINKED: { // The CLTU was uplinked by the ground station
                    uplinked.add(id);
                    if(uplinked.size() == cltus.size()) { // All CLTUs uplinked, so command is all on its way
                        informServiceBroker(TcPacketPhase.UPLINKED, time, tcTrackers);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        // TODO: If not AD, then either SCHEDULED or EXECUTION - Open next stage?
                    }
                    if(!useAd) {
                        lifecycleCompleted = true;
                    }
                }
                break;
                case FAILED: { // The CLTU failed uplink -> all related TC requests to be marked as failed in ground station uplink
                    informServiceBroker(TcPacketPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
                    lifecycleCompleted = true;
                }
                break;
            }
        }

        public boolean isLifecycleCompleted() {
            return lifecycleCompleted;
        }
    }
}
