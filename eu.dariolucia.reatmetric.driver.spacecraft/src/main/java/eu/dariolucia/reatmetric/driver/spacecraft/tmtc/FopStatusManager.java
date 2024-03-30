/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.ccsds.tmtc.cop1.fop.FopAlertCode;
import eu.dariolucia.ccsds.tmtc.cop1.fop.FopStatus;
import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FopStatusManager {

    private static final Logger LOG = Logger.getLogger(FopStatusManager.class.getName());
    private static final String V_S_NAME = "V_S";
    private static final String SENT_QUEUE_SIZE_NAME = "SENT_QUEUE_SIZE";
    private static final String WAIT_QUEUE_FULL_NAME = "WAIT_QUEUE_FULL";
    private static final String AD_OUT_READY_NAME = "AD_OUT_READY";
    private static final String BC_OUT_READY_NAME = "BC_OUT_READY";
    private static final String BD_OUT_READY_NAME = "BD_OUT_READY";
    private static final String STATE_NAME = "STATE";
    private static final String LAST_EVENT_NAME = "LAST_EVENT";

    private static final String CLCW_LOCKOUT = "LOCKOUT";
    private static final String CLCW_WAIT = "WAIT";
    private static final String CLCW_RETRANSMIT = "RETRANSMIT";
    private static final String CLCW_NO_RF = "NO_RF_AVAILABLE";
    private static final String CLCW_NO_BL = "NO_BITLOCK_AVAILABLE";
    private static final String CLCW_FARM = "FARMB_COUNTER";
    private static final String CLCW_REPORT = "REPORT_VALUE";

    private static final String ALERT_NAME = "ALERT";
    private static final String SUSPEND_NAME = "SUSPEND";

    private final int tcVc;
    private final SystemEntityPath parentPath;
    private final IProcessingModel processingModel;

    private Map<String, ParameterDescriptor> pname2descriptor = new TreeMap<>();
    private Map<String, EventDescriptor> ename2descriptor = new TreeMap<>();
    private volatile FopStatus lastReceivedStatus;

    private volatile Clcw lastReceivedClcw;

    public FopStatusManager(int tcVc, String systemEntityPath, IProcessingModel processingModel) {
        this.tcVc = tcVc;
        this.parentPath = SystemEntityPath.fromString(systemEntityPath);
        this.processingModel = processingModel;
    }

    public boolean initialise() {
        // Look up for all descriptors and prepare the maps
        lookupAndAdd(V_S_NAME,  ValueTypeEnum.ENUMERATED);
        lookupAndAdd(SENT_QUEUE_SIZE_NAME,  ValueTypeEnum.ENUMERATED);
        lookupAndAdd(WAIT_QUEUE_FULL_NAME,  ValueTypeEnum.BOOLEAN);
        lookupAndAdd(AD_OUT_READY_NAME,  ValueTypeEnum.BOOLEAN);
        lookupAndAdd(BC_OUT_READY_NAME,  ValueTypeEnum.BOOLEAN);
        lookupAndAdd(BD_OUT_READY_NAME,  ValueTypeEnum.BOOLEAN);
        lookupAndAdd(STATE_NAME,  ValueTypeEnum.ENUMERATED);
        lookupAndAdd(LAST_EVENT_NAME,  ValueTypeEnum.CHARACTER_STRING);
        lookupAndAdd(ALERT_NAME,  null);
        lookupAndAdd(SUSPEND_NAME, null);

        lookupAndAdd(CLCW_LOCKOUT, ValueTypeEnum.BOOLEAN);
        lookupAndAdd(CLCW_WAIT, ValueTypeEnum.BOOLEAN);
        lookupAndAdd(CLCW_RETRANSMIT, ValueTypeEnum.BOOLEAN);
        lookupAndAdd(CLCW_NO_BL, ValueTypeEnum.BOOLEAN);
        lookupAndAdd(CLCW_NO_RF, ValueTypeEnum.BOOLEAN);
        lookupAndAdd(CLCW_FARM, ValueTypeEnum.UNSIGNED_INTEGER);
        lookupAndAdd(CLCW_REPORT, ValueTypeEnum.UNSIGNED_INTEGER);

        return !pname2descriptor.isEmpty() || !ename2descriptor.isEmpty();
    }

    private void lookupAndAdd(String name, ValueTypeEnum paramType) {
        SystemEntityPath element = this.parentPath.append(name);
        try {
            AbstractSystemEntityDescriptor descriptor = processingModel.getDescriptorOf(element);
            if (paramType != null && descriptor instanceof ParameterDescriptor && ((ParameterDescriptor) descriptor).getRawDataType() == paramType) {
                this.pname2descriptor.put(name, (ParameterDescriptor) descriptor);
            } else if (paramType == null && descriptor instanceof EventDescriptor) {
                this.ename2descriptor.put(name, (EventDescriptor) descriptor);
            } else {
                LOG.log(Level.WARNING, String.format("FOP status manager: element %s not found, updates will not be provided", element));
            }
        } catch (ReatmetricException e) {
            LOG.log(Level.WARNING, String.format("FOP status manager: processing model error when fetching element %s, updates will not be provided", element), e);
        }
    }

    public void raiseAlertIndication(FopAlertCode code) {
        // Fetch descriptor
        EventDescriptor ed = this.ename2descriptor.get(ALERT_NAME);
        if(ed != null) {
            Instant time = Instant.now();
            this.processingModel.raiseEvent(
                EventOccurrence.of(ed.getExternalId(),
                        time,
                        time,
                        null,
                        code.name(),
                        null,
                        "FOP",
                        String.valueOf(this.tcVc),
                        null)
            );
        }
    }

    public void raiseSuspendIndication() {
        // Fetch descriptor
        EventDescriptor ed = this.ename2descriptor.get(SUSPEND_NAME);
        if(ed != null) {
            Instant time = Instant.now();
            this.processingModel.raiseEvent(
                    EventOccurrence.of(ed.getExternalId(),
                            time,
                            time,
                            null,
                            "",
                            null,
                            "FOP",
                            String.valueOf(this.tcVc),
                            null)
            );
        }
    }

    public void injectStatusUpdate(FopStatus currentStatus) {
        FopStatus previous = this.lastReceivedStatus;
        this.lastReceivedStatus = currentStatus;
        List<ParameterSample> toUpdate = new LinkedList<>();
        Instant time = Instant.now();
        addIfDifferent(V_S_NAME, currentStatus, previous, toUpdate, time, FopStatus::getExpectedAckFrameSequenceNumber);
        addIfDifferent(SENT_QUEUE_SIZE_NAME, currentStatus, previous, toUpdate, time, FopStatus::getSentQueueItems);
        addIfDifferent(WAIT_QUEUE_FULL_NAME, currentStatus, previous, toUpdate, time, FopStatus::isWaitQueueFull);
        addIfDifferent(AD_OUT_READY_NAME, currentStatus, previous, toUpdate, time, FopStatus::isAdOutReadyFlag);
        addIfDifferent(BC_OUT_READY_NAME, currentStatus, previous, toUpdate, time, FopStatus::isBcOutReadyFlag);
        addIfDifferent(BD_OUT_READY_NAME, currentStatus, previous, toUpdate, time, FopStatus::isBdOutReadyFlag);
        addIfDifferent(STATE_NAME, currentStatus, previous, toUpdate, time, this::currentStateFetcher);
        addIfDifferent(LAST_EVENT_NAME, currentStatus, previous, toUpdate, time, this::lastEventFetcher);
        if(!toUpdate.isEmpty()) {
            this.processingModel.injectParameters(toUpdate);
        }
    }

    private Object lastEventFetcher(FopStatus fopStatus) {
        return fopStatus.getEvent().name();
    }

    private Object currentStateFetcher(FopStatus fopStatus) {
        return fopStatus.getCurrentState().ordinal();
    }

    private void addIfDifferent(String name, FopStatus current, FopStatus previous, List<ParameterSample> toUpdate, Instant time, Function<FopStatus, Object> propertyFetcher) {
        try {
            if (previous == null || !Objects.equals(propertyFetcher.apply(previous), propertyFetcher.apply(current))) {
                ParameterDescriptor pd = pname2descriptor.get(name);
                if (pd != null) {
                    toUpdate.add(ParameterSample.of(pd.getExternalId(), time, time, null,
                            toValue(propertyFetcher.apply(current), pd.getRawDataType()), "FOP", null));
                }
            }
        } catch (ValueException e) {
            // Ignore update
        }
    }

    private Object toValue(Object value, ValueTypeEnum rawDataType) throws ValueException {
        return ValueUtil.convert(value, rawDataType);
    }

    public void injectClcwUpdate(Instant frameGenTime, Instant frameRcpTime, Clcw clcw, String route) {
        Clcw previous = this.lastReceivedClcw;
        this.lastReceivedClcw = clcw;
        List<ParameterSample> toUpdate = new LinkedList<>();
        addIfDifferent(CLCW_LOCKOUT, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::isLockoutFlag, route);
        addIfDifferent(CLCW_WAIT, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::isWaitFlag, route);
        addIfDifferent(CLCW_RETRANSMIT, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::isRetransmitFlag, route);
        addIfDifferent(CLCW_NO_RF, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::isNoRfAvailableFlag, route);
        addIfDifferent(CLCW_NO_BL, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::isNoBitlockFlag, route);
        addIfDifferent(CLCW_FARM, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::getFarmBCounter, route);
        addIfDifferent(CLCW_REPORT, clcw, previous, toUpdate, frameGenTime, frameRcpTime, Clcw::getReportValue, route);

        if(!toUpdate.isEmpty()) {
            this.processingModel.injectParameters(toUpdate);
        }
    }

    private void addIfDifferent(String name, Clcw current, Clcw previous, List<ParameterSample> toUpdate, Instant genTime, Instant rcpTime, Function<Clcw, Object> propertyFetcher, String route) {
        try {
            if (previous == null || !Objects.equals(propertyFetcher.apply(previous), propertyFetcher.apply(current))) {
                ParameterDescriptor pd = pname2descriptor.get(name);
                if (pd != null) {
                    toUpdate.add(ParameterSample.of(pd.getExternalId(), genTime, rcpTime, null,
                            toValue(propertyFetcher.apply(current), pd.getRawDataType()), route, null));
                }
            }
        } catch (ValueException e) {
            // Ignore update
        }
    }
}
