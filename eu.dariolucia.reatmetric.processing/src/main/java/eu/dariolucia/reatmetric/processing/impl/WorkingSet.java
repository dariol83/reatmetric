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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkingSet {

    private static final Logger LOG = Logger.getLogger(WorkingSet.class.getName());

    private final Set<Integer> workingSet = new HashSet<>();

    public void add(Set<Integer> ids) {
        synchronized (workingSet) {
            while(!Collections.disjoint(workingSet, ids)) {
                try {
                    if(LOG.isLoggable(Level.FINER)) {
                        LOG.finer("[Working set] Overlap of " + ids + " against " + workingSet + ": waiting...");
                    }
                    workingSet.wait();
                } catch (InterruptedException e) {
                    // Nothing to report here, just return
                    if(LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[Working set] thread interrupted - returning...");
                    }
                    return;
                }
            }
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest("[Working set] Adding " + ids);
            }
            workingSet.addAll(ids);
            workingSet.notifyAll();
        }
    }

    public void remove(Set<Integer> ids) {
        synchronized (workingSet) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest("[Working set] Removing " + ids);
            }
            workingSet.removeAll(ids);
            workingSet.notifyAll();
        }
    }
}
