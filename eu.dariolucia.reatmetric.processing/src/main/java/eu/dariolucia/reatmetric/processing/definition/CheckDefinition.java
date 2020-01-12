/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CheckDefinition {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private CheckSeverity severity = CheckSeverity.ALARM;

    @XmlAttribute
    private int numViolations = 1;

    public CheckDefinition() {
    }

    public CheckDefinition(String name, CheckSeverity severity, int numViolations) {
        this.name = name;
        this.severity = severity;
        this.numViolations = numViolations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CheckSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(CheckSeverity severity) {
        this.severity = severity;
    }

    public int getNumViolations() {
        return numViolations;
    }

    public void setNumViolations(int numViolations) {
        this.numViolations = numViolations;
    }

    public abstract AlarmState check(Object currentValue, Instant valueGenerationTime, int currentViolations, IBindingResolver resolver) throws CheckException;

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient AlarmState mappedAlarmState;

    protected final void prepareMapping() {
        if(mappedAlarmState == null) {
            // default is ALARM
            if (getSeverity() == CheckSeverity.WARNING) {
                mappedAlarmState = AlarmState.WARNING;
            } else {
                mappedAlarmState = AlarmState.ALARM;
            }
        }
    }

    protected final AlarmState deriveState(boolean violated, int currentViolations) {
        AlarmState toReturn = violated ? AlarmState.VIOLATED : AlarmState.NOMINAL;
        if(violated && ++currentViolations >= getNumViolations()) {
            toReturn = mappedAlarmState;
        }
        return toReturn;
    }

    protected final double convertToDouble(Object valueToCheck) throws CheckException {
        if(valueToCheck instanceof Number) {
            return ((Number) valueToCheck).doubleValue();
        } else {
            throw new CheckException("Cannot check " + valueToCheck + " as input to limit check: value cannot be converted to double");
        }
    }
}
