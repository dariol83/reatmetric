package eu.dariolucia.reatmetric.persist;

public class Archive {

    private final String archiveFolder;

    public Archive(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void connect() {
        // TODO: if the folder does not exist, create it
        // TODO: if no database exist in the folder, create it
        // TODO: open database
        // TODO: initialise the unique id counters
        // TODO: initialise storage services
    }

}
