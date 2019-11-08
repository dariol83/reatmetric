/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.transport;

import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.reatmetric.core.common.CommandRequestStatus;

import java.time.Instant;

public interface ITransportProcessorListener {

    void goodTmFrameReceived(byte[] goodFrame, Instant ert, byte[] annotations);

    void badTmFrameReceived(byte[] badFrame);

    void clcwReceived(Clcw clcw);

    void tcFrameStatus(long tcId, CommandRequestStatus status, String error);

}
