open module eu.dariolucia.reatmetric.driver.snmp.util {
    requires java.logging;
    requires java.rmi;
    requires jakarta.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires eu.dariolucia.reatmetric.processing;
    requires eu.dariolucia.reatmetric.driver.snmp;

    requires org.snmp4j;
}