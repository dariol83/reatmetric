/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;

public final class ActivityOccurrenceReport extends AbstractDataItem {

    private final String name;
    private final ActivityOccurrenceState state;
    private final Instant executionTime; // If not null, this report provides the activity occurrence execution time (estimated or final)
    private final ActivityReportState status;
    private final Object result; // If not null, this report provides the activity occurrence execution result (partial or final)

    public ActivityOccurrenceReport(IUniqueId internalId, Instant generationTime, Object[] additionalFields, String name, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, Object result) {
        super(internalId, generationTime, additionalFields);
        this.name = name;
        this.state = state;
        this.executionTime = executionTime;
        this.status = status;
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public ActivityOccurrenceState getState() {
        return state;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public ActivityReportState getStatus() {
        return status;
    }

    public Object getResult() {
        return result;
    }
}
