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

package eu.dariolucia.reatmetric.api.events;

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

/**
 * The descriptor of an event system entity.
 *
 * Objects of this class are immutable.
 */
public class EventDescriptor extends AbstractSystemEntityDescriptor {

    private final String description;
    private final Severity severity;
    private final String eventType;
    private final boolean conditionDriven;

    /**
     * Constructor of the class.
     *
     * @param path the event path
     * @param externalId the event ID
     * @param description the event description
     * @param severity the event severity
     * @param eventType the event type
     * @param conditionDriven true if it is driven by condition, otherwise false
     */
    public EventDescriptor(SystemEntityPath path, int externalId, String description, Severity severity, String eventType, boolean conditionDriven) {
        super(path, externalId, SystemEntityType.EVENT);
        this.description = description;
        this.severity = severity;
        this.eventType = eventType;
        this.conditionDriven = conditionDriven;
    }

    /**
     * Return the activity description.
     *
     * @return the activity description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the event severity.
     *
     * @return the event severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Return the event type.
     *
     * @return the event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Return whether the event is driven by a condition.
     *
     * @return true if the event is condition-driver, otherwise false
     */
    public boolean isConditionDriven() {
        return conditionDriven;
    }

}
