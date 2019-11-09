open module eu.dariolucia.reatmetric.persist {

    requires java.logging;
    requires java.sql;
    requires eu.dariolucia.reatmetric.api;
    requires org.apache.derby.engine;
    requires org.apache.derby.commons;
    requires org.apache.derby.client;

    exports eu.dariolucia.reatmetric.persist;

    provides eu.dariolucia.reatmetric.api.archive.IArchiveFactory with eu.dariolucia.reatmetric.persist.ArchiveFactory;
}