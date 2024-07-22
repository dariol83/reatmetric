/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.persist.timescale;

import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataArchive;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.IAcknowledgedMessageArchive;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.scheduler.IScheduledActivityDataArchive;
import eu.dariolucia.reatmetric.persist.timescale.services.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Archive implements IArchive {

    public static final String ARCHIVE_NAME = "Archive";

    private static final Logger LOG = Logger.getLogger(Archive.class.getName());

    private static final String SCHEMA_FILE_NAME = "/schema.ddl";
    private static final String SCHEMA_INSTRUCTION_SEPARATOR = "-- SEPARATOR";

    // Derby error codes
    private static final String ERROR_CODE_TABLE_ALREADY_EXIST = "42P07";

    private final String connectionString;
    private final Map<Class<? extends IDataItemArchive<?,?>>, IDataItemArchive<?,?>> registeredArchives = new HashMap<>();

    /**
     * Typical connection string: jdbc:postgresql://192.168.3.12:5432/my_database?user=fred&amp;password=secret&amp;ssl=true
     * @param initialisationString the connection string
     */
    public Archive(String initialisationString) {
        this.connectionString = initialisationString;
    }

    public void connect() throws ArchiveException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("connect() invoked: connectionString set to " + connectionString);
        }
        // now create-and-ignore tables
        createSchema();
        // initialise storage services
        initialiseArchiveServices();
    }

    private void initialiseArchiveServices() throws ArchiveException {
        try {
            registeredArchives.put(IOperationalMessageArchive.class, new OperationalMessageArchive(this));
            registeredArchives.put(IEventDataArchive.class, new EventDataArchive(this));
            registeredArchives.put(IRawDataArchive.class, new RawDataArchive(this));
            registeredArchives.put(IParameterDataArchive.class, new ParameterDataArchive(this));
            registeredArchives.put(IAlarmParameterDataArchive.class, new AlarmParameterDataArchive(this));
            registeredArchives.put(IActivityOccurrenceDataArchive.class, new ActivityOccurrenceDataArchive(this));
            registeredArchives.put(IAcknowledgedMessageArchive.class, new AcknowledgedMessageArchive(this));
            registeredArchives.put(IScheduledActivityDataArchive.class, new ScheduledActivityDataArchive(this));
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    /**
     * The following steps are a precondition:
     *
     * CREATE DATABASE EXAMPLE_DB;
     * CREATE USER EXAMPLE_USER WITH ENCRYPTED PASSWORD 'Sup3rS3cret';
     * GRANT ALL PRIVILEGES ON DATABASE EXAMPLE_DB TO EXAMPLE_USER;
     * \c EXAMPLE_DB postgres
     * # You are now connected to database "EXAMPLE_DB" as user "postgres".
     * GRANT ALL ON SCHEMA public TO EXAMPLE_USER;
     * CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
     *
     * @throws ArchiveException
     */
    private void createSchema() throws ArchiveException {
        Connection creationConnection = null;
        Statement st = null;
        try {
            // read the schema: it is split in several SQL statements using a specific line
            List<String> schemaContents = readSchemaContents();
            // open the connection
            creationConnection = createConnection(true);
            // create a statement
            st = creationConnection.createStatement();
            for(String createTable : schemaContents) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Executing statement: " + createTable);
                }
                st.execute(createTable);
                // commit
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Committing creation...");
                }
                creationConnection.commit();
            }
        } catch (IOException e) {
            throw new ArchiveException(e);
        } catch (SQLException e) {
            try {
                if (creationConnection != null) {
                    creationConnection.rollback();
                }
            } catch (SQLException ex) {
                LOG.log(Level.FINE, "Cannot rollback connection to initiate the database schema at " + connectionString, ex);
            }
            // with error X0Y32, all fine (table already exists); otherwise, wrap and throw
            if (!e.getSQLState().equals(ERROR_CODE_TABLE_ALREADY_EXIST)) {
                throw new ArchiveException(e);
            } else if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Database schema already present for " + connectionString);
            }
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    LOG.log(Level.FINE, "Cannot close statement to initiate the database schema at " + connectionString, e);
                }
            }
            try {
                if (creationConnection != null) {
                    creationConnection.close();
                }
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Cannot close connection to initiate the database schema at " + connectionString, e);
            }
        }
    }

    @Override
    public void dispose() {
        // dispose storage services
        for(IDataItemArchive<?,?> archive : registeredArchives.values()) {
            try {
                archive.dispose();
            } catch (ArchiveException e) {
                LOG.log(Level.SEVERE, "Cannot dispose " + archive, e);
            }
        }
    }

    @Override
    public <U extends IDataItemArchive<J,K>,J extends AbstractDataItem,K extends AbstractDataItemFilter<J>> U getArchive(Class<U> clazz) {
        return (U) this.registeredArchives.get(clazz);
    }

    private List<String> readSchemaContents() throws IOException {
        List<String> instructions = new LinkedList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SCHEMA_FILE_NAME)));
        StringBuilder contents = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if(line.startsWith(SCHEMA_INSTRUCTION_SEPARATOR)) {
                instructions.add(contents.toString());
                contents = new StringBuilder();
            } else {
                contents.append(line).append("\n");
            }
        }
        String lastOne = contents.toString();
        if(!lastOne.trim().isBlank()) {
            instructions.add(lastOne);
        }
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Read " + instructions.size() + " instructions to setup the database schema");
        }
        return instructions;
    }

    public Connection createConnection(boolean isWriter) throws SQLException {
        // get a connection
        Connection conn = null;
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Trying to open database at location " + connectionString);
        }
        // database must exist, no automatic creation
        conn = DriverManager.getConnection(connectionString); // NOSONAR: it is expected to work in this way
        // set autocommit false
        conn.setAutoCommit(false);
        // set type (isWriter ? serialize : readonly)
        conn.setReadOnly(!isWriter);
        conn.setTransactionIsolation(isWriter ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_READ_UNCOMMITTED);
        return conn;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        List<DebugInformation> toReturn = new ArrayList<>(20);
        for(IDataItemArchive<?,?> archive : registeredArchives.values()) {
            toReturn.addAll(archive.currentDebugInfo());
        }
        return toReturn;
    }
}
