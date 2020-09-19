package eu.dariolucia.reatmetric.api.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface subscriber for the scheduler state.
 */
public interface ISchedulerSubscriber extends Remote {

    /**
     * Report the enablement status of the scheduler when it changes.
     *
     * @param enabled true if the scheduler is enabled
     */
    void schedulerEnablementChanged(boolean enabled) throws RemoteException;

}
