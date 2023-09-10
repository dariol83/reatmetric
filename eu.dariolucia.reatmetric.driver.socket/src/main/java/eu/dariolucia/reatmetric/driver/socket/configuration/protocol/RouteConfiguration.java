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

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.socket.SocketDriver;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.AsciiMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.BinaryMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition;
import jakarta.xml.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class RouteConfiguration {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(name = "entity-offset")
    private int entityOffset = 0;

    @XmlElementWrapper(name = "activity-types")
    @XmlElement(name="type")
    private List<String> activityTypes = new LinkedList<>();

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

    public int getEntityOffset() {
        return entityOffset;
    }

    public void setEntityOffset(int entityOffset) {
        this.entityOffset = entityOffset;
    }

    public List<String> getActivityTypes() {
        return activityTypes;
    }

    public void setActivityTypes(List<String> activityTypes) {
        this.activityTypes = activityTypes;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private transient AbstractConnectionConfiguration parentConnection;

    // <MessageDefinition ID>_<secondary ID> as key
    private final transient Map<String, InboundMessageMapping> messageId2mapping = new TreeMap<>();

    private transient IDataProcessor dataProcessor;

    public void initialise(AbstractConnectionConfiguration parentConnection) {
        this.parentConnection = parentConnection;
        for(InboundMessageMapping m : getInboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
            messageId2mapping.put(m.getMessageDefinition().getId() + "_" + m.getSecondaryId(), m);
        }
        for(OutboundMessageMapping m : getOutboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
        }
    }

    public void onMessageReceived(Instant time, String messageId, String secondaryId, Map<String, Object> decodedMessage, byte[] rawMessage) {
        // Forward raw data
        RawData rawData = new RawData(dataProcessor.getNextRawDataId(), time, secondaryId != null ? secondaryId : messageId,
                "", getName(), getParentConnection().getSource(),
                Quality.GOOD, null, rawMessage, time, null, null);
        dataProcessor.forwardRawData(rawData);
        //
        if(secondaryId == null) {
            secondaryId = "";
        }
        String key = messageId + "_" + secondaryId;
        // If there is an InboundMessageMapping for the message
        InboundMessageMapping inboundMessageMapping = messageId2mapping.get(key);
        if(inboundMessageMapping != null) {
            List<ParameterSample> parameterSamples = inboundMessageMapping.mapParameters(decodedMessage, getName(), time);
            List<EventOccurrence> eventOccurrences = inboundMessageMapping.mapEvents(decodedMessage, getName(), time);
            // Inject
            dataProcessor.forwardParameters(parameterSamples);
            dataProcessor.forwardEvents(eventOccurrences);
        }
        // No mapping? Message ignored
        // TODO: use for command verification if used by any outstanding command, as per command definition
    }

    public AbstractConnectionConfiguration getParentConnection() {
        return parentConnection;
    }

    public void onBinaryMessageReceived(byte[] message) {
        Instant receivedTime = Instant.now();
        String identifier = null;
        String secondaryIdentifier = null;
        BinaryMessageDefinition definition = null;
        // You received a binary message: get the message definition, identify the message
        for(InboundMessageMapping template : getInboundMessages()) {
            MessageDefinition<?> def = template.getMessageDefinition();
            if(def instanceof BinaryMessageDefinition) {
                BinaryMessageDefinition bmd = (BinaryMessageDefinition) def;
                try {
                    secondaryIdentifier = bmd.identify(message);
                    if (secondaryIdentifier != null) {
                        identifier = def.getId();
                        definition = bmd;
                        break;
                    }
                } catch (ReatmetricException e) {
                    // TODO: Log
                }
            }
        }
        //
        if(identifier != null) {
            // Decode the message and forward everything to onMessageReceived
            try {
                Map<String, Object> decodedMessage = definition.decode(secondaryIdentifier, message);
                onMessageReceived(receivedTime, identifier, secondaryIdentifier, decodedMessage, message);
            } catch (ReatmetricException e) {
                // TODO: Log
            }
        }
    }

    public void onAsciiMessageReceived(String message, byte[] rawMessage) {
        Instant receivedTime = Instant.now();
        String identifier = null;
        String secondaryIdentifier = null;
        AsciiMessageDefinition definition = null;
        // You received a ASCII message: get the message definition, identify the message
        for(InboundMessageMapping template : getInboundMessages()) {
            MessageDefinition<?> def = template.getMessageDefinition();
            if(def instanceof AsciiMessageDefinition) {
                AsciiMessageDefinition amd = (AsciiMessageDefinition) def;
                secondaryIdentifier = amd.identify(message);
                if (secondaryIdentifier != null) {
                    identifier = def.getId();
                    definition = amd;
                    break;
                }
            }
        }
        //
        if(identifier != null) {
            // Decode the message and forward everything to onMessageReceived
            Map<String, Object> decodedMessage = definition.decode(null, message);
            onMessageReceived(receivedTime, identifier, secondaryIdentifier, decodedMessage, rawMessage);
        }
    }

    public void setDataProcessor(SocketDriver dataProcessor) {
        this.dataProcessor = dataProcessor;
    }
}
