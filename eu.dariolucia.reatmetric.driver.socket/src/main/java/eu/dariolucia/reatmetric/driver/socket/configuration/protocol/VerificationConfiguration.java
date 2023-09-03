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

package eu.dariolucia.reatmetric.driver.socket.configuration.protocol;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class VerificationConfiguration {

    // Name of the field of the outbound message, containing its identifier value
    @XmlAttribute(name = "id-field")
    private String fieldIdName = null;

    // Overall timeout in seconds since the successful routing transition
    @XmlAttribute(name = "timeout")
    private int timeout = 5;

    // These are processed in order
    @XmlElement(name = "acceptance")
    private List<StageConfiguration> acceptance = new LinkedList<>();

    @XmlElement(name = "execution")
    private List<StageConfiguration> execution = new LinkedList<>();

    public List<StageConfiguration> getAcceptance() {
        return acceptance;
    }

    public void setAcceptance(List<StageConfiguration> acceptance) {
        this.acceptance = acceptance;
    }

    public List<StageConfiguration> getExecution() {
        return execution;
    }

    public void setExecution(List<StageConfiguration> execution) {
        this.execution = execution;
    }

    public String getFieldIdName() {
        return fieldIdName;
    }

    public void setFieldIdName(String fieldIdName) {
        this.fieldIdName = fieldIdName;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
