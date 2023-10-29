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
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * This device has a protocol supporting two ASCII connections, one for monitoring (asynch), one for commanding, fully
 * synchronous (request, ack message, execution message)
 * Command messages are:
 * - Request telemetry data for subsystems: REQ [list of device subsystem names separated by space]\n on monitoring connection
 * - Send command to device subsystem: CMD [device subsystem name] [command id] [command code] [arguments]\n on command connection
 * - Set device subsystem value: SET [device subsystem name] [command id] [parameter name] [parameter value]\n on command connection
 * Response messages are:
 * - Telemetry data: TLM [device subsystem name] [list of parameter values separated by space]\n on monitoring connection, 1 per second per device subsystem
 * - Positive ACK of command/set: ACK [device sybsystem name] [command id]\n on command connection
 * - Positive EXE of command/set: EXE [device sybsystem name] [command id]\n on command connection
 * - Negative ACK: NOK [commandId] on command connections for failed ACKs and failed EXE
 */
public class AsciiDeviceDoubleConnection {

    private static final int PORT_TLM = 35212;
    private static final int PORT_CMD = 35213;
    private static final Device DEVICE = new Device("DEVICE2");
    private static volatile PrintStream outputStream;

    public static void main(String[] args) throws IOException {
        // Create the device subsystems
        {
            createSubsystem("SUB1", 10.1);
        }
        {
            createSubsystem("SUB2", 20.2);
        }
        // Define socket interfaces
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT_CMD)) {
                while (server.isBound()) {
                    Socket connection = server.accept();
                    System.out.println("!! Connection received CMD port");
                    // Already a connection: reject new connection
                    if(outputStream != null) {
                        connection.close();
                        continue;
                    }
                    Thread t = new Thread(() -> {
                        try {
                            handleCommandConnection(connection);
                        } catch (IOException e) {
                            outputStream = null;
                            try {
                                connection.close();
                            } catch (IOException ex) {
                                // Nothing
                            }
                            System.out.println("!! Connection CMD closed");
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT_TLM)) {
                while (server.isBound()) {
                    Socket connection = server.accept();
                    System.out.println("!! Connection received TLM port");
                    Thread t = new Thread(() -> {
                        try {
                            handleTelemetryConnection(connection);
                        } catch (IOException e) {
                            try {
                                connection.close();
                            } catch (IOException ex) {
                                // Nothing
                            }
                            System.out.println("!! Connection TLM closed");
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleCommandConnection(Socket connection) throws IOException {
        // Read request, write response
        while(true) {
            outputStream = new PrintStream(connection.getOutputStream());
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String request = readRequest(inputStream);
            System.out.println(new Date() + " >> CMD Received: " + request);
            String response = processCmdRequest(request);
            System.out.println(new Date() + " << CMD Sending: " + response);
            sendOnCommandConnection(response);
        }
    }

    private static void handleTelemetryConnection(Socket connection) throws IOException {
        // Read request, write response
        PrintStream outputStream = new PrintStream(connection.getOutputStream());
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String request = readRequest(inputStream);
        System.out.println(new Date() + " >> TLM Received: " + request);
        String[] subsystems = processTlmRequest(request);
        if(subsystems.length == 0) {
            connection.close();
            return;
        }
        while(true) {
            for(String ss : subsystems) {
                DeviceSubsystem ds = DEVICE.getSubsystem(ss);
                if(ds != null) {
                    Map<String, Object> values = ds.poll();
                    StringBuilder response = new StringBuilder("TLM " + ss + " ");
                    for(Map.Entry<String, Object> e : values.entrySet()) {
                        response.append(ValueUtil.toString(ds.getTypeOf(e.getKey()), e.getValue())).append(" ");
                    }
                    String finalString = response.toString().trim();
                    outputStream.print(finalString + "\n");
                    outputStream.flush();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
                break;
            }
        }
    }

    private static String[] processTlmRequest(String request) {
        if(request.startsWith("REQ ")) {
            return request.trim().substring("REQ ".length()).split(" ", -1);
        } else {
            return new String[0];
        }
    }

    private static String processCmdRequest(String request) {
        String[] tokens = request.split(" ", -1);
        switch (tokens[0]) {
            case "CMD": return commandFor(tokens[1], tokens[2], tokens[3], Arrays.copyOfRange(tokens, 4, tokens.length));
            case "SET": return setFor(tokens[1], tokens[2], tokens[3], tokens[4]);
            default: return nok(null);
        }
    }

    private static String setFor(String subsystem, String commandId, String parameter, String value) {
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return nok(commandId);
        } else {
            try {
                if(ds.set(parameter, ValueUtil.parse(ds.getTypeOf(parameter), value), false, result ->
                        sendOnCommandConnection(result ? String.format("EXE %s %s\n", subsystem, commandId) : nok(commandId)))) {
                    return String.format("ACK %s %s\n", subsystem, commandId);
                } else {
                    return nok(commandId);
                }
            } catch (Exception e) {
                return nok(commandId);
            }
        }
    }

    private static void sendOnCommandConnection(String toSend) {
        synchronized (AsciiDeviceDoubleConnection.class) {
            if(outputStream != null) {
                outputStream.print(toSend);
                outputStream.flush();
            }
        }
    }

    private static String commandFor(String subsystem, String commandId, String command, String[] arguments) {
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return nok(commandId);
        } else {
            try {
                if(ds.invoke(command, arguments, false, result ->
                        sendOnCommandConnection(result ? String.format("EXE %s %s\n", subsystem, commandId) : nok(commandId)))) {
                    return String.format("ACK %s %s\n", subsystem, commandId);
                } else {
                    return nok(commandId);
                }
            } catch (Exception e) {
                return nok(commandId);
            }
        }
    }

    private static String nok(String commandId) {
        return String.format("NOK %s\n", Objects.requireNonNullElse(commandId, "###"));
    }

    private static String readRequest(BufferedReader inputStream) throws IOException {
        return inputStream.readLine();
    }

    private static void createSubsystem(String name, double temperature) {
        DeviceSubsystem ds = DEVICE.createSubsystem(name);
        ds.addParameter("Status", ValueTypeEnum.ENUMERATED, 1)
                .addParameter("Frequency", ValueTypeEnum.UNSIGNED_INTEGER, 3000L)
                .addParameter("Temperature", ValueTypeEnum.REAL, temperature)
                .addParameter("Offset", ValueTypeEnum.SIGNED_INTEGER, 0L)
                .addParameter("Mode", ValueTypeEnum.ENUMERATED, 0)
                .addParameter("Sweep", ValueTypeEnum.ENUMERATED, 0);
        ds.addHandler("RST", (command, args1, parameterSetter, exCompleted) -> {
            parameterSetter.apply("Status", 0);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                if(exCompleted != null) {
                    exCompleted.accept(false);
                }
                return false;
            }
            parameterSetter.apply("Status", 1);
            if(exCompleted != null) {
                exCompleted.accept(true);
            }
            return true;
        });
        ds.addHandler("SWP", (command, args1, parameterSetter, exCompleted) -> {
            int times = Integer.parseInt(args1[0]);
            parameterSetter.apply("Sweep", 1);
            for(int i = 0; i < times; ++i) {
                long offset = ((Number) ds.get("Offset")).longValue();
                offset += 100L;
                parameterSetter.apply("Offset", offset);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    if(exCompleted != null) {
                        exCompleted.accept(false);
                    }
                    return false;
                }
            }
            parameterSetter.apply("Sweep", 0);
            if(exCompleted != null) {
                exCompleted.accept(true);
            }
            return true;
        });
        ds.addHandler("RBT", (command, args1, parameterSetter, exCompleted) -> {
            int delay = Integer.parseInt(args1[0]);
            int running = Integer.parseInt(args1[1]);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                if(exCompleted != null) {
                    exCompleted.accept(false);
                }
                return false;
            }
            int status = (int) ds.get("Status");
            long frequency = (long) ds.get("Frequency");
            double temperature1 = (double) ds.get("Temperature");
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
                if(exCompleted != null) {
                    exCompleted.accept(false);
                }
                return false;
            }
            parameterSetter.apply("Status", status);
            parameterSetter.apply("Frequency", frequency);
            parameterSetter.apply("Temperature", temperature1);
            parameterSetter.apply("Offset", offset);
            parameterSetter.apply("Mode", mode);
            parameterSetter.apply("Sweep", sweep);
            if(exCompleted != null) {
                exCompleted.accept(true);
            }
            return true;
        });
    }
}
