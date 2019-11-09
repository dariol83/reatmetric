package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.persist.services.AbstractDataItemArchive;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

// TODO: archive factory plus service?
public class Archive {

    private final String archiveFolder;

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() throws IOException {
        // if the folder does not exist, create it
        File dataFolder = new File(archiveFolder);
        if(!dataFolder.exists()) {
            if(!dataFolder.mkdirs()) {
                throw new IOException("Cannot create archive data folder " + archiveFolder);
            }
        }
        // if no Apache Derby database exist in the folder, create it
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IOException(e);
        }
        Connection creationConnection = createConnection(true);
        // TODO: load schema? maybe it is the best approach
        // TODO: initialise storage services
        // TODO: initialise the unique id counters
    }

    public Connection createConnection(boolean isWriter) {
        // get a connection
        Connection conn = DriverManager.getConnection("jdbc:derby:" + archiveFolder + ";create=true");
        // set autocommit false
        conn.setAutoCommit(false);
        // set type (isWriter ? serialize : readonly)
        conn.setReadOnly(!isWriter);
        conn.setTransactionIsolation(isWriter ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_NONE);
        return conn;
    }
}
