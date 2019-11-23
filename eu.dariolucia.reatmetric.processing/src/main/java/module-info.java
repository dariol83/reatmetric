module eu.dariolucia.reatmetric.processing {

    requires java.logging;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.processing;
    exports eu.dariolucia.reatmetric.processing.input;
    exports eu.dariolucia.reatmetric.processing.extension;
    exports eu.dariolucia.reatmetric.processing.definition;

    provides eu.dariolucia.reatmetric.processing.IProcessingModelFactory with eu.dariolucia.reatmetric.processing.impl.ProcessingModelFactoryImpl;
}