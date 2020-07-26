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

import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataArchive;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.persist.services.*;

import java.io.BufferedReader;
import java.io.File;
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
    private static final String ERROR_CODE_TABLE_ALREADY_EXIST = "X0Y32";
    private static final String ERROR_CODE_DATABASE_NOT_FOUND = "XJ004";

    private final String archiveFolder;

    private final Map<Class<? extends IDataItemArchive>, IDataItemArchive> registeredArchives = new HashMap<>();

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() throws ArchiveException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("connect() invoked: archiveFolder set to " + archiveFolder);
        }
        // if the parent folder does not exist, create it
        verifyParentFolderExistence();
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
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

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
            // execute the schema creation
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Creating database schema: " + schemaContents);
            }
            for(String createTable : schemaContents) {
                st.execute(createTable);
            }
            // commit
            creationConnection.commit();
        } catch (IOException e) {
            throw new ArchiveException(e);
        } catch (SQLException e) {
            try {
                if (creationConnection != null) {
                    creationConnection.rollback();
                }
            } catch (SQLException ex) {
                LOG.log(Level.FINE, "Cannot rollback connection to initiate the database schema at " + archiveFolder, ex);
            }
            // with error X0Y32, all fine (table already exists); otherwise, wrap and throw
            if (!e.getSQLState().equals(ERROR_CODE_TABLE_ALREADY_EXIST)) {
                throw new ArchiveException(e);
            } else if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Database schema already present for location " + archiveFolder);
            }
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    LOG.log(Level.FINE, "Cannot close statement to initiate the database schema at " + archiveFolder, e);
                }
            }
            try {
                if (creationConnection != null) {
                    creationConnection.close();
                }
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Cannot close connection to initiate the database schema at " + archiveFolder, e);
            }
        }
    }

    private void verifyParentFolderExistence() throws ArchiveException {
        File dataFolder = new File(archiveFolder);
        if (!dataFolder.getParentFile().exists() && !dataFolder.getParentFile().mkdirs()) {
            throw new ArchiveException("Cannot create archive data parent folder " + archiveFolder);
        }
    }

    @Override
    public void dispose() {
        // dispose storage services
        for(IDataItemArchive archive : registeredArchives.values()) {
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
        String jdbcConnectionString = "jdbc:derby:" + archiveFolder;
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Trying to open database at location " + archiveFolder);
        }
        try {
            conn = DriverManager.getConnection(jdbcConnectionString); // NOSONAR: it is expected to work in this way
        } catch (SQLException e) {
            if (e.getSQLState().equals(ERROR_CODE_DATABASE_NOT_FOUND)) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine("No database found at location " + archiveFolder + ": creating one");
                }
                // database does not exist
                conn = DriverManager.getConnection("jdbc:derby:" + archiveFolder + ";create=true"); // NOSONAR: it is expected to work in this way
            } else {
                throw e;
            }
        }
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
        for(IDataItemArchive archive : registeredArchives.values()) {
            toReturn.addAll(archive.currentDebugInfo());
        }
        return toReturn;
    }
}
