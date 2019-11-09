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
import java.util.List;

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
            messageArchive.store(new OperationalMessage(new LongUniqueId(1), "msgId1", "Text message", t, "Source1", Severity.ALARM, new Object[0]));
            Thread.sleep(2000);
            messages = messageArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(2, messages.size());
            archive.dispose();
        } finally {
            // TODO: this below fails - need recursive delete
            // if(tempLocation.toFile().exists()) {
            //    Files.deleteIfExists(tempLocation);
            // }
        }
    }
}