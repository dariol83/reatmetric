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


package eu.dariolucia.reatmetric.api.model;

/**
 * Status of a system entity.
 */
public enum Status {
    /**
     * The system entity is enabled and perform a full processing of the incoming data
     */
    ENABLED,
    /**
     * The system entity is disabled and does not perform any processing
     */
    DISABLED,
    /**
     * The system entity is in an ignored state: parameters will not perform checks, events will not log alarm messages.
     * All the rest will work as usual.
     */
    IGNORED,
    /**
     * The status of the system entity is not known.
     */
    UNKNOWN,
    /**
     * The system entity does not have a status.
     */
    NOT_APPLICABLE
}
