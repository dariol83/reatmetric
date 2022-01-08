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
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Terminal {

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
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 5000000, Integer.MAX_VALUE);

        comPort.setBaudRate(Integer.parseInt(args[1]));
        comPort.setParity(Integer.parseInt(args[4]));
        comPort.setNumDataBits(Integer.parseInt(args[2]));
        comPort.setNumStopBits(Integer.parseInt(args[3]));
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        comPort.openPort();

        InputStream is = comPort.getInputStream();
        OutputStream os = comPort.getOutputStream();

        Thread.sleep(1000);

        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                try {
                    byte[] buffer = new byte[1024];
                    int read = is.read(buffer);
                    String data = new String(buffer, 0, read);
                    for(int i = 0; i < data.length(); ++i) {
                        char c = data.charAt(i);
                        System.out.print(c);
                        if(c == 0x0D) {
                            System.out.print((char) 0x0A);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Last error code: " + comPort.getLastErrorCode() + ", " + comPort.getLastErrorLocation());
                    e.printStackTrace();
                }
            }
        });
        try {
            // Read from keyboard and send on Enter
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line = null;
            while((line = br.readLine()) != null) {
                byte[] toSend = line.getBytes(StandardCharsets.US_ASCII);
                System.out.println("Sending " + toSend.length + " bytes: " + Arrays.toString(toSend));
                os.write(toSend);
                os.write(new byte[] { 0x0D, 0x0A });
                os.flush();
                System.out.println("Sending done");
            }
        } catch (Exception e) {
            System.out.println("Last error code: " + comPort.getLastErrorCode() + ", " + comPort.getLastErrorLocation());
            e.printStackTrace();
        } finally {
            comPort.closePort();
        }
    }
}
