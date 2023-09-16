/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket;

/**
 * TODO: this device has a protocol supporting one binary connection, fully asynchronous with command identification tags
 * Command messages are:
 * - Poll monitoring values of a given device subsystem
 * - Send command to device subsystem
 * - Set device subsystem value
 * Response messages are:
 * - Telemetry data
 * - Positive ACK of command/set
 * - Negative ACK
 * - Event
 */
public class BinaryDeviceSingleConnection {
}
