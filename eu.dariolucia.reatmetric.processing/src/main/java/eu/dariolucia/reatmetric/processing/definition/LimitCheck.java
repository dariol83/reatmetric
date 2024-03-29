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
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public class LimitCheck extends CheckDefinition implements Serializable {

    @XmlAttribute(required = true)
    private ValueTypeEnum type;

    @XmlAttribute(name = "high")
    private String highLimit = null; // null means no limit

    @XmlAttribute(name = "low")
    private String lowLimit = null; // null means no limit

    public LimitCheck() {
        super();
    }

    public LimitCheck(String name, CheckSeverity severity, int numViolations, ValueTypeEnum type, String highLimit, String lowLimit) {
        super(name, severity, numViolations);
        this.highLimit = highLimit;
        this.lowLimit = lowLimit;
        this.type = type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public String getHighLimit() {
        return highLimit;
    }

    public void setHighLimit(String highLimit) {
        this.highLimit = highLimit;
    }

    public String getLowLimit() {
        return lowLimit;
    }

    public void setLowLimit(String lowLimit) {
        this.lowLimit = lowLimit;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Object low;
    private transient Object high;

    @Override
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) throws CheckException {
        // Prepare transient state
        prepareMapping();
        // Convert limits
        if(low == null && lowLimit != null) {
            low = ValueUtil.parse(type, lowLimit);
        }
        if(high == null && highLimit != null) {
            high = ValueUtil.parse(type, highLimit);
        }
        // Check
        boolean violated = false;
        violated |= low != null && ValueUtil.compare(currentValue, low) < 0;
        violated |= high != null && ValueUtil.compare(currentValue, high) > 0;
        // Return result
        return deriveState(violated, currentViolations);
    }
}
