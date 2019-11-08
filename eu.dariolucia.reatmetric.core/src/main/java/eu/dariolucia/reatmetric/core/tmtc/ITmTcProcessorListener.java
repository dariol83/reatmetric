/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc;

import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.core.common.CommandRequestStatus;

import java.util.List;

public interface ITmTcProcessorListener {

    void packetDecoded(SpacePacket packet, List<ParameterValue> values);

    void tcRequestBuilt(long tcId, SpacePacket tcPacket, String error);

    void tcRequestStatus(long tcId, CommandRequestStatus status, String error);

}
