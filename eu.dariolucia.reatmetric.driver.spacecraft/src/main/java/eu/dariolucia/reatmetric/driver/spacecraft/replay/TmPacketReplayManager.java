/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.replay;

import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This connector allow to re-inject all TM packets of good quality (no time packets) from the provided archive location
 * and between the two provided times.
 */
public class TmPacketReplayManager extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(TmPacketReplayManager.class.getName());

    public static final String ARCHIVE_LOCATION_KEY = "archive.location";
    public static final String SPACECRAFT_ID_KEY = "spacecraft.id";
    public static final String START_TIME_KEY = "start.time";
    public static final String END_TIME_KEY = "end.time";

    protected final SpacecraftConfiguration spacecraftConfiguration;
    protected final IRawDataBroker broker;

    private volatile Instant lastSamplingTime;
    private volatile long rxBytes; // injected bytes

    private volatile IArchive externalArchive;
    private volatile Thread extractionThread;
    private volatile boolean extracting;

    public TmPacketReplayManager(SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        super(spacecraftConfiguration.getName() + " Replay Connector", "Replay connector based on raw data re-ingestion");
        this.broker = broker;
        this.spacecraftConfiguration = spacecraftConfiguration;
    }

    @Override
    protected void addToInitialisationMap(Map<String, Object> initialisationMap, Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap) {
        initialisationDescriptionMap.put(ARCHIVE_LOCATION_KEY, Pair.of("Location of the archive", ValueTypeEnum.CHARACTER_STRING));
        initialisationDescriptionMap.put(START_TIME_KEY, Pair.of("Start time", ValueTypeEnum.ABSOLUTE_TIME));
        initialisationDescriptionMap.put(END_TIME_KEY, Pair.of("End time", ValueTypeEnum.ABSOLUTE_TIME));
        initialisationDescriptionMap.put(SPACECRAFT_ID_KEY, Pair.of("Spacecraft ID", ValueTypeEnum.UNSIGNED_INTEGER));

        initialisationMap.put(SPACECRAFT_ID_KEY, spacecraftConfiguration.getId());
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        Instant now = Instant.now();
        if(lastSamplingTime != null) {
            long rxRate = Math.round((rxBytes * 8000.0) / (now.toEpochMilli() - lastSamplingTime.toEpochMilli()));
            lastSamplingTime = now;
            return Pair.of(9L, rxRate);
        } else {
            lastSamplingTime = now;
            return null;
        }
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        if(extracting) {
            throw new TransportException("Already extracting, cannot proceed");
        }
        // Check presence of required properties, start thread that reads packets in chunks (100 or 1000 each) and re-inject
        // the data, stop thread and close archive once done
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        String archiveLocation = (String) getInitialisationMap().get(ARCHIVE_LOCATION_KEY);
        final Instant startTime = (Instant) getInitialisationMap().get(START_TIME_KEY);
        final Instant endTime = (Instant) getInitialisationMap().get(END_TIME_KEY);
        final String spacecraftId = String.valueOf(getInitialisationMap().getOrDefault(SPACECRAFT_ID_KEY, spacecraftConfiguration.getId()));
        if(archiveLocation == null || startTime == null || endTime == null) {
            updateAlarmState(AlarmState.ALARM);
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            throw new TransportException("Required properties are not all set: archive location=" + archiveLocation + ", start time=" + startTime + ", end time=" + endTime);
        }
        ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
        if (archiveLoader.findFirst().isPresent()) {
            try {
                externalArchive = archiveLoader.findFirst().get().buildArchive(archiveLocation);
                externalArchive.connect();
            } catch (ArchiveException e) {
                updateAlarmState(AlarmState.ALARM);
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                throw new TransportException("Cannot open archive at " + archiveLocation + ": " + e.getMessage(), e);
            }
        } else {
            updateAlarmState(AlarmState.ALARM);
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            throw new TransportException("Archive service not found");
        }
        // You have the archive now, so start the thread to process it
        updateAlarmState(AlarmState.NOMINAL);
        updateConnectionStatus(TransportConnectionStatus.OPEN);
        extracting = true;
        extractionThread = new Thread(() -> {
            extract(startTime, endTime, spacecraftId);
        });
        extractionThread.setDaemon(true);
        extractionThread.start();
    }

    private void extract(Instant startTime, Instant endTime, String spacecraftId) {
        IArchive arc = externalArchive;
        IRawDataArchive rawDataArch = arc.getArchive(IRawDataArchive.class);
        RawDataFilter rdf = new RawDataFilter(true, null, null, Collections.singletonList(Constants.T_TM_PACKET), Collections.singletonList(spacecraftId), Collections.singletonList(Quality.GOOD));
        RawData lastExtracted = null;
        boolean errorDetected = false;
        while(extracting) {
            try {
                List<RawData> extractedPackets = null;
                if (lastExtracted == null) {
                    extractedPackets = rawDataArch.retrieve(startTime, 1000, RetrievalDirection.TO_FUTURE, rdf);
                } else {
                    extractedPackets = rawDataArch.retrieve(lastExtracted, 1000, RetrievalDirection.TO_FUTURE, rdf);
                }
                // Iterate and map packets
                List<RawData> mappedPackets = new ArrayList<>(extractedPackets.size());
                for (RawData pkt : extractedPackets) {
                    if (!extracting) {
                        break;
                    }
                    if (pkt.getGenerationTime().isAfter(endTime)) {
                        // Need to stop
                        extracting = false;
                        break; // break the for loop
                    } else {
                        // Map packet
                        mappedPackets.add(mapPacket(pkt));
                    }
                    lastExtracted = pkt;
                }
                // Distribute and eventually stop
                if(!mappedPackets.isEmpty()) {
                    broker.distribute(mappedPackets);
                }
            } catch (ArchiveException e) {
                LOG.log(Level.SEVERE, "Archive error when retrieving data for replay: " + e.getMessage(), e);
                updateAlarmState(AlarmState.ALARM);
                errorDetected = true;
                extracting = false;
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Raw data broker error when distributing data for replay: " + e.getMessage(), e);
                updateAlarmState(AlarmState.ALARM);
                errorDetected = true;
                extracting = false;
            }
        }
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        // Stop archive
        try {
            arc.dispose();
        } catch (ArchiveException e) {
            LOG.log(Level.SEVERE, "Archive error when disposing: " + e.getMessage(), e);
            errorDetected = true;
        }
        updateConnectionStatus(errorDetected ? TransportConnectionStatus.ERROR : TransportConnectionStatus.IDLE);
        if(!errorDetected) {
            updateAlarmState(AlarmState.NOT_APPLICABLE);
        }
    }

    private RawData mapPacket(RawData pkt) {
        RawData toReturn = new RawData(broker.nextRawDataId(), pkt.getGenerationTime(), pkt.getName(), pkt.getType(), pkt.getRoute(), pkt.getSource(), pkt.getQuality(), pkt.getRelatedItem(), pkt.getContents(), pkt.getReceptionTime(), pkt.getExtension());
        // Create also the packet object
        SpacePacket sp = new SpacePacket(toReturn.getContents(), pkt.getQuality() == Quality.GOOD);
        toReturn.setData(sp);
        return toReturn;
    }

    @Override
    protected synchronized void doDisconnect() throws TransportException {
        if(extracting) {
            extracting = false;
        }
    }

    @Override
    protected synchronized void doDispose() {
        if(extracting) {
            extracting = false;
        }
    }

    @Override
    public synchronized void abort() throws TransportException {
        disconnect();
    }
}
