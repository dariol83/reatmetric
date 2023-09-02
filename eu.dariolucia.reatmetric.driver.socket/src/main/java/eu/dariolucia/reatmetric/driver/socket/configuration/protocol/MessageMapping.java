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

import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;

public class MessageMapping {
    @XmlIDREF
    @XmlAttribute(name = "message", required = true)
    private MessageDefinition<?> messageDefinition;

    @XmlAttribute(name = "secondary-id")
    private String secondaryId = "";

    public String getSecondaryId() {
        return secondaryId;
    }

    public void setSecondaryId(String secondaryId) {
        this.secondaryId = secondaryId;
    }

    public MessageDefinition<?> getMessageDefinition() {
        return messageDefinition;
    }

    public void setMessageDefinition(MessageDefinition<?> messageDefinition) {
        this.messageDefinition = messageDefinition;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private transient AbstractConnectionConfiguration connectionConfiguration;
    private transient int entityOffset;

    public void initialise(AbstractConnectionConfiguration defaultConnection, int entityOffset) {
        this.entityOffset = entityOffset;
        if(this.connectionConfiguration != null) {
            this.connectionConfiguration = defaultConnection;
        }
    }

    protected int getEntityOffset() {
        return entityOffset;
    }
 }
