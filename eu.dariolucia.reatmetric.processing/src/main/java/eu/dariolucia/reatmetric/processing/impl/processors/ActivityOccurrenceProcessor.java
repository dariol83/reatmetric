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

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.operations.ActivityOccurrenceUpdateOperation;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityOccurrenceProcessor implements Supplier<ActivityOccurrenceData> {

    private static final Logger LOG = Logger.getLogger(ActivityOccurrenceProcessor.class.getName());

    public static final String SELF_BINDING = "self";

    private final ActivityProcessor parent;
    private final IUniqueId occurrenceId;
    private final Instant creationTime;
    private final Map<String, Object> arguments;
    private final Map<String, String> properties;
    private final List<ActivityOccurrenceReport> reports;
    private final String route;
    private final String source;

    private ActivityOccurrenceState currentState;
    private Instant executionTime;

    private ActivityOccurrenceState currentTimeoutState;
    private Instant currentTimeoutAbsoluteTime;
    private TimerTask currentTimeoutTask;

    private final List<ActivityOccurrenceData> temporaryDataItemList = new ArrayList<>(10);

    private volatile ActivityOccurrenceData lastGeneratedState = null;

    public ActivityOccurrenceProcessor(ActivityProcessor parent, IUniqueId occurrenceId, Instant creationTime, Map<String, Object> arguments, Map<String, String> properties, List<ActivityOccurrenceReport> reports, String route, String source) {
        this.parent = parent;
        this.occurrenceId = occurrenceId;
        this.creationTime = creationTime;
        this.arguments = Collections.unmodifiableMap(arguments);
        this.properties = Collections.unmodifiableMap(properties);
        this.reports = reports;
        this.route = route;
        this.source = source;
    }

    public ActivityOccurrenceProcessor(ActivityProcessor parent, ActivityOccurrenceData occurrenceToRestore) {
        this(parent, occurrenceToRestore.getInternalId(), occurrenceToRestore.getGenerationTime(), occurrenceToRestore.getArguments(), occurrenceToRestore.getProperties(), occurrenceToRestore.getProgressReports(), occurrenceToRestore.getRoute(), occurrenceToRestore.getSource());
        this.currentState = occurrenceToRestore.getCurrentState();
        this.executionTime = occurrenceToRestore.getExecutionTime();
        this.lastGeneratedState = occurrenceToRestore;
    }

    public IUniqueId getOccurrenceId() {
        return occurrenceId;
    }

    /**
     * This method initialises the activity occurrence and forwards it to the selected activity handler.
     *
     * @return the list of state changes at the end of the dispatching
     */
    public List<AbstractDataItem> dispatch() {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("dispatch() - About to dispatch activity occurrence " + getOccurrenceId());
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // Set the initial state and generate the report for the creation of the activity occurrence (start of the lifecycle)
        currentState = ActivityOccurrenceState.CREATION;
        generateReport(ActivityOccurrenceReport.CREATION_REPORT_NAME, creationTime, null, ActivityOccurrenceState.CREATION, ActivityReportState.OK, null, ActivityOccurrenceState.RELEASE);
        // Forward occurrence to the activity handler
        boolean forwardOk = false;
        Instant nextTime = Instant.now();
        try {
            forwardOccurrence();
            forwardOk = true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, String.format("Failure forwarding activity occurrence %s of activity %s to the activity handler on route %s", occurrenceId, parent.getPath(), route), e);
        }
        // Generate ActivityOccurrenceReport and notify activity release, negative, if exception is thrown (FATAL)
        if (!forwardOk) {
            generateReport(ActivityOccurrenceReport.FORWARDING_REPORT_NAME, nextTime, null, ActivityOccurrenceState.RELEASE, ActivityReportState.FATAL, null, ActivityOccurrenceState.COMPLETED);
        }
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    /**
     * This method creates an activity occurrence with the given state. The activity occurrence is not forwarded to the
     * activity handler. This method is used to register activity occurrences that are created externally and not by
     * the Reatmetric system.
     *
     * @param progress the progress information to be used to derive the activity occurrence state
     * @return the list of state changes at the end of the creation
     */
    public List<AbstractDataItem> create(ActivityProgress progress) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("create() - About to create activity occurrence " + getOccurrenceId() + " from progress: " + progress);
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // Set the initial state and generate the report for the creation of the activity occurrence (start of the lifecycle)
        currentState = ActivityOccurrenceState.CREATION;
        generateReport(ActivityOccurrenceReport.CREATION_REPORT_NAME, creationTime, progress.getExecutionTime(), ActivityOccurrenceState.CREATION, ActivityReportState.OK, progress.getResult(), progress.getState());
        List<AbstractDataItem> toReturn = new LinkedList<>(temporaryDataItemList);
        // Process the progress state
        toReturn.addAll(progress(progress));
        // Return list
        return toReturn;
    }

    private void forwardOccurrence() throws ProcessingModelException {
        // Notify pending release
        generateReport(ActivityOccurrenceReport.FORWARDING_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.RELEASE, ActivityReportState.PENDING, null, ActivityOccurrenceState.RELEASE);
        // Forward to the activity handler
        parent.processor.forwardActivityToHandler(occurrenceId, parent.getSystemEntityId(), creationTime, parent.getPath(), parent.getDefinition().getType(), arguments, properties, route, source);
    }

    private void generateReport(String name, Instant generationTime, Instant executionTime, ActivityOccurrenceState announcedState, ActivityReportState reportState, Object result, ActivityOccurrenceState nextState) {
        // Create the report
        ActivityOccurrenceReport report = new ActivityOccurrenceReport(new LongUniqueId(parent.processor.getNextId(ActivityOccurrenceReport.class)), generationTime, null, name, announcedState, executionTime, reportState, nextState, result);
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer("Generating report for activity occurrence " + getOccurrenceId() + ": " + report);
        }
        // Add the report to the list
        reports.add(report);
        // Set the current state: prevent going back
        if (currentState == null || currentState.ordinal() <= nextState.ordinal()) {
            currentState = nextState;
        } else {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning(String.format("Reported progress %s of activity occurrence %s of activity %s has next state set to %s, but current state is %s, current state not updated", name, occurrenceId, parent.getPath(), nextState, currentState));
            }
        }
        // Set the execution time if any
        this.executionTime = executionTime != null ? executionTime : this.executionTime;
        // Generate the ActivityOccurrenceData and add it to the temporary list
        ActivityOccurrenceData activityOccurrenceData = new ActivityOccurrenceData(this.occurrenceId, creationTime, null, parent.getSystemEntityId(), parent.getPath().getLastPathElement(), parent.getPath(), parent.getDefinition().getType(), this.arguments, this.properties, List.copyOf(this.reports), this.route, this.source);
        temporaryDataItemList.add(activityOccurrenceData);
        // If the current state is now COMPLETION, stop the timeout
        if (currentState == ActivityOccurrenceState.COMPLETED) {
            abortTimeout();
        }
        // Set the last generated state
        this.lastGeneratedState = activityOccurrenceData;
    }

    public List<AbstractDataItem> progress(ActivityProgress progress) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("progress() - About to process progress of activity occurrence " + getOccurrenceId() + ": " + progress);
        }
        if (currentState == ActivityOccurrenceState.COMPLETED) {
            // Activity occurrence in its final state, update discarded
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning(String.format("Reported progress of activity occurrence %s of activity %s completed", occurrenceId, parent.getPath()));
            }
            return Collections.emptyList();
        }
        if (progress.getNextState() == ActivityOccurrenceState.COMPLETED) {
            // Progress with COMPLETION as next state is not allowed
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.severe(String.format("Reported progress for activity occurrence %s of activity %s has next state set to COMPLETION, which is not supported. Reported states can be up to VERIFICATION.", occurrenceId, parent.getPath()));
            }
            return Collections.emptyList();
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // Generate ActivityOccurrenceReport according to progress
        ActivityOccurrenceState previousState = currentState;
        // A FATAL blocks the activity occurrence tracking
        ActivityOccurrenceState nextState = progress.getStatus() == ActivityReportState.FATAL ? ActivityOccurrenceState.COMPLETED : progress.getNextState();
        generateReport(progress.getName(), progress.getGenerationTime(), progress.getExecutionTime(), progress.getState(), progress.getStatus(), progress.getResult(), nextState);

        // Enable timeout, if the situation is appropriate
        if (previousState != ActivityOccurrenceState.TRANSMISSION && currentState == ActivityOccurrenceState.TRANSMISSION) {
            // If progress triggers the transition to TRANSMISSION, start the TRANSMISSION timeout if specified
            if (parent.getDefinition().getTransmissionTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.TRANSMISSION, parent.getDefinition().getTransmissionTimeout());
            }
        } else if (previousState != ActivityOccurrenceState.SCHEDULING && currentState == ActivityOccurrenceState.SCHEDULING) {
            // Stop transmission timeout
            stopTimeout(ActivityOccurrenceState.TRANSMISSION);
        } else if (previousState != ActivityOccurrenceState.EXECUTION && currentState == ActivityOccurrenceState.EXECUTION) {
            // Stop transmission timeout
            stopTimeout(ActivityOccurrenceState.TRANSMISSION);
            // If progress triggers the transition to EXECUTION, start the EXECUTION timeout if specified
            if (parent.getDefinition().getExecutionTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.EXECUTION, parent.getDefinition().getExecutionTimeout());
            }
        } else if (previousState != ActivityOccurrenceState.VERIFICATION && currentState == ActivityOccurrenceState.VERIFICATION) {
            // Stop execution timeout
            stopTimeout(ActivityOccurrenceState.EXECUTION);
            // If progress triggers the transition to VERIFICATION, start the VERIFICATION timeout if specified and if an expression is defined
            if (parent.getDefinition().getVerification() != null && parent.getDefinition().getVerificationTimeout() > 0) {
                startTimeout(ActivityOccurrenceState.VERIFICATION, parent.getDefinition().getVerificationTimeout());
            }
            // If an expression is specified, run the expression now as verification (if in the correct state)
            if (parent.getDefinition().getVerification() != null) {
                try {
                    Boolean verificationResult = (Boolean) parent.getDefinition().getVerification().execute(parent.processor, Map.of(SELF_BINDING, this), ValueTypeEnum.BOOLEAN);
                    if (verificationResult) {
                        if (LOG.isLoggable(Level.INFO)) {
                            LOG.log(Level.INFO, String.format("Verification of activity occurrence %s of activity %s completed", occurrenceId, parent.getPath()));
                        }
                        // Activity occurrence confirmed by parameter data, add report with OK state and move state to COMPLETION
                        generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.OK, null, ActivityOccurrenceState.COMPLETED);
                    } else if (parent.getDefinition().getVerificationTimeout() == 0) {
                        if (LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, String.format("Verification of activity occurrence %s of activity %s failed", occurrenceId, parent.getPath()));
                        }
                        // No timeout, so derive the final state now
                        generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.FAIL, null, ActivityOccurrenceState.COMPLETED);
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, String.format("Verification of activity occurrence %s of activity %s pending", occurrenceId, parent.getPath()));
                        }
                        // Expression not OK but there is a timeout, announce the PENDING
                        generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.PENDING, null, ActivityOccurrenceState.VERIFICATION);
                    }
                } catch (ScriptException | ClassCastException e) {
                    // Expression has a radical error
                    LOG.log(Level.SEVERE, String.format("Error while evaluating verification expression of activity occurrence %s of activity %s: %s", occurrenceId, parent.getPath(), e.getMessage()), e);
                    generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.ERROR, null, ActivityOccurrenceState.COMPLETED);
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, String.format("Verification of activity occurrence %s of activity %s completed: no expression defined", occurrenceId, parent.getPath()));
                }
                // If no expression is defined, move currentState to COMPLETION
                generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.OK, null, ActivityOccurrenceState.COMPLETED);
            }
        }
        // Verify timeout completions: this can generate an additional ActivityOccurrenceData object
        verifyTimeout();
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Returning list after processing progress: " + progress);
            for(ActivityOccurrenceData aod : temporaryDataItemList) {
                LOG.finer("Last report for element is: " + aod.getProgressReports().get(aod.getProgressReports().size() - 1));
            }
        }
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    private void abortTimeout() {
        if (this.currentTimeoutTask != null) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(String.format("Cancelling timeout timer for state %s on activity occurrence %s of activity %s", currentTimeoutState, occurrenceId, parent.getPath()));
            }
            this.currentTimeoutTask.cancel();
            this.currentTimeoutTask = null;
            this.currentTimeoutState = null;
            this.currentTimeoutAbsoluteTime = null;
        }
    }

    private void stopTimeout(ActivityOccurrenceState theState) {
        if (currentTimeoutState == theState) {
            abortTimeout();
        }
    }

    private void startTimeout(ActivityOccurrenceState theState, int transmissionTimeout) {
        // If there is another timeout set, stop it
        abortTimeout();
        // Schedule operation to re-evaluate this occurrence at a given time
        this.currentTimeoutAbsoluteTime = Instant.now().plusSeconds(transmissionTimeout);
        this.currentTimeoutState = theState;
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(String.format("Starting timeout timer for state %s on activity occurrence %s of activity %s with timeout %s", theState, occurrenceId, parent.getPath(), this.currentTimeoutAbsoluteTime));
        }
        this.currentTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                if (currentTimeoutTask == this) {
                    parent.processor.scheduleTask(Collections.singletonList(new ActivityOccurrenceUpdateOperation(parent.getSystemEntityId(), occurrenceId)), ProcessingModelImpl.USER_DISPATCHING_QUEUE);
                }
            }
        };
        this.parent.processor.scheduleAt(this.currentTimeoutAbsoluteTime, this.currentTimeoutTask);
    }

    private boolean verifyTimeout() {
        if (currentState == ActivityOccurrenceState.COMPLETED) {
            // Stop the clock
            abortTimeout();
            return false;
        }
        // Check if the current timeout is applicable to the current state and if it is in timeout. If it is the case,
        // then generates a report and stop the timer
        if (this.currentTimeoutState == this.currentState) {
            Instant toCheck = Instant.now();
            if (this.currentTimeoutTask != null && (toCheck.equals(this.currentTimeoutAbsoluteTime) || toCheck.isAfter(this.currentTimeoutAbsoluteTime))) {
                generateReport(currentTimeoutState.getFormatString() + " Timeout", Instant.now(), null, this.currentState, ActivityReportState.TIMEOUT, null, this.currentState);
                abortTimeout();
                return true;
            }
        }
        return false;
    }

    public List<AbstractDataItem> purge() {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("purge() - About to purge activity occurrence " + getOccurrenceId());
        }
        if (currentState == ActivityOccurrenceState.COMPLETED) {
            // Activity occurrence in its final state, purge discarded
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning(String.format("Purge request for activity occurrence %s of activity %s discarded, activity occurrence already completed", occurrenceId, parent.getPath()));
            }
            return Collections.emptyList();
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // Abort timeout
        abortTimeout();
        // Move to COMPLETION state
        generateReport(ActivityOccurrenceReport.PURGE_REPORT_NAME, Instant.now(), null, this.currentState, ActivityReportState.OK, null, ActivityOccurrenceState.COMPLETED);
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    public void abort()  {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("abort() - About to abort activity occurrence " + getOccurrenceId());
        }
        if (currentState == ActivityOccurrenceState.COMPLETED) {
            // Activity occurrence in its final state, abort discarded
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning(String.format("Abort request for activity occurrence %s of activity %s discarded, activity occurrence already completed", occurrenceId, parent.getPath()));
            }
        }
        // Forward to the activity handler
        try {
            parent.processor.forwardAbortToHandler(occurrenceId, parent.getSystemEntityId(), route, parent.getDefinition().getType());
        } catch (ProcessingModelException e) {
            LOG.log(Level.SEVERE, String.format("Abort request for activity occurrence %s:%d cannot be forwarded to the activity handler identified by route %s and type %s: %s", parent.getPath(), occurrenceId.asLong(), route, parent.getDefinition().getType(), e.getMessage()));
        }
    }

    public List<AbstractDataItem> evaluate() {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("evaluate() - About to re-evaluate activity occurrence " + getOccurrenceId());
        }
        // Clear temporary list
        temporaryDataItemList.clear();
        // If currentTimeoutState is applicable, currentTimeoutTask is pending and it is expired, generate ActivityOccurrenceReport accordingly
        boolean expired = verifyTimeout();
        // If currentState is VERIFICATION, check expression: if OK, then announce ActivityOccurrenceReport OK and move to COMPLETION
        if (currentState == ActivityOccurrenceState.VERIFICATION) {
            try {
                Boolean verificationResult = (Boolean) parent.getDefinition().getVerification().execute(parent.processor, Map.of(SELF_BINDING, this), ValueTypeEnum.BOOLEAN);
                if (verificationResult) {
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.log(Level.INFO, String.format("Verification of activity occurrence %s of activity %s completed", occurrenceId, parent.getPath()));
                    }
                    // Activity occurrence confirmed by parameter data, add report with OK state and move state to COMPLETION
                    generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.OK, null, ActivityOccurrenceState.COMPLETED);
                } else if (expired) {
                    if (LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, String.format("Verification of activity occurrence %s of activity %s failed", occurrenceId, parent.getPath()));
                    }
                    // No timeout, so derive the final state now
                    generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.FAIL, null, ActivityOccurrenceState.COMPLETED);
                }
            } catch (ScriptException | ClassCastException e) {
                // Expression has a radical error
                LOG.log(Level.SEVERE, String.format("Error while evaluating verification expression of activity occurrence %s of activity %s: %s", occurrenceId, parent.getPath(), e.getMessage()), e);
                generateReport(ActivityOccurrenceReport.VERIFICATION_REPORT_NAME, Instant.now(), null, ActivityOccurrenceState.VERIFICATION, ActivityReportState.ERROR, null, ActivityOccurrenceState.COMPLETED);
            }
        }
        // Return list
        return List.copyOf(temporaryDataItemList);
    }

    public Set<String> arguments() {
        return this.arguments.keySet();
    }

    public Object argument(String argName) {
        return this.arguments.get(argName);
    }

    public Set<String> properties() {
        return this.properties.keySet();
    }

    public String property(String argName) {
        return this.properties.get(argName);
    }

    public String route() {
        return this.route;
    }

    public String source() {
        return this.source;
    }

    public Instant creationTime() {
        return creationTime;
    }

    public Instant executionTime() {
        return executionTime;
    }

    @Override
    public ActivityOccurrenceData get() {
        return lastGeneratedState;
    }

    public void visit(IProcessingModelVisitor visitor) {
        visitor.onVisit(lastGeneratedState);
    }
}
