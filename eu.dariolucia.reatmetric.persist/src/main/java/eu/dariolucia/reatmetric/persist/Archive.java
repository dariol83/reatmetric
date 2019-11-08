package eu.dariolucia.reatmetric.persist;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.persist.services.AbstractDataItemArchive;

import java.sql.Connection;

public class Archive {

    private final String archiveFolder;

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() {
        // TODO: if the folder does not exist, create it
        // TODO: if no database exist in the folder, create it
        // TODO: open database
        // TODO: initialise storage services
        // TODO: initialise the unique id counters
    }

    public Connection createConnection(boolean isWriter) {
        // TODO: set autocommit false
        // TODO: set type (isWriter ? serialize : readonly)
        return null;
    }
}
