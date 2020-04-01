/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;

import java.time.Instant;

/**
 * This helper class is used to build a {@link ParameterData} instance.
 */
public class AlarmParameterDataBuilder extends AbstractDataItemBuilder<AlarmParameterData> {

    private Instant generationTime;

    private Instant receptionTime;

    private AlarmState currentState = AlarmState.UNKNOWN;

    private Object currentValue;

    private Object lastNominalValue;

    private Instant lastNominalValueTime;

    public AlarmParameterDataBuilder(int id, SystemEntityPath path) {
        super(id, path);
    }

    public void setLastNominalValue(Object engValue, Instant generationTime) {
        // Not marking state change here
        this.lastNominalValue = engValue;
        this.lastNominalValueTime = generationTime;
    }

    public void setCurrentValue(AlarmState alarmState, Object engValue, Instant generationTime, Instant receptionTime) {
        // Compute state change: if you have from AlarmState(true) to AlarmState(false) or from whatever state to AlarmState(true)
        this.changedSinceLastBuild = alarmState.isAlarm() || currentState.isAlarm();
        this.currentState = alarmState;
        this.currentValue = engValue;
        this.generationTime = generationTime;
        this.receptionTime = receptionTime;
    }

    @Override
    public AlarmParameterData build(IUniqueId updateId) {
        return new AlarmParameterData(updateId, generationTime, id, path.getLastPathElement(), path, currentState, currentValue, lastNominalValue, lastNominalValueTime, receptionTime, null);
    }

    @Override
    public void setInitialisation(AlarmParameterData item) {
        this.generationTime = item.getGenerationTime();
        this.receptionTime = item.getReceptionTime();
        this.currentState = item.getCurrentAlarmState();
        this.currentValue = item.getCurrentValue();
        this.lastNominalValue = item.getLastNominalValue();
        this.lastNominalValueTime = item.getLastNominalValueTime();
    }
}
