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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * This device has a protocol supporting one binary connection, fully asynchronous.

 * Command messages are:
 * - Register for monitoring values of a given device subsystem (upon connection)
 * - Register for events (upon connection)
 * - Send command to device subsystem
 * - Set device subsystem value
 * Response messages are:
 * - Telemetry data
 * - Event messages
 * - Positive ACK of command/set
 * - Negative ACK

 * Each data binary message has the following structure
 * - Header composed by REAT in ASCII bytes (4 bytes), total length of the message in bytes (4 bytes big-endian)
 * - Subsystem ID (4 bytes big-endian)
 * - Operation (4 bytes big-endian)
 * - Then the list of other fields/parameters, so encoded: integer 4 bytes big-endian, long 8 bytes big-endian, double 8 bytes big-endian, string as
 * 4 bytes big-endian for the length in bytes, then the string (no padding at the end) in US ASCII encoding
 * - Tail composed by METR in ASCII bytes (4 bytes)

 * Parameter registration request
 * - Header (length set to 24 bytes)
 * - Subsystem ID
 * - Operation (0x00000001)
 * - Request counter (4 bytes)
 * - Tail

 * Parameter update
 * - Header
 * - Subsystem ID
 * - Operation (0x80000001)
 * - Request counter (4 bytes)
 * - List of parameters as per subsystem
 * - Tail

 * Parameter registration response (negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF1)
 * - Request counter (4 bytes)
 * - Tail

 * Event registration request
 * - Header (length set to 24 bytes)
 * - Subsystem ID
 * - Operation (0x00000004)
 * - Request counter (4 bytes)
 * - Tail

 * Event generation
 * - Header
 * - Subsystem ID
 * - Operation (0x80000004)
 * - Request counter (4 bytes)
 * - Severity (4 bytes)
 * - String of the event (4 bytes plus string encoded as US ASCII)
 * - Tail

 * Event registration response (negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF4)
 * - Request counter (4 bytes)
 * - Tail

 * Set parameter request (CMD connection)
 * - Header
 * - Subsystem ID
 * - Operation (0x0000000A)
 * - Request counter (4 bytes)
 * - Parameter Index to set (4 bytes)
 * - Parameter value (according to the parameter type)
 * - Tail

 * Command parameter request (CMD connection)
 * - Header
 * - Subsystem ID
 * - Operation (0x0000000B)
 * - Request counter (4 bytes)
 * - Command ID (4 bytes)
 * - List of argument values (according to the command ID)
 * - Tail

 * ACK response (CMD connection - positive)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0x80000002)
 * - Request counter (4 bytes)
 * - Tail
 * ACK response (CMD connection - negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF2)
 * - Request counter (4 bytes)
 * - Tail
 * EXE response (CMD connection - positive)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0x80000003)
 * - Request counter (4 bytes)
 * - Tail
 * EXE response (CMD connection - negative)
 * - Header (length is 24 bytes)
 * - Subsystem ID
 * - Operation (0xFFFFFFF3)
 * - Request counter (4 bytes)
 * - Tail
 *
 * Unknown message NOK
 * - Header (length is 24 bytes)
 * - Subsystem ID 0xFFFFFFFF
 * - Operation (0xFFFFFFFF)
 * - Request counter (4 bytes)
 * - Tail
 */
public class BinaryDeviceSingleConnection {

    private static final int PORT_CMD = 37280;
    private static final Device DEVICE = new Device("DEVICE4");

    public static final int PARAM_REG_OPERATION = 0x00000001;
    public static final int PARAM_POSITIVE_OPERATION = 0x80000001;
    public static final int PARAM_NEGATIVE_OPERATION = 0xFFFFFFF1;

    public static final int EVENT_REG_OPERATION = 0x00000004;
    public static final int EVENT_POSITIVE_OPERATION = 0x80000004;
    public static final int EVENT_NEGATIVE_OPERATION = 0xFFFFFFF4;
    public static final int SET_OPERATION = 0x0000000A;
    public static final int CMD_OPERATION = 0x0000000B;
    public static final int ACK_NEGATIVE = 0xFFFFFFF2;
    public static final int ACK_POSITIVE = 0x80000002;
    public static final int EXE_NEGATIVE = 0xFFFFFFF3;
    public static final int EXE_POSITIVE = 0x80000003;

    private static final Map<Integer, String> COMMAND_MAP = Map.of(1, "RST", 2, "SWP", 3, "RBT");

