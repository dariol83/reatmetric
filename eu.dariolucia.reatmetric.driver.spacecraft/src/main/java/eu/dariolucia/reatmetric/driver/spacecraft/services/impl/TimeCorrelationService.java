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

import eu.dariolucia.ccsds.encdec.bit.BitEncoderDecoder;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.value.TimeUtil;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.*;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.PacketErrorControlType;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.services.TimeCorrelationServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the ECSS PUS 9 time reporting service.
 */
public class TimeCorrelationService extends AbstractPacketService<TimeCorrelationServiceConfiguration> implements IRawDataSubscriber, ITimeCorrelation {

    private static final Logger LOG = Logger.getLogger(TimeCorrelationService.class.getName());

    private static final int MATCHING_FRAMES_MAX_SIZE = 16;

    private int spacecraftId;
    private long propagationDelay;
    private ExecutorService timeCoefficientsDistributor;
    private Instant epoch;
    private volatile int generationPeriod;

    private final List<RawData> matchingFrames = new LinkedList<>();
    private final List<Pair<Instant, Instant>> timeCouples = new LinkedList<>();
    // Why BigDecimal? If you want to keep nanosecond precision, double resolution can keep up to microsecond and
    // CUC 4,3 has a resolution of 59.6 nsec, CUC 4,4 is at picosecond level
    private volatile Pair<BigDecimal, BigDecimal> obt2gtCoefficients;

    @Override
    public void postInitialisation() {
        this.epoch = spacecraftConfiguration().getEpoch() == null ? null : Instant.ofEpochMilli(spacecraftConfiguration().getEpoch().getTime());
        this.spacecraftId = spacecraftConfiguration().getId();
        this.propagationDelay = spacecraftConfiguration().getPropagationDelay();
        this.generationPeriod =  configuration().getGenerationPeriod();
        this.timeCoefficientsDistributor = Executors.newSingleThreadExecutor((r) -> {
            Thread t = new Thread(r, "Spacecraft " + spacecraftConfiguration().getName() + " - Time Correlation Service Coefficients Distributor");
            t.setDaemon(true);
            return t;
        });
        this.obt2gtCoefficients = Pair.of(BigDecimal.valueOf(configuration().getInitialCoefficientM()), BigDecimal.valueOf(configuration().getInitialCoefficientQ()));
        subscribeToRawDataBroker();
    }

    @Override
    protected TimeCorrelationServiceConfiguration loadConfiguration(String serviceConfigurationPath) throws IOException {
        return TimeCorrelationServiceConfiguration.load(new FileInputStream(serviceConfigurationPath));
    }

    @Override
    protected void initialiseModelFrom(IArchive archiveToUse, Instant latestGenerationTime) throws ReatmetricException {
        IRawDataArchive archive = archiveToUse.getArchive(IRawDataArchive.class);
        List<RawData> data = archive.retrieve(latestGenerationTime, 1, RetrievalDirection.TO_PAST, new RawDataFilter(true, Constants.N_TIME_COEFFICIENTS, null, Collections.singletonList(Constants.T_TIME_COEFFICIENTS), Collections.singletonList(String.valueOf(spacecraftId)), Collections.singletonList(Quality.GOOD)));
        if(!data.isEmpty()) {
            String coeffs = new String(data.get(0).getContents(), StandardCharsets.US_ASCII);
            BigDecimal first = new BigDecimal(coeffs.substring(0, coeffs.indexOf('|')));
            BigDecimal second = new BigDecimal(coeffs.substring(coeffs.indexOf('|') + 1));
            this.obt2gtCoefficients = Pair.of(first, second);
        } else {
            if(LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Time coefficients for spacecraft " + spacecraftId + " at time " + latestGenerationTime + " not found");
            }
        }
    }

    private void subscribeToRawDataBroker() {
        // Subscribe to raw data to receive the frames of the spacecraft on VC 0
        RawDataFilter frameFilter = new RawDataFilter(true,
                null,
                null,
                Arrays.asList(Constants.T_TM_FRAME, Constants.T_AOS_FRAME),
                Collections.singletonList(String.valueOf(spacecraftId)),
                Collections.singletonList(Quality.GOOD));
        // I do not put there the check on the generation period modulus, because
        // it can change at runtime
        context().getRawDataBroker().subscribe(this, null, frameFilter, o -> {
            AbstractTransferFrame atf = (AbstractTransferFrame) o.getData();
            return atf != null && atf.getVirtualChannelId() == 0;
        });
    }

