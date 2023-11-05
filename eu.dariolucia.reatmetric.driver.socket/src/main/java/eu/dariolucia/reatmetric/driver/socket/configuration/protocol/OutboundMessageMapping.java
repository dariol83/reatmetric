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
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AsciiEncoding;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.AsciiMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.BinaryMessageDefinition;
import jakarta.xml.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@XmlAccessorType(XmlAccessType.FIELD)
public class OutboundMessageMapping extends MessageMapping {

    @XmlAttribute
    private OutboundMessageType type;

    // If the type is "activity-driven", here you need to have the entity ID (and the offset will be added of course)
    @XmlAttribute
    private int entity = -1;

    // If the type is "periodic", here you specify the period in seconds. You cannot have periodic commands on on-demand
    // driven connections
    @XmlAttribute
    private int period = 0;

    // Delay after sending the command in ms
    @XmlAttribute(name="post-send-delay")
    private int postSentDelay = 0;

    @XmlElement(name = "argument")
    private List<ArgumentMapping> argumentMappings = new LinkedList<>();

    @XmlElement(name = "auto-increment")
    private List<AutoIncrementField> autoIncrementFields = new LinkedList<>();

    @XmlElement(name = "verification")
    private VerificationConfiguration verification = null;

    public OutboundMessageType getType() {
        return type;
    }

    public void setType(OutboundMessageType type) {
        this.type = type;
    }

    public int getEntity() {
        return entity;
    }

    public void setEntity(int entity) {
        this.entity = entity;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public List<ArgumentMapping> getArgumentMappings() {
        return argumentMappings;
    }

    public void setArgumentMappings(List<ArgumentMapping> argumentMappings) {
        this.argumentMappings = argumentMappings;
    }

    public List<AutoIncrementField> getAutoIncrementFields() {
        return autoIncrementFields;
    }

    public void setAutoIncrementFields(List<AutoIncrementField> autoIncrementFields) {
        this.autoIncrementFields = autoIncrementFields;
    }

    public VerificationConfiguration getVerification() {
        return verification;
    }

    public void setVerification(VerificationConfiguration verification) {
        this.verification = verification;
    }

    public int getPostSentDelay() {
        return postSentDelay;
    }

    public void setPostSentDelay(int postSentDelay) {
        this.postSentDelay = postSentDelay;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    @XmlTransient
    private final Map<String, ArgumentMapping> id2argumentMapping = new TreeMap<>();

    @Override
    public void initialise(AbstractConnectionConfiguration defaultConnection, int entityOffset) {
        super.initialise(defaultConnection, entityOffset);
        //
        for(ArgumentMapping am : getArgumentMappings()) {
            id2argumentMapping.put(am.getName(), am);
        }
        //
        for(AutoIncrementField aif : getAutoIncrementFields()) {
            aif.initialise(defaultConnection.getRoute());
        }
        // Sanitize verification
        if(verification != null && verification.getAcceptance().isEmpty() && verification.getExecution().isEmpty()) {
            verification = null;
        }
    }

    public Pair<byte[], Map<String, Object>> encodeCommand(IActivityHandler.ActivityInvocation request, AsciiEncoding asciiEncoding) throws ReatmetricException {
        Map<String, Object> mappedArguments = new TreeMap<>();
        // Add and map arguments
        if(getType() == OutboundMessageType.ACTIVITY_DRIVEN && getEntity() + getEntityOffset() == request.getActivityId()) {
            Map<String, Object> activityArguments = request.getArguments();
            for(Map.Entry<String, Object> argEntry : activityArguments.entrySet()) {
                ArgumentMapping am = id2argumentMapping.get(argEntry.getKey());
                if(am != null) {
                    mappedArguments.put(am.getField(), argEntry.getValue());
                } else {
                    mappedArguments.put(argEntry.getKey(), argEntry.getValue());
                }
            }
        }
        // Add auto-increment fields
        for(AutoIncrementField aif : getAutoIncrementFields()) {
            if(!mappedArguments.containsKey(aif.getField())) {
                mappedArguments.put(aif.getField(), aif.next());
            }
        }
        // Encode
        if(getMessageDefinition() instanceof AsciiMessageDefinition) {
            AsciiMessageDefinition amd = (AsciiMessageDefinition) getMessageDefinition();
            String encoded = amd.encode(getSecondaryId(), mappedArguments);
            byte[] data = encoded.getBytes(asciiEncoding.getCharset());
            return Pair.of(data, mappedArguments);
        } else if(getMessageDefinition() instanceof BinaryMessageDefinition) {
            BinaryMessageDefinition bmd = (BinaryMessageDefinition) getMessageDefinition();
            byte[] data = bmd.encode(getSecondaryId(), mappedArguments);
            return Pair.of(data, mappedArguments);
        } else {
            throw new ReatmetricException("Unknown message definition type: " + getMessageDefinition());
        }
    }

    @Override
    public String toString() {
        return "OutboundMessageMapping{" +
                "id=" + getId() +
                ", message=" + getMessageDefinition() +
                ", type=" + type +
                ", entity=" + entity +
                ", period=" + period +
                '}';
    }
}
