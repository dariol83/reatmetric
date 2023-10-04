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

import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.time.Instant;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class InboundMessageMapping extends MessageMapping {

    //  The attribute "command-match" is a way (OutboundMessageLink) to specify that the mapping is linked to an OutboundMessageMapping,
    //  with optionally a specific argument matching a value expressed here.
    //  When such message is sent and a message is received, this mapping is effectively used only if it is linked
    //  to that command.
    //  This feature covers the situation when a request is asking for a given response, the request contains an ID of a
    //  device to query, but the response does not have such ID. The correlation to the ID, and therefore to the mapping
    //  to be used, must be derived by the fact that a previous command was sent.
    //  This approach of course works only for full synchronous protocols.

    // Example:
    // Request:"STATUS DEVICE X", where X can be A or B.
    // Response:"P1,P2,P3". If there are two devices A and B with the same parameter message structure, I need to create
    // two InboundMessageMapping objects, one linked to the command "STATUS DEVICE X", with X = A, and the other
    // to the same command, with X = B.
    // When "P1,P2,P3" is received

    @XmlElement(name = "command-match")
    private OutboundMessageMappingReference command = null;

    @XmlElement(name = "inject")
    private List<ParameterMapping> parameterMappings = new LinkedList<>();

    @XmlElement(name = "raise")
    private List<EventMapping> eventMappings = new LinkedList<>();

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public void setParameterMappings(List<ParameterMapping> parameterMappings) {
        this.parameterMappings = parameterMappings;
    }

    public List<EventMapping> getEventMappings() {
        return eventMappings;
    }

    public void setEventMappings(List<EventMapping> eventMappings) {
        this.eventMappings = eventMappings;
    }

    public OutboundMessageMappingReference getCommand() {
        return command;
    }

    public void setCommand(OutboundMessageMappingReference command) {
        this.command = command;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private Map<String, ParameterMapping> id2parameterMapping = new TreeMap<>();

    @Override
    public void initialise(AbstractConnectionConfiguration defaultConnection, int entityOffset) {
        super.initialise(defaultConnection, entityOffset);
        //
        for(ParameterMapping pm : getParameterMappings()) {
            id2parameterMapping.put(pm.getName(), pm);
        }
    }

    public List<ParameterSample> mapParameters(Map<String, Object> decodedMessage, String route, Instant time) {
        List<ParameterSample> samples = new ArrayList<>(decodedMessage.size());
        for(Map.Entry<String, Object> entry : decodedMessage.entrySet()) {
            String key = entry.getKey();
            ParameterMapping pm = id2parameterMapping.get(key);
            if(pm != null) {
                ParameterSample ps = ParameterSample.of(pm.getEntity() + getEntityOffset(), time, time, null, entry.getValue(), route, null);
                samples.add(ps);
            }
        }
        return samples;
    }

    public List<EventOccurrence> mapEvents(Map<String, Object> decodedMessage, String route, Instant time) {
        List<EventOccurrence> occurrences = new ArrayList<>(getEventMappings().size());
        for(EventMapping em : getEventMappings()) {
            String qualifier = null;
            if(em.getQualifier() != null) {
                qualifier = em.getQualifier();
            } else if(em.getQualifierReference() != null) {
                qualifier = Objects.toString(decodedMessage.getOrDefault(em.getQualifierReference(), ""));
            }
            occurrences.add(EventOccurrence.of(em.getEntity() + getEntityOffset(), time, time, null,
                    qualifier, null, route, em.getSource(), null));
        }
        return occurrences;
    }
}