    /**
     * Given the OBT as extracted by the encdec functions (and therefore already mapped to UTC time), this method
     * returns the corresponding ground-correlated UTC time.
     *
     * @param obt the OBT in UTC time scale
     * @return the corresponding ground correlated time in UTC time scale
     */
    @Override
    public Instant toUtc(Instant obt) {
        Pair<BigDecimal, BigDecimal> coeffs = this.obt2gtCoefficients;
        if(coeffs == null) {
            return obt;
        }
        BigDecimal obtBd = convertToBigDecimal(obt);
        BigDecimal converted = obtBd.multiply(coeffs.getFirst()).add(coeffs.getSecond());
        return convertToInstant(converted);
    }

    /**
     * Given the ground-correlated UTC, this method returns the corresponding OBT in UTC time. Note that encoding the
     * return value using the eu.dariolucia.encdec.TimeUtil class using CUC will cause the correct transformation into
     * TAI time.
     *
     * @param utc the ground time in UTC time scale
     * @return the corresponding OBT in UTC time scale
     */
    @Override
    public Instant toObt(Instant utc) {
        Pair<BigDecimal, BigDecimal> coeffs = this.obt2gtCoefficients;
        if(coeffs == null) {
            return utc;
        }
        BigDecimal utcBd = convertToBigDecimal(utc);
        BigDecimal converted = utcBd.subtract(coeffs.getSecond()).divide(coeffs.getFirst(), 9, RoundingMode.HALF_UP); // 9 digits after dot
        return convertToInstant(converted);
    }

    private Instant convertToInstant(BigDecimal converted) {
        // Integral part: seconds. Decimal part: nanos. Ugly. Ugly ugly ugly. But there is no precision loss.
        BigInteger epochSeconds = converted.toBigInteger();
        BigInteger nanoSeconds = converted.subtract(new BigDecimal(epochSeconds.longValue())).multiply(new BigDecimal(1000000000)).toBigInteger();
        return Instant.ofEpochSecond(epochSeconds.longValue(), nanoSeconds.intValue());
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        for(RawData rd : messages) {
            AbstractTransferFrame atf = (AbstractTransferFrame) rd.getData();
            if(atf != null && atf.getVirtualChannelId() == 0 && atf.getVirtualChannelFrameCount() % generationPeriod == 0) {
                // Remember this frame!
                addMatchingFrame(rd);
            }
        }
    }

    private void addMatchingFrame(RawData frame) {
        synchronized (matchingFrames) {
            this.matchingFrames.add(0, frame);
            if (matchingFrames.size() > MATCHING_FRAMES_MAX_SIZE) {
                matchingFrames.remove(matchingFrames.size() - 1);
            }
        }
    }

