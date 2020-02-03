module eu.dariolucia.reatmetric.driver.test {
    requires java.logging;
    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.test.TestDriver;
}