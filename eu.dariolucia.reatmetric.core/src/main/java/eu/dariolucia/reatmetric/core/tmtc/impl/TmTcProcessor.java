/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.impl;

import eu.dariolucia.ccsds.encdec.bit.BitEncoderDecoder;
import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.impl.FieldGroupBasedPacketIdentifier;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.ccsds.encdec.time.impl.DefaultGenerationTimeProcessor;
import eu.dariolucia.ccsds.encdec.value.TimeUtil;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.AbstractReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.AosReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.IVirtualChannelReceiverOutput;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.TmReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.common.CommandRequestStatus;
import eu.dariolucia.reatmetric.core.message.IMessageProcessor;
import eu.dariolucia.reatmetric.core.storage.impl.StorageProcessor;
import eu.dariolucia.reatmetric.core.tmtc.ITmTcProcessor;
import eu.dariolucia.reatmetric.core.tmtc.ITmTcProcessorListener;
import eu.dariolucia.reatmetric.core.tmtc.definition.CdsConfiguration;
import eu.dariolucia.reatmetric.core.tmtc.definition.CucConfiguration;
import eu.dariolucia.reatmetric.core.tmtc.definition.TmTcConfiguration;
import eu.dariolucia.reatmetric.core.util.ThreadUtil;
import eu.dariolucia.reatmetric.core.util.UniqueIdUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class TmTcProcessor implements ITmTcProcessor {

    public static final String TMTC_CONFIGURATION_KEY = "tmtc.configuration";
    public static final String TMTC_ENCDEC_FILE_KEY = "tmtc.encdec.configuration";

    public static final String RAWDATA_BAD_FRAME_TYPE = "BAD TF";
    public static final String RAWDATA_TM_FRAME_TYPE = "TM TF";
    public static final String RAWDATA_AOS_FRAME_TYPE = "AOS TF";
    public static final String RAWDATA_TC_FRAME_TYPE = "TC TF";
    public static final String RAWDATA_TM_PACKET_TYPE = "TM PKT";
    public static final String RAWDATA_TC_PACKET_TYPE = "TC PKT";

    public static final String SOURCE_ID = "TM/TC Processor";

    private final long startupTime = System.currentTimeMillis();

    private final TmTcConfiguration configuration;

    private final Definition encodingDecodingDefinition;

    private final IPacketIdentifier packetIdentifier;

    private final IPacketDecoder packetDecoder;

    private final List<TmTcSubscription> listeners = new CopyOnWriteArrayList<>();

    private volatile IMessageProcessor logger;

    private volatile StorageProcessor storer;

    private volatile ITmTcProcessorListener listener;

    private Map<Integer, Map<Integer, AbstractReceiverVirtualChannel<?>>> type2vc2pipeline = new HashMap<>();

    private final IVirtualChannelReceiverOutput packetOutputListener;

    private final ExecutorService frameHandler = ThreadUtil.newSingleThreadExecutor("TM Handler");

    private final Map<Class<?>, String> frameClass2type = new HashMap<>();

    public TmTcProcessor() {
        this.configuration = readTmTcConfiguration();
        this.encodingDecodingDefinition = readEncodingDecodingDefinition();
        this.packetOutputListener = createPacketListener();
        this.packetIdentifier = new FieldGroupBasedPacketIdentifier(this.encodingDecodingDefinition);
        this.packetDecoder = new DefaultPacketDecoder(this.encodingDecodingDefinition);

        this.frameClass2type.put(AosTransferFrame.class, RAWDATA_AOS_FRAME_TYPE);
        this.frameClass2type.put(TmTransferFrame.class, RAWDATA_TM_FRAME_TYPE);
        this.frameClass2type.put(TcTransferFrame.class, RAWDATA_TC_FRAME_TYPE);
    }

    private IVirtualChannelReceiverOutput createPacketListener() {
        return new IVirtualChannelReceiverOutput() {
            @Override
            public void transferFrameReceived(AbstractReceiverVirtualChannel abstractReceiverVirtualChannel, AbstractTransferFrame abstractTransferFrame) {
                // Ignore
            }

            @Override
            public void spacePacketExtracted(AbstractReceiverVirtualChannel abstractReceiverVirtualChannel, AbstractTransferFrame abstractTransferFrame, byte[] packet, boolean quality) {
                processPacket(abstractReceiverVirtualChannel.getVirtualChannelId(), new SpacePacket(packet, quality));
            }

            @Override
            public void dataExtracted(AbstractReceiverVirtualChannel abstractReceiverVirtualChannel, AbstractTransferFrame abstractTransferFrame, byte[] bytes) {
                // Ignore
            }

            @Override
            public void bitstreamExtracted(AbstractReceiverVirtualChannel abstractReceiverVirtualChannel, AbstractTransferFrame abstractTransferFrame, byte[] bytes, int i) {
                // Ignore
            }

            @Override
            public void gapDetected(AbstractReceiverVirtualChannel abstractReceiverVirtualChannel, int expected, int received, int missing) {
                logger.raiseMessage("Gap detected on VC " + abstractReceiverVirtualChannel.getVirtualChannelId() + ", expected VCFC " + expected + ", received VCFC " + received + ", " + missing + " missing frames", SOURCE_ID, Severity.WARN);
            }
        };
    }

    private Definition readEncodingDecodingDefinition() {
        try {
            return Definition.load(new FileInputStream(System.getProperty(TMTC_ENCDEC_FILE_KEY)));
        } catch (IOException e) {
            throw new RuntimeException("Error in configuration of the TM TC processor when reading definitions at " + System.getProperty(TMTC_ENCDEC_FILE_KEY), e);
        }
    }

    private TmTcConfiguration readTmTcConfiguration() {
        try {
            return TmTcConfiguration.load(new FileInputStream(System.getProperty(TMTC_CONFIGURATION_KEY)));
        } catch (IOException e) {
            throw new RuntimeException("Error in configuration of the TM TC processor when reading configuration at " + System.getProperty(TMTC_CONFIGURATION_KEY), e);
        }
    }

    @Override
    public void setLogger(IMessageProcessor logger) {
        this.logger = logger;
    }

    @Override
    public void setStorer(StorageProcessor storer) {
        this.storer = storer;
    }

    @Override
    public void setListener(ITmTcProcessorListener listener) {
        this.listener = listener;
    }

    @Override
    public void sendTcRequest(long tcId, String definition, Map<String, Object> values) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void onFrameReceived(byte[] frame, boolean quality, Instant ert, byte[] annotations) {
        this.frameHandler.submit(() -> {
            // frame construction and storage, packet extraction and storage, packet decoding and push
            if (quality) {
                processGoodFrame(frame, ert, annotations);
            } else {
                processBadFrame(frame, ert, annotations);
            }
        });
    }

    private void processBadFrame(byte[] frame, Instant ert, byte[] annotations) {
        // Create raw data object
        IUniqueId id = UniqueIdUtil.generateNextId(RawData.class);
        RawData rd = new RawData(id, "TM FRAME", RAWDATA_BAD_FRAME_TYPE, "", ert, Instant.now(), "", Quality.BAD, new Object[] {
                null, null, null, null, null, null, annotations, frame
        });
        // Store and distribute
        store(rd);
        distribute(rd);
    }

    private void store(RawData rawData) {
        this.storer.storeRawData(Collections.singletonList(rawData));
    }

    private void distribute(RawData rawData) {
        this.listeners.forEach(o -> o.distribute(Collections.singletonList(rawData)));
    }

    private void processGoodFrame(byte[] frame, Instant ert, byte[] annotations) {
        // Check if AOS or TM: first 2 bits: if 00 is TM, if 01 is AOS
        boolean tm = (frame[0] & (byte) 0xC0) == 0;
        if (tm) {
            TmTransferFrame ttf = new TmTransferFrame(frame, configuration.getTmVirtualChannelConfigurations().isFecfPresent());
            storeAndDistributeFrame(ttf, ert, annotations);
            if(!ttf.isIdleFrame()) {
                injectPipeline(ttf);
            }
        } else {
            AosTransferFrame ttf = new AosTransferFrame(frame,
                    configuration.getTmVirtualChannelConfigurations().isAosFrameHeaderErrorControlPresent(),
                    configuration.getTmVirtualChannelConfigurations().getAosTransferFrameInsertZoneLength(),
                    AosTransferFrame.UserDataType.M_PDU,
                    configuration.getTmVirtualChannelConfigurations().isOcfPresent(),
                    configuration.getTmVirtualChannelConfigurations().isFecfPresent());
            storeAndDistributeFrame(ttf, ert, annotations);
            // CLCW processing for COP-1
            if(!ttf.isIdleFrame()) {
                injectPipeline(ttf);
            }
        }
    }

    private void storeAndDistributeFrame(AbstractTransferFrame ttf, Instant ert, byte[] annotations) {
        // Create raw data object
        IUniqueId id = UniqueIdUtil.generateNextId(RawData.class);
        String type = frameClass2type.get(ttf.getClass());

        RawData rd = new RawData(id, "TM FRAME", type, "", ert, Instant.now(), "", Quality.GOOD, new Object[] {
                ttf.getSpacecraftId(), ttf.getVirtualChannelId(), null, ttf.getVirtualChannelFrameCount(), null, null, annotations, ttf.getFrame()
        });
        // Store and distribute
        store(rd);
        distribute(rd);
    }

    private void injectPipeline(AbstractTransferFrame ttf) {
        Map<Integer, AbstractReceiverVirtualChannel<?>> framePipeline = this.type2vc2pipeline.get((int) ttf.getTransferFrameVersionNumber());
        if (framePipeline == null) {
            framePipeline = new HashMap<>();
            this.type2vc2pipeline.put((int) ttf.getTransferFrameVersionNumber(), framePipeline);
        }
        AbstractReceiverVirtualChannel pipeline = framePipeline.get((int) ttf.getVirtualChannelId());
        if (pipeline == null) {
            if (ttf instanceof TmTransferFrame) {
                pipeline = new TmReceiverVirtualChannel(ttf.getVirtualChannelId(), VirtualChannelAccessMode.Packet, true);
                pipeline.register(this.packetOutputListener);
            } else if (ttf instanceof AosTransferFrame) {
                pipeline = new AosReceiverVirtualChannel(ttf.getVirtualChannelId(), VirtualChannelAccessMode.Packet, true);
                pipeline.register(this.packetOutputListener);
            } else {
                logger.raiseMessage("Transfer frame type not supported: " + ttf.getClass().getName() + ", no packet extraction, transfer frame skipped", SOURCE_ID, Severity.WARN);
                return;
            }
            framePipeline.put((int) ttf.getVirtualChannelId(), pipeline);
        }
        pipeline.processFrame(ttf);
    }

    private void processPacket(int vcId, SpacePacket spacePacket) {
        // Identify packet
        String name = null;
        try {
            if(spacePacket.isIdle()) {
                name = "IDLE PACKET";
            } else {
                name = this.packetIdentifier.identify(spacePacket.getPacket());
            }
        } catch(Exception e) {
            logger.raiseMessage("Cannot identify space packet of " + spacePacket.getLength() + " bytes from VC " + vcId, SOURCE_ID, Severity.WARN);
        }
        // Get PUS information: type, subtype, OBT
        PusCharacteristics pusData = computePusCharacteristics(spacePacket);
        // Create raw data object
        IUniqueId id = UniqueIdUtil.generateNextId(RawData.class);
        RawData rd = new RawData(id, name != null ? name : "UNKNOWN", RAWDATA_TM_PACKET_TYPE, "", pusData.getGenerationTime(), Instant.now(), "", spacePacket.isQualityIndicator() ? Quality.GOOD : Quality.BAD, new Object[] {
                null, vcId, (int) spacePacket.getApid(), (int) spacePacket.getPacketSequenceCount(), pusData.getPusType(), pusData.getPusSubType(), null, spacePacket.getPacket()
        });

        // Store
        store(rd);
        // Distribute
        distribute(rd);

        // TC processing associated (PUS1)

        // Decode and notify up: only if packet was identified
        if(!spacePacket.isIdle() && name != null && spacePacket.isQualityIndicator()) {
            try {
                DecodingResult result = this.packetDecoder.decode(name,
                        spacePacket.getPacket(),
                        pusData.getDataOffset(),
                        spacePacket.getLength() - pusData.getDataOffset(),
                        new DefaultGenerationTimeProcessor(pusData.getGenerationTime()));
                // Inform upper layer
                this.listener.packetDecoded(spacePacket, result.getDecodedParameters());
            } catch (Exception e) {
                e.printStackTrace();
                logger.raiseMessage("Cannot decode space packet " + name + " due to decoding error: " + e.getMessage(), SOURCE_ID, Severity.ALARM);
            }
        }
    }

    private PusCharacteristics computePusCharacteristics(SpacePacket spacePacket) {
        int destLen = this.configuration.getPusConfiguration().getDestinationLength();
        ByteBuffer bb = ByteBuffer.wrap(spacePacket.getPacket(), 7, spacePacket.getLength() - 7);
        int pusType = Byte.toUnsignedInt(bb.get());
        int pusSubType = Byte.toUnsignedInt(bb.get());
        for(int i = 0; i < destLen; ++i) {
            bb.get();
        }
        BitEncoderDecoder timeReader = new BitEncoderDecoder(spacePacket.getPacket(), 6 + 3 + destLen, spacePacket.getLength() - 9 - destLen);
        // Read the time
        Instant genTime = null;
        Date epochDate = this.configuration.getPusConfiguration().getEpoch();
        Instant epoch = epochDate != null ? Instant.ofEpochMilli(epochDate.getTime()) : null;
        boolean isPField = this.configuration.getPusConfiguration().isExplicitPField();
        if(this.configuration.getPusConfiguration().getObtConfiguration() instanceof CucConfiguration) {
            CucConfiguration cuc = (CucConfiguration) this.configuration.getPusConfiguration().getObtConfiguration();
            if(isPField) {
                genTime = TimeUtil.fromCUC(timeReader, epoch);
            } else {
                byte[] tField = timeReader.getNextByte(Byte.SIZE * (cuc.getCoarse() + cuc.getFine()));
                genTime = TimeUtil.fromCUC(tField, epoch, cuc.getCoarse(), cuc.getFine());
            }
        } else if(this.configuration.getPusConfiguration().getObtConfiguration() instanceof CdsConfiguration) {
            CdsConfiguration cds = (CdsConfiguration) this.configuration.getPusConfiguration().getObtConfiguration();
            if(isPField) {
                genTime = TimeUtil.fromCDS(timeReader, epoch);
            } else {
                int daysByte = cds.isShortDays() ? 2 : 3;
                int subMilliBytes = cds.getSubtimeResolution();
                byte[] tField = timeReader.getNextByte(Byte.SIZE * (daysByte + 4 + subMilliBytes));
                genTime = TimeUtil.fromCDS(tField, epoch, cds.isShortDays(), cds.getSubtimeResolution()/2); // 1 means short, 2 means int
            }
        } else {
            logger.raiseMessage("OBT specification in configuration unknown: " + this.configuration.getPusConfiguration().getObtConfiguration().getClass(), SOURCE_ID, Severity.ALARM);
        }

        if(genTime == null) {
            logger.raiseMessage("Cannot derive OBT for space packet with APID " + spacePacket.getApid() + ", PUS Type " + pusType + ", PUS SubType " + pusSubType, SOURCE_ID, Severity.WARN);
            genTime = Instant.now();
        }

        return new PusCharacteristics(genTime, destLen, pusType, pusSubType, 6 + 3 + destLen + timeReader.getCurrentBitIndex()/8);
    }

    @Override
    public void onClcwReceived(Clcw clcw) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void onTcFrameStatusUpdate(long tcId, CommandRequestStatus status, String error) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public byte[] getRawDataContents(RawData data) {
        // In this implementation, the byte array is always in the last entry of the additional field
        return (byte[]) data.getAdditionalFields()[data.getAdditionalFields().length - 1];
    }

    @Override
    public void subscribe(IRawDataSubscriber subscriber, RawDataFilter filter) {
        this.listeners.add(new TmTcSubscription(subscriber, filter));
    }

    @Override
    public void unsubscribe(IRawDataSubscriber subscriber) {
        Optional<TmTcSubscription> toBeRemoved = this.listeners.stream().filter(o -> o.getSubscriber().equals(subscriber)).findFirst();
        toBeRemoved.ifPresent(TmTcSubscription::terminate);
        toBeRemoved.ifPresent(this.listeners::remove);
    }

    @Override
    public List<RawData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        return this.storer.retrieveRawData(startTime, numRecords, direction, filter);
    }

    @Override
    public List<RawData> retrieve(RawData startItem, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        return this.storer.retrieveRawData(startItem, numRecords, direction, filter);
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Arrays.asList(
                new FieldDescriptor("SCID", FieldType.INTEGER, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("VC", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("APID", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("SCC", FieldType.INTEGER, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("PUS Type", FieldType.INTEGER, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("PUS SubType", FieldType.INTEGER, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("Annotations", FieldType.HIDDEN, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("Data", FieldType.HIDDEN, FieldFilterStrategy.SINGLE_VALUE)
        );
    }
}
