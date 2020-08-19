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

import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.scheduler.IScheduler;
import eu.dariolucia.reatmetric.api.scheduler.ISchedulerFactory;

public class SchedulerFactory implements ISchedulerFactory {

    @Override
    public IScheduler buildScheduler(IArchive archive, IActivityExecutionService activityExecutor, IEventDataProvisionService eventMonService, IActivityOccurrenceDataProvisionService activityMonService) {
        return new Scheduler(archive, activityExecutor, eventMonService, activityMonService);
    }
}
