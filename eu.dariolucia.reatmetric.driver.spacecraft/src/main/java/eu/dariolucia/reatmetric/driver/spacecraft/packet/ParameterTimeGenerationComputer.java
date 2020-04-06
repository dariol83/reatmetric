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
