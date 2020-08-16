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

package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;

/**
 * This interface is the service interface representing the scheduling service in a ReatMetric system. It contains
 * a single factory method that can be used to instantiate an implementation of the {@link IScheduler} interface.
 */
public interface ISchedulerFactory {

    /**
     * Return an implementation of the {@link IScheduler} interface supplied by the provider of the service. The
     * archive argument allows the implementation to restore the current status from the archive.
     *
     * The returned {@link IScheduler} object is not required to be a different new object: {@link ISchedulerFactory}
     * implementations are allowed to cache objects or use a singleton-based design.
     *
     * @param archive the archive system, can be null: in such case, nothing is restored
     * @return an implementation of {@link IScheduler} interface
     * @throws SchedulingException in case of problems arising from the construction of the specific {@link IScheduler} object
     */
    IScheduler buildScheduler(IArchive archive) throws SchedulingException;

}
