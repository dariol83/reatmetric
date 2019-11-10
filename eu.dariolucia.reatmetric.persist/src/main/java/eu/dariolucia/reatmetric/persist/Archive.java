package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.persist.services.OperationalMessageArchive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Archive implements IArchive {

    private static final Logger LOG = Logger.getLogger(Archive.class.getName());

    private static final String SCHEMA_FILE_NAME = "/schema.ddl";

    private final String archiveFolder;

    private OperationalMessageArchive operationalMessageArchive;

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() throws ArchiveException {
        // if the parent folder does not exist, create it
        verifyParentFolderExistance();
        // now create-and-ignore tables
        Connection creationConnection = null;
        Statement st = null;
        try {
            // read the schema
            String schemaContents = readSchemaContents();
            // open the connection
            creationConnection = createConnection(true);
            // create a statement
            st = creationConnection.createStatement();
            // execute the schema creation
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Creating database schema: " + schemaContents);
            }
            st.execute(schemaContents);
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
            if(!e.getSQLState().equals("X0Y32")) {
                throw new ArchiveException(e);
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
        // TODO: initialise storage services
        try {
            this.operationalMessageArchive = new OperationalMessageArchive(this);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private void verifyParentFolderExistance() throws ArchiveException {
        File dataFolder = new File(archiveFolder);
        if (!dataFolder.getParentFile().exists() && !dataFolder.getParentFile().mkdirs()) {
            throw new ArchiveException("Cannot create archive data parent folder " + archiveFolder);
        }
    }

    @Override
    public void dispose() throws ArchiveException {
        // TODO: dispose storage services
        operationalMessageArchive.dispose();
    }

    @Override
    public IOperationalMessageArchive getOperationalMessageArchive() {
        return operationalMessageArchive;
    }

    private String readSchemaContents() throws IOException {
        StringBuilder contents = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SCHEMA_FILE_NAME)));
        String line;
        while ((line = br.readLine()) != null) {
            contents.append(line).append("\n");
        }
        return contents.toString();
    }

    public Connection createConnection(boolean isWriter) throws SQLException {
        // get a connection
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:derby:" + archiveFolder); // NOSONAR: it is expected to work in this way
        } catch (SQLException e) {
            if(e.getSQLState().equals("XJ004")) {
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
}
