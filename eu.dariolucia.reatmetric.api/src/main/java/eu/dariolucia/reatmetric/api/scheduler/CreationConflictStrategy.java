package eu.dariolucia.reatmetric.api.scheduler;

public enum CreationConflictStrategy {
    /**
     * Abort the operation if there is a resource conflict: the schedule is unmodified
     */
    ABORT,
    /**
     * Do not add the new activity if a conflict exists
     */
    SKIP_NEW,
    /**
     * Remove the conflicting scheduled items before adding the new activity
     */
    REMOVE_PREVIOUS,
    /**
     * Add the activity anyway, with the risk of having problems later
     */
    ADD_ANYWAY
}
