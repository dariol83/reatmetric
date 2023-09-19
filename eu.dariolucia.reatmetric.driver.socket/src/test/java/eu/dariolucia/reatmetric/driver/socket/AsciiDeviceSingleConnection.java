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

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.text.DecimalFormat;
import java.util.function.BiFunction;

/**
 * TODO: this device has a protocol supporting a single ASCII connection, fully synchronous (request, response)
 * Command messages are:
 * - Poll monitoring values of a given device subsystem: {ST,[device subsystem name]}
 * - Send command to device subsystem: {CMD,[device subsystem name],[command code],[argument 1],[argument 2]}
 * - Set device subsystem value: {SET,[device subsystem name],[parameter name],[parameter value]}
 * Response messages are:
 * - Telemetry data: {TLM,[list of parameter values separated by comma]}
 * - Positive ACK of command: {ACK,[device sybsystem name]}
 * - Negative ACK/Negative response: {NOK}
 */
public class AsciiDeviceSingleConnection {

    private static final int PORT = 34212;
    private static final Device DEVICE = new Device("DEVICE1");

    public static void main(String[] args) {
        // Create the device subsystems
        {
            DeviceSubsystem ds = DEVICE.createSubsystem("SUB1");
            ds.addParameter("Status", ValueTypeEnum.ENUMERATED, 0)
                .addParameter("Frequency", ValueTypeEnum.UNSIGNED_INTEGER, 3000)
                .addParameter("Temperature", ValueTypeEnum.REAL, 22.1)
                .addParameter("Offset", ValueTypeEnum.SIGNED_INTEGER, 0L)
                .addParameter("Mode", ValueTypeEnum.ENUMERATED, 0)
                .addParameter("Sweep", ValueTypeEnum.ENUMERATED, 0);
            ds.addHandler("RST", (command, args1, parameterSetter) -> true);
            ds.addHandler("SWP", (command, args1, parameterSetter) -> {
                int times = Integer.parseInt(args1[0]);
                parameterSetter.apply("Sweep", 1);
                for(int i = 0; i < times; ++i) {
                    long offset = ((Number) ds.get("Offset")).longValue();
                    offset += 100L;
                    parameterSetter.apply("Offset", offset);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
                parameterSetter.apply("Sweep", 0);
                return true;
            });
        }
        {
            DeviceSubsystem ds = DEVICE.createSubsystem("SUB2");
            ds.addParameter("Status", ValueTypeEnum.ENUMERATED, 0)
                    .addParameter("Frequency", ValueTypeEnum.UNSIGNED_INTEGER, 3000)
                    .addParameter("Temperature", ValueTypeEnum.REAL, 22.1)
                    .addParameter("Offset", ValueTypeEnum.SIGNED_INTEGER, 0L)
                    .addParameter("Mode", ValueTypeEnum.ENUMERATED, 0)
                    .addParameter("Sweep", ValueTypeEnum.ENUMERATED, 0);
            ds.addHandler("RST", (command, args1, parameterSetter) -> true);
            ds.addHandler("SWP", (command, args1, parameterSetter) -> {
                int times = Integer.parseInt(args1[0]);
                parameterSetter.apply("Sweep", 1);
                for(int i = 0; i < times; ++i) {
                    long offset = ((Number) ds.get("Offset")).longValue();
                    offset += 100L;
                    parameterSetter.apply("Offset", offset);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
                parameterSetter.apply("Sweep", 0);
                return true;
            });
        }
        // Define socket interface
        // TODO
    }
}
