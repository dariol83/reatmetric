/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.plugin;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.SystemStatus;

/**
 *
 * @author dario
 */
public interface IReatmetricServiceListener {

    void startGlobalOperationProgress();

    void stopGlobalOperationProgress();

    void systemConnected(IReatmetricSystem system);
    
    void systemDisconnected(IReatmetricSystem system);

    void systemStatusUpdate(SystemStatus status);
}
