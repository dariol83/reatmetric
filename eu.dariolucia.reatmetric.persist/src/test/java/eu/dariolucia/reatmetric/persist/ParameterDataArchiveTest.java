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
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterDataArchiveTest {

    @BeforeEach
    void setup() {
//        for(Handler h : Logger.getLogger("").getHandlers()) {
//            h.setLevel(Level.ALL);
//        }
//        Logger.getLogger("eu.dariolucia.reatmetric.persist").setLevel(Level.ALL);
    }

    @Test
    void testParameterDataStoreRetrieve() throws IOException, ArchiveException, InterruptedException, SQLException {
        Path tempLocation = Files.createTempDirectory("reatmetric_");
        // Now delete it
        Files.delete(tempLocation);
        try {
            // create archive
            ArchiveFactory af = new ArchiveFactory();
            Archive archive = (Archive) af.buildArchive(tempLocation.toString());
            archive.connect();
            IParameterDataArchive parameterDataArchive = archive.getParameterDataArchive();
            Instant t = Instant.ofEpochSecond(3600);
            // store some parameter data
            parameterDataArchive.store(Arrays.asList(
                    new ParameterData(new LongUniqueId(0), t.plusMillis(0), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"),1,1,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0]),
                    new ParameterData(new LongUniqueId(1), t.plusMillis(100), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"),2,2,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0]),
                    new ParameterData(new LongUniqueId(2), t.plusMillis(20), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"),10,10,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0]),
                    new ParameterData(new LongUniqueId(3), t.plusMillis(100), 1001, "PARAM2", SystemEntityPath.fromString("TEST.PARAM2"),11,11,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0]),
                    new ParameterData(new LongUniqueId(4), t.plusMillis(0), 1002, "PARAM3", SystemEntityPath.fromString("TEST.PARAM3"),100,100,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0]),
                    new ParameterData(new LongUniqueId(5), t.plusMillis(300), 1000, "PARAM1", SystemEntityPath.fromString("TEST.PARAM1"),3,3,"R1", Validity.VALID, AlarmState.NOMINAL, t, new Object[0])
            ));
            Thread.sleep(2000);
            Connection c = archive.createConnection(false);
            Statement st = c.createStatement();
            Instant time = t.plusMillis(250);
            // String query = "SELECT t1.Path, MAX(t1.GenerationTime) FROM PARAMETER_DATA_TABLE as t1 WHERE t1.GenerationTime < '" + new Timestamp(time.toEpochMilli()) + "' GROUP BY t1.Path";
            String query = "SELECT PARAMETER_DATA_TABLE.* " +
                    "FROM (" +
                        "SELECT DISTINCT Path, MAX(GenerationTime) as LatestTime FROM PARAMETER_DATA_TABLE WHERE GenerationTime <= '" + new Timestamp(time.toEpochMilli()) + "' GROUP BY Path" +
                    ") AS LATEST_SAMPLES " +
                    "INNER JOIN PARAMETER_DATA_TABLE " +
                    "ON PARAMETER_DATA_TABLE.Path = LATEST_SAMPLES.Path AND PARAMETER_DATA_TABLE.GenerationTime = LATEST_SAMPLES.LatestTime";
            System.out.println(query);
            ResultSet rs = st.executeQuery(query);
            while(rs.next()) {
                System.out.printf("%d\t%s\t%s\n", rs.getLong(1), rs.getTimestamp(2).toString(), rs.getString(4));
            }
            rs.close();
            st.close();
            c.commit();
            c.close();
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