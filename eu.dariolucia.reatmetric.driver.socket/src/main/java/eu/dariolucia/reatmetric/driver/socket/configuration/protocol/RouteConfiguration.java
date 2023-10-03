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
import eu.dariolucia.reatmetric.driver.socket.SocketDriver;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.InitType;
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

    // If this is true, it means that the execution of this command must complete, before sending the next one
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

    private AbstractConnectionConfiguration parentConnection;

    // <MessageDefinition ID>_<secondary ID> as key
    private final Map<String, List<InboundMessageMapping>> messageId2mapping = new TreeMap<>();

    private IDataProcessor dataProcessor;
    private final Map<OutboundMessageMapping, CommandTracker> outboundMessage2lastCommand = new ConcurrentHashMap<>();
    private final Map<String, List<CommandTracker>> progressMessageId2commandTracker = new TreeMap<>();
    private final List<CommandTracker> activeCommandTrackers = new CopyOnWriteArrayList<>();
    private final AtomicInteger connectionUsage = new AtomicInteger(0);
    private final Semaphore connectionSequencer = new Semaphore(1);
    private final List<TimerTask> periodCommandTasks = new CopyOnWriteArrayList<>();

    public void initialise(AbstractConnectionConfiguration parentConnection) {
        this.parentConnection = parentConnection;
        for(InboundMessageMapping m : getInboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
            messageId2mapping.computeIfAbsent(m.getMessageDefinition().getId() + "_" + m.getSecondaryId(), k -> new LinkedList<>()).add(m);
        }
        for(OutboundMessageMapping m : getOutboundMessages()) {
            m.initialise(parentConnection, getEntityOffset());
        }
    }

    private void internalMessageReceived(Instant time, String messageId, String secondaryId, Map<String, Object> decodedMessage, byte[] rawMessage) {
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
        List<InboundMessageMapping> inboundMessageMappingsForMessage = messageId2mapping.get(key);
        if(inboundMessageMappingsForMessage != null) {
            for(InboundMessageMapping imm : inboundMessageMappingsForMessage) {
                if(imm.getCommand() != null) {
                    OutboundMessageMapping omm = imm.getCommand().getOutboundMapping();
                    // Get the last sent command, if any, linked to this
                    CommandTracker lastCommand = outboundMessage2lastCommand.get(omm);
                    // At this stage, if the command is there and no specific argument is specified or the argument matches...
                    if(imm.getCommand().match(lastCommand)) {
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
                internalMessageReceived(receivedTime, identifier, secondaryIdentifier, decodedMessage, message);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, String.format("Error detected when identifying binary message %s with definition %s on route %s: %s", secondaryIdentifier, definition.getId(), getName(), e.getMessage()), e);
            }
        } else {
            if(LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, String.format("Binary message not identified on route %s", getName()));
            }
        }
    }

    public void onAsciiMessageReceived(String message, byte[] rawMessage) {
        final Instant receivedTime = Instant.now();
        this.dataProcessor.execute(() -> internalAsciiMessageReceived(message, rawMessage, receivedTime));
    }

    private void internalAsciiMessageReceived(String message, byte[] rawMessage, Instant receivedTime) {
        String identifier = null;
        String secondaryIdentifier = null;
        AsciiMessageDefinition definition = null;
        // You received an ASCII message: get the message definition, identify the message
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
            internalMessageReceived(receivedTime, identifier, secondaryIdentifier, decodedMessage, rawMessage);
        } else {
            LOG.log(Level.WARNING, "ASCII message not identified on route " + getName());
        }
    }

    public void setDataProcessor(SocketDriver dataProcessor) {
        this.dataProcessor = dataProcessor;
    }

    public void dispatchActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        Instant time = Instant.now();
        // Get the mapped OutboundMessageMapping
        OutboundMessageMapping mapping = getOutboundMessageFor(activityInvocation);
        if(mapping == null) {
            throw new ActivityHandlingException("No outbound message found, mapped to activity invocation for " + activityInvocation.getPath() + " (" + activityInvocation.getActivityId() + ")");
        }
        // Encode command
        Pair<byte[], Map<String, Object>> encodedCommand = null;
        try {
            encodedCommand = encodeCommand(activityInvocation, mapping);
        } catch (ReatmetricException e) {
            throw new ActivityHandlingException("Error while encoding command " + activityInvocation.getActivityId() + ": " + e.getMessage(), e);
        }
        Pair<byte[], Map<String, Object>> finalCommand = encodedCommand;
        // If the connection is one of those requiring a single command active at a time, then put the command in the dispatch
        // phase only if you actually can
        if(isCommandLock()) {
            try {
                connectionSequencer.acquire();
            } catch (InterruptedException e) {
                throw new ActivityHandlingException("Waiting dispatch interrupted for activity " + activityInvocation.getActivityOccurrenceId() + " on route " + getName(), e);
            }
        }
        // At this stage, the work can be done fully asynchronously
        this.dataProcessor.execute(() -> internalDispatch(activityInvocation, time, mapping, finalCommand));
    }

    public void startDispatchingOfPeriodicCommands() {
        if(getParentConnection().getInit() == InitType.CONNECTOR) {
            // Schedule execution of all the periodic commands
            for (OutboundMessageMapping omm : getOutboundMessages()) {
                if (omm.getType() == OutboundMessageType.PERIODIC) {
                    TimerTask periodTask = new TimerTask() {
                        @Override
                        public void run() {
                            dataProcessor.execute(() -> internalPeriodicDispatch(omm));
                        }
                    };
                    this.periodCommandTasks.add(periodTask);
                    this.dataProcessor.getTimerService().schedule(periodTask, omm.getPeriod() * 1000L, omm.getPeriod() * 1000L);
                }
            }
        } // ignore the ON-DEMAND connections
    }

    private void internalPeriodicDispatch(OutboundMessageMapping mapping) {
        if(!getParentConnection().isOpen()) {
            return;
        }
        // Encode command
        Pair<byte[], Map<String, Object>> encodedCommand;
        try {
            encodedCommand = encodeCommand(null, mapping);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot encode command " + mapping.getMessageDefinition().getId() + " on route " + getName() + " for periodic dispatch: " + e.getMessage(), e);
            return;
        }
        // If you want a lock, wait
        if(isCommandLock()) {
            try {
                connectionSequencer.acquire();
            } catch (InterruptedException e) {
                LOG.log(Level.FINE, "Command lock interrupted on dispatch of periodic command " + mapping.getMessageDefinition().getId() + " on route " + getName());
                return;
            }
        }
        acquireConnectionUsage();
        try {
            writeToConnection(encodedCommand.getFirst());
            waitForPostDelay(mapping);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot transmit command " + mapping.getMessageDefinition().getId() + " on route " + getName() + " for periodic dispatch: " + e.getMessage(), e);
        } finally {
            releaseConnectionUsage();
            if(isCommandLock()) {
                connectionSequencer.release();
            }
        }
    }

    public void stopDispatchingOfPeriodicCommands() {
        if(getParentConnection().getInit() == InitType.CONNECTOR) {
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
            writeToConnection(encodedCommand.getFirst());
            // If all nominal...
            // ... register the command as last command sent for the given outbound message mapping
            outboundMessage2lastCommand.put(mapping, tracker);
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
        // On demand and last use? Close it.
        if(usages == 0 && getParentConnection().getInit() == InitType.ON_DEMAND) {
            getParentConnection().closeConnection();
        }
    }

    private void deregisterFromVerifier(CommandTracker tracker) {
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
            connectionSequencer.release();
        }
    }

    private void stopTimeoutTimer(CommandTracker tracker) {
        if(tracker != null && tracker.getMapping().getVerification() != null) {
            tracker.cancelTimeoutTimer(dataProcessor, false);
        }
    }

    private void writeToConnection(byte[] encodedCommand) throws IOException {
        getParentConnection().writeMessage(encodedCommand);
    }

    private CommandTracker registerToVerifier(Instant time, IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping, Pair<byte[], Map<String, Object>> encodedCommand) {
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
        if(tracker.isAlive()) {
            // Cancel the timeout timer, marked as expired
            tracker.cancelTimeoutTimer(dataProcessor, true);
            // Release connection
            releaseConnectionUsage();
            // Remove from verifier
            deregisterFromVerifier(tracker);
        }
    }

    private Pair<byte[], Map<String, Object>> encodeCommand(IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping) throws ReatmetricException {
        return mapping.encodeCommand(activityInvocation, getParentConnection().getAsciiEncoding());
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
}
