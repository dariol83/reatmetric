package eu.dariolucia.reatmetric.driver.socket.configuration.protocol;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.time.Instant;
import java.util.*;

public class CommandTracker {

    public static final String ACCEPTANCE_STAGE_NAME = "Acceptance";
    public static final String EXECUTION_STAGE_NAME = "Execution";
    private final Instant time;
    private final IActivityHandler.ActivityInvocation activityInvocation;
    private final OutboundMessageMapping mapping;
    private final Pair<byte[], Map<String, Object>> encodedCommand;
    private final String route;
    private final long id;
    private TimerTask timeoutTask;

    private String currentlyOpenStage = null;

    public CommandTracker(Instant time, IActivityHandler.ActivityInvocation activityInvocation, OutboundMessageMapping mapping, Pair<byte[], Map<String, Object>> encodedCommand, String route) {
        this.time = time;
        this.activityInvocation = activityInvocation;
        this.mapping = mapping;
        this.encodedCommand = encodedCommand;
        this.route = route;
        if(activityInvocation != null) {
            long idToCompute = activityInvocation.getActivityId();
            idToCompute <<= 32;
            idToCompute |= (activityInvocation.getActivityOccurrenceId().asLong() & 0x00000000FFFFFFFFL);
            this.id = idToCompute;
        } else {
            this.id = -1;
        }
    }

    public long getId() {
        return id;
    }

    public void registerTimeoutTask(TimerTask task) {
        this.timeoutTask = task;
    }

    public Set<String> getProgressMessageIds() {
        Set<String> theMessages = new LinkedHashSet<>();
        for(StageConfiguration sc : this.mapping.getVerification().getAcceptance()) {
            String message = sc.getMessage().getId();
            if(sc.getMessageSecondaryId() != null) {
                message += "_" + sc.getMessageSecondaryId();
            } else {
                message += "_";
            }
            theMessages.add(message);
        }
        for(StageConfiguration sc : this.mapping.getVerification().getExecution()) {
            String message = sc.getMessage().getId();
            if(sc.getMessageSecondaryId() != null) {
                message += "_" + sc.getMessageSecondaryId();
            } else {
                message += "_";
            }
            theMessages.add(message);
        }
        return theMessages;
    }

    public Instant getTime() {
        return time;
    }

    public IActivityHandler.ActivityInvocation getActivityInvocation() {
        return activityInvocation;
    }

    public OutboundMessageMapping getMapping() {
        return mapping;
    }

    public Pair<byte[], Map<String, Object>> getEncodedCommand() {
        return encodedCommand;
    }

