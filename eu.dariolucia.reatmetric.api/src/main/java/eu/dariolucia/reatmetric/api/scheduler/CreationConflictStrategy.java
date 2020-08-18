package eu.dariolucia.reatmetric.api.scheduler;

public enum CreationConflictStrategy {
    /**
     * Abort the complete operation if there is a resource conflict: the schedule is unmodified
     */
    ABORT,
    /**
     * Do not add the new activity if a resource conflict exists
     */
    SKIP_NEW,
    /**
     * Remove the resource-conflicting scheduled items before adding the new activity
     */
    REMOVE_PREVIOUS,
    /**
     * Add the activity anyway, with the risk of having problems later
     */
    ADD_ANYWAY
}
