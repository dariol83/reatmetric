/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.FieldFilterStrategy;
import eu.dariolucia.reatmetric.api.common.FieldType;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import java.time.Instant;
import java.util.List;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author dario
 */
public class TestRawDataService extends DataGenerationService<RawData, RawDataFilter, IRawDataSubscriber> implements IRawDataProvisionService {

    private final AtomicInteger sequencer = new AtomicInteger(0);
    
    public TestRawDataService() {
        super(2000);
        startProcessing();
    }
    
    @Override
    protected RawData generateItem() {
        RawData om = new RawData(
                new UniqueIdImpl(UUID.randomUUID()),
                "PKT" + String.valueOf(this.sequencer.incrementAndGet()),
                "TM",
                "Route A", 
                Instant.now(), 
                Instant.now(), 
                "Test System 1",
                Quality.values()[(int) Math.floor(Math.random() * 3)],
                new Object[] { 3, 123, 3, 25 });
        return om;
    }
    
    @Override
    protected boolean match(RawDataFilter value, RawData om) {
        if(value == null) {
            return true;
        }
        if(value.getQualityList() != null) {
            if(!value.getQualityList().contains(om.getQuality())) {
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
        if(value.getNameRegExp() != null) {
            if(!om.getName().matches(value.getNameRegExp())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void subscribe(IRawDataSubscriber subscriber, RawDataFilter filter) {
        doSubscribe(subscriber, filter);
    }

    @Override
    public void unsubscribe(IRawDataSubscriber subscriber) {
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
    public List<RawData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        return doRetrieve(startTime, numRecords, direction, filter);
    }
    
    @Override
    public List<RawData> retrieve(RawData excludeStart, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        return doRetrieve(excludeStart, numRecords, direction, filter);
    }

    @Override
    public byte[] getRawDataContents(RawData data) throws ReatmetricException {
        return new byte[] {
          0x02,0x01,(byte) 0xF5,(byte) 0xAA,0x00,0x01,  
          0x01,
          0x00,
          0x30,
          0x20,
          0x10,
          0x70,
          0x50,
          0x00,
          0x70,
          (byte) 0xA0,
          0x12,
          0x02,0x01,(byte) 0xF5,(byte) 0xAA,0x00,0x01,  
          0x01,
          0x00,
          0x30,
          0x20,
          0x10,
          0x70,
          0x50,
          0x00,
          0x70,
          (byte) 0xA0,
          0x12
        };
    }
}
