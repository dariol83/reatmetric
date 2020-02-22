/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.messages;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

/**
 *
 * @author dario
 */
public final class OperationalMessage extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4447421541107887154L;

	private final String id;

    private final String message;

    private final String source;

    private final Severity severity;

    public OperationalMessage(IUniqueId internalId, Instant generationTime, String id, String message, String source, Severity severity, Object extension) {
        super(internalId, generationTime, extension);
        this.id = id;
        this.message = message;
        this.source = source;
        this.severity = severity;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "OperationalMessage{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                ", severity=" + severity +
                ", generationTime=" + generationTime +
                ", internalId=" + internalId +
                '}';
    }
}
