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

package eu.dariolucia.reatmetric.driver.spacecraft.services;

public enum TcPacketPhase {
    /**
     * The command has been encoded and ready to be released by the system
     */
    ENCODED,
    /**
     * The command has been released and it is on its way towards the spacecraft
     */
    RELEASED,
    /**
     * The command has been radiated and it will reach the spacecraft at radiation time plus propagation delay
     */
    UPLINKED,
    /**
     * The command has reached the spacecraft and it will undergo the envisaged processing
     */
    RECEIVED_ONBOARD,
    /**
     * The command has reached the spacecraft and it is scheduled for future execution
     */
    SCHEDULED,
    /**
     * The command is available onboard for processing by the onboard computer
     */
    AVAILABLE_ONBOARD,
    /**
     * The command started its execution lifecycle
     */
    STARTED,
    /**
     * The command completed its execution lifecycle
     */
    COMPLETED,
    /**
     * The command failed
     */
    FAILED
}
