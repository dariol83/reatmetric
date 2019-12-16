/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;

import java.time.Instant;
import java.util.*;

public class ActivityOccurrenceProcessor {

    private final ActivityProcessor parent;
    private final IUniqueId occurrenceId;
    private final Instant creationTime;
    private final Map<String, Object> arguments;
    private final Map<String, String> properties;
    private final List<ActivityOccurrenceReport> reports;
    private final String route;

    private ActivityOccurrenceState currentState;

    private ActivityOccurrenceState currentTimeoutState;
    private Instant currentTimeoutAbsoluteTime;
    private TimerTask currentTimeoutTask;

    private List<ActivityOccurrenceData> temporaryDataItemList = new ArrayList<>(10);

    public ActivityOccurrenceProcessor(ActivityProcessor parent, IUniqueId occurrenceId, Instant creationTime, Map<String, Object> arguments, Map<String, String> properties, List<ActivityOccurrenceReport> reports, String route) {
        this.parent = parent;
        this.occurrenceId = occurrenceId;
        this.creationTime = creationTime;
        this.arguments = arguments;
        this.properties = properties;
        this.reports = reports;
        this.route = route;
    }

    public List<ActivityOccurrenceData> dispatch() {
        // Clear temporary list
        temporaryDataItemList.clear();
        // Generate ActivityOccurrenceReport and notify activity creation
        currentState = ActivityOccurrenceState.CREATION;
        generateReport("Creation", null, ActivityReportState.OK, null, ActivityOccurrenceState.RELEASE);
        // Forward occurrence to the activity handler
        currentState = ActivityOccurrenceState.RELEASE;
        boolean forwardOk = false;
        try {
            forwardOccurrence();
            forwardOk = true;
        } catch(Exception e) {
            // TODO: log
        }
        // Generate ActivityOccurrenceReport and notify activity release: positive or negative if exception is thrown (FATAL)
        if(forwardOk) {
            generateReport("Release to Activity Handler", null, ActivityReportState.OK, null, ActivityOccurrenceState.RELEASE);
        } else {
            generateReport("Release to Activity Handler", null, ActivityReportState.FATAL, null, ActivityOccurrenceState.COMPLETION);
        }
        // Return list
    }

    public List<ActivityOccurrenceData> progress(ActivityProgress progress) {
        // Clear temporary list
        temporaryDataItemList.clear();
        // Generate ActivityOccurrenceReport according to progress

        // If progress triggers the transition to TRANSMISSION, start the TRANSMISSION timeout if specified

        // If progress triggers the transition to EXECUTION, start the EXECUTION timeout if specified

        // If progress triggers the transition to VERIFICATION, start the VERIFICATION timeout if specified if an expression is defined.
        // If no expression is defined, move currentState to COMPLETION

        // Return list
    }

    public List<ActivityOccurrenceData> evalute() {
        // Clear temporary list
        temporaryDataItemList.clear();
        // If currentTimeoutState is applicable, currentTimeoutTask is pending and it is expired, generate ActivityOccurrenceReport accordingly

        // If currentState is VERIFICATION, check expression: if OK, then announce ActivityOccurrenceReport OK and move to COMPLETION

        // Return list
    }
}
