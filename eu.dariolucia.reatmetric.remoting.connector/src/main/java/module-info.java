open module eu.dariolucia.reatmetric.remoting.connector {
    requires java.logging;
    requires java.rmi;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.remoting.connector;
    exports eu.dariolucia.reatmetric.remoting.connector.configuration;

    provides eu.dariolucia.reatmetric.api.IReatmetricRegister with eu.dariolucia.reatmetric.remoting.connector.ReatmetricConnectorRegistry;
}