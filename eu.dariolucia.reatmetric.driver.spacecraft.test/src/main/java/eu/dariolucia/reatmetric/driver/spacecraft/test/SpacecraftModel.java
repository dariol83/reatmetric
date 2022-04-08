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

package eu.dariolucia.reatmetric.driver.spacecraft.test;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.definition.IdentField;
import eu.dariolucia.ccsds.encdec.definition.IdentFieldMatcher;
import eu.dariolucia.ccsds.encdec.definition.PacketDefinition;
import eu.dariolucia.ccsds.encdec.pus.AckField;
import eu.dariolucia.ccsds.encdec.pus.PusChecksumUtil;
import eu.dariolucia.ccsds.encdec.pus.TcPusHeader;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.IPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.PacketDefinitionIndexer;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.resolvers.DefaultNullBasedResolver;
import eu.dariolucia.ccsds.encdec.structure.resolvers.DefinitionValueBasedResolver;
import eu.dariolucia.ccsds.encdec.value.TimeUtil;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.incoming.pdus.CltuTransferDataInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferBuffer;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.ccsds.sle.utl.si.cltu.*;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstanceProvider;
import eu.dariolucia.ccsds.tmtc.coding.ChannelDecoder;
import eu.dariolucia.ccsds.tmtc.coding.decoder.CltuDecoder;
import eu.dariolucia.ccsds.tmtc.coding.decoder.CltuRandomizerDecoder;
import eu.dariolucia.ccsds.tmtc.cop1.farm.FarmEngine;
import eu.dariolucia.ccsds.tmtc.cop1.farm.FarmState;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.AbstractReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.IVirtualChannelReceiverOutput;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.TcReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.TmSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.mux.TmMasterChannelMuxer;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.ccsds.tmtc.ocf.pdu.AbstractOcf;
import eu.dariolucia.ccsds.tmtc.transport.builder.SpacePacketBuilder;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.*;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple implementation of a spacecraft model. It generates:
 * <ul>
 *     <li>TM packets 3,25 present in the definition, using parameter values derived from the default or expected values of the processing model (i.e. textual calibrations), round robin generation</li>
 *     <li>TM packets 5,x present in the definition, using random generation</li>
 *     <li>TM packets 1,x present in the definition, according to the ack flags configured in the TC</li>
 *     <li>TM packets APID 0 (time packets) at VCC = 0 for VC 0</li>
 *     <li>TM idle packets in case the remaining length inside a TM frame is less than 10 bytes</li>
 * </ul>
 *
 * All TM data is delivered on VC 0.
 *
 * The application processes:
 * <ul>
 *     <li>All TC commands, by sending back ACKs defined as per ACK flags</li>
 *     <li>PUS 11,4 command, with the limitation of 1 TC per 11,4, single subschedule ID (it is actually ignored), N present and hardcoded to 1</li>
 *     <li>Specific SETTER commands </li>
 * </ul>
 *
 *
 *
 */
public class SpacecraftModel implements IVirtualChannelReceiverOutput, IServiceInstanceListener {

    private static final Logger LOG = Logger.getLogger(SpacecraftModel.class.getName());

    public static final int BUFFER_AVAILABLE = 10000;
    public static final String APID_FIELD_NAME = "I-APID";
    public static final String PUS_TYPE_FIELD_NAME = "I-PUS-TYPE";
    public static final String PUS_SUBTYPE_FIELD_NAME = "I-PUS-SUBTYPE";
    public static final int TM_FRAME_LENGTH = 1115;
    public static final int PACKET_GENERATION_PERIOD_MS = 2;
    public static final int CYCLE_MODULE_SLEEP = 3; // 10
    public static final int SETTER_APID = 10;
    public static final int SETTER_PUS = 69;

    private final CltuServiceInstanceProvider cltuProvider;
    private final RafServiceInstanceProvider rafProvider;

    private final SpacecraftConfiguration spacecraftConfiguration;
    private final Definition encDecDefs;

    private final ProcessingDefinition processingDefinition;

