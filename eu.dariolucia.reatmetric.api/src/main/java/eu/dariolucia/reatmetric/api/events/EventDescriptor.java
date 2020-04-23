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

public class EventDescriptor extends AbstractSystemEntityDescriptor {

    private final String description;
    private final Severity severity;
    private final String eventType;
    private final boolean conditionDriven;

    public EventDescriptor(SystemEntityPath path, int externalId, String description, Severity severity, String eventType, boolean conditionDriven) {
        super(path, externalId, SystemEntityType.EVENT);
        this.description = description;
        this.severity = severity;
        this.eventType = eventType;
        this.conditionDriven = conditionDriven;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isConditionDriven() {
        return conditionDriven;
    }

}
