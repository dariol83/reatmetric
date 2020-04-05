/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.transport;

/**
 * This interface allows reception of status updates from registered {@link ITransportConnector} objects.
 */
public interface ITransportSubscriber {

    /**
     * Method called when the internal status of the transport connector changed.
     *
     * @param connector the source of the status update
     * @param status the new status
     */
    void status(ITransportConnector connector, TransportStatus status);

}
