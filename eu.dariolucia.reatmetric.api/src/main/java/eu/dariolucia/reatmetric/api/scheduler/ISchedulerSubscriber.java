package eu.dariolucia.reatmetric.api.scheduler;

/**
 * Interface subscriber for the scheduler state.
 */
public interface ISchedulerSubscriber {

    /**
     * Report the enablement status of the scheduler when it changes.
     *
     * @param enabled true if the scheduler is enabled
     */
    void schedulerEnablementChanged(boolean enabled);

}
