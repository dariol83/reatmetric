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

import java.util.HashSet;
import java.util.Set;

public class WorkingSet {

    private final Set<Integer> workingSet = new HashSet<>();

    public boolean overlaps(Set<Integer> ids) {
        synchronized (workingSet) {
            return workingSet.stream().anyMatch(ids::contains);
        }
    }

    public void add(Set<Integer> ids) {
        synchronized (workingSet) {
            if(overlaps(ids)) {
                try {
                    workingSet.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            workingSet.addAll(ids);
        }
    }

    public void remove(Set<Integer> ids) {
        synchronized (workingSet) {
            workingSet.removeAll(ids);
            workingSet.notifyAll();
        }
    }
}
