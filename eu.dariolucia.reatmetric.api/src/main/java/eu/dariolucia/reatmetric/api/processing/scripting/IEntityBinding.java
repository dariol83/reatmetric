package eu.dariolucia.reatmetric.api.processing.scripting;

import java.time.Instant;

public interface IEntityBinding {

    long id();

    String path();

    Instant generationTime();

    Instant receptionTime();
}
