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

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.AlarmParameterDataBuilder;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.ParameterDataBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    private final AtomicReference<AlarmParameterData> currentAlarmData = new AtomicReference<>();

    private final ParameterDescriptor descriptor;

    private volatile AlarmState latestGeneratedAlarmSeverityMessage;

    private Instant lastReportedLogTime = null;
    private int skippedLogMessagesCounter = 0;

    public ParameterProcessor(ParameterProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor, SystemEntityType.PARAMETER);
        this.builder = new ParameterDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()));
        this.alarmBuilder = new AlarmParameterDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()));
        // Check if there is an initialiser
        if(processor.getInitialiser() != null) {
            try {
                initialise(processor.getInitialiser());
            } catch(ReatmetricException re) {
                LOG.log(Level.SEVERE, String.format("Cannot initialise parameter %d (%s) with archived state as defined by the initialisation time", definition.getId(), definition.getLocation()), re);
            }
        } else {
            // Check presence of default value: if so, build the state right away
            if (definition.getDefaultValue() != null) {
                buildDefaultState();
            }
        }
        // Check if it is a synthetic parameter and if it has to start disabled
        if(definition.getExpression() != null && !processor.getDefinitions().isSyntheticParameterProcessingEnabled()) {
            this.entityStatus = Status.DISABLED;
            this.systemEntityBuilder.setStatus(entityStatus);
        }
        // Initialise the entity state
        this.systemEntityBuilder.setAlarmState(getInitialAlarmState());
        this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
        // Build the descriptor
        this.descriptor = new ParameterDescriptor(getPath(),
                getSystemEntityId(),
                definition.getDescription(),
                definition.getRawType(),
                definition.getEngineeringType(),
                definition.getUnit(),
                definition.getExpression() != null,
                definition.getSetter() != null,
                definition.isUserParameter(),
                definition.getSetter() != null ? definition.getSetter().getActivity().getType() : null,
                definition.getSetter() != null ? definition.getSetter().getActivity().getDefaultRoute() : null,
                definition.buildExpectedValuesRaw(),
                definition.buildExpectedValuesEng());
    }

    private void buildDefaultState() {
        Object sourceValue = null;
        Object engValue = null;
        String valueStr = definition.getDefaultValue().getValue();
        if(definition.getDefaultValue().getType() == DefaultValueType.RAW) {
            sourceValue = ValueUtil.parse(definition.getRawType(), valueStr);
        } else {
            engValue = ValueUtil.parse(definition.getEngineeringType(), valueStr);
        }
        // Set validity
        this.builder.setValidity(Validity.UNKNOWN);
        // Set the source value and the generation time
        this.builder.setSourceValue(sourceValue);
        this.builder.setGenerationTime(Instant.EPOCH);
        // Set engineering value - If not valid, the engineering value is not computed
        this.builder.setEngValue(engValue);
        // The checks are not run
        this.builder.setAlarmState(AlarmState.UNKNOWN);
        // Build final state, set it and return it
        this.builder.setRoute(null);
        this.builder.setContainerId(null);
        // Sanitize the reception time
        this.builder.setReceptionTime(Instant.EPOCH);
        // Replace the state
        this.state.set(this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class))));
    }

    private void initialise(IProcessingModelInitialiser initialiser) throws ReatmetricException {
        List<AbstractDataItem> stateList = initialiser.getState(getSystemEntityId(), SystemEntityType.PARAMETER);
        if(!stateList.isEmpty()) {
            ParameterData toSet = (ParameterData) stateList.get(0);
            this.state.set(toSet);
            this.builder.setInitialisation(toSet);
            if (stateList.size() > 1) {
                AlarmParameterData alarmData = (AlarmParameterData) stateList.get(1);
                this.currentAlarmData.set(alarmData);
                this.alarmBuilder.setInitialisation(alarmData);
            }
        }
    }

    @Override
    protected AlarmState getInitialAlarmState() {
        ParameterData currentState = getState();
        if(currentState == null) {
            return AlarmState.UNKNOWN;
        } else {
            return currentState.getAlarmState();
        }
    }

    @Override
    public synchronized List<AbstractDataItem> process(ParameterSample newValue) throws ProcessingModelException {
        // Guard condition: if this parameter is mirrored and a sample was injected, alarm and exit processing
        if(definition.isMirrored() && newValue != null) {
            LOG.log(Level.SEVERE, String.format("Parameter %d (%s) is a mirrored parameter, but a sample was injected. Processing ignored.", definition.getId(), definition.getLocation()));
            return Collections.emptyList();
        }
        // Guard condition: if this parameter has an expression, newValue must be null
        if(definition.getExpression() != null && newValue != null) {
            LOG.log(Level.SEVERE, String.format("Parameter %d (%s) is a synthetic parameter, but a sample was injected. Processing ignored.", definition.getId(), definition.getLocation()));
            return Collections.emptyList();
        }
        // TODO: optimisation? if weakly and not dirty, return empty list
        // To be returned at the end of the processing
        List<AbstractDataItem> generatedStates = new ArrayList<>(3);
        // Re-evaluation: for mirrored parameters the re-evaluation does not make sense.
        // For normal parameters, a re-evaluation state only makes sense if: this is synthetic parameter or there was a previous value to be re-evaluated.
        // If that is not the case, it is pointless to continue with the processing.
        if(definition.isMirrored()) {
            // Re-evaluation of a mirrored parameter does not make sense, only compute the entity state change if any
            computeSystemEntityState(false, generatedStates);
            return generatedStates;
        }
        ParameterData currentState = getState();
        if(newValue == null && definition.getExpression() == null && (currentState == null || currentState.getSourceValue() == null)) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, String.format("Skipping re-evaluation of parameter %d (%s) as there is no previous sample", definition.getId(), definition.getLocation()));
            }
            // Finalize entity state and prepare for the returned list of data items
            computeSystemEntityState(false, generatedStates);
            return generatedStates;
        }
        // Previous value
        Object previousValue = currentState == null ? null : currentState.getEngValue();
        // Was in alarm?
        boolean wasInAlarm = currentState != null && currentState.getAlarmState().isAlarm();
        // Required to take decision at the end
        boolean stateChanged = false;
        // The placeholder for the AlarmParameterData to be created, if needed
        AlarmParameterData alarmData = null;
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            // Prepare the values
            AlarmState alarmState = AlarmState.UNKNOWN;
            Object engValue = null;
            // Derive the source value to use and the times
            Object previousSourceValue =  currentState == null ? null : currentState.getSourceValue();
            Object sourceValue = newValue != null ? verifySourceValue(newValue.getValue()) : previousSourceValue;
            Instant generationTime = currentState == null ? null : currentState.getGenerationTime();
            generationTime = newValue != null ? newValue.getGenerationTime() : generationTime;
            // Immediate check: if there is a sample and its generation time is before the current (not null) one, then exist now
            if(currentState != null && newValue != null && newValue.getGenerationTime().isBefore(currentState.getGenerationTime())) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, String.format("Sample of parameter %d (%s) discarded, generation time %s is before current time %s", definition.getId(), definition.getLocation(), newValue.getGenerationTime(), currentState.getGenerationTime()));
                }
                // Before existing, compute the system entity state if needed
                computeSystemEntityState(false, generatedStates);
                return generatedStates;
            }
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
                        Instant theGenTime = processor.resolve(sd.getReference()).generationTime();
                        if(latestGenerationTime == null || (theGenTime != null && theGenTime.isAfter(latestGenerationTime))) {
                            latestGenerationTime = theGenTime;
                        }
                    }
                    // At this stage, if latestGenerationTime is null, it means that there is no value coming from the depending samples. So skip.
                    // If latestGenerationTime is after the current generation time, then expression shall be re-evaluated.
                    if(latestGenerationTime != null && (generationTime == null || latestGenerationTime.isAfter(generationTime))) {
                        // Use the latest generation time
                        generationTime = latestGenerationTime;
                        try {
                            sourceValue = definition.getExpression().execute(processor, null, definition.getRawType());
                            sourceValue = ValueUtil.convert(sourceValue, definition.getRawType());
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "Error when computing value of parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                            // Overrule validity to be INVALID
                            validity = Validity.INVALID;
                            // Put source value to be null
                            sourceValue = null;
                        }
                    } else {
                        // Effectively, as there is no change in the depending elements, the processing can end here
                        // Before existing, compute the system entity state if needed
                        if(LOG.isLoggable(Level.FINEST)) {
                            LOG.log(Level.FINEST, String.format("Sample of parameter %d (%s) discarded, no change in depending elements", definition.getId(), definition.getLocation()));
                        }
                        computeSystemEntityState(false, generatedStates);
                        return generatedStates;
                    }
                }
                if(validity == Validity.VALID) {
                    try {
                        // Calibrate the source value
                        engValue = CalibrationDefinition.performCalibration(definition.getCalibrations(), sourceValue, definition.getEngineeringType(), this.processor);
                        // Then run checks
                        alarmState = check(sourceValue, engValue, generationTime, newValue == null);
                    } catch (CalibrationException e) {
                        LOG.log(Level.SEVERE, "Error when calibrating parameter " + id() + " (" + path() + ") with source value " + sourceValue + ": " + e.getMessage(), e);
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
                // We have a new sample
                this.builder.setRoute(newValue.getRoute());
                this.builder.setContainerId(newValue.getContainerId());
                receptionTime = newValue.getReceptionTime(); // This can never be null
            } else {
                // We are in the case of re-evaluation
                this.builder.setRoute(currentState == null ? null : currentState.getRoute());
                this.builder.setContainerId(currentState == null ? null : currentState.getRawDataContainerId());
                // What is the reception time?
                if(definition.getExpression() != null && this.builder.isChangedSinceLastBuild()) {
                    // Re-evaluation, set reception time to now if it is an expression and if the builder is in a changed state.
                    receptionTime = Instant.now();
                } else {
                    // Re-evaluation, do not change the reception time if it is not an expression.
                    receptionTime = currentState == null || currentState.getReceptionTime() == null ? generationTime : currentState.getReceptionTime();
                }
            }
            // Set the reception time
            this.builder.setReceptionTime(receptionTime);
            // Replace the state
            if(this.builder.isChangedSinceLastBuild()) {
                ParameterData newState = this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class)));
                this.state.set(newState);
                generatedStates.add(newState);
                stateChanged = true;
            } else {
                if(LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, String.format("Sample of parameter %d (%s) discarded, no change since last update (!)", definition.getId(), definition.getLocation()));
                }
            }
            // Compute alarm state
            alarmData = computeAlarmParameterData(stateChanged);
        } else {
            // Set validity to DISABLED
            this.builder.setValidity(Validity.DISABLED);
            // Replace the state
            if(this.builder.isChangedSinceLastBuild()) {
                ParameterData newState = this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class)));
                this.state.set(newState);
                generatedStates.add(newState);
                stateChanged = true;
            }
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Parameter sample not computed for parameter " + path() + ": parameter processing is disabled");
            }
        }
        // Finalize entity state and prepare for the returned list of data items
        computeSystemEntityState(stateChanged, generatedStates);
        // If there is an AlarmParameterData, then report it and save it
        finalizeAlarmParameterData(generatedStates, alarmData);
        // At this stage, check the triggers and, for each of them, derive the correct behaviour
        activateTriggers(newValue, previousValue, wasInAlarm, stateChanged);
        // TODO: if weakly consistent, clear dirty state
        // Return the list
        return generatedStates;
    }

    private void finalizeAlarmParameterData(List<AbstractDataItem> generatedStates, AlarmParameterData alarmData) {
        if(alarmData != null) {
            generatedStates.add(alarmData);
            currentAlarmData.set(alarmData);
            // Generate alarm message
            generateAlarmMessage(alarmData);
        } else {
            // Remove the alarm data
            currentAlarmData.set(null);
            latestGeneratedAlarmSeverityMessage = null;
        }
    }

    private AlarmParameterData computeAlarmParameterData(boolean stateChanged) {
        ParameterData currentState = getState();
        AlarmParameterData alarmData = null;
        if(stateChanged && valid()) {
            // If nominal, set the last nominal value
            if(!inAlarm()) {
                this.alarmBuilder.setLastNominalValue(currentState.getEngValue(), currentState.getGenerationTime());
            }
            // Set current values
            this.alarmBuilder.setCurrentValue(currentState.getAlarmState(), currentState.getEngValue(), currentState.getGenerationTime(), currentState.getReceptionTime());
            if(this.alarmBuilder.isChangedSinceLastBuild()) {
                alarmData = this.alarmBuilder.build(new LongUniqueId(processor.getNextId(AlarmParameterData.class)));
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Alarm Parameter Data generated: " + alarmData);
                }
            }
        }
        return alarmData;
    }

    public synchronized List<AbstractDataItem> mirror(ParameterData itemToMirror) {
        // Guard condition: if this parameter is not mirrored, alarm and exit processing
        if(!definition.isMirrored()) {
            LOG.log(Level.SEVERE, String.format("Parameter %d (%s) is not a mirrored parameter, but a parameter full state was injected. Processing ignored.", definition.getId(), definition.getLocation()));
            return Collections.emptyList();
        }
        // To be returned at the end of the processing
        List<AbstractDataItem> generatedStates = new ArrayList<>(3);
        AlarmParameterData alarmData = null;
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            // Copy the status into the builder
            this.builder.setInitialisation(itemToMirror);
            // Prevent alarm detection
            if(entityStatus == Status.IGNORED) {
                this.builder.setAlarmState(AlarmState.IGNORED);
            }
            // You must always build an update
            ParameterData newState = this.builder.build(new LongUniqueId(processor.getNextId(ParameterData.class)));
            this.state.set(newState);
            generatedStates.add(newState);
            // Compute alarm state
            alarmData = computeAlarmParameterData(true);
            // Finalize entity state and prepare for the returned list of data items
            computeSystemEntityState(true, generatedStates);
            // If there is an AlarmParameterData, then report it and save it
            finalizeAlarmParameterData(generatedStates, alarmData);
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Parameter state not mirrored for parameter " + path() + ": parameter processing is disabled");
            }
        }
        // Return the list
        return generatedStates;
    }

    private void generateAlarmMessage(AlarmParameterData alarmData) {
        // Generation of alarm messages is performed only if the generated alarm data is different in terms of severity
        // from the one generated before, or if the parameter moved back into its nominal state in the meantime
        AlarmState state = alarmData.getCurrentAlarmState();
        AlarmState previousState = latestGeneratedAlarmSeverityMessage;
        if(previousState != null && previousState == state) {
            return;
        }
        latestGeneratedAlarmSeverityMessage = state;
        Instant now = Instant.now();
        if(lastReportedLogTime == null || lastReportedLogTime.plusMillis(definition.getLogRepetitionPeriod()).isBefore(now)) {
            String suffix = skippedLogMessagesCounter == 0 ? "" : " (skipped: " + skippedLogMessagesCounter + ")";
            switch (state) {
                case ALARM:
                case ERROR: {
                    LOG.log(Level.SEVERE, "Parameter " + getPath() + " in alarm, value " + alarmData.getCurrentValue() + suffix, new Object[]{definition.getLocation(), getSystemEntityId()});
                }
                break;
                case VIOLATED:
                case WARNING: {
                    LOG.log(Level.WARNING, "Parameter " + getPath() + " in alarm, value " + alarmData.getCurrentValue() + suffix, new Object[]{definition.getLocation(), getSystemEntityId()});
                }
                break;
                case NOMINAL: {
                    if(previousState == AlarmState.ALARM || previousState == AlarmState.ERROR || previousState == AlarmState.WARNING || previousState == AlarmState.VIOLATED) {
                        LOG.log(Level.INFO, "Parameter " + getPath() + " back in limit, value " + alarmData.getCurrentValue() + suffix, new Object[]{definition.getLocation(), getSystemEntityId()});
                    }
                }
                break;
            }
            lastReportedLogTime = now;
            skippedLogMessagesCounter = 0;
        } else {
            ++skippedLogMessagesCounter;
        }
    }

    private void computeSystemEntityState(boolean stateChanged, List<AbstractDataItem> generatedStates) {
        if(stateChanged) {
            this.systemEntityBuilder.setAlarmState(getState().getAlarmState());
        }
        this.systemEntityBuilder.setStatus(entityStatus);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            generatedStates.add(this.entityState);
        }
    }

    private void activateTriggers(ParameterSample sample, Object previousValue, boolean wasInAlarm, boolean stateChanged) {
        ParameterData currentState = getState();
        for(ParameterTriggerDefinition ptd : definition.getTriggers()) {
            try {
                if ((ptd.getTriggerCondition() == TriggerCondition.ON_NEW_SAMPLE && sample != null && stateChanged) ||
                        (ptd.getTriggerCondition() == TriggerCondition.ON_ALARM_RAISED && !wasInAlarm && inAlarm()) ||
                        (ptd.getTriggerCondition() == TriggerCondition.ON_BACK_TO_NOMINAL && wasInAlarm && !inAlarm()) ||
                        (ptd.getTriggerCondition() == TriggerCondition.ON_VALUE_CHANGE && !Objects.equals(previousValue, currentState == null ? null : currentState.getEngValue()))) {
                    // Raise event
                    raiseEvent(ptd.getEvent().getId());
                }
            } catch(Exception e) {
                LOG.log(Level.SEVERE, "Event " + ptd.getEvent().getId() + " cannot be raised by parameter " + id() + " (" + path() + ") due to unexpected exception: " + e.getMessage(), e);
            }
        }
    }

    private void raiseEvent(int eventId) {
        EventProcessor ev = (EventProcessor)processor.getProcessor(eventId);
        if(ev == null) {
            if(LOG.isLoggable(Level.SEVERE)) {
                LOG.severe(String.format("Event %d cannot be raised by parameter %d (%s): event processor not found", eventId, id(), path()));
            }
        } else {
            ev.raiseEvent(path());
        }
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

    private AlarmState check(Object rawValue, Object engValue, Instant generationTime, boolean reevaluation) {
        // If the status is IGNORED, then no checks are performed
        if(getEntityStatus() == Status.IGNORED) {
            return AlarmState.IGNORED;
        }
        // If there are no checks, then value is NOT_CHECKED
        if(definition.getChecks().isEmpty()) {
            return AlarmState.NOT_CHECKED;
        }
        // Otherwise evaluate the checks and derive the result
        AlarmState result = AlarmState.NOMINAL;
        for(CheckDefinition cd : definition.getChecks()) {
            // applicability condition: if not applicable, ignore the check
            if(cd.getApplicability() != null) {
                try {
                    boolean applicable = cd.getApplicability().execute(processor);
                    if(!applicable) {
                        // Next check
                        continue;
                    }
                } catch (ValidityException e) {
                    LOG.log(Level.SEVERE, "Error when evaluating applicability for check " + cd.getName() + " on parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                    // Stop here, it is in ERROR
                    return AlarmState.ERROR;
                }
            }
            // violationCounter is in the checkViolationNumber map after the next instruction
            AtomicInteger violationCounter = checkViolationNumber.computeIfAbsent(cd.getName(), k -> new AtomicInteger(0));
            AlarmState state;
            try {
                state = cd.check(cd.isRawValueChecked() ? rawValue : engValue, generationTime, violationCounter.get(), processor);
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
            } catch(Exception e) {
                LOG.log(Level.SEVERE, "Unexpected error when computing validity of parameter " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                return Validity.ERROR;
            }
        }
    }

    public AbstractInputDataItem generateSetRequest(SetParameterRequest request) throws ProcessingModelException {
        ParameterSetterDefinition setter = definition.getSetter();
        if(setter == null) {
            // Check if user settable
            if(definition.isUserParameter()) {
                Object value = request.getValue();
                if(request.isEngineeringUsed()) {
                    if(value instanceof String) {
                        // If value is of type string and there is a calibration in place, try to map it back to source value
                        value = mapEngineeringValueFromCalibration((String) value);
                    } else {
                        // If the value is any other thing, then we have no idea how to map it to a source value, raise error
                        throw new ProcessingModelException("Parameter " + getPath().asString() + " cannot be used with an engineering value");
                    }
                }
                return ParameterSample.of(request.getId(), value);
            } else {
                throw new ProcessingModelException("Parameter " + getPath().asString() + " does not have a setter operation or is marked as user-parameter, set request cannot be processed");
            }
        } else {
            Map<String, String> propertyMap = new LinkedHashMap<>();
            for (KeyValue kv : setter.getProperties()) {
                propertyMap.put(kv.getKey(), kv.getValue());
            }
            for (KeyValue kv : setter.getActivity().getProperties()) {
                if (!propertyMap.containsKey(kv.getKey())) {
                    propertyMap.put(kv.getKey(), kv.getValue());
                }
            }
            // Overwrite with the setter properties
            for (Map.Entry<String, String> kv : request.getProperties().entrySet()) {
                propertyMap.put(kv.getKey(), kv.getValue());
            }
            return new ActivityRequest(setter.getActivity().getId(), SystemEntityPath.fromString(setter.getActivity().getLocation()), buildSetArgumentList(request, setter), propertyMap, request.getRoute(), request.getSource());
        }
    }

    private Object mapEngineeringValueFromCalibration(String value) throws ProcessingModelException {
        // This method tries to map a string back to a source value
        long valueToReturn = -1;
        boolean found = false;
        if(getDefinition().getCalibrations().size() > 1) {
            throw new ProcessingModelException("Cannot retrieve source value for set of parameter " + getDefinition().getLocation() + " for value " + value + ": more calibrations defined");
        }
        for(CalibrationDefinition cd : getDefinition().getCalibrations()) {
           if(cd instanceof EnumCalibration) {
               try {
                   valueToReturn = ((EnumCalibration) cd).invert(value);
                   found = true;
                   break;
               } catch (CalibrationException e) {
                   // Ignore, go ahead
               }
           }
        }
        if(!found) {
            throw new ProcessingModelException("Cannot retrieve source value for set of parameter " + getDefinition().getLocation() + " for value " + value + ": suitable calibration not found");
        } else {
            if(getDefinition().getRawType() == ValueTypeEnum.ENUMERATED) {
                return (int) valueToReturn;
            } else {
                return valueToReturn;
            }
        }
    }

    private List<AbstractActivityArgument> buildSetArgumentList(SetParameterRequest request, ParameterSetterDefinition setter) throws ProcessingModelException {
        List<AbstractActivityArgument> toReturn = new ArrayList<>();
        // Define a map for the activity defined arguments
        Map<String, AbstractArgumentDefinition> definedArgumentMap = new LinkedHashMap<>();
        for(AbstractArgumentDefinition ad : setter.getActivity().getArguments()) {
            definedArgumentMap.put(ad.getName(), ad);
        }
        Map<String, AbstractActivityArgument> argumentMap = new LinkedHashMap<>();
        // Hardcoded arguments
        for(AbstractArgumentInvocationDefinition aaid : setter.getArguments()) {
            AbstractArgumentDefinition aad = definedArgumentMap.get(aaid.getName());
            if(aad instanceof PlainArgumentDefinition) {
                PlainActivityArgument toAdd = createSimpleArgumentInvocation((PlainArgumentInvocationDefinition) aaid, (PlainArgumentDefinition) aad);
                argumentMap.put(aaid.getName(), toAdd);
            } else if(aad instanceof ArrayArgumentDefinition) {
                ArrayActivityArgument toAdd = createGroupArgumentInvocation((ArrayArgumentInvocationDefinition) aaid, (ArrayArgumentDefinition) aad);
                argumentMap.put(aaid.getName(), toAdd);
            } else {
                throw new ProcessingModelException("Definition for argument " + aad.getName() + " is not supported");
            }
        }
        // Provide the arguments on the defined order
        for(AbstractArgumentDefinition ad : setter.getActivity().getArguments()) {
            if(ad.getName().equals(setter.getSetArgument())) {
                // At this stage, we need to check if the parameter processor needs to perform its own decalibration function and provide the raw value to the activity request
                // or if it has to continue with the standard process
                if(definition.getSetter().getDecalibration() == null || !request.isEngineeringUsed()) {
                    // Continue as usual
                    toReturn.add(new PlainActivityArgument(ad.getName(), request.isEngineeringUsed() ? null : request.getValue(), request.isEngineeringUsed() ? request.getValue() : null, request.isEngineeringUsed()));
                } else {
                    // Decalibrate and use the raw value
                    Object theRawValue;
                    try {
                        theRawValue = CalibrationDefinition.performDecalibration(definition.getSetter().getDecalibration(), request.getValue(), definition.getRawType(), processor);
                    } catch (CalibrationException e) {
                        throw new ProcessingModelException("Cannot decalibrate setter argument " + definition.getSetter().getSetArgument() + " with parameter-defined decalibration: " + e.getMessage(), e);
                    }
                    toReturn.add(new PlainActivityArgument(ad.getName(), theRawValue, null, false));
                }
            } else {
                AbstractActivityArgument alreadyBuilt = argumentMap.get(ad.getName());
                if(alreadyBuilt != null) {
                    toReturn.add(alreadyBuilt);
                }
            }
        }
        return toReturn;
    }

    private ArrayActivityArgument createGroupArgumentInvocation(ArrayArgumentInvocationDefinition agid, ArrayArgumentDefinition agd) throws ProcessingModelException {
        List<ArrayActivityArgumentRecord> records = new LinkedList<>();
        for(ArrayArgumentRecordInvocationDefinition ageid : agid.getRecords()) {
            List<AbstractActivityArgument> argsForGroupRecord = new LinkedList<>();
            for(AbstractArgumentInvocationDefinition aaid : ageid.getElements()) {
                if (aaid instanceof PlainArgumentInvocationDefinition) {
                    PlainActivityArgument toAdd = createSimpleArgumentInvocation((PlainArgumentInvocationDefinition) aaid, (PlainArgumentDefinition) getArgumentDefinitionOf(aaid.getName(), agd));
                    argsForGroupRecord.add(toAdd);
                } else if (aaid instanceof ArrayArgumentInvocationDefinition) {
                    ArrayActivityArgument toAdd = createGroupArgumentInvocation((ArrayArgumentInvocationDefinition) aaid, (ArrayArgumentDefinition) getArgumentDefinitionOf(aaid.getName(), agd));
                    argsForGroupRecord.add(toAdd);
                }
            }
            ArrayActivityArgumentRecord elem = new ArrayActivityArgumentRecord(argsForGroupRecord);
            records.add(elem);
        }
        return new ArrayActivityArgument(agd.getName(), records);
    }

    private AbstractArgumentDefinition getArgumentDefinitionOf(String name, ArrayArgumentDefinition agd) throws ProcessingModelException {
        for(AbstractArgumentDefinition aad : agd.getElements()) {
            if(aad.getName().equals(name)) {
                return aad;
            }
        }
        throw new ProcessingModelException("Cannot find argument " + name + " in elements of argument group " + agd.getName());
    }

    private PlainActivityArgument createSimpleArgumentInvocation(PlainArgumentInvocationDefinition aid, PlainArgumentDefinition ad) {
        if (aid.isRawValue()) {
            return new PlainActivityArgument(aid.getName(), ValueUtil.parse(ad.getRawType(), aid.getValue()), null, false);
        } else {
            return new PlainActivityArgument(aid.getName(), null, ValueUtil.parse(ad.getEngineeringType(), aid.getValue()), true);
        }
    }

    @Override
    public boolean isWeaklyConsistent() {
        return getDefinition().isWeakConsistency() && getDefinition().getExpression() != null;
    }

    @Override
    public List<AbstractDataItem> evaluate(boolean includeWeakly) throws ProcessingModelException {
        if(!includeWeakly && isWeaklyConsistent()) {
            return computeDirtyState();
        } else {
            return process(null);
        }
    }

    private synchronized List<AbstractDataItem> computeDirtyState() {
        // TODO: derive only the dirty status and inform manager if dirty
        // TODO: dirty derivation: get entity vertex --> successors (i.e. affecting this object) --> local map successor ID -> last gen. time --> if time in map < the time, mark as dirty
        return null;
    }

    @Override
    public void visit(IProcessingModelVisitor visitor) {
        visitor.onVisit(getState());
        AlarmParameterData currAlarmData = currentAlarmData.get();
        if(currAlarmData != null) {
            visitor.onVisit(currAlarmData);
        }
    }

    @Override
    public void putCurrentStates(List<AbstractDataItem> items) {
        items.add(getState());
        AlarmParameterData currAlarmData = currentAlarmData.get();
        if(currAlarmData != null) {
            items.add(currAlarmData);
        }
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Object rawValue() {
        ParameterData currentState = getState();
        return currentState == null ? null : currentState.getSourceValue();
    }

    @Override
    public Object value() {
        ParameterData currentState = getState();
        return currentState == null ? null : currentState.getEngValue();
    }

    @Override
    public AlarmState alarmState() {
        ParameterData currentState = getState();
        return currentState == null ? AlarmState.UNKNOWN : currentState.getAlarmState();
    }

    @Override
    public boolean inAlarm() {
        ParameterData currentState = getState();
        return currentState != null &&
                currentState.getAlarmState().isAlarm();
    }

    @Override
    public boolean valid() {
        ParameterData currentState = getState();
        return currentState != null && currentState.getValidity() == Validity.VALID;
    }

    @Override
    public Validity validity() {
        ParameterData currentState = getState();
        return currentState == null ? Validity.UNKNOWN : currentState.getValidity();
    }

    @Override
    public Long containerId() {
        ParameterData currentState = getState();
        return currentState == null || currentState.getRawDataContainerId() == null ? null : currentState.getRawDataContainerId().asLong();
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
    public String route() {
        ParameterData currentState = getState();
        return currentState == null ? null : currentState.getRoute();
    }

    @Override
    public Instant generationTime() {
        ParameterData currentState = getState();
        return currentState == null ? null : currentState.getGenerationTime();
    }

    @Override
    public Instant receptionTime() {
        ParameterData currentState = getState();
        return currentState == null ? null : currentState.getReceptionTime();
    }
}
