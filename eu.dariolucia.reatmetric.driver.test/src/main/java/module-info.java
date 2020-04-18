open module eu.dariolucia.reatmetric.driver.test {
    requires java.logging;
    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.driver.test;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.test.TestDriver;
}