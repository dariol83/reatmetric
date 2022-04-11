open module eu.dariolucia.reatmetric.driver.test {
    requires java.logging;
    requires java.rmi;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.driver.test;
    exports eu.dariolucia.reatmetric.driver.test.definition;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.test.TestDriver;
}