open module eu.dariolucia.reatmetric.processing {
    uses eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;
    uses eu.dariolucia.reatmetric.processing.extension.ICheckExtension;

    requires java.logging;
    requires java.xml.bind;
    requires java.scripting;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.processing;
    exports eu.dariolucia.reatmetric.processing.input;
    exports eu.dariolucia.reatmetric.processing.extension;
    exports eu.dariolucia.reatmetric.processing.definition;
    exports eu.dariolucia.reatmetric.processing.definition.scripting;

    provides eu.dariolucia.reatmetric.processing.IProcessingModelFactory with eu.dariolucia.reatmetric.processing.impl.ProcessingModelFactoryImpl;
}