    // TC processing part
    private final ExecutorService cltuProcessor = Executors.newSingleThreadExecutor(); // OK, test tool
    private ChannelDecoder<TcTransferFrame> cltuDecoder;
    private final Map<Integer, TcReceiverVirtualChannel> id2tcvc = new HashMap<>();
    private final Map<Integer, Boolean> id2segmentation = new HashMap<>();
    private final Timer queueeTcScheduler = new Timer();
    private final FarmEngine farm;

    // TM processing part
    private final BlockingQueue<SpacePacket> packetsToSend = new ArrayBlockingQueue<>(2000);
    private final TmMasterChannelMuxer tmMux;
    private final Map<Integer, TmSenderVirtualChannel> id2tmvc = new TreeMap<>();
    private IPacketEncoder encoder;
    private final List<TmPacketTemplate> periodicPackets = new LinkedList<>();
    private final List<TmPacketTemplate> eventPackets = new LinkedList<>();
    private final List<TmPacketTemplate> pus1Packets = new LinkedList<>();
    private final Map<Integer, AtomicInteger> apid2counter = new HashMap<>();
    private final ProcessingModelBasedResolver resolver;

    private final Timer performanceSampler = new Timer("Spacecraft Sampler", true);
    private Instant lastSampleGenerationTime;
    private long packetsPerSecond = 0;
    private long framesPerSecond = 0;

    private volatile boolean running = false;
    private Thread tmThread;
    private Thread generationThread;

    public SpacecraftModel(String tmTcFilePath, String spacecraftFilePath, CltuServiceInstanceProvider cltuProvider, RafServiceInstanceProvider rafProvider, String processingModelPath) throws IOException, JAXBException {
        this.cltuProvider = cltuProvider;
        this.rafProvider = rafProvider;
        this.rafProvider.register(this);
        this.cltuProvider.setTransferDataOperationHandler(this::cltuReceived);
        this.encDecDefs = Definition.load(new FileInputStream(tmTcFilePath));
        this.processingDefinition = ProcessingDefinition.load(new FileInputStream(processingModelPath));
        this.spacecraftConfiguration = SpacecraftConfiguration.load(new FileInputStream(spacecraftFilePath));
        this.spacecraftConfiguration.getTmPacketConfiguration().buildLookupMap();
        this.farm = new FarmEngine(0, this::tcFrameOutput, true, 10, 20, FarmState.S3, 0);
        this.farm.setStatusField(7);
        this.farm.setReservedSpare(0);
        this.farm.setNoRfAvailableFlag(false);
        this.farm.setNoBitLockFlag(false);
        initialiseSpacecraftUplink();
        this.tmMux = new TmMasterChannelMuxer(this::sendTmFrame);
        initialiseSpacecraftDownlink();
        initialiseSpacePacketGeneration();
        this.resolver = new ProcessingModelBasedResolver(processingDefinition, new DefinitionValueBasedResolver(new DefaultNullBasedResolver(), true), spacecraftConfiguration.getTmPacketConfiguration().getParameterIdOffset(), encDecDefs);
        // Create performance samples
        performanceSampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
    }

    private void sample() {
        synchronized (performanceSampler) {
            Instant genTime = Instant.now();
            if (lastSampleGenerationTime == null) {
                lastSampleGenerationTime = genTime;
                packetsPerSecond = 0;
                framesPerSecond = 0;
            } else {
                long numPackets = packetsPerSecond;
                packetsPerSecond = 0;
                long numFrames = framesPerSecond;
                framesPerSecond = 0;
                int millis = (int) (genTime.toEpochMilli() - lastSampleGenerationTime.toEpochMilli());
                lastSampleGenerationTime = genTime;
                double numPacketsPerSecond = (numPackets / (millis/1000.0));
                double numFramesPerSecond = (numFrames / (millis/1000.0));
                LOG.info("Frames per second: " + (int) numFramesPerSecond + ", packets per second: " + numPacketsPerSecond);
            }
        }
    }

    private void tcFrameOutput(TcTransferFrame tcTransferFrame) {
        // Pass TC frame to appropriate VC
        id2tcvc.get((int) tcTransferFrame.getVirtualChannelId()).accept(tcTransferFrame);
    }

