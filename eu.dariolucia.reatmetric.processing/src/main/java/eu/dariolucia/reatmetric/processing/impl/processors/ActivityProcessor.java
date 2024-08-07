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

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.value.Array;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ActivityProcessor extends AbstractSystemEntityProcessor<ActivityProcessingDefinition, ActivityOccurrenceData, ActivityProgress> {

    private static final Logger LOG = Logger.getLogger(ActivityProcessor.class.getName());

    private final Map<IUniqueId, ActivityOccurrenceProcessor> id2occurrence = new ConcurrentHashMap<>();
    private final Map<String, AbstractArgumentDefinition> name2argumentDefinition = new TreeMap<>();

    private final Map<IUniqueId, IUniqueId> mirroredId2localId = new ConcurrentHashMap<>();

    private final ActivityDescriptor descriptor;

    public ActivityProcessor(ActivityProcessingDefinition act, ProcessingModelImpl processingModel) {
        super(act, processingModel, SystemEntityType.ACTIVITY);
        for(AbstractArgumentDefinition ad : act.getArguments()) {
            // XXX: argument name duplication to be checked
            name2argumentDefinition.put(ad.getName(), ad);
        }
        // Check if there is an initialiser
        if(processor.getInitialiser() != null) {
            try {
                initialise(processor.getInitialiser());
            } catch(ReatmetricException re) {
                LOG.log(Level.SEVERE, String.format("Cannot initialise activity %d (%s) with archived occurrences as defined by the initialisation time", definition.getId(), definition.getLocation()), re);
            }
        }
        // Initialise the entity state
        this.systemEntityBuilder.setAlarmState(getInitialAlarmState());
        this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
        //
        this.descriptor = buildDescriptor(true);
    }

    private ActivityDescriptor buildDescriptor(boolean stopOnReferenceDefaultValue) {
        // Start building the descriptor
        List<AbstractActivityArgumentDescriptor> argDescriptors = new ArrayList<>(definition.getArguments().size());
        for(AbstractArgumentDefinition aad : definition.getArguments()) {
            if(aad instanceof PlainArgumentDefinition) {
                PlainArgumentDefinition aa = (PlainArgumentDefinition) aad;
                ActivityPlainArgumentDescriptor argDesc = createPlainArgumentDescriptor(stopOnReferenceDefaultValue, aa);
                if (argDesc == null) {
                    return null;
                }
                argDescriptors.add(argDesc);
            } else if(aad instanceof ArrayArgumentDefinition) {
                ArrayArgumentDefinition agd = (ArrayArgumentDefinition) aad;
                ActivityArrayArgumentDescriptor gargDesc = createArrayArgumentDescriptor(stopOnReferenceDefaultValue, agd);
                if (gargDesc == null) {
                    return null;
                }
                argDescriptors.add(gargDesc);
            }
        }
        // Now the properties
        List<Pair<String, String>> props = new ArrayList<>(definition.getProperties().size());
        for(KeyValue kv : definition.getProperties()) {
            props.add(Pair.of(kv.getKey(), kv.getValue()));
        }
        // Build the object
        return new ActivityDescriptor(getPath(), getSystemEntityId(), definition.getDescription(),definition.getDefaultRoute(), definition.getType(), argDescriptors, props, Duration.ofMillis(definition.getExpectedDuration()));
    }

    private ActivityArrayArgumentDescriptor createArrayArgumentDescriptor(boolean stopOnReferenceDefaultValue, ArrayArgumentDefinition agd) {
        List<AbstractActivityArgumentDescriptor> elements = new LinkedList<>();
        for(AbstractArgumentDefinition aad : agd.getElements()) {
            if(aad instanceof PlainArgumentDefinition) {
                PlainArgumentDefinition aa = (PlainArgumentDefinition) aad;
                ActivityPlainArgumentDescriptor argDesc = createPlainArgumentDescriptor(stopOnReferenceDefaultValue, aa);
                if (argDesc == null) {
                    return null;
                }
                elements.add(argDesc);
            } else if(aad instanceof ArrayArgumentDefinition) {
                ArrayArgumentDefinition innerAgd = (ArrayArgumentDefinition) aad;
                ActivityArrayArgumentDescriptor gargDesc = createArrayArgumentDescriptor(stopOnReferenceDefaultValue, innerAgd);
                if (gargDesc == null) {
                    return null;
                }
                elements.add(gargDesc);
            }
        }
        return new ActivityArrayArgumentDescriptor(agd.getName(), agd.getDescription(), agd.getArgumentExpander(), elements);
    }

    private ActivityPlainArgumentDescriptor createPlainArgumentDescriptor(boolean stopOnReferenceDefaultValue, PlainArgumentDefinition aa) {
        if (aa.getDefaultValue() instanceof ReferenceDefaultValue && stopOnReferenceDefaultValue) {
            // Stop here, pre-building object is not possible
            return null;
        }
        Object defaultValue = null;
        if (aa.getDefaultValue() != null) {
            try {
                defaultValue = computeDefaultValue(aa);
            } catch (ProcessingModelException e) {
                LOG.log(Level.SEVERE, String.format("Cannot retrieve default value for argument %s of activity %d (%s)", aa.getName(), definition.getId(), definition.getLocation()));
            }
        }
        return new ActivityPlainArgumentDescriptor(aa.getName(),
                aa.getDescription(),
                aa.getRawType(),
                aa.getEngineeringType(),
                aa.getUnit(),
                aa.isFixed(), aa.getDefaultValue() != null,
                null,
                defaultValue,
                aa.getDecalibration() != null,
                aa.getChecks() == null,
                aa.buildExpectedValuesRaw(),
                aa.buildExpectedValuesEng());
    }

    public List<AbstractDataItem> invoke(ActivityRequest request) throws ProcessingModelException {
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Activity invocation request for activity " + getPath() + " received by the processing model");
            }
            // Start with the checks of the activity request: argument presence and type, route presence
            // Check if the route exists
            processor.checkHandlerAvailability(request.getRoute(), definition.getType());
            // Build the map of activity arguments with the corresponding raw values (decalibration and checks done)
            Map<String, Object> name2value = buildFinalArgumentMap(request, true);
            // Verify that all arguments are specified and, if some are not, use the default values if specified. If not, throw exception
            for (AbstractArgumentDefinition ad : definition.getArguments()) {
                // If the argument was not provided, use the default value
                if (!name2value.containsKey(ad.getName())) {
                    Object finalValue;
                    if(ad instanceof PlainArgumentDefinition) {
                        // Argument not specified in the request: add default
                        if (((PlainArgumentDefinition) ad).getDefaultValue() == null) {
                            throw new ProcessingModelException("Argument " + ad.getName() + " not specified in the request, and default value not present");
                        }
                        finalValue = computeDefaultValue((PlainArgumentDefinition) ad);
                        // Check and add the value to the final value map: if null do not add it and raise an exception
                        verifyAndAdd(name2value, ad, finalValue, true);
                    } else {
                        throw new ProcessingModelException("Argument " + ad.getName() + " not specified and without default value");
                    }
                }
            }
            // At this stage, the map name2value is complete and everything is setup according to definition, but we create a LinkedHashMap that follows the definition order
            Map<String, Object> finalName2value = new LinkedHashMap<>();
            for (AbstractArgumentDefinition ad : definition.getArguments()) {
                finalName2value.put(ad.getName(), name2value.get(ad.getName()));
            }
            // Done
            Map<String, String> properties = new TreeMap<>();
            for (KeyValue kv : definition.getProperties()) {
                properties.put(kv.getKey(), kv.getValue());
            }
            properties.putAll(request.getProperties());
            ActivityOccurrenceProcessor activityOccurrence = new ActivityOccurrenceProcessor(this, new LongUniqueId(processor.getNextId(ActivityOccurrenceData.class)), Instant.now(), finalName2value, properties, new LinkedList<>(), request.getRoute(), request.getSource());
            id2occurrence.put(activityOccurrence.getOccurrenceId(), activityOccurrence);
            if(LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Activity occurrence " + activityOccurrence.getOccurrenceId() + " for activity " + getPath() + " created by the processing model", new Object[] { getPath().asString() + ":" + activityOccurrence.getOccurrenceId() });
            }
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

    private Map<String, Object> buildFinalArgumentMap(ActivityRequest request, boolean applyChecks) throws ProcessingModelException {
        Map<String, Object> name2value = new LinkedHashMap<>();
        for (AbstractActivityArgument aarg : request.getArguments()) {
            AbstractArgumentDefinition aargDef = name2argumentDefinition.get(aarg.getName());
            // Argument is defined?
            if (aargDef == null) {
                throw new ProcessingModelException("Argument " + aarg.getName() + " not present in the activity definition");
            }
            if (aarg instanceof PlainActivityArgument) {
                PlainActivityArgument arg = (PlainActivityArgument) aarg;
                PlainArgumentDefinition argDef = (PlainArgumentDefinition) aargDef;
                Object finalValue = createSimpleArgument(arg, argDef, applyChecks);
                // Check and add the value to the final value map: if null do not add it but do not raise an exception
                verifyAndAdd(name2value, argDef, finalValue, false);
            } else if (aarg instanceof ArrayActivityArgument) {
                ArrayActivityArgument garg = (ArrayActivityArgument) aarg;
                ArrayArgumentDefinition gargDef = (ArrayArgumentDefinition) aargDef;
                // Encode an object value of type Array
                Array arrayValue = createGroup(garg, gargDef, applyChecks);
                // Check and add the value to the final value map: if null do not add it but do not raise an exception
                verifyAndAdd(name2value, gargDef, arrayValue, false);
            } else {
                throw new ProcessingModelException("Argument " + aarg.getName() + " has definition type not supported: " + aarg.getClass().getName());
            }
        }
        return name2value;
    }

    private Array createGroup(ArrayActivityArgument garg, ArrayArgumentDefinition gargDef, boolean applyChecks) throws ProcessingModelException {
        List<Array.Record> records = new LinkedList<>();
        for(ArrayActivityArgumentRecord rec : garg.getRecords()) {
            List<Pair<String, Object>> elements = new LinkedList<>();
            for(AbstractActivityArgument aaa : rec.getElements()) {
                AbstractArgumentDefinition aargDef = retrieveGroupElementDefinition(aaa.getName(), gargDef);
                // Argument is defined?
                if (aargDef == null) {
                    throw new ProcessingModelException("Argument " + aaa.getName() + " not present in group definition " + gargDef.getName());
                }
                if(aaa instanceof PlainActivityArgument) {
                    PlainActivityArgument arg = (PlainActivityArgument) aaa;
                    PlainArgumentDefinition argDef = (PlainArgumentDefinition) aargDef;
                    Object finalValue = createSimpleArgument(arg, argDef, applyChecks);
                    elements.add(Pair.of(arg.getName(), finalValue));
                } else if(aaa instanceof ArrayActivityArgument) {
                    ArrayActivityArgument gaaa = (ArrayActivityArgument) aaa;
                    ArrayArgumentDefinition gaaaDef = (ArrayArgumentDefinition) aargDef;
                    // Encode an object value of type Array
                    Array arrayValue = createGroup(gaaa, gaaaDef, applyChecks);
                    elements.add(Pair.of(gaaa.getName(), arrayValue));
                } else {
                    throw new ProcessingModelException("Argument " + aaa.getName() + " has definition type not supported: " + aaa.getClass().getName());
                }
            }
            records.add(new Array.Record(elements));
        }
        return new Array(records);
    }

    private AbstractArgumentDefinition retrieveGroupElementDefinition(String name, ArrayArgumentDefinition gargDef) {
        for(AbstractArgumentDefinition arg : gargDef.getElements()) {
            if(name.equals(arg.getName())) {
                return arg;
            }
        }
        return null;
    }

    private Object createSimpleArgument(PlainActivityArgument arg, PlainArgumentDefinition argDef, boolean applyChecks) throws ProcessingModelException {
        // Type is correct?
        if (arg.getEngValue() != null && !ValueUtil.typeMatch(argDef.getEngineeringType(), arg.getEngValue())) {
            throw new ProcessingModelException("Argument " + arg.getName() + " set with engineering value not matching the argument engineering value definition type " + argDef.getEngineeringType() + ", expected " + argDef.getEngineeringType().getAssignedClass().getSimpleName() + ", actual is " + arg.getEngValue().getClass().getSimpleName());
        }
        if (arg.getRawValue() != null && !ValueUtil.typeMatch(argDef.getRawType(), arg.getRawValue())) {
            throw new ProcessingModelException("Argument " + arg.getName() + " set with raw value not matching the argument raw value definition type " + argDef.getRawType() + ", expected " + argDef.getRawType().getAssignedClass().getSimpleName() + ", actual is " + arg.getRawValue().getClass().getSimpleName());
        }
        // Argument is fixed and need to apply the checks? Then check if there is corresponding value.
        if (argDef.isFixed() && applyChecks) {
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
        // Check this argument now
        if(applyChecks) {
            checkSimpleArgument(finalValue, arg.getEngValue(), argDef);
        }
        // If all fine, go
        return finalValue;
    }

    /**
     * This method returns the default value in RAW format for the provided argument.
     *
     * @param ad the argument definition for which the value shall be derived
     * @return the default value as object in raw format
     *
     * @throws ProcessingModelException if the default value cannot be computed
     */
    private Object computeDefaultValue(PlainArgumentDefinition ad) throws ProcessingModelException {
        Object finalValue;// If default value is fixed, then use it
        if (ad.getDefaultValue() instanceof FixedDefaultValue) {
            String formattedValue = ((FixedDefaultValue) ad.getDefaultValue()).getValue();
            if (ad.getDefaultValue().getType() == DefaultValueType.RAW) {
                finalValue = ValueUtil.parse(ad.getRawType(), formattedValue);
                checkSimpleArgument(finalValue, null, ad);
            } else if (ad.getDefaultValue().getType() == DefaultValueType.ENGINEERING) {
                Object engValue = ValueUtil.parse(ad.getEngineeringType(), formattedValue);
                try {
                    finalValue = CalibrationDefinition.performDecalibration(ad.getDecalibration(), engValue, ad.getRawType(), processor);
                } catch (CalibrationException e) {
                    throw new ProcessingModelException("Cannot decalibrate default (fixed) value of argument " + ad.getName() + ": " + e.getMessage(), e);
                }
                checkSimpleArgument(finalValue, engValue, ad);
            } else {
                throw new ProcessingModelException("Default value of argument " + ad.getName() + " has undefined value type: " + ad.getDefaultValue().getType());
            }
            // If default value comes from another parameter, then retrieve and use it
        } else if (ad.getDefaultValue() instanceof ReferenceDefaultValue) {
            try {
                finalValue = ((ReferenceDefaultValue) ad.getDefaultValue()).readTargetValue(ad.getName(), processor);
            } catch (ValueReferenceException e) {
                throw new ProcessingModelException(e);
            }
            if (ad.getDefaultValue().getType() == DefaultValueType.ENGINEERING) {
                Object engValue = finalValue;
                try {
                    finalValue = CalibrationDefinition.performDecalibration(ad.getDecalibration(), engValue, ad.getRawType(), processor);
                } catch (CalibrationException e) {
                    throw new ProcessingModelException("Cannot decalibrate default (reference) value of argument " + ad.getName() + ": " + e.getMessage(), e);
                }
                checkSimpleArgument(finalValue, engValue, ad);
            } else {
                checkSimpleArgument(finalValue, null, ad);
            }
        } else {
            throw new ProcessingModelException("Default value of argument " + ad.getName() + " has unsupported type: " + ad.getDefaultValue().getClass().getName());
        }
        return finalValue;
    }

    private void checkSameValue(PlainArgumentDefinition plainArgumentDefinition, PlainActivityArgument suppliedArgument) throws ProcessingModelException {
        DefaultValueType definedType = plainArgumentDefinition.getDefaultValue().getType();
        DefaultValueType suppliedType = suppliedArgument.isEngineering() ? DefaultValueType.ENGINEERING : DefaultValueType.RAW;
        if(definedType != suppliedType) {
            throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument type: defined " + definedType + ", but provided " + suppliedType);
        }
        Object suppliedValue = suppliedArgument.isEngineering() ? suppliedArgument.getEngValue() : suppliedArgument.getRawValue();
        if(plainArgumentDefinition.getDefaultValue() instanceof FixedDefaultValue) {
            String definedValueStr = ((FixedDefaultValue) plainArgumentDefinition.getDefaultValue()).getValue();
            Object definedValue = ValueUtil.parse(definedType == DefaultValueType.ENGINEERING ? plainArgumentDefinition.getEngineeringType() : plainArgumentDefinition.getRawType(), definedValueStr);
            if(!Objects.equals(definedValue, suppliedValue)) {
                throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument value: defined (fixed) " + definedValue + ", but provided " + suppliedValue);
            }
        } else if(plainArgumentDefinition.getDefaultValue() instanceof ReferenceDefaultValue) {
            Object referencedValue;
            try {
                referencedValue = ((ReferenceDefaultValue) plainArgumentDefinition.getDefaultValue()).readTargetValue(plainArgumentDefinition.getName(), processor);
            } catch (ValueReferenceException e) {
                throw new ProcessingModelException(e);
            }
            if(!Objects.equals(referencedValue, suppliedValue)) {
                throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " violates fixed argument value: defined (reference to " + ((ReferenceDefaultValue) plainArgumentDefinition.getDefaultValue()).getParameter().getLocation() + ") " + referencedValue + ", but provided " + suppliedValue);
            }
        } else {
            throw new ProcessingModelException("Supplied argument " + suppliedArgument.getName() + " is fixed but the argument definition does not define a valid default value");
        }
    }

    private List<AbstractDataItem> removeActivityOccurrenceIfCompleted(IUniqueId occurrenceId, List<AbstractDataItem> result) {
        for(AbstractDataItem adi : result) {
            if(adi instanceof ActivityOccurrenceData) {
                ActivityOccurrenceState theState = ((ActivityOccurrenceData) adi).getCurrentState();
                if(theState == ActivityOccurrenceState.COMPLETED) {
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

    private void verifyAndAdd(Map<String, Object> argumentMap, AbstractArgumentDefinition aargDef, Object finalValue, boolean throwExceptionOfFinalNull) throws ProcessingModelException {
        // Final nullity check
        if(finalValue == null) {
            if(throwExceptionOfFinalNull) {
                throw new ProcessingModelException("Value of argument " + aargDef.getName() + " is null and cannot be processed at this stage");
            }
        } else {
            argumentMap.put(aargDef.getName(), finalValue);
        }
    }

    private void checkSimpleArgument(Object rawValue, Object engValue, PlainArgumentDefinition argDef) throws ProcessingModelException {
        for (CheckDefinition cd : argDef.getChecks()) {
            // applicability condition: if not applicable, ignore the check
            if (cd.getApplicability() != null) {
                try {
                    boolean applicable = cd.getApplicability().execute(processor);
                    if (!applicable) {
                        // Next check
                        continue;
                    }
                } catch (ValidityException e) {
                    throw new ProcessingModelException("Error when evaluating applicability for check " + cd.getName() + " on activity " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                }
            }
            // if the check is on the eng. value, but this is null, then the check is skipped
            if(!cd.isRawValueChecked() && engValue == null) {
                continue;
            }
            Object valueToCheck = cd.isRawValueChecked() ? rawValue : engValue;
            try {
                AlarmState as = cd.check(valueToCheck, null, 0, processor);
                if (as != AlarmState.NOMINAL) {
                    throw new ProcessingModelException("Value " + valueToCheck + " of argument " + argDef.getName() + " failed execution of check " + cd.getName() + ": " + as);
                }
            } catch (CheckException e) {
                throw new ProcessingModelException("Value " + valueToCheck + " of argument " + argDef.getName() + " failed execution of check " + cd.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    public List<AbstractDataItem> create(ActivityRequest request, ActivityProgress progress) throws ProcessingModelException {
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            // Build the map of activity arguments with the corresponding raw values, no checks applied, we need to accept the information from the caller
            Map<String, Object> name2value = buildFinalArgumentMap(request, false);
            // At this stage we do not verify that all arguments are specified, we need to accept the information provided by the caller, without speculations
            // The order of the arguments should not be important as well
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
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
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
    public List<AbstractDataItem> evaluate(boolean includeWeakly) {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Evaluating all activity occurrences for activity " + getSystemEntityId());
        }
        List<AbstractDataItem> result = new LinkedList<>();
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
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
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
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
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
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

    public List<AbstractDataItem> abort(IUniqueId occurrenceId) {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Aborting activity occurrence " + occurrenceId + " of activity " + getSystemEntityId());
        }
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            ActivityOccurrenceProcessor aop = id2occurrence.get(occurrenceId);
            if (aop == null) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("No activity occurrence with ID " + occurrenceId + " found, abort request not processed");
                }
                return Collections.emptyList();
            } else {
                // Abort is simply forwarded and handled by the implementor: the processing model does not take any initiative here, because
                // it might be that the activity CANNOT be aborted
                aop.abort();
                return Collections.emptyList();
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Activity abort request not processed for activity " + getPath() + ": activity processing is disabled");
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
    public AbstractSystemEntityDescriptor getDescriptor() {
        // Due to potential default value based on reference, it might not be possible to pre-build a descriptor object
        if(descriptor != null) {
            return descriptor;
        } else {
            return buildDescriptor(false);
        }
    }

    private void initialise(IProcessingModelInitialiser initialiser) throws ReatmetricException {
        List<AbstractDataItem> stateList = initialiser.getState(getSystemEntityId(), SystemEntityType.ACTIVITY);
        for(AbstractDataItem data : stateList) {
            ActivityOccurrenceData aod = (ActivityOccurrenceData) data;
            if(aod.getCurrentState() != ActivityOccurrenceState.COMPLETED) {
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

    public List<AbstractDataItem> mirror(ActivityOccurrenceData input) {
        if(entityStatus == Status.ENABLED || entityStatus == Status.IGNORED) {
            // Check if a local internal ID is already present for this state
            IUniqueId localId = this.mirroredId2localId.get(input.getInternalId());
            ActivityOccurrenceProcessor activityOccurrence = null;
            if(localId == null) {
                // Not present - create new ActivityOccurrenceProcessor
                activityOccurrence = new ActivityOccurrenceProcessor(this, new LongUniqueId(processor.getNextId(ActivityOccurrenceData.class)), input.getGenerationTime(), input.getArguments(), input.getProperties(), new LinkedList<>(), input.getRoute(), input.getSource(), input.getInternalId());
                id2occurrence.put(activityOccurrence.getOccurrenceId(), activityOccurrence);
                mirroredId2localId.put(input.getInternalId(), activityOccurrence.getOccurrenceId());
                // inform the processor that the activity occurrence has been created, check equality to 1 to avoid calling the registration for every occurrence
                if (id2occurrence.size() == 1) {
                    processor.registerActiveActivityProcessor(this);
                }
            } else {
                activityOccurrence = id2occurrence.get(localId);
            }
            return removeActivityOccurrenceIfCompleted(activityOccurrence.getOccurrenceId(), activityOccurrence.mirror(input));
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Activity mirror request not computed for activity " + getPath() + ": activity processing is disabled");
            }
            return Collections.emptyList();
        }
    }
}
