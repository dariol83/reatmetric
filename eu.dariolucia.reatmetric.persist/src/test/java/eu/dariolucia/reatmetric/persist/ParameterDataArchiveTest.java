/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
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

class ParameterDataArchiveTest {

    @BeforeEach
    void setup() {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.ALL);
        }
        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testParameterDataStoreRetrieve() throws IOException, ArchiveException, InterruptedException, SQLException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create archive
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IParameterDataArchive parameterDataArchive = archive.getArchive(IParameterDataArchive.class);
            Instant t = Instant.ofEpochSecond(3600);
            // store some parameter data
            parameterDataArchive.store(Arrays.asList(
                    new ParameterData(new LongUniqueId(0), t.plusMillis(0), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), 1, 1, "R1", Validity.VALID, AlarmState.NOMINAL, null, t, new Object[0]),
                    new ParameterData(new LongUniqueId(1), t.plusMillis(100), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), 2, 2, "R1", Validity.VALID, AlarmState.NOMINAL, null, t, new Object[0]),
                    new ParameterData(new LongUniqueId(2), t.plusMillis(20), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"), 10, 10, "R1", Validity.VALID, AlarmState.NOMINAL, null, t, new Object[0]),
                    new ParameterData(new LongUniqueId(3), t.plusMillis(100), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"), 11, 11, "R1", Validity.VALID, AlarmState.VIOLATED, null, t, new Object[0]),
                    new ParameterData(new LongUniqueId(4), t.plusMillis(0), 1002, "PARAM3", SystemEntityPath.fromString("TEST.PARAM3"), 100, 100, "R1", Validity.VALID, AlarmState.NOMINAL, null, t, new Object[0]),
                    new ParameterData(new LongUniqueId(5), t.plusMillis(300), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"), 3, 3, null, Validity.VALID, AlarmState.ALARM, null, t, new Object[0])
            ));
            Thread.sleep(2000);
            // Retrieve at t + 250 ms
            List<ParameterData> params = parameterDataArchive.retrieve(t.plusMillis(250), null);
            assertEquals(3, params.size());
            for (ParameterData pd : params) {
                if (pd.getPath().asString().equals("TEST.PARAM1")) {
                    assertEquals(1L, pd.getInternalId().asLong());
                    assertEquals(2, pd.getEngValue());
                }
            }
            // Retrieve 1 item
            ParameterData d = parameterDataArchive.retrieve(new LongUniqueId(3));
            assertEquals("PARAM2", d.getName());
            // Retrieve no item
            d = parameterDataArchive.retrieve(new LongUniqueId(31));
            assertNull(d);
            // Retrieve at t + 250 ms in AlarmState ALARM or VIOLATED
            params = parameterDataArchive.retrieve(t.plusMillis(250), new ParameterDataFilter(null, null, Arrays.asList("R1", "R2"), null, Arrays.asList(AlarmState.ALARM, AlarmState.VIOLATED)));
            assertEquals(1, params.size());
            for (ParameterData pd : params) {
                if (pd.getPath().asString().equals("TEST.PARAM2")) {
                    assertEquals(3L, pd.getInternalId().asLong());
                    assertEquals(AlarmState.VIOLATED, pd.getAlarmState());
                } else {
                    fail("PARAM2 expected");
                }
            }
            // Retrieve all samples of PARAM1 and PARAM2
            params = parameterDataArchive.retrieve(t, 20, RetrievalDirection.TO_FUTURE, new ParameterDataFilter(SystemEntityPath.fromString("TEST"), Arrays.asList(
                    SystemEntityPath.fromString("TEST.PARAM1"),
                    SystemEntityPath.fromString("TEST.PARAM2")
            ), Arrays.asList("R1", "R2"), Arrays.asList(Validity.ERROR, Validity.VALID), Arrays.asList(AlarmState.VIOLATED, AlarmState.ALARM, AlarmState.NOMINAL)));
            int p1count = 0;
            int p2count = 0;
            assertEquals(4, params.size());
            for (ParameterData pd : params) {
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
            assertEquals(2, p2count);
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