    private void initialiseSpacePacketGeneration() {
        this.encoder = new DefaultPacketEncoder(new PacketDefinitionIndexer(encDecDefs), DefaultPacketEncoder.DEFAULT_MAX_PACKET_SIZE, spacecraftConfiguration.getEpoch() != null ? spacecraftConfiguration.getEpoch().toInstant() : null);
        // Look for all packets 3,25 and generate them cyclically
        // Look for all packets 5,x and generate them from time to time
        for (PacketDefinition pd : encDecDefs.getPacketDefinitions()) {
            if (pd.getType().equals(Constants.ENCDEC_TM_PACKET_TYPE)) {
                int apid = 0;
                int type = 0;
                int subtype = 0;
                for (IdentFieldMatcher md : pd.getMatchers()) {
                    if (md.getField().getId().equals(APID_FIELD_NAME)) {
                        apid = md.getValue();
                    }
                    if (md.getField().getId().equals(PUS_TYPE_FIELD_NAME)) {
                        type = md.getValue();
                    }
                    if (md.getField().getId().equals(PUS_SUBTYPE_FIELD_NAME)) {
                        subtype = md.getValue();
                    }
                }
                if (type == 3 && subtype == 25) {
                    registerPeriodic(pd, apid, type, subtype);
                }
                if (type == 5) {
                    registerEvent(pd, apid, type, subtype);
                }
                if (type == 1) {
                    registerPus1(pd, apid, type, subtype);
                }
            }
        }
    }

    private void registerEvent(PacketDefinition pd, int apid, int type, int subtype) {
        this.eventPackets.add(new TmPacketTemplate(pd, apid, type, subtype, spacecraftConfiguration.getTmPacketConfiguration().getPusConfigurationFor((short) apid)));
    }

    private void registerPeriodic(PacketDefinition pd, int apid, int type, int subtype) {
        this.periodicPackets.add(new TmPacketTemplate(pd, apid, type, subtype, spacecraftConfiguration.getTmPacketConfiguration().getPusConfigurationFor((short) apid)));
    }

    private void registerPus1(PacketDefinition pd, int apid, int type, int subtype) {
        this.pus1Packets.add(new TmPacketTemplate(pd, apid, type, subtype, spacecraftConfiguration.getTmPacketConfiguration().getPusConfigurationFor((short) apid)));
    }

    private void initialiseSpacecraftDownlink() {
        if(spacecraftConfiguration.getTmDataLinkConfigurations().getProcessVcs() == null) {
            TmSenderVirtualChannel vc = new TmSenderVirtualChannel(spacecraftConfiguration.getId(), 0, VirtualChannelAccessMode.PACKET,
                    spacecraftConfiguration.getTmDataLinkConfigurations().isFecfPresent(), TM_FRAME_LENGTH, tmMux::getNextCounter, this::provideOcf);
            vc.register(tmMux);
            this.id2tmvc.put(0, vc);
        } else {
            for (Integer vcId : spacecraftConfiguration.getTmDataLinkConfigurations().getProcessVcs()) {
                TmSenderVirtualChannel vc = new TmSenderVirtualChannel(spacecraftConfiguration.getId(), vcId, VirtualChannelAccessMode.PACKET,
                        spacecraftConfiguration.getTmDataLinkConfigurations().isFecfPresent(), TM_FRAME_LENGTH, tmMux::getNextCounter, this::provideOcf);
                vc.register(tmMux);
                this.id2tmvc.put(vcId, vc);
            }
        }
    }

    private AbstractOcf provideOcf(int tmVcId) {
        return farm.get();
    }

