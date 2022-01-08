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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TerminalFileSender {

    public static void main(String[] args) {
        if(args.length != 2) {
            System.err.println("Usage: TerminalFileSender <interface name> <path to file>");
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort sp : ports) {
                System.out.println("Available serial port: " + sp);
            }
            System.exit(1);
        }

        SerialPort comPort = SerialPort.getCommPort(args[0]);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 10000, 0);

        comPort.setBaudRate(4800);
        comPort.setParity(SerialPort.EVEN_PARITY);
        comPort.setNumDataBits(7);
        comPort.setNumStopBits(1);
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);

        comPort.openPort();

        try {
            // Open file
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[1])));
            String line = null;
            while((line = br.readLine()) != null) {
                comPort.writeBytes(line.getBytes(StandardCharsets.US_ASCII), line.length());
                comPort.writeBytes(new byte[] { 0x0D, 0x0A }, 2);
            }
            System.out.println("File " + args[1] + " sent to serial device " + args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            comPort.closePort();
        }
    }
}
