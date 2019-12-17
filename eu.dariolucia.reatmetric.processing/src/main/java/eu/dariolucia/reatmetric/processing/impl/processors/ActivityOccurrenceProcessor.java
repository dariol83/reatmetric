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
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;

import javax.script.ScriptException;
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
        this.arguments = Collections.unmodifiableMap(arguments);
        this.properties = Collections.unmodifiableMap(properties);
        this.reports = reports;
        this.route = route;
    }

    public List<ActivityOccurrenceData> dispatch() {
        // Clear temporary list
        temporaryDataItemList.clear();
        // Set the initial state and generate the report for the creation of the activity occurrence (start of the lifecycle)
        currentState = ActivityOccurrenceState.CREATION;
        generateReport("Creation", creationTime,null, ActivityReportState.OK, null, ActivityOccurrenceState.RELEASE); // TODO: use constant
        // Forward occurrence to the activity handler
        boolean forwardOk = false;
        Instant nextTime = Instant.now();
        try {
            forwardOccurrence();
            forwardOk = true;
        } catch(Exception e) {
            // TODO: log
        }
        // Generate ActivityOccurrenceReport and notify activity release: positive or negative if exception is thrown (FATAL)
        if(forwardOk) {
            generateReport("Release to Activity Handler", nextTime,null, ActivityReportState.OK, null, ActivityOccurrenceState.RELEASE); // TODO: use constant
        } else {
            generateReport("Release to Activity Handler", nextTime,null, ActivityReportState.FATAL, null, ActivityOccurrenceState.COMPLETION); // TODO: use constant
        }
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    private void generateReport(String name, Instant generationTime, Instant executionTime, ActivityReportState reportState, Object result, ActivityOccurrenceState nextState) {
        // Create the report
        ActivityOccurrenceReport report = new ActivityOccurrenceReport(parent.nextReportId(), generationTime, null, name, reportState, executionTime, reportState, nextState, result);
        // Add the report to the list
        reports.add(report);
        // Set the current state
        currentState = nextState;
        // Generate the ActivityOccurrenceData and add it to the temporary list
        ActivityOccurrenceData activityOccurrenceData = new ActivityOccurrenceData(this.occurrenceId, creationTime, null, parent.getSystemEntityId(), parent.getPath().getLastPathElement(), parent.getPath(), parent.getDefinition().getType(), this.arguments, this.properties, List.copyOf(this.reports), this.route);
        temporaryDataItemList.add(activityOccurrenceData);
    }

    public List<ActivityOccurrenceData> progress(ActivityProgress progress) {
        if(currentState == ActivityOccurrenceState.COMPLETION) {
            // Activity occurrence in its final state, update discarded
            // TODO: log
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // Generate ActivityOccurrenceReport according to progress
        ActivityOccurrenceState previousState = currentState;
        generateReport(progress.getName(), progress.getGenerationTime(), progress.getExecutionTime(), progress.getStatus(), progress.getResult(), progress.getNextState());
        // Verify timeout completions: this can generate an additional ActivityOccurrenceData object
        verifyTimeout();
        // Enable timeout, if the situation is appropriate
        if(previousState != ActivityOccurrenceState.TRANSMISSION && currentState == ActivityOccurrenceState.TRANSMISSION) {
            // If progress triggers the transition to TRANSMISSION, start the TRANSMISSION timeout if specified
            if(parent.getDefinition().getTransmissionTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.TRANSMISSION, parent.getDefinition().getTransmissionTimeout());
            }
        } else if(previousState != ActivityOccurrenceState.EXECUTION && currentState == ActivityOccurrenceState.EXECUTION) {
            // If progress triggers the transition to EXECUTION, start the EXECUTION timeout if specified
            if(parent.getDefinition().getExecutionTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.EXECUTION, parent.getDefinition().getExecutionTimeout());
            }
        } else if(previousState != ActivityOccurrenceState.VERIFICATION && currentState == ActivityOccurrenceState.VERIFICATION) {
            // If progress triggers the transition to VERIFICATION, start the VERIFICATION timeout if specified and if an expression is defined
            if(parent.getDefinition().getVerification() != null && parent.getDefinition().getVerificationTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.VERIFICATION, parent.getDefinition().getVerificationTimeout());
            }
        }
        // If an expression is specified, run the expression now as verification (if in the correct state)
        if(currentState == ActivityOccurrenceState.VERIFICATION && parent.getDefinition().getVerification() != null) {
            try {
                Boolean verificationResult = (Boolean) parent.getDefinition().getVerification().execute(parent.processor, Map.of("self", this)); // TODO: use constant
            } catch (ScriptException|ClassCastException e) {
                // TODO: log and add report with ERROR state, move state to COMPLETION.
            }
        } else {
            // If no expression is defined, move currentState to COMPLETION
            generateReport("Completion", progress.getGenerationTime(), null, ActivityReportState.OK, null, ActivityOccurrenceState.COMPLETION);
        }
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    public List<ActivityOccurrenceData> evalute() {
        // Clear temporary list
        temporaryDataItemList.clear();
        // If currentTimeoutState is applicable, currentTimeoutTask is pending and it is expired, generate ActivityOccurrenceReport accordingly

        // If currentState is VERIFICATION, check expression: if OK, then announce ActivityOccurrenceReport OK and move to COMPLETION

        // Return list
    }
}
