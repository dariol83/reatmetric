import eu.dariolucia.reatmetric.driver.automation.groovy.GroovyAutomationDriver;

open module eu.dariolucia.reatmetric.driver.automation.groovy {
    requires java.logging;
    requires java.xml.bind;
    requires java.scripting;
    requires java.rmi;

    requires org.codehaus.groovy;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;
    requires eu.dariolucia.reatmetric.driver.automation.base;

    exports eu.dariolucia.reatmetric.driver.automation.groovy;

    provides eu.dariolucia.reatmetric.core.api.IDriver with GroovyAutomationDriver;
}