/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.events;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 *
 * @author dario
 */
public final class EventData extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final int externalId;

	private final String name;

    private final SystemEntityPath path;
    
    private final String qualifier;
    
    private final Instant receptionTime;

    private final String type;
    
    private final String route;

    private final String source;
    
    private final Severity severity;

	public EventData(IUniqueId internalId, Instant generationTime, int externalId, String name,
                     SystemEntityPath path, String qualifier, String type, String route, String source, Severity severity, Instant receptionTime,
                     Object[] additionalFields) {
		super(internalId, generationTime, additionalFields);
		this.externalId = externalId;
		this.name = name;
		this.path = path;
		this.qualifier = qualifier;
		this.receptionTime = receptionTime;
		this.type = type;
		this.route = route;
		this.source = source;
		this.severity = severity;
	}

    public int getExternalId() {
        return externalId;
    }

    public String getName() {
		return name;
	}

	public SystemEntityPath getPath() {
		return path;
	}

	public String getQualifier() {
		return qualifier;
	}

	public Instant getReceptionTime() {
		return receptionTime;
	}

	public String getType() {
		return type;
	}

	public String getRoute() {
		return route;
	}

	public String getSource() {
		return source;
	}

	public Severity getSeverity() {
		return severity;
	}

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.externalId);
        hash = 59 * hash + Objects.hashCode(this.path);
        hash = 59 * hash + Objects.hashCode(this.qualifier);
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + Objects.hashCode(this.receptionTime);
        hash = 59 * hash + Objects.hashCode(this.generationTime);
        hash = 59 * hash + Objects.hashCode(this.route);
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
        final EventData other = (EventData) obj;
        if (!Objects.equals(this.externalId, other.externalId)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (!Objects.equals(this.receptionTime, other.receptionTime)) {
            return false;
        }
        if (!Objects.equals(this.generationTime, other.generationTime)) {
            return false;
        }
        if (!Objects.equals(this.qualifier, other.qualifier)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.route, other.route)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (this.severity != other.severity) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EventData{" +
                "externalId=" + externalId +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", qualifier='" + qualifier + '\'' +
                ", receptionTime=" + receptionTime +
                ", type='" + type + '\'' +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                ", severity=" + severity +
                ", generationTime=" + generationTime +
                ", internalId=" + internalId +
                '}';
    }
}
