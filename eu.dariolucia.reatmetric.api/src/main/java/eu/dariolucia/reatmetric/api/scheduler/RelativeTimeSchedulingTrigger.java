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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class allows the scheduling of an activity invocation to start after the completion of all
 * the referenced (by external id) scheduled activities.
 *
 * Optionally, a delay time in seconds since the completion of the last scheduled activity can be provided.
 */
public class RelativeTimeSchedulingTrigger extends AbstractSchedulingTrigger {

    private final Set<String> predecessors;

    private final int delayTime;

    public RelativeTimeSchedulingTrigger(Set<String> predecessors) {
        this(predecessors, 0);
    }

    public RelativeTimeSchedulingTrigger(Set<String> predecessors, int delayTime) {
        if(delayTime < 0) {
            throw new IllegalArgumentException("Delay time is less than 0");
        }
        this.predecessors = Collections.unmodifiableSet(new LinkedHashSet<>(predecessors));
        this.delayTime = delayTime;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public Set<String> getPredecessors() {
        return predecessors;
    }

    @Override
    public String toString() {
        return "[relative-time " +
                "predecessors " + predecessors +
                ", delay-time " + delayTime + " s]";
    }
}
