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
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IActivityExecutor;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TcVcConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPacketPhase;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.CltuServiceInstanceManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TcDataLinkProcessor implements IRawDataSubscriber, IVirtualChannelSenderOutput<TcTransferFrame>, CltuServiceInstanceManager.ICltuStatusSubscriber, IActivityExecutor {

    private static final Logger LOG = Logger.getLogger(TcDataLinkProcessor.class.getName());

    private final Map<String, CltuServiceInstanceManager> cltuSenders;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final AtomicLong cltuSequencer = new AtomicLong();

    private final Pair<TcVcConfiguration, TcSenderVirtualChannel>[] tcChannels;
    private final int defaultTcVcId;
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
        this.tcChannels = new Pair[8];
        for(int i = 0; i < tcChannels.length; ++i) {
            TcVcConfiguration tcConf = getTcVcConfiguration(i, configuration.getTcDataLinkConfiguration().getTcVcDescriptors());
            if(tcConf != null) {
                tcChannels[i] = Pair.of(tcConf, new TcSenderVirtualChannel(configuration.getId(), i, VirtualChannelAccessMode.PACKET, configuration.getTcDataLinkConfiguration().isFecf(), tcConf.isSegmentation()));
                tcChannels[i].getSecond().register(this);
            }
        }
        this.defaultTcVcId = configuration.getTcDataLinkConfiguration().getDefaultTcVc();
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

    private TcVcConfiguration getTcVcConfiguration(int tcVcId, List<TcVcConfiguration> tcVcDescriptors) {
        for(TcVcConfiguration vcc : tcVcDescriptors) {
            if(tcVcId == vcc.getTcVc()) {
                return vcc;
            }
        }
        return null;
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

    public void sendTcPacket(SpacePacket sp, TcTracker tcTracker) throws ActivityHandlingException {
        LOG.log(Level.INFO, "TC packet with APID (" + sp.getApid() + ") for activity " + tcTracker.getInvocation().getPath() + " encoded: " + StringUtil.toHexDump(sp.getPacket()));
        try {
            // Overridden TC VC ID
            int tcVcId = defaultTcVcId;
            if (tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_OVERRIDE_TCVC_ID)) {
                tcVcId = Integer.parseInt(tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_TCVC_ID));
            }

            // Look for TC VC ID
            Pair<TcVcConfiguration, TcSenderVirtualChannel> vcToUse = tcChannels[tcVcId];
            if (vcToUse == null) {
                LOG.log(Level.SEVERE, "Transmission of space packet from activity " + tcTracker.getInvocation().getPath() + " on TC VC " + tcVcId + " not possible: TC VC " + tcVcId + " not configured");
                Instant t = Instant.now();
                informServiceBroker(TcPacketPhase.FAILED, t, Collections.singletonList(tcTracker));
                reportActivityState(Collections.singletonList(tcTracker), t, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE);
                return;
            }

            // Map ID: only used if segmentation is used
            int map = tcTracker.getInfo().isMapUsed() ? tcTracker.getInfo().getMap() : vcToUse.getFirst().getMapId(); // This means segmentation is needed, overridden map already taken into account

            // Overriden mode (AD or BD)
            boolean useAd = this.useAdMode;
            if (tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_OVERRIDE_USE_AD_FRAME)) {
                useAd = Boolean.parseBoolean(tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_USE_AD_FRAME));
            }

            // Check if this command is part of a group command: a group command is a sequence of TCs that is encoded and sent in a single TC frame
            String groupName = tcTracker.getInvocation().getProperties().getOrDefault(Constants.ACTIVITY_PROPERTY_TC_GROUP_NAME, null);
            if (groupName != null) {
                // Retrieve the group if it exists, or create a new one
                List<TcTracker> groupList = this.pendingGroupTcs.computeIfAbsent(groupName, o -> new LinkedList<>());
                // Add the TC
                groupList.add(tcTracker);
                // If this is the last TC in the group, send the group
                String transmit = tcTracker.getInvocation().getProperties().getOrDefault(Constants.ACTIVITY_PROPERTY_TC_GROUP_TRANSMIT, Boolean.FALSE.toString());
                if (transmit.equals(Boolean.TRUE.toString())) {
                    this.pendingGroupTcs.remove(groupName);
                    lastGeneratedFrames.clear();
                    pendingTcPackets.clear();
                    pendingTcPackets.addAll(groupList);
                    vcToUse.getSecond().dispatch(useAd, map, groupList.stream().map(TcTracker::getPacket).collect(Collectors.toList()));
                    // Now lastGeneratedFrames will contain the TC frames ready to be sent
                } else {
                    // You are done for now
                    return;
                }
            } else {
                // No group, send it right away
                lastGeneratedFrames.clear();
                pendingTcPackets.clear();
                pendingTcPackets.add(tcTracker);
                vcToUse.getSecond().dispatch(useAd, map, sp);
                // Now lastGeneratedFrames will contain the TC frames ready to be sent
            }
            LOG.log(Level.INFO, lastGeneratedFrames.size() + " TC frames generated");
            // Now you have the generated frames, prepare for tracking them, encode them and send them
            // Retrieve the route and hence the service instance to use
            String route = tcTracker.getInvocation().getRoute();
            CltuServiceInstanceManager serviceInstance = this.cltuSenders.get(route);
            // Create the request tracker
            RequestTracker tracker = new RequestTracker(useAd);
            // Encode the TC frames and remember them
            List<Pair<Long, byte[]>> toSend = new LinkedList<>();
            for (TcTransferFrame frame : lastGeneratedFrames) {
                byte[] encodedCltu = encoder.apply(frame);
                long frameInTransmissionId = this.cltuSequencer.incrementAndGet();
                cltuId2requestTracker.put(frameInTransmissionId, tracker);
                toSend.add(Pair.of(frameInTransmissionId, encodedCltu));
            }
            // Initialise the tracker
            tracker.initialise(pendingTcPackets, toSend);
            // Send the CLTUs
            for (Pair<Long, byte[]> p : toSend) {
                serviceInstance.sendCltu(p.getSecond(), p.getFirst());
            }
        } catch (Exception e) {
            throw new ActivityHandlingException("TC frame construction/processing error: " + e.getMessage(), e);
        }
    }

    public void dispose() {
        for(CltuServiceInstanceManager m : cltuSenders.values()) {
            m.deregister(this);
        }
        this.context.getRawDataBroker().unsubscribe(this);
        for (Pair<TcVcConfiguration, TcSenderVirtualChannel> tcChannel : tcChannels) {
            tcChannel.getSecond().deregister(this);
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

    @Override
    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        // TODO: COP-1 directives
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(Constants.TC_COP1_ACTIVITY_TYPE);
    }

    @Override
    public List<String> getSupportedRoutes() {
        return this.cltuSenders.values().stream().map(CltuServiceInstanceManager::getServiceInstanceIdentifier).collect(Collectors.toList());
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
                        // TODO: If not AD, then either SCHEDULED or EXECUTION
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
