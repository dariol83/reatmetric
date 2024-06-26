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
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.driver.socket.SocketDriver;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.InitType;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.ProtocolType;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.AsciiMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.BinaryMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition;
import jakarta.xml.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class RouteConfiguration {

    private static final Logger LOG = Logger.getLogger(RouteConfiguration.class.getName());

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(name = "entity-offset")
    private int entityOffset = 0;

    // If this is true, it means that the execution of this command must complete, before freeing up the connection to send the next one
    @XmlAttribute(name = "command-lock")
    private boolean commandLock = true;

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

    public boolean isCommandLock() {
        return commandLock;
    }

    public void setCommandLock(boolean commandLock) {
        this.commandLock = commandLock;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    @XmlTransient
    private AbstractConnectionConfiguration parentConnection;
    // <MessageDefinition ID>_<secondary ID> as key

    @XmlTransient
    private final Map<String, List<InboundMessageMapping>> messageId2mapping = new TreeMap<>();

    @XmlTransient
    private IDataProcessor dataProcessor;

    @XmlTransient
    private final Map<OutboundMessageMapping, CommandTracker> outboundMessage2lastCommand = new ConcurrentHashMap<>();

    @XmlTransient
    private volatile CommandTracker lastDispatchedCommand = null;

    @XmlTransient
    private final Map<String, List<CommandTracker>> progressMessageId2commandTracker = new TreeMap<>();

    @XmlTransient
    private final List<CommandTracker> activeCommandTrackers = new CopyOnWriteArrayList<>();

    @XmlTransient
    private final AtomicInteger connectionUsage = new AtomicInteger(0);

    @XmlTransient
    private final Semaphore connectionSequencer = new Semaphore(1);

    @XmlTransient
    private final List<TimerTask> periodCommandTasks = new CopyOnWriteArrayList<>();

    @XmlTransient
    private String driverName;

    @XmlTransient
    private final Map<String, AtomicInteger> autoIncrementSequencers = new ConcurrentHashMap<>();

    public void initialise(AbstractConnectionConfiguration parentConnection) {
        this.parentConnection = parentConnection;
        // Create the sequencers?
        for(InboundMessageMapping m : getInboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
            messageId2mapping.computeIfAbsent(m.getMessageDefinition().getId() + "_" + m.getSecondaryId(), k -> new LinkedList<>()).add(m);
        }
        for(OutboundMessageMapping m : getOutboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
        }
    }

    public void notifyConnectionDisconnection() {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, String.format("Resetting verification trackers and connection usage state on %s", getName()));
        }
        List<CommandTracker> trackers = new LinkedList<>(activeCommandTrackers);
        for(CommandTracker ct : trackers) {
            // Mark command verification as unknown
            ct.unknownVerification(this.dataProcessor);
            // Remove from verifier and reset timeout
            deregisterFromVerifier(ct);
        }
        // Reset internal connection usage
        this.connectionUsage.set(0);
        if(connectionSequencer.availablePermits() == 0) {
            connectionSequencer.release();
        }
    }

    public int getNextSequenceOf(String id) {
        AtomicInteger ai = autoIncrementSequencers.computeIfAbsent(id, o -> new AtomicInteger(0));
        return ai.incrementAndGet();
    }

    private void internalMessageReceived(Instant time, String messageId, String secondaryId, Map<String, Object> decodedMessage, byte[] rawMessage) {
        // Forward raw data
        RawData rawData = new RawData(dataProcessor.getNextRawDataId(), time, secondaryId != null ? secondaryId : messageId,
                SocketDriver.SOCKET_MESSAGE_TYPE, getName(), getParentConnection().getSource(),
                Quality.GOOD, null, rawMessage, time, driverName, null);
        dataProcessor.forwardRawData(rawData);
        //
        if(secondaryId == null) {
            secondaryId = "";
        }
        String key = messageId + "_" + secondaryId;
        // If there is an InboundMessageMapping for the message
        List<InboundMessageMapping> inboundMessageMappingsForMessage = messageId2mapping.get(key);
        if(inboundMessageMappingsForMessage != null) {
            for(InboundMessageMapping imm : inboundMessageMappingsForMessage) {
                if(imm.getCommand() != null) {
                    // Just check the last command of the given type, if any
                    OutboundMessageMapping omm = imm.getCommand().getOutboundMapping();
                    CommandTracker lastCommand = null;
                    if(imm.getCommand().isLastCommand()) {
                        // Check the really last sent command
                        lastCommand = lastDispatchedCommand;
                    } else {
                        // Get the last sent command, if any, linked to this
                        lastCommand = outboundMessage2lastCommand.get(omm);
                    }
                    // At this stage, if the command is there and no specific argument is specified or the argument matches...
                    if (imm.getCommand().match(lastCommand)) {
                        // ... inject the message according to this definition
                        performInjection(time, decodedMessage, imm);
                    }
                } else {
                    // No link to command: mapping is OK, map parameters and events, and inject everything
                    performInjection(time, decodedMessage, imm);
                }
            }
        }
        // No mapping? Message ignored for injection, check for verification
        internalVerifyAcknowledgement(time, messageId, secondaryId, decodedMessage, rawMessage, getName());
    }

    private void performInjection(Instant time, Map<String, Object> decodedMessage, InboundMessageMapping imm) {
        // Add computed fields, they can overwrite existing fields!
        for(ComputedField cf : imm.getComputedFields()) {
            String field = cf.getField();
            Object value;
            try {
                value = cf.compute(imm.getMessageDefinition().getId(), imm.getSecondaryId(), decodedMessage);
                decodedMessage.put(field, value);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot compute field " + field + " for inbound message " + imm.getId() + ": " + e.getMessage(), e);
            }
        }
        List<ParameterSample> parameterSamples = imm.mapParameters(decodedMessage, getName(), time);
        List<EventOccurrence> eventOccurrences = imm.mapEvents(decodedMessage, getName(), time);
        injectAndRaise(parameterSamples, eventOccurrences);
    }

    private void injectAndRaise(List<ParameterSample> parameterSamples, List<EventOccurrence> eventOccurrences) {
        // Inject
        dataProcessor.forwardParameters(parameterSamples);
        dataProcessor.forwardEvents(eventOccurrences);
    }

    private void internalVerifyAcknowledgement(Instant time, String messageId, String secondaryId, Map<String, Object> decodedMessage, byte[] rawMessage, String route) {
        // Get the list of trackers registered on this message
        if(secondaryId == null) {
            secondaryId = "";
        }
        String key = messageId + "_" + secondaryId;
        List<CommandTracker> trackerList = this.progressMessageId2commandTracker.get(key);
        List<CommandTracker> toBeRemoved = new LinkedList<>();
        for(CommandTracker tracker : trackerList) {
            // Send message to tracker: if verification is completed, timeout timer is cancelled and true is returned
            boolean verificationCompleted = tracker.messageReceived(dataProcessor, time, messageId, secondaryId, decodedMessage, route);
            if(verificationCompleted) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, String.format("Verification lifecycle for command %s on route %s completed", tracker.getId(), route));
                }
                // Add tracker for removal
                toBeRemoved.add(tracker);
            }
        }
        // Remove fully verified trackers
        for(CommandTracker tracker : toBeRemoved) {
            releaseConnectionUsage();
            deregisterFromVerifier(tracker);
        }
    }

    public AbstractConnectionConfiguration getParentConnection() {
        return parentConnection;
    }

    public void onBinaryMessageReceived(byte[] message) {
        Instant receivedTime = Instant.now();
        this.dataProcessor.execute(() -> internalBinaryMessageReceived(message, receivedTime));
    }

    private void internalBinaryMessageReceived(byte[] message, Instant receivedTime) {
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
                    LOG.log(Level.SEVERE, String.format("Error detected when identifying binary message with definition %s on route %s: %s", def.getId(), getName(), e.getMessage()), e);
                }
            }
        }
        //
        if(identifier != null) {
            // Decode the message and forward everything to onMessageReceived
            try {
                Map<String, Object> decodedMessage = definition.decode(secondaryIdentifier, message);
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, String.format("Binary message (%s, %s) received on route %s", identifier, secondaryIdentifier, getName()));
                }
                internalMessageReceived(receivedTime, identifier, secondaryIdentifier, decodedMessage, message);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, String.format("Error detected when decoding binary message %s with definition %s on route %s: %s", secondaryIdentifier, definition.getId(), getName(), e.getMessage()), e);
            }
        } else {
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, String.format("Binary message not identified on route %s: %s", getName(), StringUtil.toHexDump(message)));
            }
        }
    }

    public void onAsciiMessageReceived(String message, byte[] rawMessage) {
        final Instant receivedTime = Instant.now();
        this.dataProcessor.execute(() -> internalAsciiMessageReceived(message, rawMessage, receivedTime));
    }

    private void internalAsciiMessageReceived(String message, byte[] rawMessage, Instant receivedTime) {
        String identifier = null;
        AsciiMessageDefinition definition = null;
        // You received an ASCII message: get the message definition, identify the message
        for(InboundMessageMapping template : getInboundMessages()) {
            MessageDefinition<?> def = template.getMessageDefinition();
            if(def instanceof AsciiMessageDefinition) {
                AsciiMessageDefinition amd = (AsciiMessageDefinition) def;
                identifier = amd.identify(message);
                if (identifier != null) {
                    identifier = def.getId();
                    definition = amd;
                    break;
                }
            }
        }
        //
        if(identifier != null) {
            // Decode the message and forward everything to onMessageReceived
            try {
                Map<String, Object> decodedMessage = definition.decode(null, message);
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, String.format("ASCII message (%s) received on route %s: %s", identifier, getName(), message));
                }
                internalMessageReceived(receivedTime, identifier, null, decodedMessage, rawMessage);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, String.format("Error detected when decoding ASCII message '%s' with definition %s on route %s: %s", message, definition.getId(), getName(), e.getMessage()), e);
            }
        } else {
            LOG.log(Level.WARNING, String.format("ASCII message not identified on route %s", getName()));
        }
    }

    public void setDataProcessor(SocketDriver dataProcessor) {
        this.dataProcessor = dataProcessor;
        this.driverName = dataProcessor.getHandlerName();
    }

    public void dispatchActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, String.format("Dispatching activity %s (%s) on route %s", activityInvocation.getPath(), activityInvocation.getActivityOccurrenceId(), getName()));
        }
        Instant time = Instant.now();
        // Get the mapped OutboundMessageMapping
        OutboundMessageMapping mapping = getOutboundMessageFor(activityInvocation);
        if(mapping == null) {
            throw new ActivityHandlingException(String.format("No outbound message found, mapped to activity invocation for %s (%d)", activityInvocation.getPath(), activityInvocation.getActivityId()));
        }
        // Encode command
        Pair<byte[], Map<String, Object>> encodedCommand;
        try {
            encodedCommand = encodeCommand(activityInvocation, mapping);
        } catch (ReatmetricException e) {
            throw new ActivityHandlingException(String.format("Error while encoding command %d: %s", activityInvocation.getActivityId(), e.getMessage()), e);
        }
        // If the connection is one of those requiring a single command active at a time, then put the command in the dispatch
        // phase only if you actually can
        if(isCommandLock()) {
            try {
                connectionSequencer.acquire();
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, String.format("Acquired command lock semaphore on route %s", getName()));
                }
            } catch (InterruptedException e) {
                throw new ActivityHandlingException(String.format("Waiting dispatch interrupted for activity %s on route %s", activityInvocation.getActivityOccurrenceId(), getName()), e);
            }
        }
        // At this stage, the work can be done fully asynchronously
        this.dataProcessor.execute(() -> internalDispatch(activityInvocation, time, mapping, encodedCommand));
    }

    public void startDispatchingOfPeriodicCommands() {
        if(getParentConnection().getInit() == InitType.CONNECTOR) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, String.format("Activating dispatch of periodic commands on connection %s (route %s)", getParentConnection().getName(), getName()));
            }
            // Schedule execution of all the periodic commands
            for (OutboundMessageMapping omm : getOutboundMessages()) {
                if (omm.getType() == OutboundMessageType.PERIODIC) {
                    TimerTask periodTask = new TimerTask() {
                        @Override
                        public void run() {
                            dispatchInternalCommand(omm);
                        }
                    };
                    this.periodCommandTasks.add(periodTask);
                    this.dataProcessor.getTimerService().schedule(periodTask, omm.getPeriod() * 1000L, omm.getPeriod() * 1000L);
                }
            }
        } // ignore the ON-DEMAND connections
    }

    public void dispatchOnConnectionCommands() {
        for(OutboundMessageMapping omm : getOutboundMessages()) {
            if (omm.getType() == OutboundMessageType.CONNECTION_ACTIVE) {
                dispatchInternalCommand(omm);
            }
        }
    }

    /**
     * This method is invoked only for period and on-connection commands. In case of command lock, it waits for the
     * maximum waiting time specified in the definition.
     *
     * @param mapping the message to send
     */
    private void dispatchInternalCommand(OutboundMessageMapping mapping) {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, String.format("Dispatching internal command %s (%s) on route %s", mapping.getId(), mapping.getMessageDefinition().getId(), getName()));
        }
        Instant time = Instant.now();
        if(!getParentConnection().isOpen()) {
            return;
        }
        // Encode command
        Pair<byte[], Map<String, Object>> encodedCommand;
        try {
            encodedCommand = encodeCommand(null, mapping);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, String.format("Cannot encode command %s on route %s for internal dispatch: %s", mapping.getMessageDefinition().getId(), getName(), e.getMessage()), e);
            return;
        }
        // If you want a lock, wait for a while
        if(isCommandLock()) {
            try {
                boolean acquired = connectionSequencer.tryAcquire(mapping.getMaxWaitingTime(), TimeUnit.MILLISECONDS);
                if(!acquired) {
                    if(LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, String.format("Command lock active and channel is busy - Dispatch of internal command [%s,%s] on route %s skipped", mapping.getMessageDefinition().getId(), Objects.toString(mapping.getSecondaryId(),""), getName()));
                    }
                    return;
                }
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, String.format("Acquired command lock semaphore on route %s", getName()));
                }
            } catch (InterruptedException e) {
                LOG.log(Level.FINE, String.format("Command lock interrupted on dispatch of internal command %s on route %s", mapping.getMessageDefinition().getId(), getName()));
                return;
            }
        }
        dataProcessor.execute(() -> internalDispatch(time, mapping, encodedCommand));
    }

    private void internalDispatch(Instant time, OutboundMessageMapping mapping, Pair<byte[], Map<String, Object>> encodedCommand) {
        // ... create a dummy tracker
        CommandTracker tracker = registerToVerifier(time, null, mapping, encodedCommand);
        try {
            // If connection is on demand, start connector if connectionUsage = 0, mark connector as connectionUsage +1.
            acquireConnectionUsage();
            // Connection should be open: write command to connector
            if(!writeToConnection(encodedCommand.getFirst())) {
                throw new IOException("output stream not available");
            }
            // If all nominal...
            // ... register the command as last command sent for the given outbound message mapping
            outboundMessage2lastCommand.put(mapping, tracker);
            lastDispatchedCommand = tracker;
            finaliseVerificationInitialisation(mapping, tracker);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, String.format("Cannot transmit command [%s,%s] on route %s for internal dispatch: %s", mapping.getMessageDefinition().getId(), Objects.toString(mapping.getSecondaryId(),""), getName(), e.getMessage()), e);
            // connectionUsage -1, close connector if on demand and connectionUsage = 0
            releaseConnectionUsage();
            // If failure, then deregister verification
            deregisterFromVerifier(tracker);
        }
    }

    private void finaliseVerificationInitialisation(OutboundMessageMapping mapping, CommandTracker tracker) {
        // If no stages for verification are present, deregister from verifier and release connection if on demand
        if(mapping.getVerification() == null) {
            // Wait post send delay
            waitForPostDelay(mapping);
            releaseConnectionUsage();
            deregisterFromVerifier(tracker);
        } else {
            tracker.announceVerificationStages(this.dataProcessor);
            // Wait post send delay
            waitForPostDelay(mapping);
        }
    }

    public void stopDispatchingOfPeriodicCommands() {
        if(getParentConnection().getInit() == InitType.CONNECTOR) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, String.format("Deactivating dispatch of periodic commands on connection %s (route %s)", getParentConnection().getName(), getName()));
            }
            this.periodCommandTasks.forEach(TimerTask::cancel);
            this.periodCommandTasks.clear();
        }
    }

    private void internalDispatch(IActivityHandler.ActivityInvocation activityInvocation, Instant time, OutboundMessageMapping mapping, Pair<byte[], Map<String, Object>> encodedCommand) {
        // Notify attempt to dispatch
        reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.PENDING,
                ActivityOccurrenceState.TRANSMISSION);
        // Register to verification if acceptance/execution stages are defined
        CommandTracker tracker = registerToVerifier(time, activityInvocation, mapping, encodedCommand);
        try {
            // If connection is on demand, start connector if connectionUsage = 0, mark connector as connectionUsage +1.
            acquireConnectionUsage();
            // If no connection at this stage, bye
            if(!getParentConnection().isOpen()) {
                throw new IOException("Connection of route " + getName() + " not open");
            }
            // Write command to connector
            if(!writeToConnection(encodedCommand.getFirst())) {
                throw new IOException("output stream not available");
            }
            // If all nominal...
            // ... register the command as last command sent for the given outbound message mapping
            if(LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Encoded activity " + activityInvocation.getPath() + " (" + activityInvocation.getActivityOccurrenceId() + ") written on route " + getName());
            }
            outboundMessage2lastCommand.put(mapping, tracker);
            lastDispatchedCommand = tracker;
            // ... announce the opening of the next stage
            ActivityOccurrenceState nextStage = ActivityOccurrenceState.VERIFICATION;
            // ... check if acceptance/execution stages are defined. If not, move to verification
            if(mapping.getVerification() != null && !mapping.getVerification().getExecution().isEmpty()) {
                nextStage = ActivityOccurrenceState.EXECUTION;
            }
            if(mapping.getVerification() != null && !mapping.getVerification().getAcceptance().isEmpty()) {
                nextStage = ActivityOccurrenceState.TRANSMISSION;
            }
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK,
                    nextStage);
            // If no stages for verification are present, deregister from verifier and release connection if on demand
            finaliseVerificationInitialisation(mapping, tracker);
        } catch (IOException e) {
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.TRANSMISSION);
            // connectionUsage -1, close connector if on demand and connectionUsage = 0
            releaseConnectionUsage();
            // If failure, then deregister verification
            deregisterFromVerifier(tracker);
        }
    }

    private void waitForPostDelay(OutboundMessageMapping mapping) {
        if(mapping.getPostSentDelay() > 0) {
            try {
                Thread.sleep(mapping.getPostSentDelay());
            } catch (InterruptedException e) {
                // Nothing to do
            }
        }
    }

    private void acquireConnectionUsage() {
        int usages = this.connectionUsage.getAndIncrement();
        // On demand and first use? Start it.
        if(usages == 0 && getParentConnection().getInit() == InitType.ON_DEMAND) {
            getParentConnection().openConnection();
            getParentConnection().waitForActive(2000);
        }
    }

    private void releaseConnectionUsage() {
        int usages = this.connectionUsage.decrementAndGet();
        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, String.format("Connection usage on route %s released, current usages %d", getName(), usages));
        }
        // On demand and last use? Close it.
        if(usages == 0 && getParentConnection().getInit() == InitType.ON_DEMAND) {
            getParentConnection().closeConnection();
        }
    }

    private void deregisterFromVerifier(CommandTracker tracker) {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, String.format("Deregistering command tracker for outbound message %s on route %s from verifier", tracker.getMapping().getId(), getName()));
        }
        activeCommandTrackers.remove(tracker);
        if(tracker.getMapping().getVerification() != null) {
            Set<String> progressMessagesId = tracker.getProgressMessageIds();
            // Remove trackers
            for (String s : progressMessagesId) {
                List<CommandTracker> trackers = this.progressMessageId2commandTracker.computeIfAbsent(s, a -> new LinkedList<>());
                trackers.remove(tracker);
            }
            // Stop timeout
            stopTimeoutTimer(tracker);
        }
        // If command lock and no more commands pending, then be ready for the next one
        if(isCommandLock() && activeCommandTrackers.isEmpty()) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, String.format("Releasing command lock semaphore on route %s", getName()));
            }
            connectionSequencer.release();
        }
    }

    private void stopTimeoutTimer(CommandTracker tracker) {
        if(tracker != null && tracker.getMapping().getVerification() != null) {
            tracker.cancelTimeoutTimer(dataProcessor, false);
        }
    }

    private boolean writeToConnection(byte[] encodedCommand) throws IOException {
        return getParentConnection().writeMessage(encodedCommand);
    }

    private CommandTracker registerToVerifier(Instant time, IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping, Pair<byte[], Map<String, Object>> encodedCommand) {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, String.format("Registering outbound message %s on route %s to verifier", mapping.getId(), getName()));
        }

        CommandTracker tracker = new CommandTracker(time, activityInvocation, mapping, encodedCommand, getName());
        // Register in the tracker list
        activeCommandTrackers.add(tracker);
        if(mapping.getVerification() != null) {
            // Get the list of messages potentially affecting the verification of this command and register this tracker there
            Set<String> progressMessagesId = tracker.getProgressMessageIds();
            for(String s : progressMessagesId) {
                List<CommandTracker> trackers = this.progressMessageId2commandTracker.computeIfAbsent(s, a -> new LinkedList<>());
                trackers.add(tracker);
            }
            // Start the timer for timeout
            startTimeoutTimer(tracker);
        } else {
            // No verification, no active tracking
            activeCommandTrackers.remove(tracker);
        }
        return tracker;
    }

    private void startTimeoutTimer(CommandTracker tracker) {
        TimerTask timeoutTask = new TimerTask() {
            @Override
            public void run() {
                dataProcessor.execute(() -> timeout(tracker));
            }
        };
        tracker.registerTimeoutTask(timeoutTask);
        this.dataProcessor.getTimerService().schedule(timeoutTask, tracker.getMapping().getVerification().getTimeout() * 1000L);
    }

    private void timeout(CommandTracker tracker) {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, String.format("Timeout for outbound message tracker %s on route %s", tracker.getId(), getName()));
        }
        if(tracker.isAlive()) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, String.format("Tracker %s on route %s is alive - continuing with timeout processing", tracker.getId(), getName()));
            }
            // Cancel the timeout timer, marked as expired
            tracker.cancelTimeoutTimer(dataProcessor, true);
            // Release connection
            releaseConnectionUsage();
            // Remove from verifier
            deregisterFromVerifier(tracker);
        } else {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, String.format("Tracker %s on route %s is not alive - timeout processing skipped", tracker.getId(), getName()));
            }
        }
    }

    private Pair<byte[], Map<String, Object>> encodeCommand(IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping) throws ReatmetricException {
        return mapping.encodeCommand(activityInvocation, getParentConnection().getAsciiEncoding());
    }

    private OutboundMessageMapping getOutboundMessageFor(IActivityHandler.ActivityInvocation activityInvocation) {
        // Slow, consider adding a map
        for(OutboundMessageMapping omm : getOutboundMessages()) {
            if(omm.getEntity() + getEntityOffset() == activityInvocation.getActivityId()) {
                return omm;
            }
        }
        return null;
    }

    public void reportActivityState(int activityId, IUniqueId activityOccurrenceId, Instant time, ActivityOccurrenceState state, String releaseReportName, ActivityReportState status, ActivityOccurrenceState nextState) {
        this.dataProcessor.forwardActivityProgress(ActivityProgress.of(activityId, activityOccurrenceId, releaseReportName, time, state, null, status, nextState, null));
    }

    public void dispose() {
        Instant time = Instant.now();
        dataProcessor.execute(() -> internalDispose(time));
    }

    private void internalDispose(Instant time) {
        List<CommandTracker> trackersToClose = new ArrayList<>(activeCommandTrackers);
        for(CommandTracker ct : trackersToClose) {
            ct.closeVerification(this.dataProcessor, time);
            releaseConnectionUsage();
            deregisterFromVerifier(ct);
        }
    }

    public LinkedHashMap<String, String> render(RawData rawData) {
        // Only inbound messages supported
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        if(parentConnection.getProtocol() == ProtocolType.ASCII) {
            // ASCII message - look for name = message id
            for(InboundMessageMapping imm : inboundMessageMappings) {
                if(imm.getMessageDefinition().getId().equals(rawData.getName())) {
                    // Found
                    addAsciiRawFields((AsciiMessageDefinition) imm.getMessageDefinition(), toReturn, rawData);
                    break;
                }
            }
        } else if(parentConnection.getProtocol() == ProtocolType.BINARY) {
            // Binary message - look for binding with secondary id = name
            for(InboundMessageMapping imm : inboundMessageMappings) {
                if(imm.getSecondaryId().equals(rawData.getName())) {
                    // Found
                    addBinaryRawFields((BinaryMessageDefinition) imm.getMessageDefinition(), toReturn, rawData);
                    break;
                }
            }
        } else {
            // Not supported
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.warning(String.format("Protocol not supported for rendering raw data %s on route %s (handler=%s): %s", rawData.getName(), rawData.getRoute(), rawData.getHandler(), parentConnection.getProtocol().name()));
            }
        }
        return toReturn;
    }

    private void addBinaryRawFields(BinaryMessageDefinition messageDefinition, LinkedHashMap<String, String> toReturn, RawData rawData) {
        try {
            Map<String, Object> fieldValues = messageDefinition.decode(rawData.getName(), rawData.getContents());
            addValuesToRenderingMap(toReturn, fieldValues);
        } catch (ReatmetricException e) {
            LOG.warning("Cannot decode binary message for rendering purposes: id=" + messageDefinition.getId() + ", secondary id=" + rawData.getName() + ", message=" + StringUtil.toHexDump(rawData.getContents()));
        }
    }

    private void addAsciiRawFields(AsciiMessageDefinition messageDefinition, LinkedHashMap<String, String> toReturn, RawData rawData) {
        try {
            Map<String, Object> fieldValues = messageDefinition.decode(rawData.getName(), new String(rawData.getContents(), parentConnection.getAsciiEncoding().getCharset()));
            addValuesToRenderingMap(toReturn, fieldValues);
        } catch (ReatmetricException e) {
            LOG.warning("Cannot decode ASCII message for rendering purposes: id=" + rawData.getName() + ", message=" + new String(rawData.getContents(), parentConnection.getAsciiEncoding().getCharset()));
        }
    }

    private void addValuesToRenderingMap(LinkedHashMap<String, String> toReturn, Map<String, Object> fieldValues) {
        for(Map.Entry<String, Object> e : fieldValues.entrySet()) {
            toReturn.put(e.getKey(), ValueUtil.toString(e.getValue()));
        }
    }
}
