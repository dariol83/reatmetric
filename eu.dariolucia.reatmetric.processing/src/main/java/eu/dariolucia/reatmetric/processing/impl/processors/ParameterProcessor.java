/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.processing.definition.CheckDefinition;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterProcessor extends AbstractSystemEntityProcessor<ParameterProcessingDefinition, ParameterData, ParameterSample> {

    private static final Logger LOG = Logger.getLogger(ParameterProcessor.class.getName());

    private final Map<String, AtomicInteger> checkViolationNumber = new TreeMap<>();

    private final ParameterDataBuilder builder;

    public ParameterProcessor(ParameterProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor);
        for(CheckDefinition cd : definition.getChecks()) {
            this.checkViolationNumber.put(cd.getName(), new AtomicInteger(0));
        }
        this.builder = new ParameterDataBuilder();
        this.builder.setId(definition.getId());
        this.builder.setPath(SystemEntityPath.fromString(definition.getLocation()));
    }

    @Override
    public synchronized ParameterData process(ParameterSample newValue) {
        if(!isEnabled()) {
            LOG.log(Level.FINE, "Parameter sample not computed for parameter " + definition.getLocation() + ": parameter processing is disabled");
            return null;
        }
        this.builder.setUpdateId(new LongUniqueId(processor.getNextId(ParameterData.class)));
        // Set times and value (if you have a new one)
        if(newValue != null) {
            this.builder.setGenerationTime(newValue.getGenerationTime());
            this.builder.setReceptionTime(newValue.getReceptionTime());
            this.builder.setSourceValue(newValue.getValue());
        }
        // Validity check
        Validity validity = deriveValidity();
        this.builder.setValidity(validity);
        // If valid, calibrate
        if(validity == Validity.VALID) {
            // TODO: if there is an expression, derive the source value from the expression and also the times
            Object sourceValue = newValue != null ? newValue.getValue() : state.getSourceValue();
            Object engValue = calibrate(sourceValue);
            this.builder.setEngValue(engValue);
            // Then run checks
            AlarmState alarmState = check(engValue);
            this.builder.setAlarmState(alarmState);
        } else {
            // If not valid, the engineering value is not computed
            this.builder.setEngValue(null);
            // The checks are not run
            this.builder.setAlarmState(AlarmState.NOT_CHECKED);
        }
        // Build final state, set it and return it
        if(newValue != null) {
            this.builder.setRoute(newValue.getRoute());
        } else {
            this.builder.setRoute(state.getRoute());
        }
        // Replace the state
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

    private Validity deriveValidity() {
        // TODO
        return Validity.VALID;
    }

    @Override
    public ParameterData evaluate() {
        return process(null);
    }
}
