/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelSubscriber;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

/**
 *
 * @author dario
 */
public class TestSystemModelService implements ISystemModelProvisionService {

    protected final Set<ISystemModelSubscriber> listeners = new HashSet<>();
    
    private final Map<SystemEntityPath, SystemEntity> path2element = new TreeMap<>();
    private final Map<SystemEntityPath, List<SystemEntity>> path2children = new TreeMap<>();
    private SystemEntity root = null;
    
    public TestSystemModelService() {
        generateModel();
    }
    
    @Override
    public void subscribe(ISystemModelSubscriber subscriber) {
        this.listeners.add(subscriber);
    }

    @Override
    public void unsubscribe(ISystemModelSubscriber subscriber) {
        this.listeners.remove(subscriber);
    }

    @Override
    public SystemEntity getRoot() {
        return this.root;
    }

    @Override
    public List<SystemEntity> getContainedEntities(SystemEntityPath se) {
        return this.path2children.get(se);
    }

    @Override
    public SystemEntity getSystemEntityAt(SystemEntityPath path) {
        return this.path2element.get(path);
    }

    private void generateModel() {
        this.root = new SystemEntity(new LongUniqueId(TestSystem.SEQUENCER.getAndIncrement()), new SystemEntityPath("mcmRoot"), "mcmRoot", Status.ENABLED, AlarmState.NOMINAL, SystemEntityType.CONTAINER);
        addToMaps(this.root);
        SystemEntity c1 = generateChildElement(this.root, "a", SystemEntityType.CONTAINER);
        SystemEntity c2 = generateChildElement(this.root, "b", SystemEntityType.CONTAINER);
        generateChildElement(this.root, "c", SystemEntityType.CONTAINER);
        generateChildElement(this.root, "param2", SystemEntityType.PARAMETER);
        
        generateChildElement(c1, "param1", SystemEntityType.PARAMETER);
        generateChildElement(c1, "event1", SystemEntityType.EVENT);
        generateChildElement(c1, "event2", SystemEntityType.EVENT);
        generateChildElement(c1, "event3", SystemEntityType.EVENT);
        generateChildElement(c1, "event4", SystemEntityType.EVENT);
        generateChildElement(c1, "activity1", SystemEntityType.ACTIVITY);
        generateChildElement(c1, "report1", SystemEntityType.REPORT);
        generateChildElement(c1, "reference1", SystemEntityType.REFERENCE);
        generateChildElement(c1, "param3", SystemEntityType.PARAMETER);
        generateChildElement(c1, "param4", SystemEntityType.PARAMETER);
        generateChildElement(c1, "param5", SystemEntityType.PARAMETER);
        generateChildElement(c1, "param6", SystemEntityType.PARAMETER);
        
        generateChildElement(c2, "event2", SystemEntityType.EVENT);
        generateChildElement(c2, "event3", SystemEntityType.EVENT);
        generateChildElement(c2, "event4", SystemEntityType.EVENT);
        generateChildElement(c2, "param7", SystemEntityType.PARAMETER);
        generateChildElement(c2, "param8", SystemEntityType.PARAMETER);
        generateChildElement(c2, "param9", SystemEntityType.PARAMETER);
    }

    private void addToMaps(SystemEntity se) {
        this.path2element.put(se.getPath(), se);
        SystemEntityPath parent = se.getPath().getParent();
        if(parent == null) {
            // Return
            return;
        }
        List<SystemEntity> children = this.path2children.get(parent);
        if(children == null) {
            children = new ArrayList<>();
            this.path2children.put(parent, children);
        }
        children.add(se);
    }

    private SystemEntity generateChildElement(SystemEntity parent, String name, SystemEntityType type) {
        SystemEntity se = new SystemEntity(new LongUniqueId(TestSystem.SEQUENCER.getAndIncrement()), parent.getPath().append(name), name, Status.ENABLED, AlarmState.UNKNOWN, type);
        addToMaps(se);
        return se;
    }
    
    public Map<SystemEntityPath, SystemEntity> getEntities() {
    	return new HashMap<>(this.path2element);
    }
    
}
