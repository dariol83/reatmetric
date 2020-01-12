/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.driver.spacecraft.common.CommandRequestStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.message.IMessageProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.storage.impl.StorageProcessor;

import java.time.Instant;
import java.util.Map;

public interface ITmTcProcessor extends IRawDataProvisionService {

    void setLogger(IMessageProcessor logger);

    void setStorer(StorageProcessor storer);

    void setListener(ITmTcProcessorListener listener);

    void sendTcRequest(long tcId, String definition, Map<String, Object> values);

    void onFrameReceived(byte[] goodFrame, boolean quality, Instant ert, byte[] annotations);

    void onClcwReceived(Clcw clcw);

    void onTcFrameStatusUpdate(long tcId, CommandRequestStatus status, String error);

}
