open module eu.dariolucia.reatmetric.driver.automation.base {
    requires java.logging;
    requires jakarta.xml.bind;
    requires java.scripting;
    requires java.rmi;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    exports eu.dariolucia.reatmetric.driver.automation.base;
    exports eu.dariolucia.reatmetric.driver.automation.base.common;
    exports eu.dariolucia.reatmetric.driver.automation.base.definition;
}