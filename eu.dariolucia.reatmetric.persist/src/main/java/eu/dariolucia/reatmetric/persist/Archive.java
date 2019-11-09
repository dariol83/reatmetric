package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.persist.services.OperationalMessageArchive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Archive implements IArchive {

    private final static String SCHEMA_FILE_NAME = "/schema.ddl";

    private final String archiveFolder;

    private OperationalMessageArchive operationalMessageArchive;

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() throws ArchiveException {
        // if the folder does not exist, create it
        File dataFolder = new File(archiveFolder);
        if (!dataFolder.getParentFile().exists()) {
            if (!dataFolder.getParentFile().mkdirs()) {
                throw new ArchiveException("Cannot create archive data parent folder " + archiveFolder);
            }
        }
        // if no Apache Derby database exist in the folder, create it
        // try {
        //     Class.forName("org.apache.derby.jdbc.ClientDriver").getDeclaredConstructor().newInstance();
        // } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
        //     throw new ArchiveException(e);
        // }
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
            System.out.println("Execute: " + schemaContents);
            st.execute(schemaContents);
            // commit
            creationConnection.commit();
        } catch (IOException e) {
            throw new ArchiveException(e);
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (creationConnection != null) {
                    creationConnection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            // TODO: if there error is 'table already exists', OK, otherwise wrap exception and rethrow
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (creationConnection != null) {
                    creationConnection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // TODO: initialise storage services
        try {
            this.operationalMessageArchive = new OperationalMessageArchive(this);
        } catch (SQLException e) {
            throw new ArchiveException(e);
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
            conn = DriverManager.getConnection("jdbc:derby:" + archiveFolder);
        } catch (SQLException e) {
            // e.printStackTrace();
            // maybe the database does not exist ...?
            conn = DriverManager.getConnection("jdbc:derby:" + archiveFolder + ";create=true");
        }
        if (conn != null) {
            // set autocommit false
            conn.setAutoCommit(false);
            // set type (isWriter ? serialize : readonly)
            conn.setReadOnly(!isWriter);
            conn.setTransactionIsolation(isWriter ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_READ_UNCOMMITTED);
            return conn;
        } else {
            throw new SQLException("Cannot acquire connection for database " + archiveFolder);
        }
    }
}
