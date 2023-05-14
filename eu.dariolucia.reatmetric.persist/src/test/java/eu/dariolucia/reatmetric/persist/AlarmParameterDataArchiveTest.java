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

package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class AlarmParameterDataArchiveTest {

    @BeforeEach
    void setup() {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.ALL);
        }
        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testAlarmParameterDataStoreRetrieve() throws IOException, ArchiveException, InterruptedException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create archive
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IAlarmParameterDataArchive alarmDataArchive = archive.getArchive(IAlarmParameterDataArchive.class);
            Instant t = Instant.ofEpochSecond(3600);
            // store some parameter data
            alarmDataArchive.store(Arrays.asList(
                    new AlarmParameterData(new LongUniqueId(0), t.plusMillis(0), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), AlarmState.ERROR, 1, null, null, t, new Object[0]),
                    new AlarmParameterData(new LongUniqueId(1), t.plusMillis(100), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), AlarmState.NOMINAL, 0, 0, t.plusMillis(100), t, new Object[0]),
                    new AlarmParameterData(new LongUniqueId(2), t.plusMillis(20), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"), AlarmState.WARNING, 11, null, null, t, new Object[0]),
                    new AlarmParameterData(new LongUniqueId(3), t.plusMillis(100), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"), AlarmState.NOMINAL, 10, 10, t.plusMillis(100), t, new Object[0]),
                    new AlarmParameterData(new LongUniqueId(4), t.plusMillis(0), 1002, "PARAM3", SystemEntityPath.fromString("TEST.PARAM3"),  AlarmState.WARNING, 11, null, null, t, new Object[0]),
                    new AlarmParameterData(new LongUniqueId(5), t.plusMillis(300), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), AlarmState.ALARM, 2, 0, t.plusMillis(100), t, new Object[0])
            ));
            Thread.sleep(2000);
            // Retrieve at t + 250 ms
            List<AlarmParameterData> params = alarmDataArchive.retrieve(t.plusMillis(250), (AlarmParameterDataFilter) null, null);
            assertEquals(3, params.size());
            for (AlarmParameterData pd : params) {
                if (pd.getPath().asString().equals("TEST.PARAM1")) {
                    assertEquals(1L, pd.getInternalId().asLong());
                    assertEquals(AlarmState.NOMINAL, pd.getCurrentAlarmState());
                }
            }
            // Retrieve 1 item
            AlarmParameterData d = alarmDataArchive.retrieve(new LongUniqueId(3));
            assertEquals("PARAM2", d.getName());
            // Retrieve no item
            d = alarmDataArchive.retrieve(new LongUniqueId(31));
            assertNull(d);
            // Retrieve at t + 250 ms in AlarmState ALARM or WARNING
            params = alarmDataArchive.retrieve(t.plusMillis(250), new AlarmParameterDataFilter(null, null, Arrays.asList(AlarmState.ALARM, AlarmState.WARNING), null), null);
            assertEquals(1, params.size());
            for (AlarmParameterData pd : params) {
                if (pd.getPath().asString().equals("TEST.PARAM3")) {
                    assertEquals(4L, pd.getInternalId().asLong());
                    assertEquals(AlarmState.WARNING, pd.getCurrentAlarmState());
                } else {
                    fail("PARAM3 expected");
                }
            }
            // Retrieve all samples of PARAM1 and PARAM2
            params = alarmDataArchive.retrieve(t, 20, RetrievalDirection.TO_FUTURE, new AlarmParameterDataFilter(SystemEntityPath.fromString("TEST"), Arrays.asList(
                    SystemEntityPath.fromString("TEST.PARAM1"),
                    SystemEntityPath.fromString("TEST.PARAM2")
            ), Arrays.asList(AlarmState.VIOLATED, AlarmState.ALARM, AlarmState.NOMINAL), null));
            int p1count = 0;
            int p2count = 0;
            assertEquals(3, params.size());
            for (AlarmParameterData pd : params) {
                switch (pd.getName()) {
                    case "PARAM1":
                        p1count++;
                        break;
                    case "PARAM2":
                        p2count++;
                        break;
                    default:
                        fail("Unexpected parameter retrieved: " + pd.getName());
                }
            }
            assertEquals(2, p1count);
            assertEquals(1, p2count);
            archive.dispose();
        } finally {
            // Delete all
            Files.walk(tempLocation)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}