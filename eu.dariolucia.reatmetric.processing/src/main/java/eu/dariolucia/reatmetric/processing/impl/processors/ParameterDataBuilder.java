/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;

import java.time.Instant;

public class ParameterDataBuilder {

    private IUniqueId updateId;

    private int id;

    private Instant generationTime;

    private SystemEntityPath path;

    private Object engValue;

    private Object sourceValue;

    private String route;

    private Instant receptionTime;

    private Validity validity = Validity.UNKNOWN;

    private AlarmState alarmState = AlarmState.UNKNOWN;

    public void setUpdateId(IUniqueId updateId) {
        this.updateId = updateId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setGenerationTime(Instant generationTime) {
        this.generationTime = generationTime;
    }

    public void setPath(SystemEntityPath path) {
        this.path = path;
    }

    public void setEngValue(Object engValue) {
        this.engValue = engValue;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public void setSourceValue(Object sourceValue) {
        this.sourceValue = sourceValue;
    }

    public void setReceptionTime(Instant receptionTime) {
        this.receptionTime = receptionTime;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public void setAlarmState(AlarmState alarmState) {
        this.alarmState = alarmState;
    }

    public ParameterData build() {
        return new ParameterData(updateId, generationTime, id, path.getLastPathElement(), path, engValue, sourceValue, route, validity, alarmState, receptionTime, new Object[0]);
    }
}
