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

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessingTask extends FutureTask<List<AbstractDataItem>> {

    private static final Logger LOG = Logger.getLogger(ProcessingTask.class.getName());

    private final Job job;

    private final boolean includeWeaklyConsistent;

    ProcessingTask(Job toRun, boolean includeWeaklyConsistent) {
        super(toRun);
        this.job = toRun;
        this.includeWeaklyConsistent = includeWeaklyConsistent;
    }

    void prepareTask(GraphModel graphModel) {
        // Delegate
        job.prepareTask(graphModel, this.includeWeaklyConsistent);
    }

    Set<Integer> getAffectedItems() {
        // Delegate
        return job.getAffectedItems();
    }

    public static class Job implements Callable<List<AbstractDataItem>> {
        private List<AbstractModelOperation<?>> operations;
        private final Consumer<List<AbstractDataItem>> output;
        private final Set<Integer> affectedItems;
        private final WorkingSet workingSet;

        public Job(List<AbstractModelOperation<?>> operations, Consumer<List<AbstractDataItem>> output, WorkingSet workingSet) {
            this.operations = operations;
            this.output = output;
            this.affectedItems = new HashSet<>();
            this.workingSet = workingSet;
        }

        @Override
        public List<AbstractDataItem> call() throws Exception {
            // XXX: think about having parallel parameter processing by introducing processing levels based on longest-dependency count
            List<AbstractDataItem> result = new ArrayList<>(operations.size());
            for (AbstractModelOperation<?> amo : operations) {
                try {
                    result.addAll(amo.execute());
                } catch (Exception e) {
                    // You need to survive here!
                    LOG.log(Level.SEVERE, "Cannot process model operation " + amo + ": " + e.getMessage(), e);
                    // If you have an operation that aborts the update (e.g. activity start), report the cause and exit
                    if(amo.isAbortOnException()) {
                        // Remove items
                        workingSet.remove(affectedItems);
                        // Report exception
                        throw e;
                    }
                }
            }
            // Remove items
            workingSet.remove(affectedItems);
            // Notify
            output.accept(result);
            // Return the result
            return result;
        }

        void prepareTask(GraphModel graphModel, boolean includeWeakConsistent) {
            // Finalize the list by extending it with the necessary re-evaluations, the setting of the processors
            // and order by topological sort
            operations = graphModel.finalizeOperationList(operations, includeWeakConsistent);
            // Build the set of affected items by ID: do not put items that are weakly consistent
            for (AbstractModelOperation<?> amo : operations) {
                if(!amo.getProcessor().isWeaklyConsistent() || includeWeakConsistent) {
                    this.affectedItems.add(amo.getSystemEntityId());
                }
            }
        }

        public Set<Integer> getAffectedItems() {
            return affectedItems;
        }
    }
}
