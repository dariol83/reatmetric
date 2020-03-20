/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.ccsds.encdec.bit.BitEncoderDecoder;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.value.TimeUtil;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TimeCorrelationServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TimeCorrelationService implements IServicePacketSubscriber, IRawDataSubscriber {

    private static final int MATCHING_FRAMES_MAX_SIZE = 16;

    private final int spacecraftId;
    private final long propagationDelay;
    private final IRawDataBroker broker;
    private final SpacecraftConfiguration configuration;
    private final ServiceBroker serviceBroker;
    private volatile int generationPeriod;

    private final List<RawData> matchingFrames = new LinkedList<>();
    private final List<Pair<Instant, Instant>> timeCouples = new LinkedList<>();
    // Why BigDecimal? If you want to keep nanosecond precision, double resolution can keep up to microsecond and CUC 4,3 has a resolution of 59.6 nsec, CUC 4,4 is at picosecond level
    private volatile Pair<BigDecimal, BigDecimal> obt2gtCoefficients;

    public TimeCorrelationService(SpacecraftConfiguration configuration, IServiceCoreContext context, ServiceBroker serviceBroker) {
        this.spacecraftId = configuration.getId();
        this.propagationDelay = configuration.getPropagationDelay();
        this.broker = context.getRawDataBroker();
        this.configuration = configuration;
        this.serviceBroker = serviceBroker;
        this.generationPeriod =  timeCorrelationConfiguration().getGenerationPeriod();
        subscribeToBrokers();
    }

    private void subscribeToBrokers() {
        // Subscribe to raw data to receive the frames of the spacecraft on VC 0
        RawDataFilter frameFilter = new RawDataFilter(true,
                null,
                null,
                Arrays.asList(Constants.T_TM_FRAME, Constants.T_AOS_FRAME),
                Collections.singletonList(String.valueOf(spacecraftId)),
                Collections.singletonList(Quality.GOOD));
        // I do not put there the check on the generation period modulus, because
        // it can change at runtime
        broker.subscribe(this, null, frameFilter, o -> {
            AbstractTransferFrame atf = (AbstractTransferFrame) o.getData();
            return atf != null && atf.getVirtualChannelId() == 0;
        });
        // Subscribe to service broker to intercept time packets (APID 0)
        serviceBroker.register(this, this::packetFilter);
    }

    private boolean packetFilter(RawData rawData, SpacePacket spacePacket, int type, int subtype, String destination, String source) {
        return spacePacket.getApid() == 0;
    }

    public Instant toUtc(Instant obt) {
        Pair<BigDecimal, BigDecimal> coeffs = this.obt2gtCoefficients;
        if(coeffs == null) {
            return obt;
        }
        BigDecimal obtBd = convertToBigDecimal(obt);
        BigDecimal converted = obtBd.multiply(coeffs.getFirst()).add(coeffs.getSecond());
        // Integral part: seconds. Decimal part: nanos. Ugly. Ugly ugly ugly. But there is no precision loss.
        BigInteger epochSeconds = converted.toBigInteger();
        BigInteger nanoSeconds = converted.subtract(new BigDecimal(epochSeconds.longValue())).multiply(new BigDecimal(1000000000)).toBigInteger();
        return Instant.ofEpochSecond(epochSeconds.longValue(), nanoSeconds.intValue());
    }

    public Instant toObt(Instant utc) {
        throw new UnsupportedOperationException("Not implemented yet");
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
        // This is a time packet, so apply C.4 Spacecraft time correlation procedures, ECSS-E-70-41A
        // 1. Locate the correct frame: get the TmFrameDescriptor associated to the packet (extension), the ERT and retrieve the previous frame that respects the generation period
        RawData frame = locateFrame((TmFrameDescriptor) packetRawData.getExtension(), this.generationPeriod);
        // 2. If the frame is located, then compute the time couple: (Earth reception time - propagation delay - onboard delay, on board time)
        if(frame != null) {
            Instant utcTime = frame.getReceptionTime().minusNanos(this.propagationDelay * 1000).minusNanos(timeCorrelationConfiguration().getOnBoardDelay() * 1000);
            Instant onboardTime = extractOnboardTime(spacePacket);
            // 3. Add the time couple: this method triggers a best-fit correlation taking into account the available time couples
            addTimeCouple(onboardTime, utcTime);
        }
    }

    private void addTimeCouple(Instant onboardTime, Instant utcTime) {
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
            // Convert to big decimals: integral part seconds, decimal part nanoseconds
            Pair<BigDecimal, BigDecimal> fTc = convert(firstTimeCouple);
            Pair<BigDecimal, BigDecimal> sTc = convert(secondTimeCouple);
            BigDecimal m = (fTc.getSecond().subtract(sTc.getSecond())).divide(fTc.getFirst().subtract(sTc.getFirst()));
            BigDecimal q = sTc.getSecond().subtract(m.multiply(sTc.getFirst()));
            this.obt2gtCoefficients = Pair.of(m, q);
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
        // According to the standard, the time packet has no secondary header, so after SpacePacket.SP_PRIMARY_HEADER_LENGTH, we should have:
        // 1. generation rate field (optional) -> check configuration.isGenerationPeriodReported()
        int idx = SpacePacket.SP_PRIMARY_HEADER_LENGTH;
        if(timeCorrelationConfiguration().isGenerationPeriodReported()) {
            this.generationPeriod = (int) Math.pow(2, Byte.toUnsignedInt(spacePacket.getPacket()[idx]));
            ++idx;
        }
        // 2. time field -> check configuration.getTimeFormat()
        if(timeCorrelationConfiguration().getTimeFormat().isExplicitPField()) {
            // P-Field, easy
            return TimeUtil.fromCUC(new BitEncoderDecoder(spacePacket.getPacket(), idx, spacePacket.getPacket().length - idx), configuration.getEpoch());
        } else {
            return TimeUtil.fromCUC(spacePacket.getPacket(), idx, spacePacket.getPacket().length - idx,
                    configuration.getEpoch(),
                    timeCorrelationConfiguration().getTimeFormat().getCoarse(),
                    timeCorrelationConfiguration().getTimeFormat().getFine());
        }
        // 3. status -> mission specific, reported as parameter already, not relevant here
    }

    private TimeCorrelationServiceConfiguration timeCorrelationConfiguration() {
        return configuration.getPacketServiceConfiguration().getTimeCorrelationServiceConfiguration();
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
                        if (timeBetweenFrames.toNanos() / 1000 <= timeCorrelationConfiguration().getMaximumFrameTimeDelay()) {
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
}
