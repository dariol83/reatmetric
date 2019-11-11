/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import java.time.Instant;
import java.util.List;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author dario
 */
public class TestOperationalMessageService extends DataGenerationService<OperationalMessage, OperationalMessageFilter, IOperationalMessageSubscriber> implements IOperationalMessageProvisionService {

    private final AtomicInteger sequencer = new AtomicInteger(0);
    
    public TestOperationalMessageService() {
        super(3000);
        startProcessing();
    }
    
    @Override
    protected OperationalMessage generateItem() {
        OperationalMessage om = new OperationalMessage(
                new LongUniqueId(TestSystem.SEQUENCER.getAndIncrement()),
                Instant.now(), String.valueOf(this.sequencer.incrementAndGet()),
                "Test Message",
                "Test System 1",
                Severity.values()[(int) Math.floor(Math.random() * 3)],
                new Object[] { "Value 1", Instant.now(), 3213L, true, 3232.21312, Severity.ALARM });
        return om;
    }
    
    @Override
    protected boolean match(OperationalMessageFilter value, OperationalMessage om) {
        if(value == null) {
            return true;
        }
        if(value.getSeverityList() != null) {
            if(!value.getSeverityList().contains(om.getSeverity())) {
                return false;
            }
        }
        if(value.getMessageTextContains() != null) {
            if(!om.getMessage().matches(value.getMessageTextContains())) {
                return false;
            }
        }
        if(value.getSourceList() != null) {
            if(!value.getSourceList().contains(om.getSource())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void subscribe(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
        doSubscribe(subscriber, filter);
    }

    @Override
    public void unsubscribe(IOperationalMessageSubscriber subscriber) {
        doUnsubscribe(subscriber);
    }
    
    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Arrays.asList(
                new FieldDescriptor("Value 1", FieldType.STRING, FieldFilterStrategy.REGEXP),
                new FieldDescriptor("Value 2", FieldType.ABSOLUTE_TIME, FieldFilterStrategy.NONE),
                new FieldDescriptor("Value 3", FieldType.INTEGER, FieldFilterStrategy.LIST_VALUE),
                new FieldDescriptor("Value 4", FieldType.BOOLEAN, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("Value 5", FieldType.REAL, FieldFilterStrategy.SINGLE_VALUE),
                new FieldDescriptor("Value 6", FieldType.ENUM, FieldFilterStrategy.SINGLE_VALUE)
        );
    }

    @Override
    public List<OperationalMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return doRetrieve(startTime, numRecords, direction, filter);
    }
    
    @Override
    public List<OperationalMessage> retrieve(OperationalMessage excludeStart, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return doRetrieve(excludeStart, numRecords, direction, filter);
    }
}
