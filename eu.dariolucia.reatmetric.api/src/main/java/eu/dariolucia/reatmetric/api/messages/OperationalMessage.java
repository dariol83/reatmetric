/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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

    public OperationalMessage(IUniqueId internalId, String id, String message, Instant generationTime, String source, Severity severity, Object[] additionalFields) {
        super(internalId, generationTime, additionalFields);
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
    public int hashCode() {
        int hash = super.hashCode();
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.generationTime);
        hash = 59 * hash + Objects.hashCode(this.message);
        hash = 59 * hash + Objects.hashCode(this.source);
        hash = 59 * hash + Objects.hashCode(this.severity);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OperationalMessage other = (OperationalMessage) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.generationTime, other.generationTime)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (this.severity != other.severity) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "OperationalMessage [" + "id=" + id + ", message=" + message + ", source=" + source + ", severity=" + severity + ']';
    }
    
}
