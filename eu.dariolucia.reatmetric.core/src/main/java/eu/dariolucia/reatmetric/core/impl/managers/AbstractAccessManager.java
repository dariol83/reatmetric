/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IDataItemSubscriber;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractAccessManager<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>, J extends IDataItemSubscriber<T>> {

    private static final Logger LOG = Logger.getLogger(AbstractAccessManager.class.getName());

    protected IDataItemArchive<T, K> archive;
    protected ExecutorService dispatcher; // XXX not sure that this is useful...

    protected Map<J, AbstractAccessSubscriber<T, K, J>> subscribers;
    protected IProcessingModel model;

    public AbstractAccessManager(IDataItemArchive<T, K> archive) {
        this.archive = archive;
        this.dispatcher = Executors.newFixedThreadPool(1, (runnable) -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            t.setName(getName() + " Dispatcher Thread");
            return t;
        });
        this.subscribers = new ConcurrentHashMap<>();
    }

    public void setProcessingModel(IProcessingModel model) {
        this.model = model;
    }

    public void distribute(List<AbstractDataItem> items) {
        this.dispatcher.execute(new DispatchJob(items));
    }

    protected abstract Class<? extends AbstractDataItem> getSupportedClass();

    protected abstract String getName();

    public List<T> retrieve(Instant time, K filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(time, filter, null);
        } else {
            throw new ReatmetricException("Parameter archive not available");
        }
    }
    
    public void subscribe(J subscriber, K filter) {
        if(!subscribers.containsKey(subscriber)) {
            subscribers.put(subscriber, createSubscriber(subscriber, filter, model));
        } else {
            subscribers.get(subscriber).update(filter);
        }
    }

    protected abstract AbstractAccessSubscriber<T, K, J> createSubscriber(J subscriber, K filter, IProcessingModel model);

    public void unsubscribe(J subscriber) {
        AbstractAccessSubscriber<T, K, J> sub = subscribers.remove(subscriber);
        if(sub != null) {
            sub.terminate();
        }
    }

    public List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(startTime, numRecords, direction, filter);
        } else {
            throw new ReatmetricException(getName() + " - Archive not available");
        }
    }

    public List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(startItem, numRecords, direction, filter);
        } else {
            throw new ReatmetricException(getName() + " - Archive not available");
        }
    }

    public void dispose() {
        for(AbstractAccessSubscriber<T, K, J> aas : this.subscribers.values()) {
            aas.terminate();
        }
        this.subscribers.clear();
    }

    private class DispatchJob implements Runnable {

        private List<AbstractDataItem> items;

        public DispatchJob(List<AbstractDataItem> items) {
            this.items = items;
        }

        public void run() {
            // Remove the items that are not handled by this access manager
            List<T> toDistribute = items.stream().filter((i) -> i.getClass().equals(getSupportedClass())).map(o -> (T) o).collect(Collectors.toList());
            // Store
            if(archive != null) {
                try {
                    archive.store(toDistribute);
                } catch (ArchiveException e) {
                    LOG.log(Level.SEVERE, getName() + " - Cannot store data items inside the archive", e);
                }
            }
            // Distribute
            for(Map.Entry<J, AbstractAccessSubscriber<T, K, J>> entry : subscribers.entrySet()) {
                try {
                    entry.getValue().notifyItems(toDistribute);
                } catch(Exception e) {
                    LOG.log(Level.SEVERE, getName() + " - Cannot notify data items to subscriber " + entry.getValue(), e);
                }
            }
        }
    }
}
