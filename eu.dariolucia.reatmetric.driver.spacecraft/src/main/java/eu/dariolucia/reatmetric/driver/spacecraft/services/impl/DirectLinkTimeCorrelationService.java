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

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.services.TimeCorrelationServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class should be used for testing purposes only:
 * <ul>
 *      <li>time packets are simply ignored: TM packets with PUS type 3 and 5 are used for this process, max 1 packet per second</li>
 *      <li>the generation time of a TM packet is set to the generation time of the TM frame containing the start of the packet (if any)
 *      <li>if no frame is available (direct packet injection), then the generation time is set to the reception time of the packet by ReatMetric minus the propagation delay</li>
 *      <li>if no frame and no packet is available (direct time correlation request), then the returned correlated UTC time is the one based on time coefficients</li>
 *      <li>the OBT of TM packets are extracted and a pair (OBT of the packet, UTC of the assigned generation time to the packet) is computed</li>
 *      <li>pairs are used to build time correlation coefficients - best fit</li>
 *      <li>time tagged commands apply these coefficients when computing the OBT time to be inserted in PUS 11,4 packets</li>
 * </ul>
 *
 * It must be clear that this implementation does not compute time coefficients as defined by the ECSS standard. It is useful in cases where the
 * system is used with simulators with unreliable OBT generation or with missing time packet information, but it shall not be used for real operational scenarios,
 * since it has the following limitations:
 * <ul>
 *     <li>The generation time of TM packets is not close to reality: it is a good approximation for live telemetry, but
 *     it loses completely its meaning in case of onboard stored TM packets</li>
 *     <li>Direct OBT correlation to UTC is going to be imprecise</li>
 *     <li>Time tagged commands will have an unreliable execution time</li>
 * </ul>
 */
public class DirectLinkTimeCorrelationService extends TimeCorrelationService implements IRawDataSubscriber, ITimeCorrelation {

    private static final Logger LOG = Logger.getLogger(DirectLinkTimeCorrelationService.class.getName());

    private volatile long lastProcessedTimePacket = -1;

    @Override
    protected TimeCorrelationServiceConfiguration loadConfiguration(String serviceConfigurationPath) throws IOException {
        return TimeCorrelationServiceConfiguration.load(new FileInputStream(serviceConfigurationPath));
    }

    @Override
    protected void initialiseModelFrom(IArchive archiveToUse, Instant latestGenerationTime) {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Time coefficients for spacecraft " + getSpacecraftId() + " at time " + latestGenerationTime + " not restored by this time service implementation");
        }
    }

    @Override
    protected void subscribeToRawDataBroker() {
        // No frame subscription is needed, time packets are not used
    }

    @Override
    public Instant toUtc(Instant obt, AbstractTransferFrame frame, SpacePacket spacePacket) {
        if(frame != null) {
            // Use the generation time of the frame
            return (Instant) frame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME);
        } else if(spacePacket != null) {
            // Use the space packet reception time
            Instant receptionTime = (Instant) spacePacket.getAnnotationValue(Constants.ANNOTATION_RCP_TIME);
            if(receptionTime != null) {
                receptionTime = receptionTime.minus(getPropagationDelay(), ChronoUnit.MICROS);
                return receptionTime;
            }
        }
        // If all is null, then fallback to parent class implementation (time coefficients)
        return super.toUtc(obt, frame, spacePacket);
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        // Do nothing, should actually not be called
        throw new IllegalStateException("Call to dataItemsReceived should not happen, this is a software bug");
    }

    @Override
    public void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        long currentTime = System.currentTimeMillis();
        // Process max 1 TM packet per second, i.e max 1 time coefficient update per second
        if(lastProcessedTimePacket == -1 || currentTime - lastProcessedTimePacket > 1000) {
            lastProcessedTimePacket = currentTime;
            // Time packets are ignored, only TM/PUS packets with type 3 and type 5 are used by this implementation (see getSubscriptionFilter)
            Instant utcTime = packetRawData.getGenerationTime();
            Instant onboardTime = tmPusHeader != null ? tmPusHeader.getAbsoluteTime() : null;
            if(onboardTime != null) {
                addTimeCouple(onboardTime, utcTime);
            }
            // Ignore the rest
        }
    }

    @Override
    public void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // Nothing to be done, update of generation time period not needed
    }

    @Override
    public String getName() {
        return "Direct Link Time Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, sp, pusType, pusSubtype, destination, source) -> sp.getApid() != 0 && pusType != null && (pusType == 3 || pusType == 5);
    }
}
