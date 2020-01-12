import eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;

open module eu.dariolucia.reatmetric.processing {
    requires java.logging;
    requires java.xml.bind;
    requires java.scripting;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.processing.extension;
    exports eu.dariolucia.reatmetric.processing.definition;

    uses eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;
    uses eu.dariolucia.reatmetric.processing.extension.ICheckExtension;

    provides IProcessingModelFactory with eu.dariolucia.reatmetric.processing.impl.ProcessingModelFactoryImpl;
    provides eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension with eu.dariolucia.reatmetric.processing.extension.internal.IdentityCalibration;
    provides eu.dariolucia.reatmetric.processing.extension.ICheckExtension with eu.dariolucia.reatmetric.processing.extension.internal.NoCheck;
}