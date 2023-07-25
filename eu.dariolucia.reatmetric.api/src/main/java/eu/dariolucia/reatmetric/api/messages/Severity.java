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


package eu.dariolucia.reatmetric.api.messages;

/**
 * Severity type for operational messages.
 */
public enum Severity {
    /**
     * The message reports an alarm situation and immediate action is required.
     */
    ALARM,
    /**
     * The message reports a non-nominal situation, which shall be immediately taken care of, as it will lead to
     * system misbehaviour.
     */
    ERROR,
    /**
     * The message reports a non-nominal situation, which might be taken care of, as it might lead to system
     * misbehaviour.
     */
    WARN,
    /**
     * An information message.
     */
    INFO,
    /**
     * No severity information reported.
     */
    NONE,
    /**
     * The severity cannot be determined.
     */
    UNKNOWN,
    /**
     * The message is a chat message.
     */
    CHAT
}
