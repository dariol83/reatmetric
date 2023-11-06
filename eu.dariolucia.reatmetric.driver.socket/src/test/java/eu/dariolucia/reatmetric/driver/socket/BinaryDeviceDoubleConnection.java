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

import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * This device has a protocol supporting two binary connections, one for monitoring, one for commanding, both fully
 * synchronous (request, response on telemetry link; request, ack, response on command link).
 * Command messages are:
 * - Poll monitoring values of a given device subsystem on monitor connection
 * - Send command to device subsystem on command connection
 * - Set device subsystem value on command connection
 * Response messages are:
 * - Telemetry data on monitor connection
 * - Positive ACK of command/set on command connection
 * - Negative ACK on command and monitor connections

 * Each data binary message has the following structure
 * - Header composed by REAT in ASCII bytes (4 bytes), total length of the message in bytes (4 bytes big-endian), 2 bytes set to 00,
 * 2 bytes set to FF
 * - Subsystem ID (4 bytes big-endian)
 * - Operation (4 bytes big-endian)
 * - Then the list of other fields/parameters, so encoded: integer 4 bytes big-endian, long 8 bytes big-endian, double 8 bytes big-endian, string as
 * 4 bytes big-endian for the length in bytes, then the string with padding with 0 bytes at the end to reach alignment to 4 bytes
 * - Tail composed by METR in ASCII bytes (4 bytes)

 * Poll request (TLM connection)
 * - Header (length set to 24 bytes)
 * - Subsystem ID
 * - Operation (0x00000001)
 * - Tail

 * Poll response (TLM connection - positive)
 * - Header
 * - Subsystem ID
 * - Operation (0x80000001)
 * - List of parameters as per subsystem
 * - Tail
 * Poll response (TLM connection - negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF1)
 * - Tail

 * Set parameter request (CMD connection)
 * - Header
 * - Subsystem ID
 * - Operation (0x0000000A)
 * - Parameter Index to set (4 bytes)
 * - Parameter value (according to the parameter type)
 * - Tail

 * Command parameter request (CMD connection)
 * - Header
 * - Subsystem ID
 * - Operation (0x0000000B)
 * - Command ID (4 bytes)
 * - List of argument values (according to the command ID)
 * - Tail

 * ACK response (CMD connection - positive)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0x80000002)
 * - Tail
 * ACK response (CMD connection - negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF2)
 * - Tail
 * EXE response (CMD connection - positive)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0x80000003)
 * - Tail
 * EXE response (CMD connection - negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF3)
 * - Tail
 */
public class BinaryDeviceDoubleConnection {

    private static final int PORT_TLM = 37212;
    private static final int PORT_CMD = 37213;
    private static final Device DEVICE = new Device("DEVICE3");
    private static volatile OutputStream outputStream;

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
            outputStream = connection.getOutputStream();
            InputStream inputStream = connection.getInputStream();
            byte[] request = readRequest(inputStream);
            if(request == null) {
                throw new IOException("Connection closed");
            }
            System.out.println(new Date() + " >> CMD Received: " + StringUtil.toHexDump(request));
            byte[] response = processCmdRequest(request);
            sendOnCommandConnection(response);
        }
    }

    private static void handleTelemetryConnection(Socket connection) throws IOException {
        // Read request, write response
        OutputStream outputStream = connection.getOutputStream();
        InputStream inputStream = connection.getInputStream();
        while(true) {
            byte[] request = readRequest(inputStream);
            System.out.println(new Date() + " >> TLM Received: " + StringUtil.toHexDump(request));
            byte[] response = processTlmRequest(request);
            outputStream.write(response);
        }
    }

    private static byte[] processTlmRequest(byte[] request) {
        // TODO:
        return null;
    }

    private static byte[] processCmdRequest(byte[] request) {
        // TODO: Do the processing
        return null;
    }

    private static void sendOnCommandConnection(byte[] toSend) {
        synchronized (AsciiDeviceDoubleConnection.class) {
            if(outputStream != null) {
                System.out.println(new Date() + " << CMD Sending: " + StringUtil.toHexDump(toSend));
                try {
                    outputStream.write(toSend);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static byte[] readRequest(InputStream inputStream) throws IOException {
        // TODO: read a message
        return null;
    }

    private static void createSubsystem(String name, double temperature) {
        DeviceSubsystem ds = DEVICE.createSubsystem(name);
        ds.addParameter("Status", ValueTypeEnum.ENUMERATED, 1)
                .addParameter("Frequency", ValueTypeEnum.UNSIGNED_INTEGER, 3000L)
                .addParameter("Temperature", ValueTypeEnum.REAL, temperature)
                .addParameter("Offset", ValueTypeEnum.SIGNED_INTEGER, 0L)
                .addParameter("Mode", ValueTypeEnum.ENUMERATED, 0)
                .addParameter("Summary", ValueTypeEnum.CHARACTER_STRING, "Nominal")
                .addParameter("Sweep", ValueTypeEnum.ENUMERATED, 0);
        ds.addHandler("RST", (command, args1, parameterSetter, exCompleted) -> {
            parameterSetter.apply("Status", 0);
            parameterSetter.apply("Summary", "Reset");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                if(exCompleted != null) {
                    exCompleted.accept(false);
                }
                return false;
            }
            parameterSetter.apply("Status", 1);
            parameterSetter.apply("Summary", "Nominal");
            if(exCompleted != null) {
                exCompleted.accept(true);
            }
            return true;
        });
        ds.addHandler("SWP", (command, args1, parameterSetter, exCompleted) -> {
            int times = Integer.parseInt(args1[0]);
            parameterSetter.apply("Sweep", 1);
            parameterSetter.apply("Summary", "Sweeping in progress");
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
            parameterSetter.apply("Summary", "Nominal");
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
            parameterSetter.apply("Summary", "Rebooting");
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
            parameterSetter.apply("Summary", "Rebooted");
            parameterSetter.apply("Sweep", sweep);
            if(exCompleted != null) {
                exCompleted.accept(true);
            }
            return true;
        });
    }
}
