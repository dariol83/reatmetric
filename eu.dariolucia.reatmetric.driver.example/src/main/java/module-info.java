open module eu.dariolucia.reatmetric.driver.example {
    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires java.rmi;

    exports eu.dariolucia.reatmetric.driver.example;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.example.ExampleDriver;
}