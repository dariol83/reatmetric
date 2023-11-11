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
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

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
 *
 * Unknown message NOK
 * - Header (length is 24 bytes)
 * - Subsystem ID 0xFFFFFFFF
 * - Operation (0xFFFFFFFF)
 * - Tail
 */
public class BinaryDeviceDoubleConnection {

    private static final int PORT_TLM = 37212;
    private static final int PORT_CMD = 37213;
    private static final Device DEVICE = new Device("DEVICE3");
    public static final int SET_OPERATION = 0x0000000A;
    public static final int CMD_OPERATION = 0x0000000B;
    public static final int ACK_NEGATIVE = 0xFFFFFFF2;
    public static final int ACK_POSITIVE = 0x80000002;
    public static final int EXE_NEGATIVE = 0xFFFFFFF3;
    public static final int EXE_POSITIVE = 0x80000003;
    private static volatile OutputStream outputStream;

    private static final Map<Integer, String> COMMAND_MAP = Map.of(1, "RST", 2, "SWP", 3, "RBT");

    public static void main(String[] args) {
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

    private static byte[] processTlmRequest(byte[] request) throws IOException {
        if(!validate(request)) {
            return nok();
        }
        // Read the request
        ByteBuffer bb = ByteBuffer.wrap(request);
        bb.getLong();
        bb.getInt();
        int subsystem = bb.getInt();
        String subSystemStr = "SUB" + subsystem;
        int operation = bb.getInt();
        if(operation != 0x00000001) {
            return reply(subsystem, 0x8FFFFFF1);
        } else {
            DeviceSubsystem ds = DEVICE.getSubsystem(subSystemStr);
            if(ds == null) {
                return reply(subsystem, 0x8FFFFFF1);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ds.encodeStateTo(bos);
            return reply(subsystem, 0x80000001, bos.toByteArray());
        }
    }

    private static byte[] reply(int subsystem, int operation) {
        return reply(subsystem, operation, new byte[0]);
    }

    private static byte[] reply(int subsystem, int operation, byte[] body) {
        byte[] reply = new byte[24 + body.length];
        ByteBuffer bb = ByteBuffer.wrap(reply);
        bb.put(new byte[] { 0x52, 0x45, 0x41, 0x54 });
        bb.putInt(24 + body.length);
        bb.putInt(0x0000FFFF);
        bb.putInt(subsystem);
        bb.putInt(operation);
        bb.put(body);
        bb.put(new byte[] { 0x4D, 0x45, 0x54, 0x52 });
        return reply;
    }

    private static byte[] processCmdRequest(byte[] request) {
        if(!validate(request)) {
            return nok();
        }
        // Read the request
        ByteBuffer bb = ByteBuffer.wrap(request);
        bb.getLong();
        bb.getInt();
        int subsystem = bb.getInt();
        String subSystemStr = "SUB" + subsystem;
        int operation = bb.getInt();
        switch(operation) {
            case SET_OPERATION:
            {
                return setFor(subsystem, bb);
            }
            case CMD_OPERATION:
            {
                return commandFor(subsystem, bb);
            }
            default:  return ackNegative(subsystem); // ACK negative
        }
    }

    private static byte[] ackNegative(int subsystem) {
        return reply(subsystem, ACK_NEGATIVE);
    }

    private static byte[] ackPositive(int subsystem) {
        return reply(subsystem, ACK_POSITIVE);
    }

    private static byte[] exeNegative(int subsystem) {
        return reply(subsystem, EXE_NEGATIVE);
    }

    private static byte[] exePositive(int subsystem) {
        return reply(subsystem, EXE_POSITIVE);
    }

    private static byte[] commandFor(int subsystemId, ByteBuffer bb) {
        String subsystem = "SUB" + subsystemId;
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return ackNegative(subsystemId);
        } else {
            // read command
            int commandId = bb.getInt();
            String command = COMMAND_MAP.get(commandId);
            if(command == null) {
                return ackNegative(subsystemId);
            }
            switch(command) {
                case "RST": // No argument
                    return runCommand(subsystemId, ds, command, new String[0]);
                case "SWP":
                    return runCommand(subsystemId, ds, command, new String[] { String.valueOf(bb.getInt()) });
                case "RBT":
                    return runCommand(subsystemId, ds, command, new String[] { String.valueOf(bb.getInt()), String.valueOf(bb.getInt()) });
                default:
                    return ackNegative(subsystemId);
            }
        }
    }

    private static byte[] setFor(int subsystemId, ByteBuffer bb) {
        String subsystem = "SUB" + subsystemId;
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return ackNegative(subsystemId);
        } else {
            int parameterId = bb.getInt();
            Object valueToSet = ds.decodeValueFrom(parameterId, bb);
            try {
                if(ds.set(parameterId, valueToSet, false, result ->
                        sendOnCommandConnection(result ? exePositive(subsystemId) : exeNegative(subsystemId)))) {
                    return ackPositive(subsystemId);
                } else {
                    return ackNegative(subsystemId);
                }
            } catch (Exception e) {
                return ackNegative(subsystemId);
            }
        }
    }

    private static byte[] runCommand(int subsystemId, DeviceSubsystem ds, String command, String[] arguments) {
        try {
            if (ds.invoke(command, arguments, false, result ->
                    sendOnCommandConnection(result ? exePositive(subsystemId) : exeNegative(subsystemId)))) {
                return ackPositive(subsystemId);
            } else {
                return ackNegative(subsystemId);
            }
        } catch (Exception e) {
            return ackNegative(subsystemId);
        }
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
        return readMessage(inputStream);
    }

    private static byte[] readMessage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buff = inputStream.readNBytes(4); // REAT
        byte[] lenArray = inputStream.readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(lenArray);
        int length = bb.getInt();
        byte[] rest = inputStream.readNBytes(length - 8);
        bos.write(buff);
        bos.write(lenArray);
        bos.write(rest);
        return bos.toByteArray();
    }

    private static boolean validate(byte[] message) {
        ByteBuffer bb = ByteBuffer.wrap(message);
        // Read first 4 bytes as String
        byte[] headPrefix = new byte[4];
        bb.get(headPrefix);
        String headPrefixStr = new String(headPrefix, StandardCharsets.US_ASCII);
        if(!headPrefixStr.equals("REAT")) {
            return false;
        }
        // Read length
        int length = bb.getInt();
        if(length != message.length) {
            return false;
        }
        // Read filler
        int filler = bb.getInt();
        if(filler != 0xFFFF0000) {
            return false;
        }
        // Check the last 4 bytes
        bb = ByteBuffer.wrap(message, message.length - 4, 4);
        byte[] tailPrefix = new byte[4];
        bb.get(tailPrefix);
        String tailPrefixStr = new String(tailPrefix, StandardCharsets.US_ASCII);
        return tailPrefixStr.equals("METR");
    }

    private static byte[] nok() {
        return new byte[] {
        0x52, 0x45, 0x41, 0x54, // REAT
        0x00, 0x00, 0x00, 0x18, // 24
        0x00, 0x00, (byte) 0xFF, (byte) 0xFF, // 00 00 FF FF
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        0x4D, 0x45, 0x54, 0x52 // METR
        };
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
