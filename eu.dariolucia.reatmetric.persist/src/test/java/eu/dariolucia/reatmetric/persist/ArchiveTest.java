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
import eu.dariolucia.reatmetric.api.messages.Severity;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveTest {

    @Test
    void testOperationalMessageStoreRetrieve() throws IOException, ArchiveException, InterruptedException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IOperationalMessageArchive messageArchive = archive.getOperationalMessageArchive();
            Instant t = Instant.now();
            messageArchive.store(new OperationalMessage(new LongUniqueId(0), "msgId1", "Text message", t, "Source1", Severity.ALARM, new Object[0]));
            Thread.sleep(2000);
            List<OperationalMessage> messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, messages.size());
            assertEquals(0L, messages.get(0).getInternalId().asLong());
            assertEquals("Text message", messages.get(0).getMessage());
            assertEquals(Severity.ALARM, messages.get(0).getSeverity());
            messageArchive.store(new OperationalMessage(new LongUniqueId(1), "msgId2", "Text message", t, "Source2", Severity.ALARM, new Object[] { "test", 13, Instant.ofEpochMilli(1000)}));
            Thread.sleep(2000);
            messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            assertEquals(1L, messages.get(1).getInternalId().asLong());
            assertEquals("msgId2", messages.get(1).getId());
            assertEquals("Source2", messages.get(1).getSource());
            assertArrayEquals(new Object[] {"test", 13, Instant.ofEpochMilli(1000)}, messages.get(1).getAdditionalFields());
            archive.dispose();
            // Create a new one and retrieve the data
            archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            messageArchive = archive.getOperationalMessageArchive();
            messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            assertEquals(1L, messages.get(1).getInternalId().asLong());
            assertEquals("msgId2", messages.get(1).getId());
            assertEquals("Source2", messages.get(1).getSource());
            assertArrayEquals(new Object[] {"test", 13, Instant.ofEpochMilli(1000)}, messages.get(1).getAdditionalFields());
            assertEquals(1, messageArchive.retrieveLastId().asLong());
            messageArchive.store(new OperationalMessage(new LongUniqueId(2), "msgId3", "Text message", t.plusSeconds(1), "Source2", Severity.ALARM, new Object[] { "test", 13, Instant.ofEpochMilli(1000)}));
            Thread.sleep(2000);
            messages = messageArchive.retrieve(Instant.now(), 10, RetrievalDirection.TO_PAST, null);
            assertEquals(3, messages.size());
            assertEquals(2L, messages.get(0).getInternalId().asLong());
            assertEquals("msgId3", messages.get(0).getId());
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