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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import jakarta.xml.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@XmlAccessorType(XmlAccessType.FIELD)
public class RouteConfiguration {

    @XmlAttribute(required = true)
    private String name;

    @XmlIDREF
    @XmlAttribute(required = true)
    private AbstractConnectionConfiguration defaultConnection = null;

    @XmlElement(name = "inbound")
    private List<InboundMessageMapping> inboundMessageMappings = new LinkedList<>();

    @XmlElement(name = "outbound")
    private List<OutboundMessageMapping> outboundMessageMappings = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InboundMessageMapping> getInboundMessages() {
        return inboundMessageMappings;
    }

    public void setInboundMessages(List<InboundMessageMapping> inboundMessageMappings) {
        this.inboundMessageMappings = inboundMessageMappings;
    }

    public List<OutboundMessageMapping> getOutboundMessages() {
        return outboundMessageMappings;
    }

    public void setOutboundMessages(List<OutboundMessageMapping> outboundMessageMappings) {
        this.outboundMessageMappings = outboundMessageMappings;
    }

    public AbstractConnectionConfiguration getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(AbstractConnectionConfiguration defaultConnection) {
        this.defaultConnection = defaultConnection;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private transient ProtocolConfiguration protocolConfiguration;

    // <MessageDefinition ID>_<secondary ID> as key
    private transient Map<String, InboundMessageMapping> messageId2mapping = new TreeMap<>();

    public void initialise(ProtocolConfiguration protocolConfiguration) {
        this.protocolConfiguration = protocolConfiguration;
        for(InboundMessageMapping m : getInboundMessages()) {
            m.initialise(getDefaultConnection(), protocolConfiguration.getEntityOffset());
            messageId2mapping.put(m.getMessageDefinition().getId() + "_" + m.getSecondaryId(), m);
        }
        for(OutboundMessageMapping m : getOutboundMessages()) {
            m.initialise(getDefaultConnection(), protocolConfiguration.getEntityOffset());
        }
    }

    public Pair<List<ParameterSample>, List<EventOccurrence>> onMessageReceived(String messageId, String secondaryId, Map<String, Object> decodedMessage) {
        Instant now = Instant.now();
        if(secondaryId == null) {
            secondaryId = "";
        }
        String key = messageId + "_" + secondaryId;
        // If there is an InboundMessageMapping for the message
        InboundMessageMapping inboundMessageMapping = messageId2mapping.get(key);
        if(inboundMessageMapping != null) {
            List<ParameterSample> parameterSamples = inboundMessageMapping.mapParameters(decodedMessage, getName(), now);
            List<EventOccurrence> eventOccurrences = inboundMessageMapping.mapEvents(decodedMessage, getName(), now);
            return Pair.of(parameterSamples, eventOccurrences);
        } else {
            // No mapping? Message ignored
            // TODO: use for command verification if used by any outstanding command, as per command definition
            return null;
        }
    }

}
