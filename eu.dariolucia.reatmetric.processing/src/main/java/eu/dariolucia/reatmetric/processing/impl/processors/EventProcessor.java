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

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.scripting.IEventBinding;
import eu.dariolucia.reatmetric.processing.definition.EventProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.EventDataBuilder;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to process a system entity of type EVENT.
 */
public class EventProcessor extends AbstractSystemEntityProcessor<EventProcessingDefinition, EventData, EventOccurrence> implements IEventBinding {

    private static final Logger LOG = Logger.getLogger(EventProcessor.class.getName());

    private boolean conditionTriggerState = false;

    private boolean internallyTriggered = false;
    private List<String> internalSource = new LinkedList<>(); // nulls allowed

    private Instant lastReportedEventTime = null;

    private final EventDataBuilder builder;

    private final EventDescriptor descriptor;

    public EventProcessor(EventProcessingDefinition definition, ProcessingModelImpl processor) {
        super(definition, processor, SystemEntityType.EVENT);
        this.builder = new EventDataBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()), definition.getSeverity(), definition.getType());
        // Check if there is an initialiser
        if(processor.getInitialiser() != null) {
            try {
                initialise(processor.getInitialiser());
            } catch(ReatmetricException re) {
                LOG.log(Level.SEVERE, String.format("Cannot initialise event %d (%s) with archived state as defined by the initialisation time", definition.getId(), definition.getLocation()), re);
            }
        }
        // Initialise the entity state
        this.systemEntityBuilder.setAlarmState(getInitialAlarmState());
        this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
        // Create the descriptor
        this.descriptor = new EventDescriptor(getPath(), definition.getId(), definition.getDescription(), definition.getSeverity(), definition.getType(), definition.getCondition() != null);
    }

    private void initialise(IProcessingModelInitialiser initialiser) throws ReatmetricException {
        List<AbstractDataItem> stateList = initialiser.getState(getSystemEntityId(), SystemEntityType.EVENT);
        if(!stateList.isEmpty()) {
            this.state = (EventData) stateList.get(0);
            builder.setInitialisation(this.state);
        }
    }

    @Override
    public List<AbstractDataItem> process(EventOccurrence newValue) {
        // Guard condition: if this event has an expression, newValue must be null
        if(definition.getCondition() != null && newValue != null) {
            if(LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, String.format("Event %d (%s) is a condition-driven event, but an external occurrence was injected. Processing ignored.", id(), path()));
            }
            return Collections.emptyList();
        }
        boolean mustBeRaised = false;
        Object report = null;
        IUniqueId containerId = null;
        String route = null;
        List<String> sourceList = null;
        String qualifier = null;
        List<AbstractDataItem> generatedStates = new ArrayList<>(2);
        // If the object is enabled, then you have to process it as usual
        if(entityStatus == Status.ENABLED) {
            // Prepare the time values
            Instant generationTime = this.state == null ? Instant.now() : this.state.getGenerationTime();
            generationTime = newValue != null ? newValue.getGenerationTime() : generationTime;
            if(definition.getCondition() != null) {
                // If there is an expression, then evaluate the expression and check for a transition false -> true
                sourceList = Collections.singletonList(getPath().asString());
                boolean triggered;
                try {
                    triggered = (Boolean) definition.getCondition().execute(processor, null);
                    mustBeRaised = triggered && !conditionTriggerState; // raise the event if it must be raised and it is not already reported from previous evaluation
                    conditionTriggerState = triggered;
                    // No need to set more
                } catch (ScriptException|ClassCastException e) {
                    LOG.log(Level.SEVERE, "Error when evaluating condition of event " + definition.getId() + " (" + definition.getLocation() + "): " + e.getMessage(), e);
                    // Finalize entity state and prepare for the returned list of data items
                    computeEntityState(generatedStates);
                    // Stop doing the processing
                    return generatedStates;
                }
            } else if(newValue != null) {
                // No condition, use the input since it is available
                report = newValue.getReport();
                containerId = newValue.getContainer();
                qualifier = newValue.getQualifier();
                sourceList = Collections.singletonList(newValue.getSource());
                route = newValue.getRoute();
                mustBeRaised = true;
            } else {
                // No condition, no input data: simple re-evaluation, check if there is an external trigger: if no trigger, then no event
                if(internallyTriggered) {
                    mustBeRaised = true;
                    sourceList = new ArrayList<>(internalSource);
                    // Reset the flag
                    internallyTriggered = false;
                    internalSource.clear();
                }
            }
            // Check inhibition time - If an event is detected/reported during the inhibition period, the raising is discarded
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, String.format("Check inhibition condition for event %s: mustBeRaised=%s, lastReportedEventTime=%s, inhibitionPeriod=%d ms", definition.getLocation(), mustBeRaised, lastReportedEventTime, definition.getInhibitionPeriod()));
            }
            if(mustBeRaised && this.lastReportedEventTime != null && this.lastReportedEventTime.plusMillis(definition.getInhibitionPeriod()).isAfter(generationTime)) {
                // Do not raise the event: inhibited
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Event occurrence not raised for event " + definition.getLocation() + ": event processing is inhibited");
                }
                mustBeRaised = false;
                // Reset also the condition triggered state
                conditionTriggerState = false;
            }
            // Check if you have to raise the event
            if(mustBeRaised) {
                for(String source : sourceList) {
                    // Set necessary objects
                    this.builder.setEventState(qualifier, source, route, report, containerId);
                    // Set the generation time
                    this.builder.setGenerationTime(generationTime);
                    // Build final state, set it and return it
                    // Set the reception time
                    this.builder.setReceptionTime(newValue != null ? newValue.getReceptionTime() : Instant.now());
                    // Replace the state
                    this.state = this.builder.build(new LongUniqueId(processor.getNextId(EventData.class)));
                    generatedStates.add(this.state);
                }
                // Remember the generation time (needed to check if inhibition is needed)
                this.lastReportedEventTime = generationTime;
            }
        } else {
            // Completely ignore the processing
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Event occurrence not computed for event " + definition.getLocation() + ": event processing is disabled");
            }
        }
        // Finalize entity state and prepare for the returned list of data items
        computeEntityState(generatedStates);
        // Return the list
        return generatedStates;
    }

    private void computeEntityState(List<AbstractDataItem> generatedStates) {
        this.systemEntityBuilder.setAlarmState(AlarmState.NOT_APPLICABLE);
        this.systemEntityBuilder.setStatus(entityStatus);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            generatedStates.add(this.entityState);
        }
    }

    void raiseEvent(String source) {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, String.format("Raising event %s from internal source %s, entity status is %s", path(), source, entityStatus));
        }
        // If the event is enabled, then you can mark it as raised
        if(entityStatus == Status.ENABLED) {
            this.internallyTriggered = true;
            this.internalSource.add(source);
        }
    }

    @Override
    public List<AbstractDataItem> evaluate() {
        return process(null);
    }

    @Override
    public void visit(IProcessingModelVisitor visitor) {
        visitor.onVisit(getState());
    }

    @Override
    public void putCurrentStates(List<AbstractDataItem> items) {
        items.add(getState());
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Severity severity() {
        return this.state == null ? null : this.state.getSeverity();
    }

    @Override
    public String route() {
        return this.state == null ? null : this.state.getRoute();
    }

    @Override
    public String source() {
        return this.state == null ? null : this.state.getSource();
    }

    @Override
    public String type() {
        return this.state == null ? null : this.state.getType();
    }

    @Override
    public String qualifier() {
        return this.state == null ? null : this.state.getQualifier();
    }

    @Override
    public Object report() {
        return this.state == null ? null : this.state.getReport();
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
