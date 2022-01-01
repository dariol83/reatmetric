/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandVerifier implements IRawDataSubscriber {

    private static final Logger LOG = Logger.getLogger(CommandVerifier.class.getName());

    private final Map<Integer, IActivityHandler.ActivityInvocation> commandTag2activityInvocation = new HashMap<>();
    private final TestDriver driver;
    private final IRawDataBroker broker;

    public CommandVerifier(TestDriver driver, IRawDataBroker broker) {
        this.driver = driver;
        // Since this object is performing the command verification function, it registers to the broker to receive
        // notification of received command acks
        this.broker = broker;
        this.broker.subscribe(this, null, new RawDataFilter(true, null, null, Collections.singletonList(TestDriver.STATION_ACK), null, Collections.singletonList(Quality.GOOD)), null);
    }

    public synchronized void removeCommandVerification(int cmdTag) {
        this.commandTag2activityInvocation.remove(cmdTag);
    }

    public void recordCommandVerification(int cmdTag, IActivityHandler.ActivityInvocation activityInvocation) {
        synchronized (this) {
            this.commandTag2activityInvocation.put(cmdTag, activityInvocation);
        }
        // Announce all expected execution stages
        this.driver.announce(activityInvocation, Instant.now(), TestDriver.ACCEPTANCE_STAGE, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.EXECUTION);
        this.driver.announce(activityInvocation, Instant.now(), TestDriver.EXECUTION_START_STAGE, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.EXECUTION);
        this.driver.announce(activityInvocation, Instant.now(), TestDriver.EXECUTION_COMPLETED_STAGE, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.EXECUTION);
    }

    private void processCommandAck(RawData rd) {
        ByteBuffer bb = ByteBuffer.wrap(rd.getContents());
        // Read the tag
        byte firstByte = bb.get();
        firstByte &= 0x0F;
        long timestamp = bb.getLong();
        int commandId = bb.getInt();
        boolean ok = bb.get() == 1;
        IActivityHandler.ActivityInvocation inv;
        synchronized (this) {
            inv = commandTag2activityInvocation.get(commandId);
        }
        if(inv != null) {
            switch (firstByte) {
                case 2: // Acceptance
                    this.driver.announce(inv, Instant.ofEpochMilli(timestamp), TestDriver.ACCEPTANCE_STAGE, ok ? ActivityReportState.OK : ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.EXECUTION);
                    break;
                case 3: // Start
                    this.driver.announce(inv, Instant.ofEpochMilli(timestamp), TestDriver.EXECUTION_START_STAGE, ok ? ActivityReportState.OK : ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.EXECUTION, Instant.now(), null);
                    break;
                case 4: // Completion
                    this.driver.announce(inv, Instant.ofEpochMilli(timestamp), TestDriver.EXECUTION_COMPLETED_STAGE, ok ? ActivityReportState.OK : ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION, ActivityOccurrenceState.VERIFICATION, null, null);
                    break;
            }
        } else {
            LOG.log(Level.WARNING, "Received command report with tag " + commandId + ", not matching any pending command");
        }
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        for(RawData rd : messages) {
            processCommandAck(rd);
        }
    }

    public void shutdown() {
        this.broker.unsubscribe(this);
    }

    @Override
    public String toString() {
        return "Command Verifier";
    }
}
