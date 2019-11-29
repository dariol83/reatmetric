package eu.dariolucia.reatmetric.processing.definition.scripting;

import java.time.Instant;

public interface IEntityBinding {

    long id();

    String path();

    Instant generationTime();

    Instant receptionTime();
}
