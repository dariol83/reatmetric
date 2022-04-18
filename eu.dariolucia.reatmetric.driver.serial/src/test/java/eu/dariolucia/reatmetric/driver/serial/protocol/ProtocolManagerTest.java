/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.serial.protocol;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class ProtocolManagerTest {

    @Test
    public void testProtocolManager() {
        IMonitoringDataManager man = new MonitoringDataManagerStub();
        ProtocolManager pm = new ProtocolManager(man,10);

        send("HELLO TestApp", pm);
        send("PING", pm);
        send("REG_PARAM aa.bb.cc.dd", pm);
        send("REG_PARAM aa.bb.cc.ee", pm);
        send("REG_PARAM aa.bb.cc.ff", pm);
        send("UPDATE_PARAM", pm);
        send("DEREG_PARAM 2", pm);
        send("UPDATE_PARAM", pm);
        send("DEREG_PARAM_ALL", pm);
        send("UPDATE_PARAM", pm);
        send("UPDATE_LOG", pm);
        send("PING", pm);
        send("BYE", pm);
        send("CYA", pm);
        send("PING", pm);
        send("HELLO   ", pm);

    }

    private void send(String command, ProtocolManager pm) {
        System.out.println("Sent: " + command);
        print(pm.event(command));
    }

    private void print(byte[] data) {
        System.out.println("Received: ");
        System.out.println(new String(data, StandardCharsets.US_ASCII));
    }

    private static class MonitoringDataManagerStub implements IMonitoringDataManager {

        private AtomicInteger sequencer = new AtomicInteger(0);
        private List<Integer> freedIds = new LinkedList<>();
        private Map<Integer, ParameterData> registeredParameter = new LinkedHashMap<>();

        @Override
        public int registerParameter(String parameterPath) {
            // Always fine: get the id
            int id = -1;
            if(freedIds.size() > 0) {
                id = freedIds.remove(0);
            } else {
                id = sequencer.getAndIncrement();
            }
            SystemEntityPath path = SystemEntityPath.fromString(parameterPath);
            ParameterData pd = new ParameterData(new LongUniqueId(id), Instant.now(), 0, path.getLastPathElement(), path, 23.0, 11.1, "", Validity.VALID,
                    AlarmState.WARNING, null, Instant.now(), null);
            registeredParameter.put(id, pd);
            return id;
        }

        @Override
        public boolean deregisterParameter(int parameterId) {
            if(registeredParameter.containsKey(parameterId)) {
                registeredParameter.remove(parameterId);
                freedIds.add(parameterId);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void deregisterAllParameter() {
            freedIds.addAll(registeredParameter.keySet());
            registeredParameter.clear();
        }

        @Override
        public List<ParameterData> updateParameters() {
            List<ParameterData> toReturn = new ArrayList<>(registeredParameter.size());
            for(Map.Entry<Integer, ParameterData> e : registeredParameter.entrySet()) {
                toReturn.add(e.getValue());
            }
            return toReturn;
        }

        @Override
        public List<OperationalMessage> updateLogs() {
            List<OperationalMessage> messages = new LinkedList<>();
            for(int i = 0;i < 5; ++i) {
                OperationalMessage om = new OperationalMessage(new LongUniqueId(i), Instant.now(), "Test"+i, "Test message number " + i, "", Severity.INFO, null, null);
                messages.add(om);
            }
            return messages;
        }
    }
}