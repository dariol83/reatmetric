import eu.dariolucia.reatmetric.persist.timescale.ArchiveFactory;

open module eu.dariolucia.reatmetric.persist.timescale {

    requires java.logging;
    requires java.sql;
    requires eu.dariolucia.reatmetric.api;
    requires org.postgresql.jdbc;

    exports eu.dariolucia.reatmetric.persist.timescale;

    provides eu.dariolucia.reatmetric.api.archive.IArchiveFactory with ArchiveFactory;
}