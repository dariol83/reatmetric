/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public class LimitCheck extends CheckDefinition {

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
