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

import java.time.Instant;

/**
 * This class allows the scheduling of an activity invocation using a predefined release time.
 */
public class AbsoluteTimeSchedulingTrigger extends AbstractSchedulingTrigger {

    /**
     * The release time at which the scheduled activity should be triggered.
     */
    private final Instant releaseTime;

    /**
     * Constructor for an absolute time scheduling trigger.
     *
     * @param releaseTime the release time for the scheduled activity, cannot be null
     */
    public AbsoluteTimeSchedulingTrigger(Instant releaseTime) {
        this.releaseTime = releaseTime;
    }

    /**
     * Returns the release time at which the scheduled activity should be triggered.
     *
     * @return the release time (cannot be null)
     */
    public Instant getReleaseTime() {
        return releaseTime;
    }

    /**
     * Returns a string representation of this scheduling trigger.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "[absolute-time " + releaseTime + "]";
    }
}
