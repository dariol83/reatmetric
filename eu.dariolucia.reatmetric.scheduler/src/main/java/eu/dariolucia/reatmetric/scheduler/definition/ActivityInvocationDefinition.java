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

package eu.dariolucia.reatmetric.scheduler.definition;

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.AbstractActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.AbsoluteTimeSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.ConflictStrategy;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.IInternalResolver;
import eu.dariolucia.reatmetric.scheduler.ScheduledTask;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@XmlAccessorType(XmlAccessType.FIELD)
public class ActivityInvocationDefinition implements Serializable {

    private static final AtomicLong SEQUENCER = new AtomicLong(0);

    @XmlAttribute(name="activity", required = true)
    private String activity;

    @XmlAttribute(name="route")
    private String route;

    @XmlAttribute(name="max-invocation-time")
    private int maxInvocationTime; // In seconds

    @XmlAttribute(name="conflict-strategy")
    private ConflictStrategy conflictStrategy = ConflictStrategy.WAIT;

    @XmlElements({
            @XmlElement(name="fixed-argument",type= PlainArgumentInvocationDefinition.class),
            @XmlElement(name="fixed-array",type= ArrayArgumentInvocationDefinition.class)
    })
    private List<AbstractArgumentInvocationDefinition> arguments = new LinkedList<>();

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    @XmlElementWrapper(name = "resources")
    @XmlElement(name="resource")
    private List<String> resources = new LinkedList<>();

    public ConflictStrategy getConflictStrategy() {
        return conflictStrategy;
    }

    public void setConflictStrategy(ConflictStrategy conflictStrategy) {
        this.conflictStrategy = conflictStrategy;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public List<AbstractArgumentInvocationDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<AbstractArgumentInvocationDefinition> arguments) {
        this.arguments = arguments;
    }

    public List<KeyValue> getProperties() {
        return properties;
    }

    public void setProperties(List<KeyValue> properties) {
        this.properties = properties;
    }

    public int getMaxInvocationTime() {
        return maxInvocationTime;
    }

    public void setMaxInvocationTime(int maxInvocationTime) {
        this.maxInvocationTime = maxInvocationTime;
    }

    /*
     * Transient objects
     */

    private transient Map<String, String> propertiesMap;

    private transient List<AbstractActivityArgument> argumentsList;

    public SchedulingRequest build(IInternalResolver resolver, String source) {
        Instant now = Instant.now();
        ActivityDescriptor descriptor = resolver.resolveDescriptor(activity);
        if(descriptor != null) {
            ActivityRequest ar = new ActivityRequest(descriptor.getExternalId(), descriptor.getPath(), getArgumentsList(), getPropertiesMap(), route, source);
            return new SchedulingRequest(ar, new HashSet<>(getResources()), source, source + "-" + SEQUENCER.incrementAndGet(), new AbsoluteTimeSchedulingTrigger(now), now.plusSeconds(maxInvocationTime), getConflictStrategy(), descriptor.getExpectedDuration());
        } else {
            return null;
        }
    }

    private List<AbstractActivityArgument> getArgumentsList() {
        if(argumentsList == null) {
            argumentsList = new LinkedList<>();
            for(AbstractArgumentInvocationDefinition aaid : arguments) {
                argumentsList.add(aaid.build());
            }
        }
        return argumentsList;
    }

    public Map<String, String> getPropertiesMap() {
        if(propertiesMap == null) {
            propertiesMap = new LinkedHashMap<>();
            for(KeyValue pair : properties) {
                propertiesMap.put(pair.getKey(), pair.getValue());
            }
        }
        return propertiesMap;
    }
}
