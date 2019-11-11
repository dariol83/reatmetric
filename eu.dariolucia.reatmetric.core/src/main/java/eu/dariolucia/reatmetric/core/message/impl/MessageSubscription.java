/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.message.impl;

import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.core.util.ThreadUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MessageSubscription {

    private final IOperationalMessageSubscriber subscriber;

    private final OperationalMessageFilter filter;

    private final ExecutorService subscriptionHandler = ThreadUtil.newSingleThreadExecutor("Operational Message Subscription Handler");

    private volatile boolean running;

    MessageSubscription(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
        this.subscriber = subscriber;
        this.filter = filter;
    }

    void distribute(List<OperationalMessage> toDistribute) {
        if(running) {
            subscriptionHandler.submit(() -> doDistribute(toDistribute));
        }
    }

    private void doDistribute(List<OperationalMessage> toDistribute) {
        subscriber.dataItemsReceived(toDistribute.stream().filter(this::match).collect(Collectors.toList()));
    }

    private boolean match(OperationalMessage om) {
        if(filter == null) {
            return true;
        }
        if(filter.getSeverityList() != null) {
            if(!filter.getSeverityList().contains(om.getSeverity())) {
                return false;
            }
        }
        if(filter.getMessageTextContains() != null) {
            if(!om.getMessage().matches(filter.getMessageTextContains())) {
                return false;
            }
        }
        if(filter.getSourceList() != null) {
            if(!filter.getSourceList().contains(om.getSource())) {
                return false;
            }
        }
        return true;
    }

    void terminate() {
        running = false;
        subscriptionHandler.shutdownNow();
        try {
            subscriptionHandler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.interrupted();
        }
    }

    IOperationalMessageSubscriber getSubscriber() {
        return subscriber;
    }
}
