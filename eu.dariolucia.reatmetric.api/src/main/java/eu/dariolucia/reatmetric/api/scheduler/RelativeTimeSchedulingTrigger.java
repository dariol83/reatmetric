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

    /** The set of predecessor activity names. */
    private final Set<String> predecessors;

    /** The delay time in seconds. */
    private final int delayTime;

    /**
     * Constructs a new RelativeTimeSchedulingTrigger with the specified predecessors and no delay.
     *
     * @param predecessors the set of predecessor activity names
     */
    public RelativeTimeSchedulingTrigger(Set<String> predecessors) {
        this(predecessors, 0);
    }

    /**
     * Constructs a new RelativeTimeSchedulingTrigger.
     *
     * @param predecessors the set of predecessor activity names
     * @param delayTime the delay time in seconds (must be >= 0)
     * @throws IllegalArgumentException if delayTime is negative
     */
    public RelativeTimeSchedulingTrigger(Set<String> predecessors, int delayTime) {
        if(delayTime < 0) {
            throw new IllegalArgumentException("Delay time is less than 0");
        }
        this.predecessors = Collections.unmodifiableSet(new LinkedHashSet<>(predecessors));
        this.delayTime = delayTime;
    }

    /**
     * Returns the delay time in seconds.
     *
     * @return the delay time in seconds
     */
    public int getDelayTime() {
        return delayTime;
    }

    /**
     * Returns the set of predecessor activity names.
     *
     * @return the set of predecessor activity names
     */
    public Set<String> getPredecessors() {
        return predecessors;
    }

    @Override
    /**
     * Returns a string representation of this RelativeTimeSchedulingTrigger.
     *
     * @return the string representation
     */
    public String toString() {
        return "[relative-time " +
                "predecessors " + predecessors +
                ", delay-time " + delayTime + " s]";
    }
}
