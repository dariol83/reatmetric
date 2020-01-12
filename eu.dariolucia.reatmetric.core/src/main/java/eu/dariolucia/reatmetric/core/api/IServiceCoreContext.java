/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.api;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public interface IServiceCoreContext {

    IProcessingModel getProcessingModel();

    IServiceFactory getServiceFactory();

    IRawDataBroker getRawDataBroker();
}