    public String getRoute() {
        return route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandTracker that = (CommandTracker) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void cancelTimeoutTimer(IDataProcessor dataProcessor, boolean expired) {
        Instant now = Instant.now();
        if(this.timeoutTask != null) {
            if(!expired) {
                this.timeoutTask.cancel();
            }
            this.timeoutTask = null;
            if(expired) {
                // Inform model
                if(this.currentlyOpenStage != null) {
                    if(this.currentlyOpenStage.equals(ACCEPTANCE_STAGE_NAME)) {
                        if(!mapping.getVerification().getExecution().isEmpty()) {
                            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                                    ACCEPTANCE_STAGE_NAME, now, ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.TIMEOUT, ActivityOccurrenceState.TRANSMISSION, null));
                            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                                    EXECUTION_STAGE_NAME, now, ActivityOccurrenceState.EXECUTION, null, ActivityReportState.TIMEOUT, ActivityOccurrenceState.VERIFICATION, null));
                        } else {
                            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                                    ACCEPTANCE_STAGE_NAME, now, ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.TIMEOUT, ActivityOccurrenceState.VERIFICATION, null));
                        }
                    } else if(this.currentlyOpenStage.equals(EXECUTION_STAGE_NAME)) {
                        dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                                EXECUTION_STAGE_NAME, now, ActivityOccurrenceState.EXECUTION, null, ActivityReportState.TIMEOUT, ActivityOccurrenceState.VERIFICATION, null));
                    }
                }
            }
        }
    }

    public boolean isAlive() {
        return this.timeoutTask != null;
    }

    public void announceVerificationStages(IDataProcessor dataProcessor) {
        Instant now = Instant.now();
        if(!mapping.getVerification().getAcceptance().isEmpty()) {
            if(activityInvocation != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        ACCEPTANCE_STAGE_NAME, now, ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION, null));
            }
            this.currentlyOpenStage = ACCEPTANCE_STAGE_NAME;
        }
        if(!mapping.getVerification().getExecution().isEmpty()) {
            if(activityInvocation != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        EXECUTION_STAGE_NAME, now, ActivityOccurrenceState.EXECUTION, null, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null));
            }
            if(this.currentlyOpenStage == null) {
                this.currentlyOpenStage = EXECUTION_STAGE_NAME;
            }
        }
    }

    public boolean messageReceived(IDataProcessor dataProcessor, Instant time, String messageId, String secondaryId, Map<String, Object> decodedMessage, String route) {
        // Depending on the open stage, iterate
        if(this.currentlyOpenStage != null) {
            if(this.currentlyOpenStage.equals(ACCEPTANCE_STAGE_NAME)) {
                ActivityReportState acceptanceStatus = processStage(mapping.getVerification().getAcceptance(), messageId, secondaryId, decodedMessage, route);
                if(acceptanceStatus == null) {
                    // No match, skip
                    return false;
                }
                switch(acceptanceStatus) {
                    case OK:
                        return processAcceptanceOK(dataProcessor, time, acceptanceStatus);
                    case FAIL:
                    case ERROR:
                    case FATAL:
                        return processAcceptanceFail(dataProcessor, time, acceptanceStatus);
                    default:
                        return processAcceptanceDefault(dataProcessor, time, acceptanceStatus);
                }
            } else if(this.currentlyOpenStage.equals(EXECUTION_STAGE_NAME)) {
                ActivityReportState executionStatus = processStage(mapping.getVerification().getExecution(), messageId, secondaryId, decodedMessage, route);
                if(executionStatus == null) {
                    // No match, skip
                    return false;
                }
                switch(executionStatus) {
                    case OK:
                        return processExecutionOK(dataProcessor, time, executionStatus);
                    case FAIL:
                    case ERROR:
                    case FATAL:
                        return processExecutionFail(dataProcessor, time, executionStatus);
                    default:
                        return processExecutionDefault(dataProcessor, time, executionStatus);
                }
            } else {
                // Stage is unknown, ignore
                return false;
            }
        } else {
            // Opened stage is null, ignore
            return false;
        }
    }

    private ActivityReportState processStage(List<StageConfiguration> stageConfigurationList, String messageId, String secondaryId, Map<String, Object> decodedMessage, String route) {
        for(StageConfiguration sc : stageConfigurationList) {
            boolean matching = false;
            if(messageId.equals(sc.getMessage().getId())) {
                if(sc.getMessageSecondaryId() == null || Objects.equals(secondaryId, sc.getMessageSecondaryId())) {
                    // If the field for the ID correlation is defined, look for it
                    if(sc.getMessageFieldIdName() != null) {
                        Object commandId = decodedMessage.get(sc.getMessageFieldIdName());
                        Object originalId = this.encodedCommand.getSecond().get(mapping.getVerification().getFieldIdName());
                        if(Objects.equals(commandId, originalId)) {
                            matching = true;
                        }
                    } else {
                        // If not, this message is anyway good
                        matching = true;
                    }
                }
            }
            if(matching) {
                // Good, derive result
                if(sc.getMessageFieldValueName() != null) {
                    Object messageDeliveredValue = decodedMessage.get(sc.getMessageFieldValueName());
                    // Either reference to argument or fixed value
                    if(sc.getReferenceArgument() != null) {
                        Object argumentValue = this.encodedCommand.getSecond().get(sc.getReferenceArgument());
                        if(Objects.equals(messageDeliveredValue, argumentValue)) {
                            return sc.getResult();
                        }
                    } else if(sc.getExpectedFixedValue() != null) {
                        Object expectedValue = ValueUtil.parse(sc.getExpectedFixedValueType(), sc.getExpectedFixedValue());
                        if(Objects.equals(messageDeliveredValue, expectedValue)) {
                            return sc.getResult();
                        }
                    }
                } else {
                    // Derive direct result
                    return sc.getResult();
                }
            }
        }
        return null;
    }

    private boolean processExecutionDefault(IDataProcessor dataProcessor, Instant time, ActivityReportState executionStatus) {
        if(getActivityInvocation() != null) {
            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                    EXECUTION_STAGE_NAME, time, ActivityOccurrenceState.EXECUTION, time, executionStatus, ActivityOccurrenceState.EXECUTION, null));
        }
        return false;
    }

    private boolean processExecutionFail(IDataProcessor dataProcessor, Instant time, ActivityReportState executionStatus) {
        if(getActivityInvocation() != null) {
            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                    EXECUTION_STAGE_NAME, time, ActivityOccurrenceState.EXECUTION, time, executionStatus, ActivityOccurrenceState.VERIFICATION, null));
        }
        // Stop timer
        cancelTimeoutTimer(dataProcessor, false);
        // You are done
        return true;
    }

    private boolean processExecutionOK(IDataProcessor dataProcessor, Instant time, ActivityReportState executionStatus) {
        if(getActivityInvocation() != null) {
            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                    EXECUTION_STAGE_NAME, time, ActivityOccurrenceState.EXECUTION, time, executionStatus, ActivityOccurrenceState.VERIFICATION, null));
        }
        // Stop timer
        cancelTimeoutTimer(dataProcessor, false);
        // You are done
        return true;
    }

    private boolean processAcceptanceDefault(IDataProcessor dataProcessor, Instant time, ActivityReportState acceptanceStatus) {
        if(getActivityInvocation() != null) {
            dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                    ACCEPTANCE_STAGE_NAME, time, ActivityOccurrenceState.TRANSMISSION, time, acceptanceStatus, ActivityOccurrenceState.TRANSMISSION, null));
        }
        return false;
    }

    private boolean processAcceptanceFail(IDataProcessor dataProcessor, Instant time, ActivityReportState acceptanceStatus) {
        if(!mapping.getVerification().getExecution().isEmpty()) {
            if(getActivityInvocation() != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        ACCEPTANCE_STAGE_NAME, time, ActivityOccurrenceState.TRANSMISSION, null, acceptanceStatus, ActivityOccurrenceState.EXECUTION, null));
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        EXECUTION_STAGE_NAME, time, ActivityOccurrenceState.EXECUTION, null, ActivityReportState.UNKNOWN, ActivityOccurrenceState.VERIFICATION, null));
            }
        } else {
            if(getActivityInvocation() != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        ACCEPTANCE_STAGE_NAME, time, ActivityOccurrenceState.TRANSMISSION, time, acceptanceStatus, ActivityOccurrenceState.VERIFICATION, null));
            }
        }
        // Stop timer
        cancelTimeoutTimer(dataProcessor, false);
        // You are done
        return true;
    }

    private boolean processAcceptanceOK(IDataProcessor dataProcessor, Instant time, ActivityReportState acceptanceStatus) {
        if(!mapping.getVerification().getExecution().isEmpty()) {
            if(getActivityInvocation() != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        ACCEPTANCE_STAGE_NAME, time, ActivityOccurrenceState.TRANSMISSION, null, acceptanceStatus, ActivityOccurrenceState.EXECUTION, null));
            }
            this.currentlyOpenStage = EXECUTION_STAGE_NAME;
            return false;
        } else {
            if(getActivityInvocation() != null) {
                dataProcessor.forwardActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(),
                        ACCEPTANCE_STAGE_NAME, time, ActivityOccurrenceState.TRANSMISSION, time, acceptanceStatus, ActivityOccurrenceState.VERIFICATION, null));
            }
            // Stop timer
            cancelTimeoutTimer(dataProcessor, false);
            // You are done
            return true;
        }
    }

    public void closeVerification(IDataProcessor dataProcessor, Instant time) {
        cancelTimeoutTimer(dataProcessor, false);
        // Report all open stage as UNKNOWN, close verification
        if(this.currentlyOpenStage != null) {
            if(this.currentlyOpenStage.equals(ACCEPTANCE_STAGE_NAME)) {
                processAcceptanceDefault(dataProcessor, time, ActivityReportState.UNKNOWN);
                if(!mapping.getVerification().getExecution().isEmpty()) {
                    processExecutionDefault(dataProcessor, time, ActivityReportState.UNKNOWN);
                }
            } else if(this.currentlyOpenStage.equals(EXECUTION_STAGE_NAME)) {
                processExecutionDefault(dataProcessor, time, ActivityReportState.UNKNOWN);
            }
        }
    }

    @Override
    public String toString() {
        return "CommandTracker{" +
                "mapping=" + mapping +
                ", route='" + route + '\'' +
                ", id=" + id +
                ", currentlyOpenStage='" + currentlyOpenStage + '\'' +
                '}';
    }
}
