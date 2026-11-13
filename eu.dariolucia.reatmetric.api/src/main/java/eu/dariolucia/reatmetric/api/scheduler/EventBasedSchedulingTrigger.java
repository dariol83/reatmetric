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

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 * This class allows the scheduling of an activity invocation to after the completion of all
 * the referenced scheduled activities.
 */
public class EventBasedSchedulingTrigger extends AbstractSchedulingTrigger {

    /** The event path. */
    private final SystemEntityPath event;

    /** The protection time in milliseconds. */
    private final int protectionTime;

    /** Whether this trigger is enabled. */
    private final boolean enabled;

    /**
     * Constructs a new EventBasedSchedulingTrigger.
     *
     * @param event the event path
     * @param protectionTime the protection time in milliseconds
     * @param enabled whether this trigger is enabled
     */
    public EventBasedSchedulingTrigger(SystemEntityPath event, int protectionTime, boolean enabled) {
        this.event = event;
        this.protectionTime = protectionTime;
        this.enabled = enabled;
    }

    /**
     * Returns the event path.
     *
     * @return the event path
     */
    public SystemEntityPath getEvent() {
        return event;
    }

    /**
     * The protection time in ms: a new activity occurrence is released only if a new occurrence of the
     * specified event happens at least protectionTime millisecond after the previous occurrence.
     * This mechanism avoids flooding the system with activity invocations in case of many occurrences
     * of the same event happening very close in time.
     *
     * @return the protection time
     */
    public int getProtectionTime() {
        return protectionTime;
    }

    /**
     * Returns whether this trigger is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    /**
     * Returns a string representation of this EventBasedSchedulingTrigger.
     *
     * @return the string representation
     */
    public String toString() {
        return "[event " + event +
                ", protection-time " + protectionTime + " ms" +
                ", " + (enabled ? "enabled" : "disabled") +
                "]";
    }
}
