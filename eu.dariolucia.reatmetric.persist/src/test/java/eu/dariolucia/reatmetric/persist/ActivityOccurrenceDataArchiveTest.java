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

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ActivityOccurrenceDataArchiveTest {

    @BeforeEach
    void setup() {
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.ALL);
        }
        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testActivityOccurrenceDataStoreRetrieve() throws IOException, ArchiveException, InterruptedException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create archive
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IActivityOccurrenceDataArchive activityArchive = archive.getArchive(IActivityOccurrenceDataArchive.class);
            Instant t = Instant.now();
            // store one activity occurrence
            ActivityOccurrenceReport creationReport = new ActivityOccurrenceReport(new LongUniqueId(0), t, null, "Creation", ActivityOccurrenceState.CREATION, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null);
            activityArchive.store(new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Collections.singletonList(creationReport), "routeA", "sourceB"));
            Thread.sleep(2000);
            // retrieve: expected 1
            List<ActivityOccurrenceData> items = activityArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, items.size());
            assertEquals(0L, items.get(0).getInternalId().asLong());
            assertEquals(12, items.get(0).getExternalId());
            assertEquals("root.activity1", items.get(0).getPath().toString());
            assertEquals("t1", items.get(0).getType());
            assertEquals("routeA", items.get(0).getRoute());
            assertEquals(1, items.get(0).getProgressReports().size());
            assertEquals("Creation", items.get(0).getProgressReports().get(0).getName());
            assertNull(items.get(0).getResult());
            assertNull(items.get(0).getExecutionTime());
            // store the second progress report
            ActivityOccurrenceReport releaseReport = new ActivityOccurrenceReport(new LongUniqueId(1), t.plusMillis(20), null, "Release", ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, null);
            activityArchive.store(new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Arrays.asList(creationReport, releaseReport), "routeA", "sourceB"));
            Thread.sleep(2000);
            // retrieve: expected 1
            items = activityArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, items.size());
            assertEquals(0L, items.get(0).getInternalId().asLong());
            assertEquals(12, items.get(0).getExternalId());
            assertEquals("root.activity1", items.get(0).getPath().toString());
            assertEquals("t1", items.get(0).getType());
            assertEquals("routeA", items.get(0).getRoute());
            assertNull(items.get(0).getResult());
            assertNull(items.get(0).getExecutionTime());
            assertEquals(2, items.get(0).getProgressReports().size());
            assertEquals("Creation", items.get(0).getProgressReports().get(0).getName());
            assertEquals("Release", items.get(0).getProgressReports().get(1).getName());
            // store the third progress report (transmission) and fourth (execution)
            ActivityOccurrenceReport transmissionReport = new ActivityOccurrenceReport(new LongUniqueId(2), t.plusMillis(40), null, "Transmission", ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, null);
            ActivityOccurrenceReport executionReport = new ActivityOccurrenceReport(new LongUniqueId(3), t.plusMillis(50), null, "Execution", ActivityOccurrenceState.EXECUTION, t.plusMillis(45), ActivityReportState.OK, ActivityOccurrenceState.VERIFICATION, 5);
            activityArchive.store(Arrays.asList(
                    new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Arrays.asList(creationReport, releaseReport, transmissionReport), "routeA", "sourceB"),
                    new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Arrays.asList(creationReport, releaseReport, transmissionReport, executionReport), "routeA", "sourceB")
            ));
            Thread.sleep(2000);
            // retrieve: expected 1
            items = activityArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, items.size());
            assertEquals(0L, items.get(0).getInternalId().asLong());
            assertEquals(12, items.get(0).getExternalId());
            assertEquals("root.activity1", items.get(0).getPath().toString());
            assertEquals("t1", items.get(0).getType());
            assertEquals("routeA", items.get(0).getRoute());
            assertEquals(5, items.get(0).getResult());
            assertEquals(t.plusMillis(45), items.get(0).getExecutionTime());
            assertEquals(4, items.get(0).getProgressReports().size());
            assertEquals("Creation", items.get(0).getProgressReports().get(0).getName());
            assertEquals("Release", items.get(0).getProgressReports().get(1).getName());
            assertEquals("Transmission", items.get(0).getProgressReports().get(2).getName());
            assertEquals("Execution", items.get(0).getProgressReports().get(3).getName());
            // now generate other two activity occurrences
            // 1
            Map<String, String> properties = Map.of("prop1", "val1", "prop2", "val2");
            Map<String, Object> args = Map.of("arg1", true, "arg2", "val2", "arg3", Instant.ofEpochMilli(300), "arg4", 793L);
            creationReport = new ActivityOccurrenceReport(new LongUniqueId(4), t.plusMillis(1000), null, "Creation", ActivityOccurrenceState.CREATION, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null);
            releaseReport = new ActivityOccurrenceReport(new LongUniqueId(5), t.plusMillis(2000), null, "Release", ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, null);
            transmissionReport = new ActivityOccurrenceReport(new LongUniqueId(6), t.plusMillis(4000), null, "Transmission", ActivityOccurrenceState.TRANSMISSION, null, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, null);
            activityArchive.store(Arrays.asList(
                    new ActivityOccurrenceData(new LongUniqueId(1), t.plusMillis(1000), null, 13,"activity2", SystemEntityPath.fromString("root.activity2"), "t1", args, properties, Collections.singletonList(creationReport), "routeB", "sourceB"),
                    new ActivityOccurrenceData(new LongUniqueId(1), t.plusMillis(1000), null, 13,"activity2", SystemEntityPath.fromString("root.activity2"), "t1", args, properties, Arrays.asList(creationReport, releaseReport), "routeB", "sourceB"),
                    new ActivityOccurrenceData(new LongUniqueId(1), t.plusMillis(1000), null, 13,"activity2", SystemEntityPath.fromString("root.activity2"), "t1", args, properties, Arrays.asList(creationReport, releaseReport, transmissionReport), "routeB", "sourceB")
            ));
            // 2
            properties = Map.of("prop1", "val1", "prop2", "val2");
            args = Map.of("arg1", true, "arg2", "val2", "arg3", Instant.ofEpochMilli(300), "arg4", 793L);
            creationReport = new ActivityOccurrenceReport(new LongUniqueId(7), t.plusMillis(5000), null, "Creation", ActivityOccurrenceState.CREATION, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null);
            releaseReport = new ActivityOccurrenceReport(new LongUniqueId(8), t.plusMillis(5100), null, "Release", ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, null);
            activityArchive.store(Arrays.asList(
                    new ActivityOccurrenceData(new LongUniqueId(2), t.plusMillis(5000), null, 13,"activity2", SystemEntityPath.fromString("root.activity2"), "t2", args, properties, Collections.singletonList(creationReport), "routeA", "sourceB"),
                    new ActivityOccurrenceData(new LongUniqueId(2), t.plusMillis(5000), null, 13,"activity2", SystemEntityPath.fromString("root.activity2"), "t2", args, properties, Arrays.asList(creationReport, releaseReport), "routeA", "sourceB")
            ));
            Thread.sleep(2000);
            // several retrievals (past, future, with filter, at time)
            // retrieve: expected 3
            items = activityArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(3, items.size());
            assertEquals(0L, items.get(0).getInternalId().asLong());
            assertEquals(1L, items.get(1).getInternalId().asLong());
            assertEquals(2L, items.get(2).getInternalId().asLong());
            // retrieve: expected 1
            items = activityArchive.retrieve(t.plusMillis(3000), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, items.size());
            assertEquals(2L, items.get(0).getInternalId().asLong());
            // retrieve: expected 2
            items = activityArchive.retrieve(t.plusMillis(10000), 2, RetrievalDirection.TO_PAST, null);
            assertEquals(2, items.size());
            assertEquals(2L, items.get(0).getInternalId().asLong());
            assertEquals(1L, items.get(1).getInternalId().asLong());
            // retrieve: expected 1
            items = activityArchive.retrieve(t.plusMillis(10000), 10, RetrievalDirection.TO_PAST, new ActivityOccurrenceDataFilter(null, null, Arrays.asList("routeB", "routeC"), Arrays.asList("t1", "t2"), null, null, null));
            assertEquals(1, items.size());
            assertEquals(1L, items.get(0).getInternalId().asLong());
            assertEquals("routeB", items.get(0).getRoute());
            // retrieve: expected 2
            items = activityArchive.retrieve(t.plusMillis(10000), 10, RetrievalDirection.TO_PAST, new ActivityOccurrenceDataFilter(SystemEntityPath.fromString("root.activity2"), null, Arrays.asList("routeA", "routeB", "routeC"), null, null, null, null));
            assertEquals(2, items.size());
            assertEquals(2L, items.get(0).getInternalId().asLong());
            assertEquals(1L, items.get(1).getInternalId().asLong());
            assertEquals("routeA", items.get(0).getRoute());
            assertEquals("routeB", items.get(1).getRoute());
            // retrieve: expected 2
            items = activityArchive.retrieve(t.plusMillis(2000), null, null);
            assertEquals(2, items.size());
            assertEquals(1L, items.get(0).getInternalId().asLong());
            assertEquals(0L, items.get(1).getInternalId().asLong());
            // retrieve: expected 1
            items = activityArchive.retrieve(t.plusMillis(2000),  new ActivityOccurrenceDataFilter(null, null, Arrays.asList("routeB", "routeC"), Arrays.asList("t1", "t2"), Collections.singletonList(ActivityOccurrenceState.EXECUTION), Arrays.asList("sourceA", "sourceB"), null), null);
            assertEquals(1, items.size());
            assertEquals(1L, items.get(0).getInternalId().asLong());
            assertEquals("val1", items.get(0).getProperties().get("prop1"));
            assertEquals("val2", items.get(0).getProperties().get("prop2"));
            assertEquals(true, items.get(0).getArguments().get("arg1"));
            assertEquals("val2", items.get(0).getArguments().get("arg2"));
            assertEquals(Instant.ofEpochMilli(300), items.get(0).getArguments().get("arg3"));
            assertEquals(793L, items.get(0).getArguments().get("arg4"));
            assertEquals(ActivityOccurrenceState.EXECUTION, items.get(0).getCurrentState());
            // retrieve: unique id
            ActivityOccurrenceData item = activityArchive.retrieve(new LongUniqueId(1));
            assertNotNull(item);
            assertEquals(1L, item.getInternalId().asLong());
            assertEquals("val1", item.getProperties().get("prop1"));
            assertEquals("val2", item.getProperties().get("prop2"));
            assertEquals(true, item.getArguments().get("arg1"));
            assertEquals("val2", item.getArguments().get("arg2"));
            assertEquals(Instant.ofEpochMilli(300), item.getArguments().get("arg3"));
            assertEquals(793L, item.getArguments().get("arg4"));
            assertEquals(ActivityOccurrenceState.EXECUTION, item.getCurrentState());
            // retrieve: null
            item = activityArchive.retrieve(new LongUniqueId(25));
            assertNull(item);
            // last ids
            assertEquals(2L, activityArchive.retrieveLastId().asLong());
            assertEquals(2L, activityArchive.retrieveLastId(ActivityOccurrenceData.class).asLong());
            assertEquals(8L, activityArchive.retrieveLastId(ActivityOccurrenceReport.class).asLong());

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