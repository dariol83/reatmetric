package eu.dariolucia.reatmetric.api.scheduler;

/**
 * The conflict strategy to be adopted when an activity should start according to its scheduling
 * trigger, but it cannot start due to missing resources.
 *
 * If not started, an activity is always probed to start when the latest invocation time is reached.
 * If it cannot start by then, the activity is aborted.
 */
public enum ConflictStrategy {
    /**
     * Wait until the latest invocation time (if present) or indefinitely (if not present).
     * Start as soon as the resources are freed.
     */
    WAIT,
    /**
     * Do not invoke the activity and forget about its execution. The activity is basically skipped.
     */
    DO_NOT_START_AND_FORGET,
    /**
     * Abort ALL activities that are holding up the required resources, and then start the activity.
     */
    ABORT_OTHER_AND_START;
}