    public static void main(String[] args) {
        // Create the device subsystems
        {
            createSubsystem("SUB1", 33.4);
        }
        {
            createSubsystem("SUB2", 59.321);
        }
        // Define socket interfaces
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT_CMD)) {
                while (server.isBound()) {
                    Socket connection = server.accept();
                    System.out.println("!! Connection received");
                    Thread t = new Thread(() -> {
                        try {
                            handleCommandConnection(connection);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleCommandConnection(Socket connection) throws IOException {
        // Read request, write response
        while(true) {
            InputStream inputStream = connection.getInputStream();
            byte[] request = readRequest(inputStream);
            System.out.println(new Date() + " >> Received: " + StringUtil.toHexDump(request));
            byte[] response = processCmdRequest(connection, request);
            sendOnCommandConnection(connection, response);
        }
    }

    private static byte[] reply(int subsystem, int operation, int requestId) {
        return reply(subsystem, operation, requestId, new byte[0]);
    }

    private static byte[] reply(int subsystem, int operation, int requestId, byte[] body) {
        byte[] reply = new byte[24 + body.length];
        ByteBuffer bb = ByteBuffer.wrap(reply);
        bb.put(new byte[] { 0x52, 0x45, 0x41, 0x54 });
        bb.putInt(24 + body.length);
        bb.putInt(subsystem);
        bb.putInt(operation);
        bb.putInt(requestId);
        bb.put(body);
        bb.put(new byte[] { 0x4D, 0x45, 0x54, 0x52 });
        return reply;
    }

    private static byte[] processCmdRequest(Socket connection, byte[] request) {
        if(!validate(request)) {
            return nok(0xFFFFFFFF);
        }
        // Read the request
        ByteBuffer bb = ByteBuffer.wrap(request);
        bb.getLong(); // Header and length
        int subsystem = bb.getInt(); // SubID
        int operation = bb.getInt(); // Operation
        int requestId = bb.getInt(); // Request
        switch(operation) {
            case PARAM_REG_OPERATION:
            {
                return paramFor(connection, subsystem, requestId);
            }
            case EVENT_REG_OPERATION:
            {
                return eventFor(connection, subsystem, requestId);
            }
            case SET_OPERATION:
            {
                return setFor(connection, subsystem, requestId,  bb);
            }
            case CMD_OPERATION:
            {
                return commandFor(connection, subsystem, requestId,  bb);
            }
            default:  return nok(requestId); // NOK
        }
    }

    private static byte[] paramFor(Socket connection, int subsystem, int requestId) {
        String subSystemStr = "SUB" + subsystem;
        DeviceSubsystem ds = DEVICE.getSubsystem(subSystemStr);
        if(ds == null) {
            return reply(subsystem, PARAM_NEGATIVE_OPERATION, requestId);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ds.encodeStateTo(bos, false);
            // Schedule send every 3 seconds
            new Thread(() -> scheduleParamUpdate(connection, subsystem, requestId)).start();
            return reply(subsystem, PARAM_POSITIVE_OPERATION, requestId, bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return reply(subsystem, PARAM_NEGATIVE_OPERATION, requestId);
        }
    }

    private static void scheduleParamUpdate(Socket connection, int subsystem, int requestId) {
        while(true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return;
            }
            String subSystemStr = "SUB" + subsystem;
            DeviceSubsystem ds = DEVICE.getSubsystem(subSystemStr);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ds.encodeStateTo(bos, false);
                if(!sendOnCommandConnection(connection, reply(subsystem, PARAM_POSITIVE_OPERATION, requestId, bos.toByteArray()))) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private static byte[] eventFor(Socket connection, int subsystem, int requestId) {
        String subSystemStr = "SUB" + subsystem;
        DeviceSubsystem ds = DEVICE.getSubsystem(subSystemStr);
        if(ds == null) {
            return reply(subsystem, requestId, EVENT_NEGATIVE_OPERATION);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(new byte[] { 00, 00, 00, 01 }); // INFO
            String eventMessage = "Registration OK on subsystem " + subsystem;
            bos.write(eventMessage.getBytes(StandardCharsets.US_ASCII));
            // Schedule random event generation
            new Thread(() -> scheduleEventUpdate(connection, subsystem, requestId)).start();
            return reply(subsystem, EVENT_POSITIVE_OPERATION, requestId, bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return reply(subsystem, EVENT_NEGATIVE_OPERATION, requestId);
        }
    }

    private static void scheduleEventUpdate(Socket connection, int subsystem, int requestId) {
        while(true) {
            try {
                Thread.sleep(10000 + (long) (20000 * Math.random()));
            } catch (InterruptedException e) {
                return;
            }
            String subSystemStr = "SUB" + subsystem;
            DeviceSubsystem ds = DEVICE.getSubsystem(subSystemStr);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                bos.write(new byte[] { 00, 00, 00, (byte) Math.floor(Math.random() * 4) }); // 0: debug 1: INFO 2: WARN 3: ALARM
                String eventMessage = "Random event on subsystem " + subsystem;
                bos.write(eventMessage.getBytes(StandardCharsets.US_ASCII));
                if(!sendOnCommandConnection(connection, reply(subsystem, EVENT_POSITIVE_OPERATION, requestId, bos.toByteArray()))) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }


    private static byte[] ackNegative(int subsystem, int requestId) {
        return reply(subsystem, requestId, ACK_NEGATIVE);
    }

    private static byte[] ackPositive(int subsystem, int requestId) {
        return reply(subsystem, requestId, ACK_POSITIVE);
    }

    private static byte[] exeNegative(int subsystem, int requestId) {
        return reply(subsystem, requestId, EXE_NEGATIVE);
    }

    private static byte[] exePositive(int subsystem, int requestId) {
        return reply(subsystem, requestId, EXE_POSITIVE);
    }

    private static byte[] commandFor(Socket connection, int subsystemId, int requestId, ByteBuffer bb) {
        String subsystem = "SUB" + subsystemId;
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return ackNegative(subsystemId, requestId);
        } else {
            // read command
            int commandId = bb.getInt();
            String command = COMMAND_MAP.get(commandId);
            if(command == null) {
                return ackNegative(subsystemId, requestId);
            }
            switch(command) {
                case "RST": // No argument
                    return runCommand(connection, subsystemId, requestId, ds, command, new String[0]);
                case "SWP":
                    return runCommand(connection, subsystemId, requestId, ds, command, new String[] { String.valueOf(bb.getInt()) });
                case "RBT":
                    return runCommand(connection, subsystemId, requestId, ds, command, new String[] { String.valueOf(bb.getInt()), String.valueOf(bb.getInt()) });
                default:
                    return ackNegative(subsystemId, requestId);
            }
        }
    }

    private static byte[] setFor(Socket connection, int subsystemId, int requestId, ByteBuffer bb) {
        String subsystem = "SUB" + subsystemId;
        DeviceSubsystem ds = DEVICE.getSubsystem(subsystem);
        if(ds == null) {
            return ackNegative(subsystemId, requestId);
        } else {
            int parameterId = bb.getInt();
            Object valueToSet = ds.decodeValueFrom(parameterId, bb);
            try {
                if(ds.set(parameterId, valueToSet, false, result ->
                        sendOnCommandConnection(connection, result ? exePositive(subsystemId, requestId) : exeNegative(subsystemId, requestId)))) {
                    return ackPositive(subsystemId, requestId);
                } else {
                    return ackNegative(subsystemId, requestId);
                }
            } catch (Exception e) {
                return ackNegative(subsystemId, requestId);
            }
        }
    }

    private static byte[] runCommand(Socket connection, int subsystemId, int requestId, DeviceSubsystem ds, String command, String[] arguments) {
        try {
            if (ds.invoke(command, arguments, false, result ->
                    sendOnCommandConnection(connection, result ? exePositive(subsystemId, requestId) : exeNegative(subsystemId, requestId)))) {
                return ackPositive(subsystemId, requestId);
            } else {
                return ackNegative(subsystemId, requestId);
            }
        } catch (Exception e) {
            return ackNegative(subsystemId, requestId);
        }
    }

    private static boolean sendOnCommandConnection(Socket connection, byte[] toSend) {
        synchronized (AsciiDeviceDoubleConnection.class) {
            try {
                connection.getOutputStream().write(toSend);
                connection.getOutputStream().flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private static byte[] readRequest(InputStream inputStream) throws IOException {
        return readMessage(inputStream);
    }

    private static byte[] readMessage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buff = inputStream.readNBytes(4); // REAT
        if(buff.length == 0) {
            throw new IOException("End of stream");
        }
        byte[] lenArray = inputStream.readNBytes(4);
        if(lenArray.length == 0) {
            throw new IOException("End of stream");
        }
        ByteBuffer bb = ByteBuffer.wrap(lenArray);
        int length = bb.getInt();
        byte[] rest = inputStream.readNBytes(length - 8);
        if(rest.length == 0) {
            throw new IOException("End of stream");
        }
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
        // Check the last 4 bytes
        bb = ByteBuffer.wrap(message, message.length - 4, 4);
        byte[] tailPrefix = new byte[4];
        bb.get(tailPrefix);
        String tailPrefixStr = new String(tailPrefix, StandardCharsets.US_ASCII);
        return tailPrefixStr.equals("METR");
    }

    private static byte[] nok(int requestId) {
        return reply(0xFFFFFFFF, 0xFFFFFFFF, requestId);
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
