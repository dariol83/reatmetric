package eu.dariolucia.reatmetric.api.scheduler;

/**
 * The scheduling state of a scheduled activity invocation.
 */
public enum SchedulingState {
    /**
     * The activity is scheduled and waiting for its trigger condition to arrive.
     */
    SCHEDULED,
    /**
     * The activity has been triggered but it is waiting for the necessary resources to become available.
     */
    WAITING,
    /**
     * The activity has been triggered but it is ignored to missing resources.
     */
    IGNORED,
    /**
     * The activity was triggered and the activity occurrence was dispatched for invocation.
     */
    RUNNING,
    /**
     * The activity occurrence was aborted by the user or by another activity.
     */
    ABORTED,
    /**
     * The activity occurrence was completed successfully (status is COMPLETED-OK)
     */
    FINISHED_NOMINAL,
    /**
     * The activity occurrence was completed unsuccessfully (status is COMPLETED-!OK)
     */
    FINISHED_FAIL,
    /**
     * The activity has been removed from the scheduler when it was in SCHEDULED state.
     */
    REMOVED;
}
