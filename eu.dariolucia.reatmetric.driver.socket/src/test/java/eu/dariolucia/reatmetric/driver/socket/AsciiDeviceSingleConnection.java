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
import java.util.Map;

/**
 * This device has a protocol supporting a single ASCII connection, fully synchronous (request, response)
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

    public static void main(String[] args) throws IOException {
        // Create the device subsystems
        {
            createSubsystemA("SUB1");
        }
        {
            createSubsystemA("SUB2");
        }
        // Define socket interface
        try (ServerSocket server = new ServerSocket(34212)) {
            while (server.isBound()) {
                Socket connection = server.accept();
                System.out.println("!! Connection received");
                Thread t = new Thread(() -> {
                    try {
                        handleConnection(connection);
                    } catch (IOException e) {
                        try {
                            connection.close();
                        } catch (IOException ex) {
                            // Nothing
                        }
                        System.out.println("!! Connection closed");
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private static void handleConnection(Socket connection) throws IOException {
        // Read request, write response
        while(true) {
            PrintStream outputStream = new PrintStream(connection.getOutputStream());
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String request = readRequest(inputStream);
            System.out.println(">> Received: " + request);
            String response = processRequest(request);
            System.out.println("<< Sending: " + request);
            outputStream.print(response);
            outputStream.flush();
        }
    }

    private static String processRequest(String request) {
        // Remove the { and }
        request = request.substring(1, request.length() - 1);
        String[] tokens = request.split(",", -1);
        switch (tokens[0]) {
            case "ST": return pollFor(tokens[1]);
            case "CMD": return commandFor(tokens[1], tokens[2], Arrays.copyOfRange(tokens, 3, tokens.length));
            case "SET": return setFor(tokens[1], tokens[2], tokens[3]);
            default: return nok();
        }
    }

    private static String setFor(String subsystem, String parameter, String value) {
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return nok();
        } else {
            try {
                ds.set(parameter, ValueUtil.parse(ds.getTypeOf(parameter), value), true);
                return String.format("{ACK,%s}", subsystem);
            } catch (Exception e) {
                return nok();
            }
        }
    }

    private static String commandFor(String subsystem, String command, String[] arguments) {
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return nok();
        } else {
            try {
                ds.invoke(command, arguments, true);
                return String.format("{ACK,%s}", subsystem);
            } catch (Exception e) {
                return nok();
            }
        }
    }

    private static String pollFor(String subsystem) {
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return nok();
        } else {
            Map<String, Object> values = ds.poll();
            StringBuilder response = new StringBuilder("{TLM");
            for(Map.Entry<String, Object> e : values.entrySet()) {
                response.append(",").append(ValueUtil.toString(ds.getTypeOf(e.getKey()), e.getValue()));
            }
            response.append("}");
            return response.toString();
        }
    }

    private static String nok() {
        return "{NOK}";
    }

    private static String readRequest(BufferedReader inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int nbCharRead = 0;
        boolean finished = false;
        while(!finished) {
            // Read char by char
            int readChar = inputStream.read();
            if (readChar == -1) {
                throw new IOException("End of stream");
            }
            char theChar = (char) readChar;
            sb.append(theChar);
            if(nbCharRead == 0 && theChar != '{') {
                // First char must be {, error
                throw new IOException("Wrong start of request");
            }
            ++nbCharRead;
            if(theChar == '}') {
                // Request is over
                finished = true;
            }
        }
        return sb.toString();
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
