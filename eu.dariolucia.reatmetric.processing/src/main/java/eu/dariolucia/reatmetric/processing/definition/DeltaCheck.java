/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.processing.impl.IParameterResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

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

    @Override
    public AlarmState check(Object currentValue, int currentViolations, IParameterResolver resolver) {
        // TODO
        return AlarmState.NOMINAL;
    }
}
