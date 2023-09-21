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
            createSubsystemA("SUB1");
        }
        {
            createSubsystemA("SUB2");
        }
        // Define socket interface
        // TODO
    }

    private static void createSubsystemA(String name) {
        DeviceSubsystem ds = DEVICE.createSubsystem(name);
        ds.addParameter("Status", ValueTypeEnum.ENUMERATED, 1)
            .addParameter("Frequency", ValueTypeEnum.UNSIGNED_INTEGER, 3000L)
            .addParameter("Temperature", ValueTypeEnum.REAL, 22.1)
            .addParameter("Offset", ValueTypeEnum.SIGNED_INTEGER, 0L)
            .addParameter("Mode", ValueTypeEnum.ENUMERATED, 0)
            .addParameter("Sweep", ValueTypeEnum.ENUMERATED, 0);
        ds.addHandler("RST", (command, args1, parameterSetter) -> {
            parameterSetter.apply("Status", 0);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }
            parameterSetter.apply("Status", 1);
            return true;
        });
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
        ds.addHandler("RBT", (command, args1, parameterSetter) -> {
            int delay = Integer.parseInt(args1[0]);
            int running = Integer.parseInt(args1[0]);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return false;
            }
            int status = (int) ds.get("Status");
            long frequency = (long) ds.get("Frequency");
            double temperature = (double) ds.get("Temperature");
            long offset = (long) ds.get("Offset");
            int mode = (int) ds.get("Mode");
            int sweep = (int) ds.get("Sweep");
            parameterSetter.apply("Status", 0);
            parameterSetter.apply("Frequency", 0L);
            parameterSetter.apply("Temperature", 0.0);
            parameterSetter.apply("Offset", 0L);
            parameterSetter.apply("Mode", 0);
            parameterSetter.apply("Sweep", 0);
            try {
                Thread.sleep(running);
            } catch (InterruptedException e) {
                return false;
            }
            parameterSetter.apply("Status", status);
            parameterSetter.apply("Frequency", frequency);
            parameterSetter.apply("Temperature", temperature);
            parameterSetter.apply("Offset", offset);
            parameterSetter.apply("Mode", mode);
            parameterSetter.apply("Sweep", sweep);
            return true;
        });
    }
}
