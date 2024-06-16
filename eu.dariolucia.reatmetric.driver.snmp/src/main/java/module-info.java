import eu.dariolucia.reatmetric.driver.snmp.SnmpDriver;

open module eu.dariolucia.reatmetric.driver.snmp {
    requires java.logging;
    requires java.rmi;
    requires jakarta.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    requires org.snmp4j;

    provides eu.dariolucia.reatmetric.core.api.IDriver with SnmpDriver;
}