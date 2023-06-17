open module eu.dariolucia.reatmetric.remoting {
    requires java.logging;
    requires java.rmi;

    // In order to activate JAXB implementation
    requires jakarta.xml.bind;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.remoting;
    exports eu.dariolucia.reatmetric.remoting.stubs;

    uses eu.dariolucia.reatmetric.api.IReatmetricRegister;
}