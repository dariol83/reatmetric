import eu.dariolucia.reatmetric.driver.automation.js.JsAutomationDriver;

open module eu.dariolucia.reatmetric.driver.automation.js {
    requires java.logging;
    requires jakarta.xml.bind;
    requires java.scripting;
    requires java.rmi;

    requires org.graalvm.polyglot;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.driver.automation.base;

    exports eu.dariolucia.reatmetric.driver.automation.js;

    provides eu.dariolucia.reatmetric.core.api.IDriver with JsAutomationDriver;
}