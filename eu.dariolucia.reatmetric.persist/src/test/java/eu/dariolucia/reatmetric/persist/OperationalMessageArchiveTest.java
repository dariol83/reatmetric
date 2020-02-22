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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalMessageArchiveTest {

    @BeforeEach
    void setup() {
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.ALL);
        }
        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testOperationalMessageStoreRetrieve() throws IOException, ArchiveException, InterruptedException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create the archive
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IOperationalMessageArchive messageArchive = archive.getArchive(IOperationalMessageArchive.class);
            Instant t = Instant.now();
            // store one item
            messageArchive.store(new OperationalMessage(new LongUniqueId(0), t, "msgId1", "Text message", "Source1", Severity.ALARM, new Object[0]));
            Thread.sleep(2000);
            // retrieve: expected 1 item
            List<OperationalMessage> messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, messages.size());
            assertEquals(0L, messages.get(0).getInternalId().asLong());
            assertEquals("Text message", messages.get(0).getMessage());
            assertEquals(Severity.ALARM, messages.get(0).getSeverity());
            // store one item
            messageArchive.store(new OperationalMessage(new LongUniqueId(1), t, "msgId2", "Text message", "Source2", Severity.INFO, new Object[] { "test", 13, Instant.ofEpochMilli(1000)}));
            Thread.sleep(2000);
            // retrieve: expected 2 items
            messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            assertEquals(1L, messages.get(1).getInternalId().asLong());
            assertEquals("msgId2", messages.get(1).getId());
            assertEquals("Source2", messages.get(1).getSource());
            assertArrayEquals(new Object[] {"test", 13, Instant.ofEpochMilli(1000)}, (Object[]) messages.get(1).getExtension());
            // dispose
            archive.dispose();
            // create a new connection and retrieve the data
            archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            messageArchive = archive.getArchive(IOperationalMessageArchive.class);
            // retrieve: expected two items
            messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            assertEquals(1L, messages.get(1).getInternalId().asLong());
            assertEquals("msgId2", messages.get(1).getId());
            assertEquals("Source2", messages.get(1).getSource());
            assertArrayEquals(new Object[] {"test", 13, Instant.ofEpochMilli(1000)}, (Object[]) messages.get(1).getExtension());
            // last ID: expected 1
            assertEquals(1, messageArchive.retrieveLastId().asLong());
            // store one item
            messageArchive.store(new OperationalMessage(new LongUniqueId(2), t.plusSeconds(1), "msgId3", "Text message", "Source2", Severity.ALARM, new Object[] { "test", 13, Instant.ofEpochMilli(1000)}));
            Thread.sleep(2000);
            // retrieve: expected 3 items
            messages = messageArchive.retrieve(Instant.now(), 10, RetrievalDirection.TO_PAST, null);
            assertEquals(3, messages.size());
            assertEquals(2L, messages.get(0).getInternalId().asLong());
            assertEquals("msgId3", messages.get(0).getId());
            // store three items
            t = Instant.now().minusMillis(500);
            List<OperationalMessage> toStore = Arrays.asList(
                    new OperationalMessage(new LongUniqueId(3), t, "msgId3", "Text message", "Source2", Severity.ALARM, new Object[] { }),
                    new OperationalMessage(new LongUniqueId(4), t, "msgId4", "Text message special", "Source2", Severity.WARN, new Object[] { }),
                    new OperationalMessage(new LongUniqueId(5), t, "msgId1", "Text message", "Source2", Severity.INFO, new Object[] { })
            );
            messageArchive.store(toStore);
            Thread.sleep(2000);
            // retrieve all, num records 3
            messages = messageArchive.retrieve(Instant.now().minusSeconds(30), 3, RetrievalDirection.TO_FUTURE, null);
            assertEquals(3, messages.size());
            assertEquals(2, messages.get(2).getInternalId().asLong());
            // retrieve the next 2 records
            messages = messageArchive.retrieve(messages.get(2), 2, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            assertEquals(3, messages.get(0).getInternalId().asLong());
            assertEquals(4, messages.get(1).getInternalId().asLong());
            // retrieve the next 2 records (only one present)
            messages = messageArchive.retrieve(messages.get(1), 2, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, messages.size());
            assertEquals(5, messages.get(0).getInternalId().asLong());
            // filter retrieve: message text matching
            messages = messageArchive.retrieve(Instant.now().minusSeconds(30), 10, RetrievalDirection.TO_FUTURE, new OperationalMessageFilter("special", null, null, null));
            assertEquals(1, messages.size());
            assertEquals("Text message special", messages.get(0).getMessage());
            // filter retrieve: id
            messages = messageArchive.retrieve(Instant.now().minusSeconds(30), 10, RetrievalDirection.TO_FUTURE, new OperationalMessageFilter(null, Collections.singletonList("msgId1"), null, null));
            assertEquals(2, messages.size());
            // filter retrieve: source
            messages = messageArchive.retrieve(Instant.now().minusSeconds(30), 10, RetrievalDirection.TO_FUTURE, new OperationalMessageFilter(null, null, Collections.singletonList("Source2"), null));
            assertEquals(5, messages.size());
            // filter retrieve: severity
            messages = messageArchive.retrieve(Instant.now().minusSeconds(30), 10, RetrievalDirection.TO_FUTURE, new OperationalMessageFilter(null, null, null, Arrays.asList(Severity.WARN, Severity.INFO)));
            assertEquals(3, messages.size());
            // dispose
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