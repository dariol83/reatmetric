package eu.dariolucia.reatmetric.api.scheduler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface subscriber for the scheduler state.
 */
public interface ISchedulerSubscriber extends Remote {

    /**
     * Report the enablement status of the scheduler when it changes.
     *
     * @param enabled true if the scheduler is enabled
     * @throws RemoteException in case of remote exception
     */
    void schedulerEnablementChanged(boolean enabled) throws RemoteException;

    /**
     * Report updates in the state of the bots. Upon subscription, this method is called with the current state
     * of all the bots.
     *
     * @param botStates the updated bot states
     * @throws RemoteException in case of remote exception
     */
    void botStateUpdated(List<BotStateData> botStates) throws RemoteException;
}
