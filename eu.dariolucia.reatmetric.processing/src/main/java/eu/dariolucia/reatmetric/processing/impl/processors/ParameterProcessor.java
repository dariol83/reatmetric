/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.definition.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.AlarmParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.ParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This class is used to process a system entity of type PARAMETER.
 */
public class ParameterProcessor extends AbstractSystemEntityProcessor<ParameterProcessingDefinition, ParameterData, ParameterSample> implements IParameterBinding {

    private static final Logger LOG = Logger.getLogger(ParameterProcessor.class.getName());

    private final Map<String, AtomicInteger> checkViolationNumber = new TreeMap<>();

    private final ParameterDataBuilder builder;

    private final AlarmParameterDataBuilder alarmBuilder;

    public ParameterProcessor(ParameterProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor, SystemEntityType.PARAMETER);
        this.builder = new ParameterDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()));
        this.alarmBuilder = new AlarmParameterDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()));
    }

    @Override
    public synchronized List<AbstractDataItem> process(ParameterSample newValue) throws ProcessingModelException {
        // Guard condition: if this parameter has an expression, newValue must be null
        if(definition.getExpression() != null && newValue != null) {
            LOG.log(Level.SEVERE, "Parameter " + definition.getId() + " (" + definition.getLocation() + ") is a synthetic parameter, but a sample was injected. Processing ignored.");
            return Collections.emptyList();
        }
        boolean stateChanged = false;
        List<AbstractDataItem> generatedStates = new ArrayList<>(3);
        AlarmParameterData alarmData = null;
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED) {
            // Prepare the values
            AlarmState alarmState = AlarmState.UNKNOWN;
            Object engValue = null;
            // Derive the source value to use and the times
            Object previousSourceValue =  this.state == null ? null : this.state.getSourceValue();
            Object sourceValue = newValue != null ? verifySourceValue(newValue.getValue()) : previousSourceValue;
            Instant generationTime = this.state == null ? null : this.state.getGenerationTime();
            generationTime = newValue != null ? newValue.getGenerationTime() : generationTime;
            Instant receptionTime;
            // Validity check
            Validity validity = deriveValidity();
            // If valid, derive source value (if expression) and calibrate
            if(validity == Validity.VALID) {
                // If there is an expression, derive the sourceValue from the expression and also the generation time
                if(definition.getExpression() != null) {
                    // Check if re-evalutation is needed: for each mapping item in the expression, get the newest generation time
                    Instant latestGenerationTime = null;
                    for(SymbolDefinition sd : definition.getExpression().getSymbols()) {
                        Instant theGenTime = processor.resolve(sd.getReference().getId()).generationTime();
                        if(latestGenerationTime == null || theGenTime.isAfter(latestGenerationTime)) {
                            latestGenerationTime = theGenTime;
                        }
                    }
                    // If latestGenerationTime is after the current generation time, then expression shall be re-evaluated
                    if(generationTime == null || (latestGenerationTime != null && latestGenerationTime.isAfter(generationTime))) {
                        try {
                            sourceValue = definition.getExpression().execute(processor, null);
                        } catch (ScriptException e) {
                            LOG.log(Level.SEVERE, "Error when computing value of parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                            // Overrule validity to be INVALID
                            validity = Validity.INVALID;
                            // Put source value to be null
                            sourceValue = null;
                        }
                    } else {
                        // Effectively, as there is no change in the depending elements, the processing can end here
                        return generatedStates;
                    }
                }
                if(validity == Validity.VALID) {
                    try {
                        // Calibrate the source value
                        engValue = calibrate(sourceValue);
                        // Then run checks
                        alarmState = check(engValue, generationTime, newValue == null);
                    } catch (CalibrationException e) {
                        LOG.log(Level.SEVERE, "Error when calibrating parameter " + definition.getId() + " (" + definition.getLocation() + ") with source value " + sourceValue + ": " + e.getMessage(), e);
                        // Validity is INVALID, to prevent other processors to take the null eng. value as good value
                        validity = Validity.INVALID;
                        // Alarm state is set to UNKNOWN
                        alarmState = AlarmState.UNKNOWN;
                    }
                }
            }
            // Set validity
            this.builder.setValidity(validity);
            // Set the source value and the generation time
            this.builder.setSourceValue(sourceValue);
            this.builder.setGenerationTime(generationTime);
            // Set engineering value - If not valid, the engineering value is not computed
            this.builder.setEngValue(engValue);
            // The checks are not run
            this.builder.setAlarmState(alarmState);
            // Build final state, set it and return it
            if(newValue != null) {
                this.builder.setRoute(newValue.getRoute());
                this.builder.setContainerId(newValue.getContainerId());
                receptionTime = newValue.getReceptionTime(); // This can never be null
            } else {
                this.builder.setRoute(state == null ? null : state.getRoute());
                this.builder.setContainerId(state == null ? null : state.getRawDataContainerId());
                // Re-evaluation, so set the reception time to now
                receptionTime = Instant.now();
            }
            // Sanitize the reception time
            this.builder.setReceptionTime(receptionTime);
            // Replace the state
            if(this.builder.isChangedSinceLastBuild()) {
                this.state = this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class)));
                generatedStates.add(this.state);
                stateChanged = true;
            }
            // Compute alarm state
            if(stateChanged && valid()) {
                // If nominal, set the last nominal value
                if(!inAlarm()) {
                    this.alarmBuilder.setLastNominalValue(this.state.getEngValue(), this.state.getGenerationTime());
                }
                // Set current values
                this.alarmBuilder.setCurrentValue(this.state.getAlarmState(), this.state.getEngValue(), this.state.getGenerationTime(), this.state.getReceptionTime());
                if(this.alarmBuilder.isChangedSinceLastBuild()) {
                    alarmData = this.alarmBuilder.build(new LongUniqueId(processor.getNextId(AlarmParameterData.class)));
                }
            }
        } else {
            // Completely ignore the processing
            LOG.log(Level.FINE, "Parameter sample not computed for parameter " + definition.getLocation() + ": parameter processing is disabled");
        }
        // Finalize entity state and prepare for the returned list of data items
        if(stateChanged) {
            this.systemEntityBuilder.setAlarmState(this.state.getAlarmState());
        }
        this.systemEntityBuilder.setStatus(entityStatus);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            generatedStates.add(this.entityState);
        }
        if(alarmData != null) {
            generatedStates.add(alarmData);
        }
        // Return the list
        return generatedStates;
    }

    private Object verifySourceValue(Object value) throws ProcessingModelException {
        // If the value is null, then set to null (no value)
        if(value == null) {
            return null;
        }
        // Check type compliance
        if(definition.getRawType().getAssignedClass().isAssignableFrom(value.getClass())) {
            return value;
        } else {
            // Try to see if you can get any compliance, especially with numeric types
            try {
                return ValueUtil.convert(value, definition.getRawType());
            } catch(ValueException e) {
                // At this stage, there is not much you can do
                throw new ProcessingModelException("Source value " + value + " does not match declared source type " + definition.getRawType() + " and cannot be automatically converted", e);
            }
        }
    }

    private Object calibrate(Object value) throws CalibrationException {
        // If the value is null, then set to null (no value)
        if(value == null) {
            return null;
        }
        Object result = value;
        // Otherwise, calibrate it
        if(this.definition.getCalibration() != null) {
            result = this.definition.getCalibration().calibrate(value, this.processor);
        }
        // Sanitize, if possible, based on type
        try {
            result = ValueUtil.convert(result, definition.getEngineeringType());
        } catch(ValueException ve) {
            throw new CalibrationException("Conversion of value " + result + " to output type " + definition.getEngineeringType() + " failed: " + ve.getMessage(), ve);
        }
        // Return the result
        return result;
    }

    private AlarmState check(Object engValue, Instant generationTime, boolean reevaluation) {
        // If there are no checks, then value is NOT_CHECKED
        if(definition.getChecks().isEmpty()) {
            return AlarmState.NOT_CHECKED;
        }
        // Otherwise evaluate the checks and derive the result
        AlarmState result = AlarmState.NOMINAL;
        for(CheckDefinition cd : definition.getChecks()) {
            // violationCounter is in the checkViolationNumber map after the next instruction
            AtomicInteger violationCounter = checkViolationNumber.computeIfAbsent(cd.getName(), k -> new AtomicInteger(0));
            AlarmState state;
            try {
                state = cd.check(engValue, generationTime, violationCounter.get(), processor);
            } catch (CheckException e) {
                LOG.log(Level.SEVERE, "Error when evaluating check " + cd.getName() + " on parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                // Return immediately (fail fast)
                return AlarmState.ERROR;
            }
            if(state != AlarmState.NOMINAL) {
                if(!reevaluation || violationCounter.get() == 0) {
                    // New sample: always increase; Re-evaluation: increase the violation number only if the alarm was not violated before (checkViolationNumber == 0)
                    violationCounter.incrementAndGet();
                }
                result = state.ordinal() < result.ordinal() ? state : result;
            } else {
                violationCounter.set(0);
            }
        }
        return result;
    }

    private Validity deriveValidity() {
        if(definition.getValidity() == null) {
            return Validity.VALID;
        } else {
            try {
                boolean valid = definition.getValidity().execute(processor);
                return valid ? Validity.VALID : Validity.INVALID;
            } catch(ValidityException e) {
                LOG.log(Level.SEVERE, "Error when computing validity of parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                return Validity.ERROR;
            }
        }
    }

    @Override
    public List<AbstractDataItem> evaluate() throws ProcessingModelException {
        return process(null);
    }

    @Override
    public Object sourceValue() {
        return this.state == null ? null : this.state.getSourceValue();
    }

    @Override
    public Object value() {
        return this.state == null ? null : this.state.getEngValue();
    }

    @Override
    public AlarmState alarmState() {
        return this.state == null ? AlarmState.UNKNOWN : this.state.getAlarmState();
    }

    @Override
    public boolean inAlarm() {
        return this.state != null &&
                this.state.getAlarmState().isAlarm();
    }

    @Override
    public boolean valid() {
        return this.state != null && this.state.getValidity() == Validity.VALID;
    }

    @Override
    public Validity validity() {
        return this.state == null ? Validity.UNKNOWN : this.state.getValidity();
    }

    @Override
    public Long containerId() {
        return this.state == null || this.state.getRawDataContainerId() == null ? null : this.state.getRawDataContainerId().asLong();
    }

    @Override
    public long id() {
        return definition.getId();
    }

    @Override
    public String path() {
        return definition.getLocation();
    }

    @Override
    public Instant generationTime() {
        return this.state == null ? null : this.state.getGenerationTime();
    }

    @Override
    public Instant receptionTime() {
        return this.state == null ? null : this.state.getReceptionTime();
    }
}