    private void initialiseSpacecraftUplink() {
        // Create N TC VC according to configuration
        for (TcVcConfiguration c : spacecraftConfiguration.getTcDataLinkConfiguration().getTcVcDescriptors()) {
            TcReceiverVirtualChannel vc = new TcReceiverVirtualChannel(c.getTcVc(), VirtualChannelAccessMode.PACKET, true);
            vc.register(this);
            id2tcvc.put(vc.getVirtualChannelId(), vc);
            id2segmentation.put(c.getTcVc(), c.isSegmentation());
        }
        // Create channel cltuDecoder
        cltuDecoder = ChannelDecoder.create(TcTransferFrame.decodingFunction(id2segmentation::get, spacecraftConfiguration.getTcDataLinkConfiguration().isFecf()));
        cltuDecoder.addDecodingFunction(new CltuDecoder());
        if (spacecraftConfiguration.getTcDataLinkConfiguration().isRandomize()) {
            cltuDecoder.addDecodingFunction(new CltuRandomizerDecoder());
        }
        cltuDecoder.configure();
    }

    public void startProcessing() {
        if (running) {
            return;
        }
        running = true;
        rafProvider.updateProductionStatus(Instant.now(), LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, ProductionStatusEnum.RUNNING);
        cltuProvider.updateProductionStatus(CltuProductionStatusEnum.OPERATIONAL, CltuUplinkStatusEnum.NOMINAL, BUFFER_AVAILABLE);
        cltuProvider.waitForBind(true, null);
        rafProvider.waitForBind(true, null);
        tmThread = new Thread(this::sendPackets, "Packet Sender");
        tmThread.start();
        generationThread = new Thread(this::generatePackets);
        generationThread.start();
    }

    private void generatePackets() {
        int hkCounter = 0;
        int evtCounter = 0;
        int counter = 0;
        while (running) {
            try {
                if(counter % CYCLE_MODULE_SLEEP == 0) {
                    Thread.sleep(PACKET_GENERATION_PERIOD_MS);
                }
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "", e);
            }
            TmPacketTemplate pkt = this.periodicPackets.get(hkCounter++);
            hkCounter = hkCounter % periodicPackets.size();
            ++counter;
            if(counter == Integer.MAX_VALUE) {
                counter = 0;
            }
            try {
                SpacePacket sp = pkt.generate();
                if(sp != null) {
                    addPacketToQueue(sp, false);
                    synchronized (performanceSampler) {
                        ++packetsPerSecond;
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "", e);
            }

            if (Math.random() < 0.01) {
                TmPacketTemplate evpkt = this.eventPackets.get(evtCounter++);
                evtCounter = evtCounter % eventPackets.size();
                try {
                    SpacePacket spev = evpkt.generate();
                    if(spev != null) {
                        addPacketToQueue(spev, false);
                        synchronized (performanceSampler) {
                            ++packetsPerSecond;
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "", e);
                }
            }
        }
    }

    private void addPacketToQueue(SpacePacket sp, boolean withPriority) throws InterruptedException {
        synchronized (packetsToSend) {
            while (!withPriority && packetsToSend.size() >= 1000) {
                packetsToSend.wait();
            }
            packetsToSend.put(sp);
            packetsToSend.notifyAll();
        }
    }

    private void sendPackets() {
        while (running) {
            try {
                SpacePacket sp;
                synchronized (packetsToSend) {
                    while (packetsToSend.isEmpty()) {
                        packetsToSend.wait();
                    }
                    sp = packetsToSend.take();
                    packetsToSend.notifyAll();
                }
                // Get the TM VC to use: 0 if configured, otherwise the first
                TmSenderVirtualChannel vcToUse = this.id2tmvc.get(0);
                if (vcToUse == null) {
                    vcToUse = id2tmvc.entrySet().iterator().next().getValue();
                }
                // Send packet
                vcToUse.dispatch(sp);
                // From time to time, send an idle packet (1% average)
                if (Math.random() < 0.01) {
                    sendIdlePacket(vcToUse);
                }
            } catch (Exception e) {
                Thread.interrupted();
                LOG.log(Level.SEVERE, "", e);
            }
        }
    }

    private void sendIdlePacket(TmSenderVirtualChannel vcToUse) {
        int free = vcToUse.getRemainingFreeSpace();
        if (free > 10) {
            SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setIdle().setPacketSequenceCount(0).setSecondaryHeaderFlag(false).setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED).setQualityIndicator(true);
            builder.addData(new byte[free - SpacePacket.SP_PRIMARY_HEADER_LENGTH]);
            vcToUse.dispatch(builder.build());
        }
    }

