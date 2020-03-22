import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptorValueExtensionHandler;

module eu.dariolucia.reatmetric.driver.spacecraft {
    requires java.logging;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    requires eu.dariolucia.ccsds.encdec;
    requires eu.dariolucia.ccsds.tmtc;
    requires eu.dariolucia.ccsds.sle.utl;

    exports eu.dariolucia.reatmetric.driver.spacecraft;
    exports eu.dariolucia.reatmetric.driver.spacecraft.common;
    exports eu.dariolucia.reatmetric.driver.spacecraft.definition;

    provides eu.dariolucia.reatmetric.api.value.IValueExtensionHandler with TmFrameDescriptorValueExtensionHandler;
}