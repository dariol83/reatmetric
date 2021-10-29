/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.junit.jupiter.api.Test;

class SerialDriver2Test {

    private static final String NAME = "Serial0";

    @Test
    public void testSerial() {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort sp : ports) {
            System.out.println("Serial port: " + sp);
        }

        SerialPort comPort = SerialPort.getCommPort(NAME);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 10000, 0);
        try {
            int retries = 0;
            while (retries < 10) {
                byte[] readBuffer = new byte[1024];
                int numRead = comPort.readBytes(readBuffer, readBuffer.length);
                // System.out.println("Read " + numRead + " bytes.");
                if(numRead == -1) {
                    retries++;
                } else {
                    retries = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
    }

}