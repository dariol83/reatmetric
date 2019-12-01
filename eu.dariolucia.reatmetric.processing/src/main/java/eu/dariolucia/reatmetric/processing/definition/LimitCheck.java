/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.IDataItemStateResolver;
import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.ScriptEngine;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public class LimitCheck extends CheckDefinition {

    @XmlAttribute
    private double highLimit = Double.POSITIVE_INFINITY;

    @XmlAttribute
    private double lowLimit = Double.NEGATIVE_INFINITY;

    public LimitCheck() {
        super();
    }

    public LimitCheck(String name, CheckSeverity severity, int numViolations, double highLimit, double lowLimit) {
        super(name, severity, numViolations);
        this.highLimit = highLimit;
        this.lowLimit = lowLimit;
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

    @Override
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) throws CheckException {
        // Prepare transient state
        prepareMapping();
        // Check
        double toCheck = convertToDouble(currentValue);
        boolean violated = toCheck < lowLimit || toCheck > highLimit;
        // Return result
        return deriveState(violated, currentViolations);
    }
}
