/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.Severity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
            activityArchive.store(new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Collections.singletonList(creationReport), "routeA"));
            Thread.sleep(3000);
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
            activityArchive.store(new ActivityOccurrenceData(new LongUniqueId(0), t, null, 12,"activity1", SystemEntityPath.fromString("root.activity1"), "t1", new HashMap<>(), new HashMap<>(), Arrays.asList(creationReport, releaseReport), "routeA"));
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

            items.forEach(System.out::println);

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