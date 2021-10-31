open module eu.dariolucia.reatmetric.driver.serial {
    requires java.logging;
    requires java.xml.bind;
    requires java.rmi;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires com.fazecast.jSerialComm;

    exports eu.dariolucia.reatmetric.driver.serial;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.serial.SerialDriver;
}