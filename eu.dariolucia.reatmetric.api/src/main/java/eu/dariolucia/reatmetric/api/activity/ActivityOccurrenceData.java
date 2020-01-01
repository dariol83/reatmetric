/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    // Derived
    private final Instant executionTime;

    // Derived
    private final ActivityOccurrenceState currentState;

    // Derived
    private final Object result;

    public ActivityOccurrenceData(IUniqueId internalId, Instant generationTime, Object[] additionalFields, int externalId, String name, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, List<ActivityOccurrenceReport> progressReports, String route) {
        super(internalId, generationTime, additionalFields);
        this.externalId = externalId;
        this.name = name;
        this.path = path;
        this.type = type;
        this.arguments = arguments;
        this.properties = properties;
        this.progressReports = progressReports;
        this.route = route;
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

    public int getExternalId() {
        return externalId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public SystemEntityPath getPath() {
        return path;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<ActivityOccurrenceReport> getProgressReports() {
        return progressReports;
    }

    public String getRoute() {
        return route;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public ActivityOccurrenceState getCurrentState() {
        return currentState;
    }

    public Object getResult() {
        return result;
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