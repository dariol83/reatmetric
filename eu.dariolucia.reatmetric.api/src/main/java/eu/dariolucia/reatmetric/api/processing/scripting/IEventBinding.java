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

package eu.dariolucia.reatmetric.api.processing.scripting;

import eu.dariolucia.reatmetric.api.messages.Severity;

/**
 * Interface representing a binding to an event entity.
 * Extends IEntityBinding with event-specific metadata.
 */
public interface IEventBinding extends IEntityBinding {

    /**
     * Returns the severity of the event.
     *
     * @return the event severity
     */
    Severity severity();

    /**
     * Returns the route of the event.
     *
     * @return the event route
     */
    String route();

    /**
     * Returns the source of the event.
     *
     * @return the event source
     */
    String source();

    /**
     * Returns the type of the event.
     *
     * @return the event type
     */
    String type();

    /**
     * Returns the qualifier of the event.
     *
     * @return the event qualifier
     */
    String qualifier();

    /**
     * Returns the report of the event.
     *
     * @return the event report
     */
    Object report();

    /**
     * Returns the container ID of the event.
     *
     * @return the container ID, may be null
     */
    Long containerId();
}
