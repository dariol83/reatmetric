/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.definition.scripting.IEventBinding;
import eu.dariolucia.reatmetric.processing.definition.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.AlarmParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.ParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to process a system entity of type EVENT.
 */
public class EventProcessor extends AbstractSystemEntityProcessor<EventProcessingDefinition, EventData, EventOccurrence> implements IEventBinding {

    private static final Logger LOG = Logger.getLogger(EventProcessor.class.getName());

    private boolean conditionTriggerState = false;

    private final EventDataBuilder builder;

    public EventProcessor(EventProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor, SystemEntityType.EVENT);
        this.builder = new EventDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()), definition.getSeverity(), definition.getType());
    }

    @Override
    public synchronized List<AbstractDataItem> process(EventOccurrence newValue) throws ProcessingModelException {
        // Guard condition: if this event has an expression, newValue must be null
        if(definition.getCondition() != null && newValue != null) {
            LOG.log(Level.SEVERE, "Event " + definition.getId() + " (" + definition.getLocation() + ") is a condition-driven event, but an external occurrence was injected. Processing ignored.");
            return Collections.emptyList();
        }
        boolean mustBeRaised = false;
        List<AbstractDataItem> generatedStates = new ArrayList<>(2);
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED) {
            // Prepare the values
            Instant generationTime = this.state == null ? null : this.state.getGenerationTime();
            generationTime = newValue != null ? newValue.getGenerationTime() : generationTime;
            Instant receptionTime;
            // If there is an expression, then evaluate the expression and check for a transition false -> true
            if(definition.getCondition() != null) {
                boolean triggered = false;
                try {
                    triggered = (Boolean) definition.getCondition().execute(processor, null);
                    mustBeRaised = triggered && !conditionTriggerState; // raise the event if it must be raised and it is not already reported from previous evaluation
                } catch (ScriptException|ClassCastException e) {
                    LOG.log(Level.SEVERE, "Error when evaluating condition of event " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                    return generatedStates;
                }
            } else if(newValue != null) {
                // No condition, no re-evaluation, so use the input
            } else {
                // Simple re-evaluation, check if there is an external trigger: if no triggers, then no event, then no states
            }
            // If valid, derive source value (if expression) and calibrate
            if(validity == Validity.VALID) {

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

    @Override
    public List<AbstractDataItem> evaluate() throws ProcessingModelException {
        return process(null);
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
