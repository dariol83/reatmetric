/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.scheduler;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scheduler implements IScheduler {

    private static final Logger LOG = Logger.getLogger(Scheduler.class.getName());

    private final IScheduledActivityDataArchive archive;
    private final IProcessingModel processingModel;

    public Scheduler(IArchive archive, IProcessingModel model) {
        if(archive != null) {
            this.archive = archive.getArchive(IScheduledActivityDataArchive.class);
        } else {
            this.archive = null;
        }
        this.processingModel = model;
    }

    @Override
    public void initialise() throws SchedulingException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("initialise() invoked");
        }
        // TODO prepare internal state
        // TODO initialise from archive
        // TODO start disabled (depending on system property - to be defined)
    }

    @Override
    public void subscribe(ISchedulerSubscriber subscriber) {

    }

    @Override
    public void unsubscribe(ISchedulerSubscriber subscriber) {

    }

    @Override
    public void enable() throws SchedulingException {

    }

    @Override
    public void disable() throws SchedulingException {

    }

    @Override
    public boolean isEnabled() throws SchedulingException {
        return false;
    }

    @Override
    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        return null;
    }

    @Override
    public List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        return null;
    }

    @Override
    public List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException {
        return null;
    }

    @Override
    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        return null;
    }

    @Override
    public boolean remove(IUniqueId scheduledId) throws SchedulingException {
        return false;
    }

    @Override
    public boolean remove(ScheduledActivityDataFilter filter) throws SchedulingException {
        return false;
    }

    @Override
    public List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) {
        return null;
    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant time, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }

    @Override
    public void subscribe(IScheduledActivityDataSubscriber subscriber, ScheduledActivityDataFilter filter) {

    }

    @Override
    public void unsubscribe(IScheduledActivityDataSubscriber subscriber) {

    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }

    @Override
    public List<ScheduledActivityData> retrieve(ScheduledActivityData startItem, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }
}
