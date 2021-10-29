open module eu.dariolucia.reatmetric.driver.serial {
    requires java.logging;
    requires java.xml.bind;
    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    exports eu.dariolucia.reatmetric.driver.serial;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.serial.SerialDriver;
}