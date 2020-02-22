/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.events;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.time.Instant;

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

    private final Object report;

    private final IUniqueId rawDataContainerId;

	public EventData(IUniqueId internalId, Instant generationTime, int externalId, String name,
                     SystemEntityPath path, String qualifier, String type, String route, String source, Severity severity, Object report, IUniqueId rawDataContainerId, Instant receptionTime,
					 Object extension) {
		super(internalId, generationTime, extension);
		this.externalId = externalId;
		this.name = name;
		this.path = path;
		this.qualifier = qualifier;
		this.receptionTime = receptionTime;
		this.type = type;
		this.route = route;
		this.source = source;
		this.severity = severity;
		this.report = report;
		this.rawDataContainerId = rawDataContainerId;
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

    public IUniqueId getRawDataContainerId() {
        return rawDataContainerId;
    }

    public Object getReport() {
        return report;
    }

    @Override
    public String toString() {
        return "EventData{" +
				"internalId=" + internalId +
				", generationTime=" + generationTime +
                ", externalId=" + externalId +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", qualifier='" + qualifier + '\'' +
                ", receptionTime=" + receptionTime +
                ", type='" + type + '\'' +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                ", report=" + report +
                ", containerId=" + rawDataContainerId +
                ", severity=" + severity +
                '}';
    }
}
