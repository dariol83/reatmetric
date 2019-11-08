/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author dario
 */
public class TestParameterDataService extends DataGenerationService<ParameterData, ParameterDataFilter, IParameterDataSubscriber> implements IParameterDataProvisionService {

    private final AtomicInteger sequencer = new AtomicInteger(0);
    
    private final Map<String, IUniqueId> path2uuid = new TreeMap<>();
    
    private int clock = 0;
    
    public TestParameterDataService() {
        super(200);
        startProcessing();
    }
   
    @Override
    protected void generateMessages() {
        this.path2uuid.put("mcmRoot.a.param1", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.a.param3", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.a.param4", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.a.param5", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.a.param6", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.b.param7", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.b.param8", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.b.param9", new UniqueIdImpl(UUID.randomUUID()));
        this.path2uuid.put("mcmRoot.param2", new UniqueIdImpl(UUID.randomUUID()));
        
        // Create initial model
        this.messages.add(new ParameterData(this.path2uuid.get("mcmRoot.a.param1"), "param1", 
                new SystemEntityPath("mcmRoot", "a", "param1"), 
                "TEXT_VALUE", 
                12,
                Instant.now(), 
                new SystemEntityPath("mcmRoot","a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null));
        this.messages.add(new ParameterData(this.path2uuid.get("mcmRoot.a.param3"), "param3", 
                new SystemEntityPath("mcmRoot", "a", "param3"), 
                0.0, 
                0.0,
                Instant.now(), 
                new SystemEntityPath("mcmRoot", "a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null));
        this.messages.add(new ParameterData(this.path2uuid.get("mcmRoot.param2"), "param2", 
                new SystemEntityPath("mcmRoot", "param2"), 
                23322, 
                4322,
                Instant.now(), 
                new SystemEntityPath("mcmRoot"), 
                Validity.VALID, 
                AlarmState.NOMINAL, 
                Instant.now(), 
                null));
        this.messages.add(new ParameterData(this.path2uuid.get("mcmRoot.a.param4"), "param4", 
                new SystemEntityPath("mcmRoot", "a", "param4"), 
                0.0, 
                0.0,
                Instant.now(), 
                new SystemEntityPath("mcmRoot", "a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null));
        this.messages.add(new ParameterData(this.path2uuid.get("mcmRoot.a.param5"), "param5", 
                new SystemEntityPath("mcmRoot", "a", "param5"), 
                0.0, 
                0.0,
                Instant.now(), 
                new SystemEntityPath("mcmRoot", "a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null));
        super.generateMessages();
    }
    
    @Override
    protected ParameterData generateItem() {
        int param = (int) Math.floor(Math.random() * 5);
        switch(param) {
            case 1:
                return new ParameterData(this.path2uuid.get("mcmRoot.a.param1"), "param1", 
                new SystemEntityPath("mcmRoot", "a", "param1"), 
                "TEXT_VALUE" + this.sequencer.incrementAndGet(), 
                12,
                Instant.now(), 
                new SystemEntityPath("mcmRoot","a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null);
            case 2:
                this.clock += 10;
                return new ParameterData(this.path2uuid.get("mcmRoot.a.param3"), "param3", 
                new SystemEntityPath("mcmRoot", "a", "param3"), 
                Math.sin(Math.toRadians(this.clock)), 
                Math.sin(Math.toRadians(this.clock)), 
                Instant.now(), 
                new SystemEntityPath("mcmRoot","a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null);
            case 3:
                this.clock += 10;
                return new ParameterData(this.path2uuid.get("mcmRoot.a.param4"), "param4", 
                new SystemEntityPath("mcmRoot", "a", "param4"), 
                Math.cos(Math.toRadians(this.clock)), 
                Math.cos(Math.toRadians(this.clock)), 
                Instant.now(), 
                new SystemEntityPath("mcmRoot","a"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null);
            case 4:
                this.clock += 10;
                return new ParameterData(this.path2uuid.get("mcmRoot.a.param5"), "param5", 
                new SystemEntityPath("mcmRoot", "a", "param5"), 
                cap(Math.tan(Math.toRadians(this.clock)), 3.0), 
                cap(Math.tan(Math.toRadians(this.clock)), 3.0), 
                Instant.now(), 
                new SystemEntityPath("mcmRoot","a"), 
                Validity.VALID, 
                AlarmState.WARNING,
                Instant.now(), 
                null);
            default:
                return new ParameterData(this.path2uuid.get("mcmRoot.param2"), "param2", 
                new SystemEntityPath("mcmRoot", "param2"), 
                5 + this.sequencer.incrementAndGet(), 
                3 + this.sequencer.get(),
                Instant.now(), 
                new SystemEntityPath("mcmRoot"), 
                Validity.VALID, 
                AlarmState.NOMINAL,
                Instant.now(), 
                null);
        }
    }

    private double cap(double v, double cap) {
		if(v > cap) {
			return cap;
		} else if(v < -cap) {
			return -cap;
		} else {
			return v;
		}
	}

	@Override
    protected boolean match(ParameterDataFilter filter, ParameterData om) {
        return filter == null || filter.isClear() || filter.getParameterPathList().contains(om.getPath());
    }

    @Override
    public List<ParameterData> retrieve(Instant startTime, ParameterDataFilter filter) {
        Map<SystemEntityPath, ParameterData> data = new TreeMap<>();
        synchronized (messages) {
            // Start from the beginning, build a map
            for(ParameterData pd : messages) {
                if(pd.getReceptionTime().isAfter(startTime)) {
                    break;
                }
                if(match(filter, pd)) {
                    data.put(pd.getPath(), pd);
                }
            }
            // 
            return new ArrayList<>(data.values());
        }
    }

    @Override
    public void subscribe(IParameterDataSubscriber subscriber, ParameterDataFilter filter) {
        doSubscribe(subscriber, filter);
    }

    @Override
    public void unsubscribe(IParameterDataSubscriber subscriber) {
        doUnsubscribe(subscriber);
    }

    @Override
    public List<ParameterData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        return doRetrieve(startTime, numRecords, direction, filter);
    }

    @Override
    public List<ParameterData> retrieve(ParameterData startItem, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        return doRetrieve(startItem, numRecords, direction, filter);
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Collections.emptyList();
    }
}
