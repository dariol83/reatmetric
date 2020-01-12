/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.message.impl;

import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.driver.spacecraft.message.IMessageProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.storage.impl.StorageProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.util.UniqueIdUtil;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageProcessor implements IMessageProcessor {

    private volatile StorageProcessor storer;

    private final Object[] additionalValues = new Object[0];

    private final List<MessageSubscription> listeners = new CopyOnWriteArrayList<>();

    public MessageProcessor() {
    }

    @Override
    public void setStorer(StorageProcessor storer) {
        this.storer = storer;
    }

    @Override
    public void raiseMessage(String message, String source, Severity severity) {
        OperationalMessage om = new OperationalMessage(
                UniqueIdUtil.generateNextId(OperationalMessage.class),
                Instant.now(), "",
                message,
                source,
                severity,
                additionalValues
        );
        store(om);
        distribute(om);
    }

    private void store(OperationalMessage msg) {
        this.storer.storeMessages(Collections.singletonList(msg));
    }

    private void distribute(OperationalMessage msg) {
        this.listeners.forEach(o -> o.distribute(Collections.singletonList(msg)));
    }

    @Override
    public void subscribe(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
        this.listeners.add(new MessageSubscription(subscriber, filter));
    }

    @Override
    public void unsubscribe(IOperationalMessageSubscriber subscriber) {
        Optional<MessageSubscription> toBeRemoved = this.listeners.stream().filter(o -> o.getSubscriber().equals(subscriber)).findFirst();
        toBeRemoved.ifPresent(MessageSubscription::terminate);
        toBeRemoved.ifPresent(this.listeners::remove);
    }

    @Override
    public List<OperationalMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return this.storer.retrieveMessages(startTime, numRecords, direction, filter);
    }

    @Override
    public List<OperationalMessage> retrieve(OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return this.storer.retrieveMessages(startItem, numRecords, direction, filter);
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Collections.emptyList();
    }
}
