import eu.dariolucia.reatmetric.driver.socket.SocketDriver;
import eu.dariolucia.reatmetric.driver.socket.types.LengthPaddedUsAsciiStringDecoder;
import eu.dariolucia.reatmetric.driver.socket.types.LengthPaddedUsAsciiStringEncoder;

open module eu.dariolucia.reatmetric.driver.socket {
    requires java.logging;
    requires java.rmi;
    requires jakarta.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    requires eu.dariolucia.ccsds.encdec;

    provides eu.dariolucia.reatmetric.core.api.IDriver with SocketDriver;

    provides eu.dariolucia.ccsds.encdec.extension.IEncoderExtension with LengthPaddedUsAsciiStringEncoder;
    provides eu.dariolucia.ccsds.encdec.extension.IDecoderExtension with LengthPaddedUsAsciiStringDecoder;
}