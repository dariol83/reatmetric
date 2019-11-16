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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

/**
 *
 * FIXME: wrong assumption with InternaId (can change!), use ExternalId, to be fixed
 *
 * @author dario
 */
public class TestAlarmParameterDataService extends DataGenerationService<AlarmParameterData, AlarmParameterDataFilter, IAlarmParameterDataSubscriber> implements IAlarmParameterDataProvisionService {

    private final AtomicInteger sequencer = new AtomicInteger(0);
    
    private final List<SystemEntity> parameters = new ArrayList<>(); 
    
    private final Map<SystemEntityPath, AlarmParameterData> currentAlarms = new HashMap<>();
    
    public TestAlarmParameterDataService(TestSystemModelService s) {
        super(2000);
        for(Map.Entry<SystemEntityPath, SystemEntity> e : s.getEntities().entrySet()) {
        	if(e.getValue().getType() == SystemEntityType.PARAMETER) {
        		parameters.add(e.getValue());
        	}
        }
        startProcessing();
    }
    
    @Override
    protected AlarmParameterData generateItem() {
    	int idxToGenerate = (int) Math.floor(Math.random() * parameters.size());
    	SystemEntity toGenerate =this.parameters.get(idxToGenerate);
    	
    	AlarmParameterData om = new AlarmParameterData(
                new LongUniqueId(TestSystem.SEQUENCER.getAndIncrement()),
                null,
                0,
                toGenerate.getName(),
                toGenerate.getPath(),
                AlarmState.values()[(int) Math.floor(Math.random() * AlarmState.values().length)],
                100,
                120,
                Instant.now(),
                Instant.now(),
                new Object[] { "Description " + sequencer.incrementAndGet() });
        return om;
    }
    
    @Override
    protected void generateMessages() {
        while(true) {
        	AlarmParameterData item = generateItem();
        	if(this.currentAlarms.containsKey(item.getPath())) {
        		// Check the state
        		if(item.getCurrentAlarmState() == AlarmState.ALARM || item.getCurrentAlarmState() == AlarmState.ERROR || item.getCurrentAlarmState() == AlarmState.WARNING) {
        			// Override, store and distribute
        			this.currentAlarms.put(item.getPath(), item);
        		} else {
        			// Delete, store and distribute
        			this.currentAlarms.remove(item.getPath());
        		}
        		storeAndDistribute(item);
        	} else {
        		if(item.getCurrentAlarmState() == AlarmState.ALARM || item.getCurrentAlarmState() == AlarmState.ERROR || item.getCurrentAlarmState() == AlarmState.WARNING) {
        			// Add, store and distribute
        			this.currentAlarms.put(item.getPath(), item);
        			storeAndDistribute(item);
        		}
        	}
            
            try {
                Thread.sleep(this.generationRate);
            } catch (InterruptedException ex) {
                Logger.getLogger(DataGenerationService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    protected boolean match(AlarmParameterDataFilter value, AlarmParameterData om) {
        if(value == null) {
            return true;
        }
        if(value.getAlarmStateList() != null) {
            if(!value.getAlarmStateList().contains(om.getCurrentAlarmState())) {
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
    public void subscribe(IAlarmParameterDataSubscriber subscriber, AlarmParameterDataFilter filter) {
        doSubscribe(subscriber, filter);
    }

    @Override
    public void unsubscribe(IAlarmParameterDataSubscriber subscriber) {
        doUnsubscribe(subscriber);
    }
    
    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Arrays.asList(new FieldDescriptor("Description", FieldType.STRING, FieldFilterStrategy.REGEXP));
    }

    @Override
    public List<AlarmParameterData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        return doRetrieve(startTime, numRecords, direction, filter);
    }
    
    @Override
    public List<AlarmParameterData> retrieve(AlarmParameterData excludeStart, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        return doRetrieve(excludeStart, numRecords, direction, filter);
    }
}
