open module eu.dariolucia.reatmetric.api {
    uses eu.dariolucia.reatmetric.api.value.IValueExtensionHandler;

    requires java.logging;

    exports eu.dariolucia.reatmetric.api;
    exports eu.dariolucia.reatmetric.api.alarms;
    exports eu.dariolucia.reatmetric.api.common;
    exports eu.dariolucia.reatmetric.api.common.exceptions;
    exports eu.dariolucia.reatmetric.api.events;
    exports eu.dariolucia.reatmetric.api.messages;
    exports eu.dariolucia.reatmetric.api.model;
    exports eu.dariolucia.reatmetric.api.parameters;
    exports eu.dariolucia.reatmetric.api.rawdata;
    exports eu.dariolucia.reatmetric.api.archive;
    exports eu.dariolucia.reatmetric.api.archive.exceptions;
    exports eu.dariolucia.reatmetric.api.value;
    exports eu.dariolucia.reatmetric.api.activity;
    exports eu.dariolucia.reatmetric.api.processing;
    exports eu.dariolucia.reatmetric.api.processing.exceptions;
    exports eu.dariolucia.reatmetric.api.processing.input;
    exports eu.dariolucia.reatmetric.api.processing.scripting;
    exports eu.dariolucia.reatmetric.api.scheduler;
    exports eu.dariolucia.reatmetric.api.scheduler.exceptions;
    exports eu.dariolucia.reatmetric.api.scheduler.input;
    exports eu.dariolucia.reatmetric.api.transport;
    exports eu.dariolucia.reatmetric.api.transport.exceptions;
}