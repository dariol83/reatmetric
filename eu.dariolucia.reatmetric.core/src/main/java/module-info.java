import eu.dariolucia.reatmetric.api.IReatmetricSystem;

open module eu.dariolucia.reatmetric.core {
    requires java.logging;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.core.api;
    exports eu.dariolucia.reatmetric.core.api.exceptions;
    exports eu.dariolucia.reatmetric.core.configuration;

    uses eu.dariolucia.reatmetric.core.api.IDriver;
    uses eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
    uses eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;

    provides IReatmetricSystem with eu.dariolucia.reatmetric.core.ServiceCoreImpl;
}