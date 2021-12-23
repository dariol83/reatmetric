import eu.dariolucia.reatmetric.driver.remote.RemoteDriver;

open module eu.dariolucia.reatmetric.driver.remote {
    requires java.logging;
    requires java.xml.bind;
    requires java.rmi;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.remoting.connector;

    exports eu.dariolucia.reatmetric.driver.remote;
    exports eu.dariolucia.reatmetric.driver.remote.definition;
    exports eu.dariolucia.reatmetric.driver.remote.connectors;

    provides eu.dariolucia.reatmetric.core.api.IDriver with RemoteDriver;
}