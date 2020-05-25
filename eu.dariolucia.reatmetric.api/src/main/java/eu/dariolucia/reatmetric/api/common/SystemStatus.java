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

package eu.dariolucia.reatmetric.api.common;

/**
 * This enumeration is used to report the status of internal software components (such as drivers) to the ReatMetric framework.
 */
public enum SystemStatus {
    /**
     * The status of the system cannot be derived
     */
    UNKNOWN,
    /**
     * The status of the system is nominal and works as expected
     */
    NOMINAL,
    /**
     * The status of the system presents some issues that might endanger one or more of its functionality
     */
    WARNING,
    /**
     * The status of the system presents issues that prevent the system to work properly
     */
    ALARM
}
