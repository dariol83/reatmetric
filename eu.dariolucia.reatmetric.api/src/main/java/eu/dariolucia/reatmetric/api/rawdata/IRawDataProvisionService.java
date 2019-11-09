/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.IDataItemProvisionService;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

/**
 *
 * @author dario
 */
public interface IRawDataProvisionService extends IDataItemProvisionService<IRawDataSubscriber, RawDataFilter, RawData> {

    public byte[] getRawDataContents(RawData data) throws ReatmetricException;
    
}
