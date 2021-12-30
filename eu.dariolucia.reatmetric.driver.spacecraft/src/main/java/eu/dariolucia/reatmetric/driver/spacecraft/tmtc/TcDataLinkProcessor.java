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
import eu.dariolucia.ccsds.tmtc.cop1.fop.*;
import eu.dariolucia.ccsds.tmtc.cop1.fop.util.BcFrameCollector;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AbstractSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.IVirtualChannelSenderOutput;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.TcSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
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
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IActivityExecutor;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.tcframe.ITcFrameConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TcVcConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TcDataLinkProcessor implements IRawDataSubscriber, IVirtualChannelSenderOutput<TcTransferFrame>, IForwardDataUnitStatusSubscriber, IActivityExecutor, IFopObserver {

    private static final Logger LOG = Logger.getLogger(TcDataLinkProcessor.class.getName());

    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final AtomicLong cltuSequencer = new AtomicLong();

    private final Pair<TcVcConfiguration, TcSenderVirtualChannel>[] tcChannels;
    private final FopEngine[] fopEngines;
    private final int defaultTcVcId;
    private final ChannelEncoder<TcTransferFrame> encoder;

    private final List<TcTracker> pendingTcPackets = new LinkedList<>();
    private final List<TcTransferFrame> lastGeneratedFrames = new LinkedList<>();

    private final Map<Long, RequestTracker> cltuId2requestTracker = new ConcurrentHashMap<>();

    private final Map<String, List<TcTracker>> pendingGroupTcs = new HashMap<>();

    private final Map<String, ICltuConnector> cltuSenders;
    private final Map<String, ITcFrameConnector> tcFrameSenders;

    private final Timer uplinkTimer = new Timer();

    private volatile boolean useAdMode;

    private final ExecutorService delegator = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "TC Data Link Processor Handler Thread");
        t.setDaemon(true);
        return t;
    });

    public TcDataLinkProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, List<ICltuConnector> cltuSenders, List<ITcFrameConnector> frameSenders) {
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
        this.fopEngines = new FopEngine[8];
        for(int i = 0; i < tcChannels.length; ++i) {
            TcVcConfiguration tcConf = getTcVcConfiguration(i, configuration.getTcDataLinkConfiguration().getTcVcDescriptors());
            if(tcConf != null) {
                tcChannels[i] = Pair.of(tcConf, new TcSenderVirtualChannel(configuration.getId(), i, VirtualChannelAccessMode.PACKET, configuration.getTcDataLinkConfiguration().isFecf(), tcConf.isSegmentation()));
                tcChannels[i].getSecond().register(this);
                BcFrameCollector bcFactory = new BcFrameCollector(tcChannels[i].getSecond());
                tcChannels[i].getSecond().register(bcFactory);
                fopEngines[i] = new FopEngine(i, tcChannels[i].getSecond()::getNextVirtualChannelFrameCounter, tcChannels[i].getSecond()::setVirtualChannelFrameCounter, bcFactory, bcFactory, this::frameOutput);
                // Default values for the FOP engine
                fopEngines[i].directive(null, FopDirective.SET_T1_INITIAL, 10);
                fopEngines[i].directive(null, FopDirective.SET_FOP_SLIDING_WINDOW, 1);
                fopEngines[i].directive(null, FopDirective.SET_TRANSMISSION_LIMIT, 1);
                fopEngines[i].directive(null, FopDirective.SET_TIMEOUT_TYPE, 0);
                fopEngines[i].register(this);
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

        for(ICltuConnector m : cltuSenders) {
            try {
                for (String route : m.getSupportedRoutes()) {
                    this.cltuSenders.put(route, m);
                }
                m.register(this);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Unexpected RemoteException: " + e.getMessage(), e);
            }
        }

        // Register to the tc frame senders
        this.tcFrameSenders = new TreeMap<>();
        for (ITcFrameConnector m : frameSenders) {
            try {
                for (String route : m.getSupportedRoutes()) {
                    this.tcFrameSenders.put(route, m);
                }
                m.register(this);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Unexpected RemoteException: " + e.getMessage(), e);
            }
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

    private void setAdMode(boolean useAdMode) {
        this.useAdMode = useAdMode;
    }

    @Override
    public void informStatusUpdate(long id, ForwardDataUnitProcessingStatus status, Instant time) {
        delegator.execute(() -> {
            RequestTracker tracker = this.cltuId2requestTracker.get(id);
            if (tracker != null) {
                tracker.trackCltuStatus(id, status, time);
                if (tracker.isLifecycleCompleted()) {
                    this.cltuId2requestTracker.remove(id);
                }
            } else {
                LOG.log(Level.WARNING, "Reported CLTU ID " + id + " not found in the CLTU tracking map");
            }
        });
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        delegator.execute(() -> {
            for (RawData rd : messages) {
                AbstractTransferFrame atf = (AbstractTransferFrame) rd.getData();
                Clcw clcw = extractClcw(atf);
                if (clcw != null) {
                    int vcId = clcw.getVirtualChannelId();
                    fopEngines[vcId].clcw(clcw);
                }
            }
        });
    }

    private Clcw extractClcw(AbstractTransferFrame atf) {
        if(atf.isOcfPresent()) {
            return new Clcw(atf.getOcfCopy());
        }
        return null;
    }

    public void sendTcPacket(SpacePacket sp, TcTracker tcTracker) throws ActivityHandlingException {
        try {
            delegator.submit(() -> {
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
                        informServiceBroker(TcPhase.FAILED, t, Collections.singletonList(tcTracker));
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
                    // Check the route from the last TcTracker: if it ends up to a CLTU connector, go for encoding.
                    // If it ends up to a Tc Frame connector, send the frames without encoding.
                    sendToFop(lastGeneratedFrames, useAd, route);
                } catch (Exception e) {
                    throw new RuntimeException("TC frame construction/processing error: " + e.getMessage(), e);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ActivityHandlingException("Problem when sending packet", e);
        }
    }

    private void sendToFop(List<TcTransferFrame> framesToSend, boolean useAd, String route) {
        // Create the global tracker
        RequestTracker tracker = new RequestTracker(useAd);
        List<Pair<Long, TcTransferFrame>> toSend = new LinkedList<>();
        for (TcTransferFrame frame : framesToSend) {
            long frameInTransmissionId = this.cltuSequencer.incrementAndGet();
            cltuId2requestTracker.put(frameInTransmissionId, tracker);
            toSend.add(Pair.of(frameInTransmissionId, frame));
            // Add annotations
            frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, route);
            frame.setAnnotationValue(Constants.ANNOTATION_TC_FRAME_ID, frameInTransmissionId);
        }
        // Initialise the tracker
        tracker.initialise(pendingTcPackets, toSend.stream().map(Pair::getFirst).collect(Collectors.toList()));
        // Send the TC frames
        for (Pair<Long, TcTransferFrame> p : toSend) {
            try {
                LOG.log(Level.INFO, " Sending TC frame to FOP");
                fopEngines[p.getSecond().getVirtualChannelId()].transmit(p.getSecond(), 15000);
            } catch (InterruptedException e) {
                LOG.warning("TC frame acceptance by FOP engine on VC " + p.getSecond().getVirtualChannelId() + " interrupted");
                Thread.interrupted();
            }
        }
    }

    // No need to delegate
    private boolean frameOutput(TcTransferFrame tcTransferFrame) {
        LOG.log(Level.INFO, "FOP Low Layer - Received TC frame " + tcTransferFrame);
        String route;
        long frameInTransmissionId;
        if(tcTransferFrame.getFrameType() == TcTransferFrame.FrameType.BC) {
            // From a directive
            IActivityHandler.ActivityInvocation tag = (IActivityHandler.ActivityInvocation) tcTransferFrame.getAnnotationValue(FopEngine.ANNOTATION_BC_FRAME_TAG);
            route = tag.getRoute();
            frameInTransmissionId = -tag.getActivityOccurrenceId().asLong();
        } else {
            route = (String) tcTransferFrame.getAnnotationValue(Constants.ANNOTATION_ROUTE);
            frameInTransmissionId = (Long) tcTransferFrame.getAnnotationValue(Constants.ANNOTATION_TC_FRAME_ID);
        }
        ICltuConnector connectorInstance = this.cltuSenders.get(route);
        if(connectorInstance != null) {
            byte[] encodedCltu = encoder.apply(tcTransferFrame);
            try {
                connectorInstance.sendCltu(encodedCltu, frameInTransmissionId);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Unexpected RemoteException: " + e.getMessage(), e);
            }
            return true;
        } else {
            ITcFrameConnector frameConnector = this.tcFrameSenders.get(route);
            if(frameConnector != null) {
                try {
                    frameConnector.sendTcFrame(tcTransferFrame, frameInTransmissionId);
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Unexpected RemoteException: " + e.getMessage(), e);
                }
                return true;
            }
        }
        return false;
    }

    public void dispose() {
        delegator.submit(() -> {
            // All pending TC groups must be aborted
            Set<String> tcGroups = new HashSet<>(pendingGroupTcs.keySet());
            for (String group : tcGroups) {
                abortTcGroup(group, pendingGroupTcs.get(group));
            }
            this.context.getRawDataBroker().unsubscribe(this);
            for (ICltuConnector m : new HashSet<>(cltuSenders.values())) {
                try {
                    m.deregister(this);
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Unexpected RemoteException: " + e.getMessage(), e);
                }
            }
            for (Pair<TcVcConfiguration, TcSenderVirtualChannel> tcChannel : tcChannels) {
                if (tcChannel != null) {
                    tcChannel.getSecond().deregister(this);
                }
            }
            for (FopEngine fop : fopEngines) {
                if (fop != null) {
                    fop.dispose();
                }
            }
        });
        delegator.shutdown();
    }

    // Called by the VC that already runs in the delegator thread
    @Override
    public void transferFrameGenerated(AbstractSenderVirtualChannel vc, TcTransferFrame generatedFrame, int bufferedBytes) {
        // Ignore BC frames
        if (generatedFrame.getFrameType() != TcTransferFrame.FrameType.BC) {
            lastGeneratedFrames.add(generatedFrame);
        }
    }

    private void informServiceBroker(TcPhase phase, Instant time, List<TcTracker> trackers) {
        for (TcTracker tracker : trackers) {
            // To be reported only if there is actually a packet, not for a BC frame for COP-1 controlling
            if(tracker.getPacket() != null) {
                serviceBroker.informTcPacket(phase, time, tracker);
            }
        }
    }

    private void reportActivityState(List<TcTracker> trackers, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        for (TcTracker tracker : trackers) {
            context.getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, null, status, nextState, null));
        }
    }

    @Override
    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        try {
            delegator.submit(() -> {
                context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), ActivityOccurrenceReport.RELEASE_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.PENDING, ActivityOccurrenceState.RELEASE, null));
                if (activityInvocation.getPath().getLastPathElement().equals(Constants.TC_COP1_DIRECTIVE_NAME)) {
                    // Three parameters: TC VC ID (enumeration: 0 to 7), directive ID (enumeration: as per FopDirective enum), qualifier (enumeration)
                    int tcVcId = (int) activityInvocation.getArguments().get(Constants.ARGUMENT_FOP_DIRECTIVE_TC_VC_ID);
                    FopDirective directive = FopDirective.values()[(int) activityInvocation.getArguments().get(Constants.ARGUMENT_FOP_DIRECTIVE_DIRECTIVE_ID)];
                    int qualifier = (int) activityInvocation.getArguments().get(Constants.ARGUMENT_FOP_DIRECTIVE_QUALIFIER);

                    context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), ActivityOccurrenceReport.RELEASE_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null));
                    // Go for execution
                    if (directive == FopDirective.INIT_AD_WITH_UNLOCK || directive == FopDirective.INIT_AD_WITH_SET_V_R) {
                        // Create the global tracker
                        RequestTracker tracker = new RequestTracker(false);
                        long frameInTransmissionId = -activityInvocation.getActivityOccurrenceId().asLong();
                        cltuId2requestTracker.put(frameInTransmissionId, tracker);
                        // Initialise the tracker
                        TcTracker tcTracker = new TcTracker(activityInvocation, null, null, null);
                        tracker.initialise(Collections.singletonList(tcTracker), Collections.singletonList(frameInTransmissionId));
                        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_GROUND_STATION_RECEPTION, Instant.now(), ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION, null));
                    } else {
                        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_FOP_DIRECTIVE, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null));
                    }
                    fopEngines[tcVcId].directive(activityInvocation, directive, qualifier);
                } else if (activityInvocation.getPath().getLastPathElement().equals(Constants.TC_COP1_SET_AD_NAME)) {
                    // One parameter: SET_AD (boolean)
                    boolean setAd = (boolean) activityInvocation.getArguments().get(Constants.ARGUMENT_TC_SET_AD);
                    context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), ActivityOccurrenceReport.RELEASE_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, null));
                    context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_LOCAL_EXECUTION, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null));
                    // Go for execution
                    setAdMode(setAd);
                    context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_LOCAL_EXECUTION, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.OK, ActivityOccurrenceState.VERIFICATION, null));
                } else {
                    throw new RuntimeException("Unknown COP-1 activity invoked: " + activityInvocation.getPath() + " (" + activityInvocation.getActivityId() + ")");
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ActivityHandlingException("Cannot execute activity " + activityInvocation.getPath(), e);
        }
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(Constants.TC_COP1_ACTIVITY_TYPE);
    }

    @Override
    public List<String> getSupportedRoutes() {
        List<String> routes = new ArrayList<>(this.cltuSenders.keySet());
        routes.addAll(this.tcFrameSenders.keySet());
        return routes;
    }

    @Override
    public void abort(int activityId, IUniqueId activityOccurrenceId) {
        delegator.submit(() -> {
            // If the activityOccurrenceId is part of a group command, then the group commands (and the related TCs up to now
            // pending release) should be aborted.
            for (Map.Entry<String, List<TcTracker>> entry : this.pendingGroupTcs.entrySet()) {
                for (TcTracker tcTracker : entry.getValue()) {
                    if (tcTracker.getInvocation().getActivityOccurrenceId().equals(activityOccurrenceId)) {
                        // Found, abort all TC group
                        abortTcGroup(entry.getKey(), entry.getValue());
                        return;
                    }
                }
            }
        });
    }

    private void abortTcGroup(String groupToAbort, List<TcTracker> tcTrackers) {
        LOG.log(Level.WARNING, "Aborting TC group " + groupToAbort + " due to abort request received");
        this.pendingGroupTcs.remove(groupToAbort);
        Instant now = Instant.now();
        informServiceBroker(TcPhase.FAILED, now, tcTrackers);
        reportActivityState(tcTrackers, now, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
    }

    @Override
    public void transferNotification(FopEngine engine, FopOperationStatus status, TcTransferFrame frame) {
        delegator.submit(() -> {
            if (frame.getFrameType() == TcTransferFrame.FrameType.AD && (status == FopOperationStatus.NEGATIVE_CONFIRM || status == FopOperationStatus.POSIIVE_CONFIRM)) {
                long frameInTransmissionId = (Long) frame.getAnnotationValue(Constants.ANNOTATION_TC_FRAME_ID);
                RequestTracker tracker = cltuId2requestTracker.get(frameInTransmissionId);
                if (tracker != null) {
                    tracker.trackReceptionConfirmation(frameInTransmissionId, status, Instant.now().minusNanos(configuration.getPropagationDelay() * 1000));
                    if (tracker.isLifecycleCompleted()) {
                        this.cltuId2requestTracker.remove(frameInTransmissionId);
                    }
                }
            }
        });
    }

    @Override
    public void directiveNotification(FopEngine engine, FopOperationStatus status, Object tag, FopDirective directive, int qualifier) {
        if(tag == null) {
            // Ignore, can still be an initialisation callback
            return;
        }
        delegator.submit(() -> {
            IActivityHandler.ActivityInvocation activityInvocation = (IActivityHandler.ActivityInvocation) tag;
            if (status == FopOperationStatus.REJECT_RESPONSE || status == FopOperationStatus.NEGATIVE_CONFIRM) {
                // For the execution time we use the ground time, because the effect of the command is on the FOP entity on ground
                context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_FOP_DIRECTIVE, Instant.now(), ActivityOccurrenceState.EXECUTION, Instant.now(), ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION, null));
            } else if (status == FopOperationStatus.POSIIVE_CONFIRM) {
                // For the execution time we use the ground time, because the effect of the command is on the FOP entity on ground
                context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Constants.STAGE_FOP_DIRECTIVE, Instant.now(), ActivityOccurrenceState.EXECUTION, Instant.now(), ActivityReportState.OK, ActivityOccurrenceState.VERIFICATION, null));
            }
            // Ignore the accept
        });
    }

    @Override
    public void alert(FopEngine engine, FopAlertCode code) {
        LOG.severe("FOP engine for TC VC " + engine.getVirtualChannelId() + " alert: " + code);
    }

    @Override
    public void suspend(FopEngine engine) {
        LOG.warning("FOP engine for TC VC " + engine.getVirtualChannelId() + " suspended");
    }

    @Override
    public void statusReport(FopEngine engine, FopStatus status) {
        if(!status.getPreviousState().equals(status.getCurrentState())) {
            if(status.getCurrentState() == FopState.S6) {
                LOG.warning("FOP engine for TC VC " + engine.getVirtualChannelId() + ": " + status.getPreviousState() + " -> " + status.getCurrentState());
            } else {
                LOG.info("FOP engine for TC VC " + engine.getVirtualChannelId() + ": " + status.getPreviousState() + " -> " + status.getCurrentState());
            }
        }
    }

    private class RequestTracker {
        private final List<TcTracker> tcTrackers = new LinkedList<>();
        private final Set<Long> dataUnits = new HashSet<>();
        private final boolean useAd;
        private volatile boolean lifecycleCompleted;

        private final Set<Long> released = new HashSet<>();
        private final Set<Long> accepted = new HashSet<>();
        private final Set<Long> uplinked = new HashSet<>();
        private final Set<Long> received = new HashSet<>(); // Only for AD mode

        public RequestTracker(boolean useAd) {
           this.useAd = useAd;
        }

        public void initialise(List<TcTracker> tcTrackers, List<Long> dataUnits) {
            this.tcTrackers.addAll(tcTrackers);
            this.dataUnits.addAll(dataUnits);
        }

        public void trackCltuStatus(Long id, ForwardDataUnitProcessingStatus status, Instant time) {
            //
            switch (status) {
                case RELEASED: { // The CLTU/Frame was sent to the ground station
                    released.add(id);
                    if(released.size() == dataUnits.size()) { // All released
                        informServiceBroker(TcPhase.RELEASED, time, tcTrackers);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                    }
                }
                break;
                case RELEASE_FAILED: { // Release problem
                    informServiceBroker(TcPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE);
                    lifecycleCompleted = true;
                }
                break;
                case ACCEPTED: { // The CLTU/Frame was accepted by the ground station
                    accepted.add(id);
                    if(accepted.size() == dataUnits.size()) { // All CLTUs/Frames accepted, so command is all at the ground station
                        // Nothing to be done here with the service broker
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                    }
                }
                break;
                case REJECTED: { // The CLTU/Frame was rejected by the ground station or discarded -> all related TC requests to be marked as failed in ground station reception
                    informServiceBroker(TcPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
                    lifecycleCompleted = true;
                }
                break;
                case UPLINKED: { // The CLTU/Frame was uplinked by the ground station
                    uplinked.add(id);
                    if(uplinked.size() == dataUnits.size()) { // All CLTUs/Frames uplinked, so command is all on its way
                        informServiceBroker(TcPhase.UPLINKED, time, tcTrackers);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ONBOARD_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                        if(!useAd) {
                            // Other stages are not in the scope of this class: send out a RECEIVED_ONBOARD success after uplink time + propagation delay on the service broker only
                            Instant estimatedOnboardReceptionTime = time.plusNanos(configuration.getPropagationDelay() * 1000);
                            if(configuration.getPropagationDelay() < 1000000) { // Less than one second propagation delay: report onboard reception now
                                informServiceBroker(TcPhase.RECEIVED_ONBOARD, estimatedOnboardReceptionTime, tcTrackers);
                                reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ONBOARD_RECEPTION, ActivityReportState.EXPECTED, ActivityOccurrenceState.TRANSMISSION);
                            } else {
                                TimerTask tt = new TimerTask() {
                                    @Override
                                    public void run() {
                                        informServiceBroker(TcPhase.RECEIVED_ONBOARD, estimatedOnboardReceptionTime, tcTrackers);
                                        reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ONBOARD_RECEPTION, ActivityReportState.EXPECTED, ActivityOccurrenceState.TRANSMISSION);
                                    }
                                };
                                uplinkTimer.schedule(tt, new Date(estimatedOnboardReceptionTime.toEpochMilli()));
                            }
                        }
                    }
                    if(!useAd) {
                        lifecycleCompleted = true;
                    }
                }
                break;
                case UPLINK_FAILED: { // The CLTU/Frame failed uplink -> all related TC requests to be marked as failed in ground station uplink
                    informServiceBroker(TcPhase.FAILED, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
                    lifecycleCompleted = true;
                }
                break;
            }
        }

        public void trackReceptionConfirmation(Long id, FopOperationStatus status, Instant time) {
            if(status == FopOperationStatus.NEGATIVE_CONFIRM) {
                informServiceBroker(TcPhase.FAILED, time, tcTrackers);
                reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ONBOARD_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
                lifecycleCompleted = true;
            } else {
                received.add(id);
                if(received.size() == dataUnits.size()) { // All CLTUs/Frames received on-board, so command is all on the spacecraft
                    informServiceBroker(TcPhase.RECEIVED_ONBOARD, time, tcTrackers);
                    reportActivityState(tcTrackers, time, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ONBOARD_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
                    lifecycleCompleted = true;
                }
            }
        }

        public boolean isLifecycleCompleted() {
            return lifecycleCompleted;
        }
    }

    @Override
    public String toString() {
        return "TC Data Link Processor";
    }
}
