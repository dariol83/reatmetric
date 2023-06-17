/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.serial.definition;

import com.fazecast.jSerialComm.SerialPort;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "serial", namespace = "http://dariolucia.eu/reatmetric/driver/serial")
@XmlAccessorType(XmlAccessType.FIELD)
public class SerialConfiguration {

    public static SerialConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SerialConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (SerialConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "device", required = true)
    private String device;

    @XmlAttribute(name = "timeout")
    private int timeout = 10; // Read timeout seconds

    @XmlAttribute(name = "baudrate")
    private int baudrate = 4800; // bauds per second

    @XmlAttribute(name = "parity")
    private ParityEnum parity = ParityEnum.EVEN; // parity

    @XmlAttribute(name = "data-bits")
    private int dataBits = 7;

    @XmlAttribute(name = "stop-bits")
    private StopBitsEnum stopBits = StopBitsEnum.ONE;

    @XmlAttribute(name = "flow-control")
    private FlowControlEnum flowControl = FlowControlEnum.NONE;

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    public ParityEnum getParity() {
        return parity;
    }

    public void setParity(ParityEnum parity) {
        this.parity = parity;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public StopBitsEnum getStopBits() {
        return stopBits;
    }

    public void setStopBits(StopBitsEnum stopBits) {
        this.stopBits = stopBits;
    }

    public FlowControlEnum getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(FlowControlEnum flowControl) {
        this.flowControl = flowControl;
    }

    public enum ParityEnum {
        NO(SerialPort.NO_PARITY),
        ODD(SerialPort.ODD_PARITY),
        EVEN(SerialPort.EVEN_PARITY),
        MARK(SerialPort.MARK_PARITY),
        SPACE(SerialPort.SPACE_PARITY);

        private final int value;

        ParityEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum StopBitsEnum {
        ONE(SerialPort.ONE_STOP_BIT),
        ONEDOTFIVE(SerialPort.ONE_POINT_FIVE_STOP_BITS),
        TWO(SerialPort.TWO_STOP_BITS);

        private final int value;

        StopBitsEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum FlowControlEnum {
        NONE(SerialPort.FLOW_CONTROL_DISABLED),
        CTS(SerialPort.FLOW_CONTROL_CTS_ENABLED),
        RTS_CTS(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED),
        DSR(SerialPort.FLOW_CONTROL_DSR_ENABLED),
        DTR_DSR(SerialPort.FLOW_CONTROL_DTR_ENABLED| SerialPort.FLOW_CONTROL_DSR_ENABLED),
        XON_XOFF(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED);

        private final int value;

        FlowControlEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
