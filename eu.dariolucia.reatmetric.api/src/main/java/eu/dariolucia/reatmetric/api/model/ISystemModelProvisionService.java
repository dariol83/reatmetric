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

    public void subscribe(ISystemModelSubscriber subscriber);
    
    public void unsubscribe(ISystemModelSubscriber subscriber);
    
    public SystemEntity getRoot();
    
    public List<SystemEntity> getContainedEntities(SystemEntityPath se);
    
    public SystemEntity getSystemEntityAt(SystemEntityPath path);

    public SystemEntity getSystemEntityOf(int externalId);

    public int getExternalIdOf(SystemEntityPath path);

    public SystemEntityPath getPathOf(int externalId);

}
