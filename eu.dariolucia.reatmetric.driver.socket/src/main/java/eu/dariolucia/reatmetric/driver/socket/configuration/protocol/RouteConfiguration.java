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

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
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

import java.io.IOException;
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

    public void dispatchActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        // TODO: introduce single thread per route and a global (driver based) Timer for timeout computation
        Instant time = Instant.now();
        // Get the mapped OutboundMessageMapping
        OutboundMessageMapping mapping = getOutboundMessageFor(activityInvocation);
        if(mapping == null) {
            throw new ActivityHandlingException("No outbound message found, mapped to activity invocation for " + activityInvocation.getPath() + " (" + activityInvocation.getActivityId() + ")");
        }

        // Notify attempt to dispatch
        reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.PENDING,
                ActivityOccurrenceState.TRANSMISSION);
        // TODO: Encode command
        byte[] encodedCommand = encodeCommand(activityInvocation, mapping);
        // TODO: Register to verification if acceptance/execution stages are defined
        registerToVerifier(time, activityInvocation, mapping, encodedCommand);
        // TODO: If connector is on demand, start connector, mark connector as used +1
        acquireConnectionUsage();
        // TODO: Write command to connector
        try {
            writeToConnection(mapping, encodedCommand);
            // TODO: If all nominal, announce the opening of the next stage
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK,
                    ActivityOccurrenceState.TRANSMISSION); // TODO: check if acceptance/execution stages are defined. If not, move to verification

            // TODO: If no stages for verification are present, deregister from verifier and release connection if on demand
            if(mapping.getVerification() == null) {
                deregisterFromVerifier(time, activityInvocation, mapping, encodedCommand);
                releaseConnectionUsage();
            }
        } catch (IOException e) {
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.TRANSMISSION);
            // TODO: If failure, then deregister verification
            deregisterFromVerifier(time, activityInvocation, mapping, encodedCommand);
            // TODO: Close connector if on demand and used -1 = 0
            releaseConnectionUsage();

        }

    }

    private void releaseConnectionUsage() {
        // TODO
    }

    private void deregisterFromVerifier(Instant time, IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping, byte[] encodedCommand) {
        // TODO
    }

    private void writeToConnection(OutboundMessageMapping mapping, byte[] encodedCommand) throws IOException {
        // TODO
    }

    private void acquireConnectionUsage() {
        // TODO
    }

    private void registerToVerifier(Instant time, IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping, byte[] encodedCommand) {
        // TODO
    }

    private byte[] encodeCommand(IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping) {
        // TODO:
        return null;
    }

    private OutboundMessageMapping getOutboundMessageFor(IActivityHandler.ActivityInvocation activityInvocation) {
        // Slow, consider adding a map
        for(OutboundMessageMapping omm : getOutboundMessages()) {
            if(omm.getEntity() == activityInvocation.getActivityId()) {
                return omm;
            }
        }
        return null;
    }

    public void reportActivityState(int activityId, IUniqueId activityOccurrenceId, Instant time, ActivityOccurrenceState state, String releaseReportName, ActivityReportState status, ActivityOccurrenceState nextState) {
        dataProcessor.forwardActivityProgress(ActivityProgress.of(activityId, activityOccurrenceId, releaseReportName, time, state, null, status, nextState, null));
    }
}
