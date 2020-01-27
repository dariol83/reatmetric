module eu.dariolucia.reatmetric.core {
    requires java.logging;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.core.api;
    exports eu.dariolucia.reatmetric.core.api.exceptions;
    exports eu.dariolucia.reatmetric.core.configuration;
}