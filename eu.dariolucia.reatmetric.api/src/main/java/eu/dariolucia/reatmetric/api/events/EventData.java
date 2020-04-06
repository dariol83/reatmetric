/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
