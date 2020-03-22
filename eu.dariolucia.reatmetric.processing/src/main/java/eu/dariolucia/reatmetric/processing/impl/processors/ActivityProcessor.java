/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.api.processing.input.ActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ActivityProcessor extends AbstractSystemEntityProcessor<ActivityProcessingDefinition, ActivityOccurrenceData, ActivityProgress> {

    private final static Logger LOG = Logger.getLogger(ActivityProcessor.class.getName());

    private final Map<IUniqueId, ActivityOccurrenceProcessor> id2occurrence = new ConcurrentHashMap<>();
    private final Map<String, ArgumentDefinition> name2argumentDefinition = new TreeMap<>();

    public ActivityProcessor(ActivityProcessingDefinition act, ProcessingModelImpl processingModel) {
        super(act, processingModel, SystemEntityType.ACTIVITY);
        for(ArgumentDefinition ad : act.getArguments()) {
            // XXX: argument name duplication to be checked
            name2argumentDefinition.put(ad.getName(), ad);
        }
    }

    public List<AbstractDataItem> invoke(ActivityRequest request) throws ProcessingModelException {
        if(entityStatus == Status.ENABLED) {
            // Start with the checks of the activity request: argument presence and type, route presence
            // Check if the route exists
            processor.checkHandlerAvailability(request.getRoute(), definition.getType());
            // Build the map of activity arguments with the corresponding raw values
            Map<String, Object> name2value = new TreeMap<>();
            for (ActivityArgument arg : request.getArguments()) {
                ArgumentDefinition argDef = name2argumentDefinition.get(arg.getName());
                // Argument is defined?
                if (argDef == null) {
                    throw new ProcessingModelException("Argument " + arg.getName() + " not present in the activity definition");
                }
                // Type is correct?
                if (arg.getEngValue() != null && !ValueUtil.typeMatch(argDef.getEngineeringType(), arg.getEngValue())) {
                    throw new ProcessingModelException("Argument " + arg.getName() + " set with engineering value not matching the argument engineering value definition type " + argDef.getEngineeringType() + ", expected " + argDef.getEngineeringType().getAssignedClass().getSimpleName());
                }
                if (arg.getRawValue() != null && !ValueUtil.typeMatch(argDef.getRawType(), arg.getRawValue())) {
                    throw new ProcessingModelException("Argument " + arg.getName() + " set with raw value not matching the argument raw value definition type " + argDef.getRawType() + ", expected " + argDef.getRawType().getAssignedClass().getSimpleName());
                }
                // Argument is fixed? Then check if there is corresponding value.
                if (argDef.isFixed()) {
                    checkSameValue(argDef, arg);
                }
                // If it is engineering value and there is a decalibration function, decalibrate
                Object finalValue = arg.getRawValue() != null ? arg.getRawValue() : arg.getEngValue();
                if (arg.getRawValue() == null) {
                    try {
                        finalValue = CalibrationDefinition.performDecalibration(argDef.getDecalibration(), finalValue, argDef.getRawType(), processor);
                    } catch (CalibrationException e) {
                        throw new ProcessingModelException("Cannot decalibrate argument " + arg.getName() + ": " + e.getMessage(), e);
                    }
                }
                // Check and add the value to the final value map: if null do not add it but do not raise an exception
                verifyAndAdd(name2value, argDef, finalValue, false);
            }
            // Verify that all arguments are specified and, if some are not, use the default values if specified. If not, throw exception
            for (ArgumentDefinition ad : definition.getArguments()) {
                // If the argument was not provided, use the default value
                if (!name2value.containsKey(ad.getName())) {
                    Object finalValue;
                    DefaultValueType valueType = ad.getDefaultValue().getType();
                    // Argument not specified in the request: add default
                    if (ad.getDefaultValue() == null) {
                        throw new ProcessingModelException("Argument " + ad.getName() + " not specified in the request, and default value not present");
                    }
                    // If default value is fixed, then use it
                    if (ad.getDefaultValue() instanceof FixedDefaultValue) {
                        String formattedValue = ((FixedDefaultValue) ad.getDefaultValue()).getValue();
                        if (valueType == DefaultValueType.RAW) {
                            finalValue = ValueUtil.parse(ad.getRawType(), formattedValue);
                        } else if (valueType == DefaultValueType.ENGINEERING) {
                            finalValue = ValueUtil.parse(ad.getEngineeringType(), formattedValue);
                            try {
                                finalValue = CalibrationDefinition.performDecalibration(ad.getDecalibration(), finalValue, ad.getRawType(), processor);
                            } catch (CalibrationException e) {
                                throw new ProcessingModelException("Cannot decalibrate default (fixed) value of argument " + ad.getName() + ": " + e.getMessage(), e);
                            }
                        } else {
                            throw new ProcessingModelException("Default value of argument " + ad.getName() + " has undefined value type: " + valueType);
                        }
                        // If default value comes from another parameter, then retrieve and use it
                    } else if (ad.getDefaultValue() instanceof ReferenceDefaultValue) {
                        try {
                            finalValue = ((ReferenceDefaultValue) ad.getDefaultValue()).readTargetValue(ad.getName(), processor);
                        } catch (ValueReferenceException e) {
                            throw new ProcessingModelException(e);
                        }
                        if (valueType == DefaultValueType.ENGINEERING) {
                            try {
                                finalValue = CalibrationDefinition.performDecalibration(ad.getDecalibration(), finalValue, ad.getRawType(), processor);
                            } catch (CalibrationException e) {
                                throw new ProcessingModelException("Cannot decalibrate default (reference) value of argument " + ad.getName() + ": " + e.getMessage(), e);
                            }
                        }
                    } else {
                        throw new ProcessingModelException("Default value of argument " + ad.getName() + " has unsupported type: " + ad.getDefaultValue().getClass().getName());
                    }
                    // Check and add the value to the final value map: if null do not add it and raise an exception
                    verifyAndAdd(name2value, ad, finalValue, true);
                }
            }
            // At this stage, the map name2value is complete and everything is setup according to definition
            Map<String, String> properties = new TreeMap<>();
            for (KeyValue kv : definition.getProperties()) {
                properties.put(kv.getKey(), kv.getValue());
            }
            properties.putAll(request.getProperties());
            ActivityOccurrenceProcessor activityOccurrence = new ActivityOccurrenceProcessor(this, new LongUniqueId(processor.getNextId(ActivityOccurrenceData.class)), Instant.now(), name2value, properties, new LinkedList<>(), request.getRoute(), request.getSource());
            id2occurrence.put(activityOccurrence.getOccurrenceId(), activityOccurrence);
            // inform the processor that the activity occurrence has been created, use equality to 1 to avoid calling the registration for every activity
            if (id2occurrence.size() == 1) {
                processor.registerActiveActivityProcessor(this);
            }
            return removeActivityOccurrenceIfCompleted(activityOccurrence.getOccurrenceId(), activityOccurrence.dispatch());
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Activity invocation request not processed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }

    private void checkSameValue(ArgumentDefinition argumentDefinition, ActivityArgument suppliedArgument) throws ProcessingModelException {
        DefaultValueType definedType = argumentDefinition.getDefaultValue().getType();
        DefaultValueType suppliedType = suppliedArgument.isEngineering() ? DefaultValueType.ENGINEERING : DefaultValueType.RAW;
        if(definedType != suppliedType) {
            throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument type: defined " + definedType + ", but provided " + suppliedType);
        }
        Object suppliedValue = suppliedArgument.isEngineering() ? suppliedArgument.getEngValue() : suppliedArgument.getRawValue();
        if(argumentDefinition.getDefaultValue() instanceof FixedDefaultValue) {
            String definedValueStr = ((FixedDefaultValue) argumentDefinition.getDefaultValue()).getValue();
            Object definedValue = ValueUtil.parse(definedType == DefaultValueType.ENGINEERING ? argumentDefinition.getEngineeringType() : argumentDefinition.getRawType(), definedValueStr);
            if(!Objects.equals(definedValue, suppliedValue)) {
                throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument value: defined (fixed) " + definedValue + ", but provided " + suppliedValue);
            }
        } else if(argumentDefinition.getDefaultValue() instanceof ReferenceDefaultValue) {
            Object referencedValue;
            try {
                referencedValue = ((ReferenceDefaultValue) argumentDefinition.getDefaultValue()).readTargetValue(argumentDefinition.getName(), processor);
            } catch (ValueReferenceException e) {
                throw new ProcessingModelException(e);
            }
            if(!Objects.equals(referencedValue, suppliedValue)) {
                throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument value: defined (reference to " + ((ReferenceDefaultValue) argumentDefinition.getDefaultValue()).getParameter().getLocation() + ") " + referencedValue + ", but provided " + suppliedValue);
            }
        } else {
            throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " is fixed but the argument definition does not define a valid default value");
        }
    }

    private List<AbstractDataItem> removeActivityOccurrenceIfCompleted(IUniqueId occurrenceId, List<AbstractDataItem> result) {
        for(AbstractDataItem adi : result) {
            if(adi instanceof ActivityOccurrenceData) {
                ActivityOccurrenceState theState = ((ActivityOccurrenceData) adi).getCurrentState();
                if(theState == ActivityOccurrenceState.COMPLETION) {
                    if(LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Removing activity occurrence " + adi.getInternalId() + " of activity " + getSystemEntityId() + ", since completed");
                    }
                    id2occurrence.remove(occurrenceId);
                    break;
                }
            }
        }
        // If at this stage there are no occurrences, this activity processor is not active anymore
        if(id2occurrence.isEmpty()) {
            processor.deregisterInactiveActivityProcessor(this);
        }
        return result;
    }

    private void verifyAndAdd(Map<String, Object> argumentMap, ArgumentDefinition argDef, Object finalValue, boolean throwExceptionOfFinalNull) throws ProcessingModelException {
        // Apply checks
        for(CheckDefinition cd : argDef.getChecks()) {
            try {
                AlarmState as = cd.check(finalValue, null, 0, processor);
                if(as != AlarmState.NOMINAL) {
                    throw new ProcessingModelException("Value " + finalValue + " of argument " + argDef.getName() + " failed execution of check " + cd.getName() + ": " + as);
                }
            } catch (CheckException e) {
                throw new ProcessingModelException("Value " + finalValue + " of argument " + argDef.getName() + " failed execution of check " + cd.getName() + ": " + e.getMessage(), e);
            }
        }
        // Final nullity check
        if(finalValue == null) {
            if(throwExceptionOfFinalNull) {
                throw new ProcessingModelException("Value of argument " + argDef.getName() + " is null and cannot be processed at this stage");
            }
        } else {
            argumentMap.put(argDef.getName(), finalValue);
        }
    }

    public List<AbstractDataItem> create(ActivityRequest request, ActivityProgress progress) {
        if(entityStatus == Status.ENABLED) {
            // Build the map of activity arguments with the corresponding raw values
            Map<String, Object> name2value = new TreeMap<>();
            for (ActivityArgument arg : request.getArguments()) {
                name2value.put(arg.getName(), arg.getRawValue()); // XXX: raw value only
            }
            //
            ActivityOccurrenceProcessor activityOccurrence = new ActivityOccurrenceProcessor(this, new LongUniqueId(processor.getNextId(ActivityOccurrenceData.class)), progress.getGenerationTime(), name2value, request.getProperties(), new LinkedList<>(), request.getRoute(), request.getSource());
            id2occurrence.put(activityOccurrence.getOccurrenceId(), activityOccurrence);
            // inform the processor that the activity occurrence has been created, check equality to 1 to avoid calling the registration for every occurrence
            if (id2occurrence.size() == 1) {
                processor.registerActiveActivityProcessor(this);
            }
            return removeActivityOccurrenceIfCompleted(activityOccurrence.getOccurrenceId(), activityOccurrence.create(progress));
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Activity creation request not computed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }

    @Override
    public List<AbstractDataItem> process(ActivityProgress input) {
        if(entityStatus == Status.ENABLED) {
            ActivityOccurrenceProcessor aop = id2occurrence.get(input.getOccurrenceId());
            if (aop == null) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("No activity occurrence with ID " + input.getOccurrenceId() + " found, progress report not processed");
                }
                return Collections.emptyList();
            } else {
                return removeActivityOccurrenceIfCompleted(input.getOccurrenceId(), aop.progress(input));
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Activity progress report not computed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }

    @Override
    public List<AbstractDataItem> evaluate() {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Evaluating all activity occurrences for activity " + getSystemEntityId());
        }
        List<AbstractDataItem> result = new LinkedList<>();
        if(entityStatus == Status.ENABLED) {
            // Copy the keys
            Set<IUniqueId> keys = new HashSet<>(id2occurrence.keySet());
            for(IUniqueId k : keys) {
                result.addAll(evaluate(k));
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Activity re-evaluation not computed for activity " + getPath() + ": activity processing is disabled");
            }
        }
        // enablement state to be supported
        this.systemEntityBuilder.setStatus(entityStatus);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            result.add(this.entityState);
        }
        return result;
    }

    public List<AbstractDataItem> evaluate(IUniqueId occurrenceId) {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Evaluating activity occurrence " + occurrenceId + " of activity " + getSystemEntityId());
        }
        if(entityStatus == Status.ENABLED) {
            ActivityOccurrenceProcessor aop = id2occurrence.get(occurrenceId);
            if (aop == null) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("No activity occurrence with ID " + occurrenceId + " found, evaluation not processed");
                }
                return Collections.emptyList();
            } else {
                return removeActivityOccurrenceIfCompleted(occurrenceId, aop.evaluate());
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Activity re-evaluation not computed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }

    public List<AbstractDataItem> purge(IUniqueId occurrenceId) {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Purging activity occurrence " + occurrenceId + " of activity " + getSystemEntityId());
        }
        if(entityStatus == Status.ENABLED) {
            ActivityOccurrenceProcessor aop = id2occurrence.get(occurrenceId);
            if (aop == null) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("No activity occurrence with ID " + occurrenceId + " found, purge request not processed");
                }
                return Collections.emptyList();
            } else {
                return removeActivityOccurrenceIfCompleted(occurrenceId, aop.purge());
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Activity purge request not processed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }

    @Override
    public void visit(IProcessingModelVisitor visitor) {
        for(ActivityOccurrenceProcessor proc : id2occurrence.values()) {
            proc.visit(visitor);
        }
    }

    @Override
    public void putCurrentStates(List<AbstractDataItem> items) {
        for(ActivityOccurrenceProcessor proc : id2occurrence.values()) {
            items.add(proc.get());
        }
    }

    @Override
    public void initialise(List<ActivityOccurrenceData> state, SystemEntity entityState) {
        this.entityState = entityState;
        for(ActivityOccurrenceData aod : state) {
            if(aod.getCurrentState() != ActivityOccurrenceState.COMPLETION) {
                id2occurrence.put(aod.getInternalId(), new ActivityOccurrenceProcessor(this, aod));
            }
        }
        // inform the processor that the activity occurrences have been created (if that is the case)
        if(!id2occurrence.isEmpty()) {
            processor.registerActiveActivityProcessor(this);
        }
    }

    public List<ActivityOccurrenceData> getActiveActivityOccurrences() {
        return id2occurrence.values().stream().map(Supplier::get).collect(Collectors.toList());
    }
}
