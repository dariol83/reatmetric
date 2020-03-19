/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TimeCorrelationService implements IServicePacketSubscriber, IRawDataSubscriber {

    private static final int MATCHING_FRAMES_MAX_SIZE = 16;

    private int spacecraftId;
    private long propagationDelay;
    private IRawDataBroker broker;
    private TimeCorrelationServiceConfiguration configuration;
    private ServiceBroker serviceBroker;
    private volatile int generationPeriod;

    private List<RawData> matchingFrames = new LinkedList<>();
    private List<Pair<Instant, Instant>> timeCouples = new LinkedList<>();

    public TimeCorrelationService(SpacecraftConfiguration configuration, IServiceCoreContext context, ServiceBroker serviceBroker) {
        this.spacecraftId = configuration.getId();
        this.propagationDelay = configuration.getPropagationDelay();
        this.broker = context.getRawDataBroker();
        this.configuration = configuration.getPacketServiceConfiguration().getTimeCorrelationServiceConfiguration();
        this.serviceBroker = serviceBroker;
        this.generationPeriod =  this.configuration.getGenerationPeriod();
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

    }

    public Instant toObt(Instant utc) {

    }

    public void setGenerationPeriod(int frameGenerationPeriod) {
        this.generationPeriod = frameGenerationPeriod;
    }

    public int getGenerationPeriod() {
        return this.generationPeriod;
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
        this.matchingFrames.add(0, frame);
        if(matchingFrames.size() > MATCHING_FRAMES_MAX_SIZE) {
            matchingFrames.remove(matchingFrames.size() - 1);
        }
    }

    @Override
    public void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        // TODO: this is a time packet, so apply C.4 Spacecraft time correlation procedures, ECSS-E-70-41A
        // 1. Locate the correct frame: get the TmFrameDescriptor associated to the packet (extension), the ERT and retrieve the previous frame that respects the generation period
        RawData frame = locateFrame((TmFrameDescriptor) packetRawData.getExtension(), this.generationPeriod);
        // 2. If the frame is located, then compute the time couple: (Earth reception time - propagation delay - onboard delay, on board time)
        if(frame != null) {
            Instant utcTime = frame.getReceptionTime().minusNanos(this.propagationDelay * 1000).minusNanos(configuration.getOnBoardDelay() * 1000);
            Instant onboardTime = extractOnboardTime(spacePacket);
            // 3. Add the time couple: this method triggers a best-fit correlation taking into account the available time couples
            addTimeCouple(utcTime, onboardTime);
        }
    }
}
