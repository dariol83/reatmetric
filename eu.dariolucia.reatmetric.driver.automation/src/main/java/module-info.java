open module eu.dariolucia.reatmetric.driver.automation {
    requires java.logging;
    requires java.xml.bind;
    requires java.scripting;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.driver.script;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.script.AutomationDriver;
}