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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public class DeltaCheck extends CheckDefinition {

    @XmlAttribute
    private double highLimit = Double.POSITIVE_INFINITY;

    @XmlAttribute
    private double lowLimit = Double.NEGATIVE_INFINITY;

    @XmlAttribute
    private boolean absolute = false; // The absolute delta value must be within limits

    public DeltaCheck() {
        super();
    }

    public DeltaCheck(String name, CheckSeverity severity, int numViolations, double highLimit, double lowLimit, boolean absolute) {
        super(name, severity, numViolations);
        this.highLimit = highLimit;
        this.lowLimit = lowLimit;
        this.absolute = absolute;
    }

    public double getHighLimit() {
        return highLimit;
    }

    public void setHighLimit(double highLimit) {
        this.highLimit = highLimit;
    }

    public double getLowLimit() {
        return lowLimit;
    }

    public void setLowLimit(double lowLimit) {
        this.lowLimit = lowLimit;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Double previousValue;
    private transient Instant previousGenerationTime;
    private transient AlarmState previousResult;

    @Override
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) throws CheckException {
        // Prepare transient state
        prepareMapping();
        // Check
        double toCheck = convertToDouble(currentValue);
        boolean violated = false;
        if(previousGenerationTime == null) {
            previousValue = toCheck;
            previousGenerationTime = generationTime;
        } else if(previousGenerationTime.equals(generationTime)) {
            // Same sample, return the result of the previous check
            return previousResult;
        } else {
            // Different sample
            double delta = toCheck - previousValue;
            if(absolute) {
                delta = Math.abs(delta);
            }
            violated = delta < lowLimit || delta > highLimit;
        }
        // Return result
        AlarmState result = deriveState(violated, currentViolations);
        previousResult = result;
        return result;
    }
}
