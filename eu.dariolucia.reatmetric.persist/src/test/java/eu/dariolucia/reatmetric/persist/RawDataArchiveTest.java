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
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class RawDataArchiveTest {

    @BeforeEach
    void setup() {
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.ALL);
        }
        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testRawDataStoreRetrieve() throws IOException, ArchiveException, InterruptedException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create archive
            ArchiveFactory af = new ArchiveFactory();
            IArchive archive = af.buildArchive(tempLocation.toString());
            archive.connect();
            IRawDataArchive rawDataArchive = archive.getArchive(IRawDataArchive.class);
            Instant t = Instant.now();
            // store one raw data
            rawDataArchive.store(new RawData(new LongUniqueId(0), t, "nameAA1", "TCPacket", "Route1", "Source1", Quality.GOOD, null, new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]));
            Thread.sleep(2000);
            // retrieve: expected 1
            List<RawData> items = rawDataArchive.retrieve(t.minusMillis(200), 10, RetrievalDirection.TO_FUTURE, null);
            assertEquals(1, items.size());
            assertEquals(0L, items.get(0).getInternalId().asLong());
            assertEquals(Quality.GOOD, items.get(0).getQuality());
            assertArrayEquals(new byte[] { 0,1, 2, 3, 4 }, items.get(0).getContents());
            // add a batch for filtered retrieval
            rawDataArchive.store(Arrays.asList(
                    new RawData(new LongUniqueId(1), t, "nameAA1", "TCPacket", "Route1", "Source1", Quality.BAD, null, new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]),
                    new RawData(new LongUniqueId(2), t, "nameAA1", "TCPacket", "Route2", "Source1", Quality.GOOD, null, new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]),
                    new RawData(new LongUniqueId(3), t, "nameAA2", "TMPacket", "Route2", "Source2", Quality.GOOD, null, new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]),
                    new RawData(new LongUniqueId(4), t, "name3", "TMPacket", "Route3", "Source2", Quality.GOOD, null, new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]),
                    new RawData(new LongUniqueId(5), t, "name4", "TMPacket", "Route2", "Source2", Quality.GOOD, new LongUniqueId(100), new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0]),
                    new RawData(new LongUniqueId(6), t, "name5", "TMFrame", "Route1", "Source1", Quality.UNKNOWN, new LongUniqueId(100), new byte[] { 0,1, 2, 3, 4 }, t, "TestHandler", new Object[0])
            ));
            Thread.sleep(2000);
            // retrieve name1 and name2, no contents
            items = rawDataArchive.retrieve(t.minusSeconds(20), 10, RetrievalDirection.TO_FUTURE, new RawDataFilter(false, "ameAA", null, null, null, null));
            assertEquals(4, items.size());
            assertFalse(items.get(0).isContentsSet());
            assertFalse(items.get(1).isContentsSet());
            assertFalse(items.get(2).isContentsSet());
            assertFalse(items.get(3).isContentsSet());
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