/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.model.impl;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;

import java.time.Instant;

public class ParameterDataBuilder {

    private IUniqueId id;

    private Instant generationTime;

    private String name;

    private SystemEntityPath path;

    private Object engValue;

    private Object sourceValue;

    private Instant receptionTime;

    private SystemEntityPath parent;

    private Validity validity = Validity.UNKNOWN;

    private AlarmState alarmState = AlarmState.UNKNOWN;

    public void setId(IUniqueId id) {
        this.id = id;
    }

    public void setGenerationTime(Instant generationTime) {
        this.generationTime = generationTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(SystemEntityPath path) {
        this.path = path;
    }

    public void setEngValue(Object engValue) {
        this.engValue = engValue;
    }

    public void setSourceValue(Object sourceValue) {
        this.sourceValue = sourceValue;
    }

    public void setReceptionTime(Instant receptionTime) {
        this.receptionTime = receptionTime;
    }

    public void setParent(SystemEntityPath parent) {
        this.parent = parent;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public void setAlarmState(AlarmState alarmState) {
        this.alarmState = alarmState;
    }

    public ParameterData build() {
        return new ParameterData(id, generationTime, name, path, engValue, sourceValue, receptionTime, validity, alarmState, parent, new Object[0]);
    }
}
