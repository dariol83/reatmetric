/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IDataItemSubscriber;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;

/**
 *
 * @author dario
 */
public abstract class DataGenerationService<T extends AbstractDataItem, V extends AbstractDataItemFilter, K extends IDataItemSubscriber<T>>  {

    private static final int MAX_STORAGE_SIZE = 2000;
    
    protected final Map<K, V> listeners = new HashMap<>();
    protected final List<T> messages = new ArrayList<>();
    
    protected final int generationRate;
    
    public DataGenerationService(int rateMs) {
        this.generationRate = rateMs;
    }
    
    protected void startProcessing() {
    	Thread t = new Thread(this::generateMessages);
        t.setDaemon(true);
        t.start();
    }
    
    protected void generateMessages() {
        while(true) {
            T item = generateItem();
            storeAndDistribute(item);
            try {
                Thread.sleep(this.generationRate);
            } catch (InterruptedException ex) {
                Logger.getLogger(DataGenerationService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    protected abstract boolean match(V filter, T om);
    
    protected abstract T generateItem();
    
    protected void storeAndDistribute(T om) {
        synchronized(this.messages) {
            this.messages.add(om);
            // 
            if (this.messages.size() > MAX_STORAGE_SIZE)
            {
            	this.messages.remove(0);
            }
        }
        Map<K, V> copy;
        synchronized(this.listeners) {
            copy = new HashMap<>(this.listeners);
        }
        copy.entrySet().stream().filter(o -> match(o.getValue(), om)).forEach(o -> o.getKey().dataItemsReceived(Collections.singletonList(om)));
    }
    
    protected final void doSubscribe(K subscriber) {
        synchronized(this.listeners) {
            this.listeners.put(subscriber, null);
        }
    }

    protected final void doSubscribe(K subscriber, V filter) {
        synchronized(this.listeners) {
            this.listeners.put(subscriber, filter);
        }
    }

    protected final void doUnsubscribe(K subscriber) {
        synchronized(this.listeners) {
            this.listeners.remove(subscriber);
        }
    }
    
    protected final List<T> doRetrieve(Instant startTime, int numRecords, RetrievalDirection direction, V filter) {
        List<T> toReturn = new ArrayList<>();
        synchronized(this.messages) {
            // Identify the point in the list where the instant is
            int idx = findIndex(startTime);
            // Navigate the list: if direction is TO_PAST, backward; otherwise, forward
            // If excludeStart is provided, then navigate the list until excludeStart is found (and discarded)
            // Retrieve numRecords messages that match the filter (if provided)
            if(direction.equals(RetrievalDirection.TO_FUTURE)) {
                if(idx == -1) {
                    idx = 0;
                }
                for(;idx < this.messages.size(); ++idx) {
                    if(match(filter, this.messages.get(idx))) {
                        toReturn.add(this.messages.get(idx));
                    }
                    if(toReturn.size() >= numRecords) {
                        break;
                    }
                }
            } else {
                for(;idx >= 0; --idx) {
                    if(match(filter, this.messages.get(idx))) {
                        toReturn.add(this.messages.get(idx));
                    }
                    if(toReturn.size() >= numRecords) {
                        break;
                    }
                }
            }
        }
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(DataGenerationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return toReturn;
    }
    
    protected final List<T> doRetrieve(T excludeStart, int numRecords, RetrievalDirection direction, V filter) {
        List<T> toReturn = new ArrayList<>();
        synchronized(this.messages) {
            // Identify the point in the list where the instant is
            int idx = findIndex(excludeStart.getGenerationTime());
            // Navigate the list: if direction is TO_PAST, backward; otherwise, forward
            // If excludeStart is provided, then navigate the list until excludeStart is found (and discarded)
            // Retrieve numRecords messages that match the filter (if provided)
            if(direction.equals(RetrievalDirection.TO_FUTURE)) {
                if(idx == -1) {
                    idx = 0;
                }
                for(;idx < this.messages.size(); ++idx) {
                    if(this.messages.get(idx).equals(excludeStart)) {
                        idx++;
                        break;
                    }
                }
                for(;idx < this.messages.size(); ++idx) {
                    if(match(filter, this.messages.get(idx))) {
                        toReturn.add(this.messages.get(idx));
                    }
                    if(toReturn.size() >= numRecords) {
                        break;
                    }
                }
            } else {
                for(;idx >= 0; --idx) {
                    if(this.messages.get(idx).equals(excludeStart)) {
                        idx--;
                        break;
                    }
                }
                for(;idx >= 0; --idx) {
                    if(match(filter, this.messages.get(idx))) {
                        toReturn.add(this.messages.get(idx));
                    }
                    if(toReturn.size() >= numRecords) {
                        break;
                    }
                }
            }
        }
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(DataGenerationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return toReturn;
    }

    private int findIndex(Instant inst) {
        int found = -2;
        for(int i = 0; i<this.messages.size(); ++i) {
            if(this.messages.get(i).getGenerationTime().isAfter(inst)) {
                found = i - 1;
                break;
            }
        }
        switch (found) {
            case -2:
                // no object found, they are all before inst
                return this.messages.size() - 1;
            case -1:
                // first object is after inst already
                return -1;
            default:
                return found;
        }
    }
}
