package eu.dariolucia.reatmetric.processing.input;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;

public final class ActivityProgress extends AbstractInputDataItem {

    public static ActivityProgress of(IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(occurrenceId, name, generationTime, state, null, status, false, null);
    }

    public static ActivityProgress of(IUniqueId occurrenceId, String name, Instant generationTime, Instant executionTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(occurrenceId, name, generationTime, state, executionTime, status, false, null);
    }

    public static ActivityProgress of(IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, boolean stateTransition, Object result) {
        return new ActivityProgress(occurrenceId, name, generationTime, state, executionTime, status, stateTransition, result);
    }

    private final IUniqueId occurrenceId;
    private final String name;
    private final Instant generationTime;
    private final ActivityOccurrenceState state;
    private final Instant executionTime; // If not null, this report provides the activity occurrence execution time (estimated or final)
    private final ActivityReportState status;
    private final boolean stateTransition;
    private final Object result; // If not null, this report provides the activity occurrence execution result (partial or final)

    private ActivityProgress(IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, boolean stateTransition, Object result) {
        this.occurrenceId = occurrenceId;
        this.name = name;
        this.generationTime = generationTime;
        this.state = state;
        this.executionTime = executionTime;
        this.status = status;
        this.stateTransition = stateTransition;
        this.result = result;
    }

    public IUniqueId getOccurrenceId() {
        return occurrenceId;
    }

    public String getName() {
        return name;
    }

    public Instant getGenerationTime() {
        return generationTime;
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

    public boolean isStateTransition() {
        return stateTransition;
    }

    public Object getResult() {
        return result;
    }
}
