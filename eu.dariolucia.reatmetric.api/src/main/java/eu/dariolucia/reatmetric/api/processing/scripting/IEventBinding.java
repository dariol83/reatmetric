package eu.dariolucia.reatmetric.api.processing.scripting;

import eu.dariolucia.reatmetric.api.messages.Severity;

public interface IEventBinding extends IEntityBinding {

    Severity severity();

    String route();

    String source();

    String type();

    String qualifier();

    Object report();

    Long containerId();
}
