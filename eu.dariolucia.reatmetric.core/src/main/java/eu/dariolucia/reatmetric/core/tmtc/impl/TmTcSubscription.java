/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.impl;

import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.util.ThreadUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TmTcSubscription {

    private final IRawDataSubscriber subscriber;

    private final RawDataFilter filter;

    private final ExecutorService subscriptionHandler = ThreadUtil.newSingleThreadExecutor("Raw Data Subscription Handler");

    private volatile boolean running;

    TmTcSubscription(IRawDataSubscriber subscriber, RawDataFilter filter) {
        this.subscriber = subscriber;
        this.filter = filter;
    }

    void distribute(List<RawData> toDistribute) {
        if(running) {
            subscriptionHandler.submit(() -> doDistribute(toDistribute));
        }
    }

    private void doDistribute(List<RawData> toDistribute) {
        subscriber.dataItemsReceived(toDistribute.stream().filter(this::match).collect(Collectors.toList()));
    }

    private boolean match(RawData om) {
        if(filter == null) {
            return true;
        }
        if(filter.getQualityList() != null) {
            if(!filter.getQualityList().contains(om.getQuality())) {
                return false;
            }
        }
        if(filter.getRouteList() != null) {
            if(!filter.getRouteList().contains(om.getRoute())) {
                return false;
            }
        }
        if(filter.getSourceList() != null) {
            if(!filter.getSourceList().contains(om.getSource())) {
                return false;
            }
        }
        if(filter.getTypeList() != null) {
            if(!filter.getTypeList().contains(om.getType())) {
                return false;
            }
        }
        if(filter.getNameRegExp() != null) {
            if(!om.getName().matches(filter.getNameRegExp())) {
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

    IRawDataSubscriber getSubscriber() {
        return subscriber;
    }
}
