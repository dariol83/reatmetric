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

import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition;
import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class StageConfiguration {

    // Message ID that contains the verification information
    @XmlAttribute(name="message", required = true)
    @XmlIDREF
    private MessageDefinition<?> message;

    // Message secondary ID that contains the verification information, it can be null
    @XmlAttribute(name = "secondary-id")
    private String messageSecondaryId = null;

    // To correlate the field ID in the command with the ID in the received message, it can be null
    @XmlAttribute(name = "id-field")
    private String messageFieldIdName = null;

    // The name of the field to check, to understand the result of the operation, it can be null
    @XmlAttribute(name = "value-field")
    private String messageFieldValueName = null;

    // The value of the value field, to understand the result of the operation, it can be null
    @XmlAttribute(name = "expected-fixed-value")
    private String expectedFixedValue = null;

    // The type of the expected value string, to understand the result of the operation, it can be null
    @XmlAttribute(name = "expected-fixed-value-type")
    private ValueTypeEnum expectedFixedValueType = ValueTypeEnum.CHARACTER_STRING;

    // The name of the argument, containing the value to compare with the one in the value-field-name of the message, it can be null
    @XmlAttribute(name = "reference-argument")
    private String referenceArgument = null;

    // The state to report for the related stage, if a message with the associated condition (ID field and condition field) is received
    @XmlAttribute(name="result", required = true)
    private ActivityReportState result;

    public MessageDefinition<?> getMessage() {
        return message;
    }

    public void setMessage(MessageDefinition<?> message) {
        this.message = message;
    }

    public String getMessageSecondaryId() {
        return messageSecondaryId;
    }

    public void setMessageSecondaryId(String messageSecondaryId) {
        this.messageSecondaryId = messageSecondaryId;
    }

    public String getMessageFieldIdName() {
        return messageFieldIdName;
    }

    public void setMessageFieldIdName(String messageFieldIdName) {
        this.messageFieldIdName = messageFieldIdName;
    }

    public String getMessageFieldValueName() {
        return messageFieldValueName;
    }

    public void setMessageFieldValueName(String messageFieldValueName) {
        this.messageFieldValueName = messageFieldValueName;
    }

    public String getExpectedFixedValue() {
        return expectedFixedValue;
    }

    public void setExpectedFixedValue(String expectedFixedValue) {
        this.expectedFixedValue = expectedFixedValue;
    }

    public ValueTypeEnum getExpectedFixedValueType() {
        return expectedFixedValueType;
    }

    public void setExpectedFixedValueType(ValueTypeEnum expectedFixedValueType) {
        this.expectedFixedValueType = expectedFixedValueType;
    }

    public String getReferenceArgument() {
        return referenceArgument;
    }

    public void setReferenceArgument(String referenceArgument) {
        this.referenceArgument = referenceArgument;
    }

    public ActivityReportState getResult() {
        return result;
    }

    public void setResult(ActivityReportState result) {
        this.result = result;
    }
}
