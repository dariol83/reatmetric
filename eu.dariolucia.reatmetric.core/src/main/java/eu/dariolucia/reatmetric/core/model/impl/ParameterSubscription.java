/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.model.impl;

import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.core.util.ThreadUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ParameterSubscription {

    private final IParameterDataSubscriber subscriber;

    private final ParameterDataFilter filter;

    private final ExecutorService subscriptionHandler = ThreadUtil.newSingleThreadExecutor("Parameter Subscription Handler");

    private volatile boolean running;

    ParameterSubscription(IParameterDataSubscriber subscriber, ParameterDataFilter filter) {
        this.subscriber = subscriber;
        this.filter = filter;
    }

    void distribute(List<ParameterData> toDistribute) {
        if(running) {
            subscriptionHandler.submit(() -> doDistribute(toDistribute));
        }
    }

    private void doDistribute(List<ParameterData> toDistribute) {
        subscriber.dataItemsReceived(toDistribute.stream().filter(this::match).collect(Collectors.toList()));
    }

    private boolean match(ParameterData om) {
        return filter == null || filter.isClear() || filter.getParameterPathList().contains(om.getPath());
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

    IParameterDataSubscriber getSubscriber() {
        return subscriber;
    }
}
