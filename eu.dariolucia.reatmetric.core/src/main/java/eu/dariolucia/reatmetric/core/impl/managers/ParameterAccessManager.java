/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.parameters.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ParameterAccessManager implements IParameterDataProvisionService {

    protected IParameterDataArchive archive;
    protected ExecutorService dispatcher; // XXX not sure that this is useful...

    protected Map<IParameterDataSubscriber, ParameterDataAccessSubscriber> subscribers;
    protected IProcessingModel model;

    public ParameterAccessManager(IParameterDataArchive archive) {
        this.archive = archive;
        this.dispatcher = Executors.newFixedThreadPool(1, (runnable) -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            t.setName("Parameter Access Manager Dispatcher Thread");
            return t;
        });
        this.subscribers = new ConcurrentHashMap<>();
    }

    public void setProcessingModel(IProcessingModel model) {
        this.model = model;
    }

    public void distribute(List<AbstractDataItem> items) {
        this.dispatcher.execute(() -> {
            // Remove the items that are not handled by this access manager
            List<ParameterData> toDistribute = items.stream().filter((i) -> i.getClass().equals(getSupportedClass())).map(o -> (ParameterData) o).collect(Collectors.toList());
            // Store
            if(archive != null) {
                try {
                    archive.store(toDistribute);
                } catch (ArchiveException e) {
                    e.printStackTrace();
                    // TODO log
                }
            }
            // Distribute
            for(Map.Entry<IParameterDataSubscriber, ParameterDataAccessSubscriber> entry : subscribers.entrySet()) {
                try {
                    entry.getValue().notifyItems(toDistribute);
                } catch(Exception e) {
                    e.printStackTrace();
                    // TODO log
                }
            }
        });
    }

    protected Class<? extends AbstractDataItem> getSupportedClass() {
        return ParameterData.class;
    }

    @Override
    public List<ParameterData> retrieve(Instant time, ParameterDataFilter filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(time, filter, null);
        } else {
            throw new ReatmetricException("Parameter archive not available");
        }
    }

    @Override
    public void subscribe(IParameterDataSubscriber subscriber, ParameterDataFilter filter) {
        if(!subscribers.containsKey(subscriber)) {
            subscribers.put(subscriber, new ParameterDataAccessSubscriber(subscriber, filter, model));
        } else {
            subscribers.get(subscriber).update(filter);
        }
    }

    @Override
    public void unsubscribe(IParameterDataSubscriber subscriber) {
        ParameterDataAccessSubscriber sub = subscribers.remove(subscriber);
        if(sub != null) {
            sub.terminate();
        }
    }

    @Override
    public List<ParameterData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(startTime, numRecords, direction, filter);
        } else {
            throw new ReatmetricException("Parameter archive not available");
        }
    }

    @Override
    public List<ParameterData> retrieve(ParameterData startItem, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        if(archive != null) {
            return archive.retrieve(startItem, numRecords, direction, filter);
        } else {
            throw new ReatmetricException("Parameter archive not available");
        }
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Collections.emptyList();
    }
}
