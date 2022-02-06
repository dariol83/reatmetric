/*
 * Copyright (c)  2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.serial.tool;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.driver.serial.protocol.IMonitoringDataManager;
import eu.dariolucia.reatmetric.driver.serial.protocol.ProtocolManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class ProtocolTest {

    public static void main(String[] args) throws InterruptedException {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort sp : ports) {
            System.out.println("Available serial port: " + sp.getSystemPortName() + " -> " + sp);
        }
        if(args.length != 5) {
            System.err.println("Usage: Terminal <interface name> <baudrate> <data bits> <stop bits> <parity: 0,1,2>");
            System.exit(1);
        }
        System.out.println("Arguments: " + Arrays.toString(args));
        SerialPort comPort = SerialPort.getCommPort(args[0]);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000000, Integer.MAX_VALUE);

        comPort.setBaudRate(Integer.parseInt(args[1]));
        comPort.setParity(Integer.parseInt(args[4]));
        comPort.setNumDataBits(Integer.parseInt(args[2]));
        comPort.setNumStopBits(Integer.parseInt(args[3]));
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        comPort.openPort();

        InputStream is = comPort.getInputStream();
        OutputStream os = comPort.getOutputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        ProtocolManager manager = new ProtocolManager(new IMonitoringDataManager() {
            @Override
            public int registerParameter(String parameterPath) {
                System.out.println("Registering parameter " + parameterPath);
                return 0;
            }

            @Override
            public boolean deregisterParameter(int parameterId) {
                System.out.println("Deregistering parameter " + parameterId);
                return true;
            }

            @Override
            public void deregisterAllParameter() {
                System.out.println("Deregistering all parameters");
            }

            @Override
            public List<ParameterData> updateParameters() {
                return Arrays.asList(
                    new ParameterData(new LongUniqueId(200), Instant.now(), 300, "TEST1", SystemEntityPath.fromString("PATH.TEST1"),
                          21.2, 21.2, "Route1", Validity.VALID, AlarmState.WARNING,
                          null, Instant.now(), null),
                    new ParameterData(new LongUniqueId(201), Instant.now(), 300, "TEST2", SystemEntityPath.fromString("PATH.TEST2"),
                            123, 123, "Route1", Validity.VALID, AlarmState.ALARM,
                            null, Instant.now(), null),
                    new ParameterData(new LongUniqueId(202), Instant.now(), 300, "TEST3", SystemEntityPath.fromString("PATH.TEST3"),
                            "My value string", 12, "Route1", Validity.INVALID, AlarmState.NOT_APPLICABLE,
                            null, Instant.now(), null)
                );
            }

            @Override
            public List<OperationalMessage> updateLogs() {
                return Arrays.asList(
                        new OperationalMessage(new LongUniqueId(123), Instant.now(), "ID1", "This is an event that is very very long", "SRC1", Severity.ALARM, null, null),
                        new OperationalMessage(new LongUniqueId(124), Instant.now(), "ID2", "Second event that is very very long", "SRC1", Severity.WARN, null, null));
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        String data = br.readLine();
                        System.out.println(data);
                        byte[] toSend = manager.event(data);
                        os.write(toSend);
                        os.flush();
                    } catch (IOException e) {
                        System.out.println("Last error code: " + comPort.getLastErrorCode() + ", " + comPort.getLastErrorLocation());
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        while(true) {
            Thread.sleep(1000);
        }
    }
}