    private CltuTransferDataResult cltuReceived(CltuTransferDataInvocation cltuTransferDataInvocation) {
        this.cltuProcessor.execute(() -> processCltu(cltuTransferDataInvocation.getCltuIdentification().longValue(), cltuTransferDataInvocation.getCltuData().value));
        return CltuTransferDataResult.noError(BUFFER_AVAILABLE);
    }

    private void processCltu(long cltuId, byte[] cltu) {
        Date startTime = new Date();
        cltuProvider.cltuProgress(cltuId, CltuStatusEnum.PRODUCTION_STARTED, startTime, null, BUFFER_AVAILABLE);
        try {
            Thread.sleep(cltu.length * 8);
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "", e);
        }
        cltuProvider.cltuProgress(cltuId, CltuStatusEnum.RADIATED, startTime, new Date(), BUFFER_AVAILABLE);
        // Process the CLTU
        TcTransferFrame decodedTcFrame = cltuDecoder.apply(cltu);
        farm.frameArrived(decodedTcFrame);
    }

    private void sendTmFrame(TmTransferFrame tmTransferFrame) {
        // Send the transfer frame
        // if vcId == 0 and vcc == 0, generate time packet: generation rate is 256
        if (tmTransferFrame.getVirtualChannelId() == 0 && tmTransferFrame.getVirtualChannelFrameCount() == 0) {
            generateTimePacket(Instant.now());
        }
        if (rafProvider.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE) {
            try {
                boolean result = rafProvider.transferData(tmTransferFrame.getFrame(), 0, 1, Instant.now(), false, StringUtil.toHexDump("ANTENNA-TEST".getBytes(StandardCharsets.ISO_8859_1)), false, new byte[0]);
                if (!result) {
                    LOG.severe("Error transferring TF");
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "", e);
            }
        }
    }

    private void generateTimePacket(Instant now) {
        int apidCounter = getNextCounter(0);
        SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setApid(0).setPacketSequenceCount(apidCounter).setSecondaryHeaderFlag(false).setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED).setQualityIndicator(true);
        builder.addData(new byte[]{8}); // Hardcoded: report of generation period
        CucConfiguration format = new CucConfiguration();
        format.setCoarse(4); // Hardcoded
        format.setFine(2); // Hardcoded
        format.setExplicitPField(true); // Hardcoded
        byte[] obt = TimeUtil.toCUC(now, spacecraftConfiguration.getEpoch() != null ? spacecraftConfiguration.getEpoch().toInstant() : null, format.getCoarse(), format.getFine(), format.isExplicitPField());
        builder.addData(obt);
        SpacePacket sp = builder.build();
        try {
            addPacketToQueue(sp, true);
            synchronized (performanceSampler) {
                ++packetsPerSecond;
            }
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "", e);
        }
    }

    private synchronized int getNextCounter(int apid) {
        AtomicInteger ai = this.apid2counter.computeIfAbsent(apid, k -> new AtomicInteger(0));
        int val = ai.getAndIncrement();
        if (val == 0x3FFF) { // Wrap around next
            ai.set(0);
        }
        return val;
    }

    @Override
    public void spacePacketExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame firstFrame, byte[] packet, boolean qualityIndicator) {
        SpacePacket sp = new SpacePacket(packet, qualityIndicator);

        // Verify TC packet checksum
        boolean validChecksum = true;
        switch (this.spacecraftConfiguration.getTcPacketConfiguration().getTcPecPresent()) {
            case CRC:
                validChecksum = (PusChecksumUtil.crcChecksum(packet, 0, packet.length) == 0);
                break;
            case ISO:
                validChecksum = (PusChecksumUtil.verifyIsoChecksum(packet, 0, packet.length));
                break;
        }

        if (!validChecksum) {
            LOG.severe("The received TC packet " + StringUtil.toHexDump(packet) + " does not have a correct checksum");
            queuePus1(sp, 2);
            return;
        }

        LOG.info("Received TC packet: " + StringUtil.toHexDump(packet));
        if (qualityIndicator) {
            AckField acks = new AckField(sp.getPacket()[SpacePacket.SP_PRIMARY_HEADER_LENGTH]);
            LOG.info("ACK flags: " + acks);
            if (acks.isAcceptanceAckSet()) {
                queuePus1(sp, 1);
            }
            if (acks.isStartAckSet()) {
                queuePus1(sp, 3);
            }
            if (acks.isProgressAckSet()) {
                queuePus1(sp, 5);
            }
            if (acks.isCompletionAckSet()) {
                queuePus1(sp, 7);
            }
            if(isTimeTaggedCommand(sp)) {
                queueCommandExecution(vc, firstFrame, sp);
            }
            if(isSetterCommand(sp)) {
                executeSetterCommand(sp);
            }
        }
    }

    private void executeSetterCommand(SpacePacket sp) {
        try {
            // Retrieve type code, name and value
            TcPusHeader pusHeader = TcPusHeader.decodeFrom(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, 8);
            int type = ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength(), 2).getShort();
            byte[] strname = new byte[8];
            ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 2, 8).get(strname);
            String name = new String(strname);
            // Depending on the type, retrieve the value
            Object value = null;
            switch (type) {
                case 1: // Enum -> 16 bits
                    value = (int) ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 10, 2).getShort();
                    break;
                case 2: // SI -> 32 bits
                    value = (long) ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 10, 4).getInt();
                    break;
                case 3: // UI -> 32 bits
                    value = (long) Integer.toUnsignedLong(ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 10, 4).getInt());
                    break;
                case 4: // Real -> 32 bits
                    value = (double) ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 10, 4).getFloat();
                    break;
                case 6: // Octet string -> 12 bytes
                    value = new byte[12];
                    ByteBuffer.wrap(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + pusHeader.getEncodedLength() + 10, 12).get((byte[]) value);
                    break;
                default:
                    LOG.warning("Setter packet ignored, unknown type " + type + " for parameter " + name);
                    break;
            }
            // Locate the parameter
            if(value != null) {
                resolver.updateParameterValue(name, value);
            } else {
                LOG.warning("Setter packet ignored, value null for parameter " + name);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Setter packet ignored, exception detected", e);
        }
    }

    private boolean isSetterCommand(SpacePacket sp) {
        return sp.getApid() == SETTER_APID && sp.getPacket()[SpacePacket.SP_PRIMARY_HEADER_LENGTH + 1] == SETTER_PUS;
    }

    private void queueCommandExecution(AbstractReceiverVirtualChannel<?> vc, AbstractTransferFrame firstFrame, SpacePacket sp) {
        // sp is a 11,4 packet: we need to extract the command to schedule, according to the 11,4 definition
        // First of all, get rid of the TC PUS header: assume sourceLen octet align
        int sourceLenSpare = (spacecraftConfiguration.getTcPacketConfiguration().getSourceIdLength() + spacecraftConfiguration.getTcPacketConfiguration().getSpareLength()) / 8;
        // Then, understand if the subschedule and the num of entries is there
        int subScheduleByteLen = 1; // Hardcoded as per test data definition
        int counterByteLen = 1; // Hardcoded as per test data definition (N-01)
        // Finally, read the absolute time and the packet (limited to time and command, only 1)
        byte[] cucTime = Arrays.copyOfRange(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + 3 + sourceLenSpare + subScheduleByteLen + counterByteLen, SpacePacket.SP_PRIMARY_HEADER_LENGTH + 3 + sourceLenSpare + subScheduleByteLen + counterByteLen + 6);
        Instant time = TimeUtil.fromCUC(cucTime, spacecraftConfiguration.getEpoch().toInstant(), 4,2);
        byte[] packet = Arrays.copyOfRange(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + 3 + sourceLenSpare + subScheduleByteLen + counterByteLen + 6, sp.getPacket().length - 2); // -2 to consider PECF, hardcoded
        LOG.info("Scheduled TC at " + time + ": " + StringUtil.toHexDump(packet));
        queueeTcScheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                spacePacketExtracted(vc, firstFrame, packet, true);
            }
        }, new Date(time.toEpochMilli()));
    }

    private boolean isTimeTaggedCommand(SpacePacket sp) {
        // Read type (offset 7) and subtype (byte 8)
        return sp.getPacket()[SpacePacket.SP_PRIMARY_HEADER_LENGTH + 1] == 11 && sp.getPacket()[SpacePacket.SP_PRIMARY_HEADER_LENGTH + 2] == 4;
    }

    private void queuePus1(SpacePacket packet, int subtype) {
        // look for a packet with the above APID, type and subtype. If there is none, get the first pus type,subtype that you find
        for (TmPacketTemplate pt : this.pus1Packets) {
            if (pt.apid == packet.getApid() && pt.subtype == subtype) {
                // Found
                SpacePacket sp = pt.generate(Arrays.copyOfRange(packet.getPacket(), 0, 4));
                LOG.info("Queueing PUS 1 packet: " + StringUtil.toHexDump(sp.getPacket()));
                try {
                    addPacketToQueue(sp, false);
                    synchronized (performanceSampler) {
                        ++packetsPerSecond;
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, "", e);
                }
                return;
            }
        }
        // Arrived here? Get the first one with type,subtype, no apid
        for (TmPacketTemplate pt : this.pus1Packets) {
            if (pt.subtype == subtype) {
                // Found
                SpacePacket sp = pt.generate(Arrays.copyOfRange(packet.getPacket(), 0, 4));
                LOG.info("Queueing PUS 1 packet (def): " + StringUtil.toHexDump(sp.getPacket()));
                try {
                    addPacketToQueue(sp, false);
                    synchronized (performanceSampler) {
                        ++packetsPerSecond;
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, "", e);
                }
                return;
            }
        }
        LOG.warning("Queueing PUS 1 packet with subtype " + subtype + " not found");
    }

    public void stopProcessing() {
        if(!running) {
            return;
        }
        running = false;
        tmThread.interrupt();
        generationThread.interrupt();
        packetsToSend.clear();
        performanceSampler.cancel();
    }

    @Override
    public void onStateUpdated(ServiceInstance si, ServiceInstanceState state) {

    }

    @Override
    public void onPduReceived(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {

    }

    @Override
    public void onPduSent(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
        if(operation instanceof RafTransferBuffer) {
            synchronized (performanceSampler) {
                framesPerSecond += ((RafTransferBuffer) operation).getFrameOrNotification().size();
            }
        }
    }

    @Override
    public void onPduSentError(ServiceInstance si, Object operation, String name, byte[] encodedOperation, String error, Exception exception) {

    }

    @Override
    public void onPduDecodingError(ServiceInstance serviceInstance, byte[] encodedOperation) {

    }

    @Override
    public void onPduHandlingError(ServiceInstance serviceInstance, Object operation, byte[] encodedOperation) {

    }

    private class TmPacketTemplate {
        private final PacketDefinition definition;
        private final int apid;
        private final int type;
        private final int subtype;
        private final TmPusConfiguration pusConfiguration;

        public TmPacketTemplate(PacketDefinition pd, int apid, int type, int subtype, TmPusConfiguration pusConfiguration) {
            this.definition = pd;
            this.apid = apid;
            this.type = type;
            this.subtype = subtype;
            this.pusConfiguration = pusConfiguration;
        }

        private SpacePacket generate() {
            int destIdLen = pusConfiguration.getDestinationLength();
            TmPusHeader pusHeader = new TmPusHeader((byte) 1, (short) type, (short) subtype, null, destIdLen == 0 ? null : 0, Instant.now(), null);
            byte[] encodedPusHeader = new byte[64];
            int secHeaderLen = pusHeader.encodeTo(encodedPusHeader, 0, destIdLen, pusConfiguration.getObtConfiguration().isExplicitPField(), spacecraftConfiguration.getEpoch() != null ? spacecraftConfiguration.getEpoch().toInstant() : null, pusConfiguration.getTimeDescriptor(), pusConfiguration.getTmSpareLength());
            int apidCounter = getNextCounter(apid);

            SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setApid(apid).setPacketSequenceCount(apidCounter).setSecondaryHeaderFlag(true).setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED).setQualityIndicator(true);
            builder.addData(encodedPusHeader, 0, secHeaderLen);
            byte[] encodedBody;
            try {
                encodedBody = encoder.encode(definition.getId(), resolver);
            } catch (EncodingException e) {
                LOG.severe("Error when encoding " + definition.getId() + " packet body: " + e.getMessage());
                return null;
            }
            builder.addData(encodedBody, SpacePacket.SP_PRIMARY_HEADER_LENGTH + secHeaderLen, encodedBody.length - (SpacePacket.SP_PRIMARY_HEADER_LENGTH + secHeaderLen));
            if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.ISO || pusConfiguration.getTmPecPresent() == PacketErrorControlType.CRC) {
                builder.addData(new byte[2]);
            }
            SpacePacket sp = builder.build();
            forceIdentificationFields(sp);
            if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.ISO) {
                short checksum = PusChecksumUtil.isoChecksum(sp.getPacket(), 0, sp.getLength() - 2);
                ByteBuffer.wrap(sp.getPacket(), sp.getLength() - 2, 2).putShort(checksum);
            } else if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.CRC) {
                short checksum = PusChecksumUtil.crcChecksum(sp.getPacket(), 0, sp.getLength() - 2);
                ByteBuffer.wrap(sp.getPacket(), sp.getLength() - 2, 2).putShort(checksum);
            }
            return sp;
        }

        private void forceIdentificationFields(SpacePacket sp) {
            byte[] data = sp.getPacket();
            for(IdentFieldMatcher matcher : definition.getMatchers()) {
                int val = matcher.getValue();
                IdentField field = matcher.getField();
                if(field.getByteOffset() == 0) { // APID not needed
                    continue;
                }
                applyValue(val, field, data);
            }
        }

        private void applyValue(int val, IdentField field, byte[] data) {
            ByteBuffer bb = ByteBuffer.wrap(data, field.getByteOffset(), field.getByteLength());
            switch(field.getByteLength()) {
                case 1: {
                    bb.put((byte) val);
                }
                break;
                case 2: {
                    bb.putShort((short) val);
                }
                break;
                case 4: {
                    bb.putInt(val);
                }
                break;
            }
        }

        private SpacePacket generate(byte[] encodedBody) {
            int destIdLen = pusConfiguration.getDestinationLength();
            TmPusHeader pusHeader = new TmPusHeader((byte) 1, (short) type, (short) subtype, null, destIdLen == 0 ? null : 0, Instant.now(), null);
            byte[] encodedPusHeader = new byte[64];
            int secHeaderLen = pusHeader.encodeTo(encodedPusHeader, 0, destIdLen, pusConfiguration.getObtConfiguration().isExplicitPField(), spacecraftConfiguration.getEpoch() != null ? spacecraftConfiguration.getEpoch().toInstant() : null, pusConfiguration.getTimeDescriptor(), pusConfiguration.getTmSpareLength());
            int apidCounter = getNextCounter(apid);

            SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setApid(apid).setPacketSequenceCount(apidCounter).setSecondaryHeaderFlag(true).setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED).setQualityIndicator(true);
            builder.addData(encodedPusHeader, 0, secHeaderLen);
            builder.addData(encodedBody);
            if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.ISO || pusConfiguration.getTmPecPresent() == PacketErrorControlType.CRC) {
                builder.addData(new byte[2]);
            }
            SpacePacket sp = builder.build();
            forceIdentificationFields(sp);
            if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.ISO) {
                short checksum = PusChecksumUtil.isoChecksum(sp.getPacket(), 0, sp.getLength() - 2);
                ByteBuffer.wrap(sp.getPacket(), sp.getLength() - 2, 2).putShort(checksum);
            } else if (pusConfiguration.getTmPecPresent() == PacketErrorControlType.CRC) {
                short checksum = PusChecksumUtil.crcChecksum(sp.getPacket(), 0, sp.getLength() - 2);
                ByteBuffer.wrap(sp.getPacket(), sp.getLength() - 2, 2).putShort(checksum);
            }
            return sp;
        }
    }
}
