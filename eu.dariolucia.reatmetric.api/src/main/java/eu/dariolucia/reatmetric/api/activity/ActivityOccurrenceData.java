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

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.*;

/**
 * An instance of this class encapsulates the state of an activity occurrence at a given point in time. An activity
 * occurrence is an instance of an 'activity', defined as an abstract operation in the system entity model. Once an
 * activity is requested, an activity occurrence is instantiated and follows a defined activity lifecycle, going through
 * the states defined in the {@link ActivityOccurrenceState}.
 *
 * Each activity state is split in one or more so called verification stages. For each stage, updates to the stage state
 * are reported by means of progress reports ({@link ActivityOccurrenceReport}, which define also the transition from
 * one {@link ActivityOccurrenceState} to the next state.
 *
 * Objects of this class are immutable.
 */
public final class ActivityOccurrenceData extends AbstractDataItem {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final int externalId;

    private final String name;

    private final SystemEntityPath path;

    private final String type;

    private final Map<String, Object> arguments;

    private final Map<String, String> properties;

    private final List<ActivityOccurrenceReport> progressReports;

    private final String route;

    private final String source;

    // Derived
    private final Instant executionTime;

    // Derived
    private final ActivityOccurrenceState currentState;

    // Derived
    private final Object result;

    /**
     * Constructor of the activity occurrence.
     *
     * @param internalId the internal ID of this object
     * @param generationTime the generation time of the activity occurrence
     * @param extension the extension object
     * @param externalId the ID of the activity entity in the system entity model
     * @param name the name of the activity
     * @param path the path of the activity in the system entity model
     * @param type the type of the activity
     * @param arguments the activity argument map
     * @param properties the activity property map
     * @param progressReports the list of progress reports
     * @param route the route followed by the activity occurrence upon its execution
     * @param source the entity that generated the activity occurrence
     */
    public ActivityOccurrenceData(IUniqueId internalId, Instant generationTime, Object extension, int externalId, String name, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, List<ActivityOccurrenceReport> progressReports, String route, String source) {
        super(internalId, generationTime, extension);
        if(type == null) {
            throw new NullPointerException("Type cannot be null");
        }
        this.externalId = externalId;
        this.name = name;
        this.path = path;
        this.type = type;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.progressReports = List.copyOf(progressReports);
        this.route = route;
        this.source = source;
        // Compute derived fields
        Instant derExecutionTime = null;
        ActivityOccurrenceState derState = ActivityOccurrenceState.CREATION;
        Object derResult = null;
        for(ActivityOccurrenceReport report : progressReports) {
            derExecutionTime = report.getExecutionTime() != null ? report.getExecutionTime() : derExecutionTime;
            derState = report.getStateTransition();
            derResult = report.getResult() != null ? report.getResult() : derResult;
        }
        executionTime = derExecutionTime;
        currentState = derState;
        result = derResult;
    }

    /**
     * The external ID of the activity: it identifies the activity in the system entity model, regardless
     * of its location in the tree.
     *
     * @return the external ID
     */
    public int getExternalId() {
        return externalId;
    }

    /**
     * The type of the activity: this information is typically needed by the processing layer to identify the
     * correct type of processing for the activity request.
     *
     * @return the activity type
     */
    public String getType() {
        return type;
    }

    /**
     * The activity name, i.e. the last part of the system entity path of the related activity.
     *
     * @return the activity name
     */
    public String getName() {
        return name;
    }

    /**
     * The activity path in the system entity model.
     *
     * @return the activity path
     */
    public SystemEntityPath getPath() {
        return path;
    }

    /**
     * The activity occurrence arguments.
     *
     * @return the arguments of the activity occurrence
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * The activity occurrence properties.
     *
     * @return the properties of the activity occurrence
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * The list of progress reports linked to the activity occurrence release, transmission and execution lifecycle.
     * Unless differently specified, the order of the progress reports is the one defined by the processing model.
     *
     * @return the list of progress reports
     */
    public List<ActivityOccurrenceReport> getProgressReports() {
        return progressReports;
    }

    /**
     * The route of the activity occurrence. It can be null.
     *
     * @return the route of the activity occurrence
     */
    public String getRoute() {
        return route;
    }

    /**
     * The activity occurrence execution time. It can be null if not known.
     *
     * @return the activity occurrence execution time
     */
    public Instant getExecutionTime() {
        return executionTime;
    }

    /**
     * The state of the activity occurrence, according to the defined lifecycle in ReatMetric, as specified in the
     * {@link ActivityOccurrenceState} enumeration documentation.
     *
     * @return the activity occurrence state
     */
    public ActivityOccurrenceState getCurrentState() {
        return currentState;
    }

    /**
     * The result as reported by the execution of this activity occurrence. The result is meaningful only if the
     * activity occurrence lifecycle is complete.
     *
     * @return the result of the activity occurrence
     */
    public Object getResult() {
        return result;
    }

    /**
     * The entity that created the activity occurrence.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ActivityOccurrenceData{" +
                "externalId=" + externalId +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", type='" + type + '\'' +
                ", arguments=" + arguments +
                ", properties=" + properties +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                ", executionTime=" + executionTime +
                ", currentState=" + currentState +
                ", result=" + result +
                "} " + super.toString());
        if(!progressReports.isEmpty()) {
            sb.append("\n");
            for(ActivityOccurrenceReport aor : progressReports) {
                sb.append("\t").append(aor).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
