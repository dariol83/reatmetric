/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import java.util.List;

/**
 *
 * @author dario
 */
public interface ISystemModelProvisionService {

    void subscribe(ISystemModelSubscriber subscriber);
    
    void unsubscribe(ISystemModelSubscriber subscriber);
    
    SystemEntity getRoot();
    
    List<SystemEntity> getContainedEntities(SystemEntityPath se);
    
    SystemEntity getSystemEntityAt(SystemEntityPath path);

    SystemEntity getSystemEntityOf(int externalId);

    int getExternalIdOf(SystemEntityPath path);

    SystemEntityPath getPathOf(int externalId);

}
