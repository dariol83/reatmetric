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
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.time.Instant;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CheckDefinition implements Serializable {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private CheckSeverity severity = CheckSeverity.ALARM;

    @XmlAttribute(name="num_violations")
    private int numViolations = 1;

    @XmlAttribute(name="raw_value_checked")
    private boolean rawValueChecked = false; // Apply check on raw/source value: for activity arguments, the check on engineering values is applied only if the value is supplied in engineering format

    @XmlElement(name = "applicability")
    private ValidityCondition applicability = null;

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

    public boolean isRawValueChecked() {
        return rawValueChecked;
    }

    public void setRawValueChecked(boolean rawValueChecked) {
        this.rawValueChecked = rawValueChecked;
    }

    public abstract AlarmState check(Object currentValue, Instant valueGenerationTime, int currentViolations, IBindingResolver resolver) throws CheckException;

    public ValidityCondition getApplicability() {
        return applicability;
    }

    public void setApplicability(ValidityCondition applicability) {
        this.applicability = applicability;
    }

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
