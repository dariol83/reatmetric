/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.packet;

import eu.dariolucia.ccsds.encdec.definition.EncodedParameter;
import eu.dariolucia.ccsds.encdec.time.IGenerationTimeProcessor;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.time.Duration;
import java.time.Instant;

public class ParameterTimeGenerationComputer implements IGenerationTimeProcessor {

    private final RawData packet; // Generation time of the packet is already UTC-ground correlated

    public ParameterTimeGenerationComputer(RawData packet) {
        this.packet = packet;
    }

    @Override
    public Instant computeGenerationTime(EncodedParameter ei, Object value, Instant derivedGenerationTime, Duration derivedOffset, Integer fixedOffsetMs) {
        Instant referenceTime = derivedGenerationTime == null ? packet.getGenerationTime() : derivedGenerationTime;
        if(derivedOffset != null) {
            referenceTime = (Instant) derivedOffset.addTo(referenceTime);
        }
        if(fixedOffsetMs != null) {
            referenceTime = referenceTime.plusMillis(fixedOffsetMs);
        }
        return referenceTime;
    }
}
