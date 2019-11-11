/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

/**
 *
 * @author dario
 */
public class TestEventDataService extends DataGenerationService<EventData, EventDataFilter, IEventDataSubscriber> implements IEventDataProvisionService {

    private final AtomicInteger sequencer = new AtomicInteger(0);
    
    private final List<SystemEntity> events = new ArrayList<>(); 
    
    public TestEventDataService(TestSystemModelService s) {
        super(2000);
        for(Map.Entry<SystemEntityPath, SystemEntity> e : s.getEntities().entrySet()) {
        	if(e.getValue().getType() == SystemEntityType.EVENT) {
        		events.add(e.getValue());
        	}
        }
        startProcessing();
    }
    
    @Override
    protected EventData generateItem() {
    	int idxToGenerate = (int) Math.floor(Math.random() * events.size());
    	SystemEntity toGenerate =this.events.get(idxToGenerate);
    	
    	EventData om = new EventData(
                new LongUniqueId(TestSystem.SEQUENCER.getAndIncrement()),
                Instant.now(),
                toGenerate.getName().hashCode(),
                toGenerate.getName(),
                toGenerate.getPath(),
                "qualifier" + sequencer.incrementAndGet(),
                "ANOMALY",
                "routeA",
                "sourceA",
                Severity.values()[(int) Math.floor(Math.random() * Severity.values().length)],
                Instant.now(),
                new Object[] { 3, 123, 3, 25 });
        return om;
    }
    
    @Override
    protected boolean match(EventDataFilter value, EventData om) {
        if(value == null) {
            return true;
        }
        if(value.getSeverityList() != null) {
            if(!value.getSeverityList().contains(om.getSeverity())) {
                return false;
            }
        }
        if(value.getRouteList() != null) {
            if(!value.getRouteList().contains(om.getRoute())) {
                return false;
            }
        }
        if(value.getSourceList() != null) {
            if(!value.getSourceList().contains(om.getSource())) {
                return false;
            }
        }
        if(value.getTypeList() != null) {
            if(!value.getTypeList().contains(om.getType())) {
                return false;
            }
        }
        if(value.getParentPath() != null) {
            if(!om.getPath().isDescendantOf(value.getParentPath())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void subscribe(IEventDataSubscriber subscriber, EventDataFilter filter) {
        doSubscribe(subscriber, filter);
    }

    @Override
    public void unsubscribe(IEventDataSubscriber subscriber) {
        doUnsubscribe(subscriber);
    }
    
    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Arrays.asList(
                new FieldDescriptor("S/C", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("APID", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("PUS Type", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("PUS SubType", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE)
        );
    }

    @Override
    public List<EventData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, EventDataFilter filter) {
        return doRetrieve(startTime, numRecords, direction, filter);
    }
    
    @Override
    public List<EventData> retrieve(EventData excludeStart, int numRecords, RetrievalDirection direction, EventDataFilter filter) {
        return doRetrieve(excludeStart, numRecords, direction, filter);
    }
}
