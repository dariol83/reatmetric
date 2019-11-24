/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.processing.definition.CheckDefinition;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.ParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This class is used to process a system entity of type PARAMETER.
 */
public class ParameterProcessor extends AbstractSystemEntityProcessor<ParameterProcessingDefinition, ParameterData, ParameterSample> {

    private static final Logger LOG = Logger.getLogger(ParameterProcessor.class.getName());

    private final Map<String, AtomicInteger> checkViolationNumber = new TreeMap<>();

    private final ParameterDataBuilder builder;

    public ParameterProcessor(ParameterProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor, SystemEntityType.PARAMETER);
        for(CheckDefinition cd : definition.getChecks()) {
            this.checkViolationNumber.put(cd.getName(), new AtomicInteger(0));
        }
        this.builder = new ParameterDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()));
    }

    @Override
    public synchronized Pair<ParameterData, SystemEntity> process(ParameterSample newValue) {
        boolean stateChanged = false;
        boolean entityStateChanged = false;
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED) {
            // Derive the source value to use
            Object sourceValue = newValue != null ? newValue.getValue() : this.state.getSourceValue();
            // Set times and value (if you have a new one)
            if(newValue != null) {
                this.builder.setGenerationTime(newValue.getGenerationTime());
                this.builder.setReceptionTime(newValue.getReceptionTime());
                this.builder.setSourceValue(sourceValue);
            }
            // Validity check
            Validity validity = deriveValidity();
            this.builder.setValidity(validity);
            // If valid, calibrate
            if(validity == Validity.VALID) {
                // TODO: if there is an expression, derive the sourceValue from the expression and also the times
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
            if(this.builder.isChangedSinceLastBuild()) {
                this.state = this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class)));
                stateChanged = true;
            }
        } else {
            // Completely ignore the processing
            LOG.log(Level.FINE, "Parameter sample not computed for parameter " + definition.getLocation() + ": parameter processing is disabled");
        }
        // Finalize entity state
        if(stateChanged) {
            this.systemEntityBuilder.setAlarmState(this.state.getAlarmState());
        }
        this.systemEntityBuilder.setStatus(entityStatus);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            entityStateChanged = true;
        }
        // Return the pair
        return Pair.of(stateChanged ? this.state : null, entityStateChanged ? this.entityState : null);
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
    public Pair<ParameterData, SystemEntity> evaluate() {
        return process(null);
    }
}