    @Override
    public void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        if(spacePacket.isTelemetryPacket() && spacePacket.getApid() == 0) {
            // This is a time packet, so apply C.4 Spacecraft time correlation procedures, ECSS-E-70-41A
            // 1. Locate the correct frame: get the TmFrameDescriptor associated to the packet (extension), the ERT and retrieve the previous frame that respects the generation period
            RawData frame = locateFrame((TmFrameDescriptor) packetRawData.getExtension(), this.generationPeriod);
            // 2. If the frame is located, then compute the time couple: (Earth reception time - propagation delay - onboard delay, on board time)
            if (frame != null) {
                Instant utcTime = frame.getReceptionTime().minusNanos(this.propagationDelay * 1000).minusNanos(configuration().getOnBoardDelay() * 1000);
                Instant onboardTime = extractOnboardTime(spacePacket);
                // 3. Add the time couple: this method triggers a best-fit correlation taking into account the available time couples
                addTimeCouple(onboardTime, utcTime);
            } else {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Cannot find corresponding TM frame for time packet");
                }
            }
        }
        // Ignore the rest
    }

    @Override
    public void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        if(tcTracker.getInfo() != null && tcTracker.getInfo().getPusHeader() != null && tcTracker.getInfo().getPusHeader().getServiceType() == 9 && tcTracker.getInfo().getPusHeader().getServiceSubType() == 1) {
            if(phase == TcPhase.COMPLETED) {
                // The TC is over, change the generation period accordingly
                byte newGenerationPeriod = tcTracker.getPacket().getPacket()[tcTracker.getPacket().getLength() - 1 - (tcTracker.getInfo().getChecksumType() == PacketErrorControlType.NONE ? 0 : 2)];
                this.generationPeriod = 1 << newGenerationPeriod;
            }
        }
    }

    private void addTimeCouple(Instant onboardTime, Instant utcTime) {
        if(LOG.isLoggable(Level.FINE))
        {
            LOG.log(Level.FINE, String.format("Adding time couple: OBT=%s, UTC=%s", onboardTime.toString(), utcTime.toString()));
        }
        this.timeCouples.add(Pair.of(onboardTime, utcTime));
        if(this.timeCouples.size() > 2) {
            this.timeCouples.remove(0);
        }
        updateCoefficients();
    }

    private void updateCoefficients() {
        // Use the two time couples, if available
        if(timeCouples.size() >= 2) {
            Pair<Instant, Instant> firstTimeCouple = timeCouples.get(0);
            Pair<Instant, Instant> secondTimeCouple = timeCouples.get(1);
            // Order so that firstTimeCouple.getFirst() is lower than secondTimeCouple.getFirst()
            if(firstTimeCouple.getFirst().isAfter(secondTimeCouple.getFirst())) {
                // Swap
                Pair<Instant, Instant> temp = firstTimeCouple;
                firstTimeCouple = secondTimeCouple;
                secondTimeCouple = temp;
            }
            // Convert to big decimals: integral part seconds, decimal part nanoseconds
            Pair<BigDecimal, BigDecimal> fTc = convert(firstTimeCouple);
            Pair<BigDecimal, BigDecimal> sTc = convert(secondTimeCouple);
            BigDecimal divisor = fTc.getFirst().subtract(sTc.getFirst());
            if(divisor.doubleValue() == 0.0) {
                // Avoid ArithmeticException
                return;
            }
            BigDecimal m = (fTc.getSecond().subtract(sTc.getSecond())).divide(divisor, 9, RoundingMode.HALF_UP);
            BigDecimal q = sTc.getSecond().subtract(m.multiply(sTc.getFirst()));
            if(LOG.isLoggable(Level.FINE))
            {
                LOG.log(Level.FINE, String.format("Time coefficient generated: m=%s, q=%s", m.toPlainString(), q.toPlainString()));
            }
            this.obt2gtCoefficients = Pair.of(m, q);
            // Distribute the coefficients: generation time is the UTC generation time of the most recent time couple
            distributeCoefficients(this.obt2gtCoefficients, secondTimeCouple.getSecond());
        }
    }

    private void distributeCoefficients(Pair<BigDecimal, BigDecimal> obt2gtCoefficients, Instant generationTime) {
        if(obt2gtCoefficients != null) {
            // Serialize coefficients as String
            String mCoeff = obt2gtCoefficients.getFirst().toPlainString();
            String qCoeff = obt2gtCoefficients.getSecond().toPlainString();
            String derivedString = mCoeff + "|" + qCoeff;
            RawData rd = new RawData(context().getRawDataBroker().nextRawDataId(), generationTime, Constants.N_TIME_COEFFICIENTS, Constants.T_TIME_COEFFICIENTS, "", String.valueOf(spacecraftId), Quality.GOOD, null, derivedString.getBytes(StandardCharsets.US_ASCII), Instant.now(), driverName(), null);
            try {
                context().getRawDataBroker().distribute(Collections.singletonList(rd));
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot store time coefficients [" + derivedString + "] for spacecraft " + spacecraftId + ": " + e.getMessage(), e);
            }
        }
    }

    private Pair<BigDecimal, BigDecimal> convert(Pair<Instant, Instant> firstTimeCouple) {
        BigDecimal first = convertToBigDecimal(firstTimeCouple.getFirst());
        BigDecimal second = convertToBigDecimal(firstTimeCouple.getSecond());
        return Pair.of(first, second);
    }

    private BigDecimal convertToBigDecimal(Instant instant) {
        // Highly inefficient, but avoids rounding errors, should be changed to something more efficient later
        return new BigDecimal(instant.getEpochSecond() + "." + String.format("%09d", instant.getNano()));
    }

    private Instant extractOnboardTime(SpacePacket spacePacket) {
        // The extraction of the OBT time applies by default UTC correction. In fact, the function used in TimeUtil
        // reads the CUC and then converts it into UTC, by applying the epoch (converted to TAI) and then removing the
        // leap seconds up to the TAI date. This action does not harm the the slope computation, because it will
        // consistently applied. The q computation is affected.

        // According to the standard, the time packet has no secondary header, so after SpacePacket.SP_PRIMARY_HEADER_LENGTH, we should have:
        // 1. generation rate field (optional) -> check configuration.isGenerationPeriodReported()
        int idx = SpacePacket.SP_PRIMARY_HEADER_LENGTH;
        if(configuration().isGenerationPeriodReported()) {
            this.generationPeriod = (int) Math.pow(2, Byte.toUnsignedInt(spacePacket.getPacket()[idx]));
            ++idx;
        }
        // 2. time field -> check configuration.getTimeFormat()
        if(configuration().getTimeFormat().isExplicitPField()) {
            // P-Field, easy
            return TimeUtil.fromCUC(new BitEncoderDecoder(spacePacket.getPacket(), idx, spacePacket.getPacket().length - idx), epoch);
        } else {
            return TimeUtil.fromCUC(spacePacket.getPacket(), idx, spacePacket.getPacket().length - idx,
                    epoch,
                    configuration().getTimeFormat().getCoarse(),
                    configuration().getTimeFormat().getFine());
        }
        // 3. status -> mission specific, reported as parameter already, not relevant here
    }

    private RawData locateFrame(TmFrameDescriptor packetFrameDescriptor, int generationPeriod) {
        // From the frame counter of the frame delivering the time packet, calculate the frame referred by the time packet
        int targetVcc = packetFrameDescriptor.getVirtualChannelFrameCounter() - (packetFrameDescriptor.getVirtualChannelFrameCounter() % generationPeriod);
        // Now look for the frame
        synchronized (matchingFrames) {
            for (RawData frame : matchingFrames) {
                if (frame.getData() != null) {
                    AbstractTransferFrame atf = (AbstractTransferFrame) frame.getData();
                    // The frame might be the correct one...
                    if (atf.getVirtualChannelFrameCount() == targetVcc) {
                        // ... but check time consistency with this frame
                        Duration timeBetweenFrames = Duration.between(packetFrameDescriptor.getEarthReceptionTime(), frame.getReceptionTime());
                        if (timeBetweenFrames.toNanos() / 1000 <= configuration().getMaximumFrameTimeDelay()) {
                            // That's the one
                            return frame;
                        }
                    }
                }
            }
        }
        // No frame available
        return null;
    }

    @Override
    public String getName() {
        return "Onboard Time Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, sp, pusType, pusSubtype, destination, source) -> sp.getApid() == 0 || (pusType != null && pusType == 9);
    }

    @Override
    public int getServiceType() {
        return 9;
    }

    @Override
    public boolean isDirectHandler(TcTracker trackedTc) {
        return false;
    }

    public void dispose() {
        context().getRawDataBroker().unsubscribe(this);
        timeCoefficientsDistributor.shutdown();
    }

    @Override
    public void registerRawDataRenderers(Map<String, Function<RawData, LinkedHashMap<String, String>>> serviceRenderers) {
        serviceRenderers.put(Constants.T_TIME_COEFFICIENTS, this::renderTimeCoefficients);
    }

    @Override
    public LinkedHashMap<String, String> renderTimeCoefficients(RawData rawData) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        String coeffs = new String(rawData.getContents(), StandardCharsets.US_ASCII);
        String first = coeffs.substring(0, coeffs.indexOf('|'));
        String second = coeffs.substring(coeffs.indexOf('|') + 1);
        toReturn.put("Time Coefficients", null);
        toReturn.put("M", first);
        toReturn.put("Q", second);
        return toReturn;
    }
}
