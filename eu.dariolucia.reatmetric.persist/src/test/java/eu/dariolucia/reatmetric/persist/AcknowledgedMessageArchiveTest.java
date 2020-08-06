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

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class AcknowledgedMessageArchiveTest {

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
            OperationalMessage toStore = new OperationalMessage(new LongUniqueId(0), t, "msgId1", "Text message", "Source1", Severity.ALARM, new Object[0]);
            messageArchive.store(toStore);

            IAcknowledgedMessageArchive ackArchive = archive.getArchive(IAcknowledgedMessageArchive.class);
            t = Instant.now();
            // store one item
            ackArchive.store(new AcknowledgedMessage(new LongUniqueId(0), t, toStore, AcknowledgementState.PENDING, null, null, new Object[0]));

            Thread.sleep(2000);
            // retrieve: expected 1 item
            List<AcknowledgedMessage> messages = ackArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, messages.size());
            assertEquals(0L, messages.get(0).getInternalId().asLong());
            assertEquals(toStore.getInternalId(), messages.get(0).getMessage().getInternalId());
            assertEquals(AcknowledgementState.PENDING, messages.get(0).getState());
            assertNull(messages.get(0).getUser());
            assertNull(messages.get(0).getAcknowledgementTime());
            // store one item
            ackArchive.store(new AcknowledgedMessage(new LongUniqueId(0), t, toStore, AcknowledgementState.ACKNOWLEDGED, Instant.now(), "user", new Object[0]));
            Thread.sleep(2000);
            // retrieve: expected 1 item
            messages = ackArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, messages.size());
            assertEquals(0L, messages.get(0).getInternalId().asLong());
            assertEquals(toStore.getInternalId(), messages.get(0).getMessage().getInternalId());
            assertEquals(AcknowledgementState.ACKNOWLEDGED, messages.get(0).getState());
            assertEquals("user", messages.get(0).getUser());
            assertNotNull(messages.get(0).getAcknowledgementTime());
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