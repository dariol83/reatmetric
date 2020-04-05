/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.replay;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This connector allow to re-inject all TM packets of good quality (no time packets) from the provided archive location
 * and between the two provided times.
 */
public class TmPacketReplayManager extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(TmPacketReplayManager.class.getName());

    public static final String ARCHIVE_LOCATION_KEY = "archive.location";
    public static final String START_TIME_KEY = "start.time";
    public static final String END_TIME_KEY = "end.time";

    protected final SpacecraftConfiguration spacecraftConfiguration;
    protected final IRawDataBroker broker;

    private volatile Instant lastSamplingTime;
    private volatile long rxBytes; // injected bytes

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
    protected void doConnect() throws TransportException {
        // TODO: implement: check presence of required properties, start thread that reads packets in chunks (100 or 1000 each) and re-inject
        //  the data, stop thread and close archive once done
    }

    @Override
    protected void doDisconnect() throws TransportException {
        // TODO: implement: stop thread and close archive
    }

    @Override
    protected void doDispose() {
        // TODO: implement: stop thread and close archive
    }

    @Override
    public void abort() throws TransportException {
        // TODO: implement: stop thread and close archive
    }
}
