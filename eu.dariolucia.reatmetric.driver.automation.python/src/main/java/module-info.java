import eu.dariolucia.reatmetric.driver.automation.python.PythonAutomationDriver;

open module eu.dariolucia.reatmetric.driver.automation.python {
    requires java.logging;
    requires java.xml.bind;
    requires java.scripting;
    requires java.rmi;

    requires jython.slim;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;
    requires eu.dariolucia.reatmetric.driver.automation.base;

    exports eu.dariolucia.reatmetric.driver.automation.python;

    provides eu.dariolucia.reatmetric.core.api.IDriver with PythonAutomationDriver;
}