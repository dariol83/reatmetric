/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.transport;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.reatmetric.driver.spacecraft.message.IMessageProcessor;

public interface ITransportProcessor {

    void openLinks();

    void closeLinks();

    void sendTcFrame(long tcId, TcTransferFrame frame);

    void setListener(ITransportProcessorListener listener);

    void setLogger(IMessageProcessor logger);
}
