import eu.dariolucia.reatmetric.driver.socket.SocketDriver;

open module eu.dariolucia.reatmetric.driver.socket {
    requires java.logging;
    requires java.rmi;
    requires jakarta.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    requires eu.dariolucia.ccsds.encdec;

    provides eu.dariolucia.reatmetric.core.api.IDriver with SocketDriver;
}