/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.message;

import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.driver.spacecraft.storage.impl.StorageProcessor;

public interface IMessageProcessor extends IOperationalMessageProvisionService {

    void raiseMessage(String message, String source, Severity severity);

    void setStorer(StorageProcessor storer);
}
