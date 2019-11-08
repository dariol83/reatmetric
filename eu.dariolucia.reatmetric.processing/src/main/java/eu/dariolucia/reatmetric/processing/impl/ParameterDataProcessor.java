/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.processing.definition.CheckDefinition;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
import eu.dariolucia.reatmetric.processing.util.UniqueIdUtil;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParameterDataProcessor {

    private final ParameterProcessingDefinition definition;

    private final ParameterProcessor processor;

    private volatile ParameterData state;

    private final Map<String, AtomicInteger> checkViolationNumber = new TreeMap<>();

    private final ParameterDataBuilder builder;

    public ParameterDataProcessor(ParameterProcessingDefinition definition, ParameterProcessor processor) {
        this.definition = definition;
        this.processor = processor;
        for(CheckDefinition cd : definition.getChecks()) {
            this.checkViolationNumber.put(cd.getName(), new AtomicInteger(0));
        }
        this.builder = new ParameterDataBuilder();
        this.builder.setName(definition.getName());
        this.builder.setParent(SystemEntityPath.fromString(definition.getLocation()));
        this.builder.setPath(SystemEntityPath.fromString(definition.getLocation()).append(definition.getName()));
    }

    public synchronized ParameterData process(ParameterValue newValue) {
        this.builder.setId(UniqueIdUtil.generateNextId(ParameterData.class));
        // Set times
        this.builder.setGenerationTime(newValue.getGenerationTime());
        this.builder.setReceptionTime(Instant.now());
        this.builder.setSourceValue(newValue.getValue());
        // Validity check
        Validity validity = validate();
        this.builder.setValidity(validity);
        // If valid, calibrate
        if(validity == Validity.VALID) {
            Object engValue = calibrate(newValue.getValue());
            this.builder.setEngValue(engValue);
            // If valid, run checks
            AlarmState alarmState = check(engValue);
            this.builder.setAlarmState(alarmState);
        } else {
            this.builder.setEngValue(null);
            this.builder.setAlarmState(AlarmState.NOT_CHECKED);
        }
        // Build final state, set and return it
        this.state = this.builder.build();
        return this.state;
    }

    private Object calibrate(Object value) {
        if(this.definition.getCalibration() != null) {
            return this.definition.getCalibration().calibrate(value, this.processor);
        } else {
            return value;
        }
    }

    private AlarmState check(Object engValue) {
        // TODO
        return AlarmState.NOMINAL;
    }

    private Validity validate() {
        // TODO
        return Validity.VALID;
    }

    public ParameterData getState() {
        return state;
    }
}
