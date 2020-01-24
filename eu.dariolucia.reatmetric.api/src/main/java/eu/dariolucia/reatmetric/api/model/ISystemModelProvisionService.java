/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;

import java.util.List;

/**
 *
 * @author dario
 */
public interface ISystemModelProvisionService {

    void subscribe(ISystemModelSubscriber subscriber);
    
    void unsubscribe(ISystemModelSubscriber subscriber);
    
    SystemEntity getRoot() throws ReatmetricException;
    
    List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ReatmetricException;
    
    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException;

    SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException;

    int getExternalIdOf(SystemEntityPath path) throws ReatmetricException;

    SystemEntityPath getPathOf(int externalId) throws ReatmetricException;

}
