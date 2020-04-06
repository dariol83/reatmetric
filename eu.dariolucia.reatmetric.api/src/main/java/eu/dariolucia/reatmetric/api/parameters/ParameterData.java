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


package eu.dariolucia.reatmetric.api.parameters;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 *
 * @author dario
 */
public final class ParameterData extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private final int externalId;

	private final String name;

    private final SystemEntityPath path;
    
    private final Object engValue;
    
    private final Object sourceValue;
    
    private final Instant receptionTime;

    private final String route;

    private final Validity validity;

    private final IUniqueId rawDataContainerId;

    private final AlarmState alarmState;

    public ParameterData(IUniqueId internalId, Instant generationTime, int externalId, String name, SystemEntityPath path,
                         Object engValue, Object sourceValue, String route, Validity validity, AlarmState alarmState,
                         IUniqueId rawDataContainerId, Instant receptionTime, Object extension) {
        super(internalId, generationTime, extension);
        this.name = name;
        this.externalId = externalId;
        this.path = path;
        this.engValue = engValue;
        this.sourceValue = sourceValue;
        this.receptionTime = receptionTime;
        this.validity = validity;
        this.alarmState = alarmState;
        this.route = route;
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

    public Object getEngValue() {
        return engValue;
    }

    public Object getSourceValue() {
        return sourceValue;
    }

    public Instant getReceptionTime() {
        return receptionTime;
    }

    public Validity getValidity() {
        return validity;
    }

    public AlarmState getAlarmState() {
        return alarmState;
    }

    public String getRoute() {
        return route;
    }

    public IUniqueId getRawDataContainerId() {
        return rawDataContainerId;
    }

    @Override
    public String toString() {
        return "ParameterData{" +
                "internalId=" + internalId +
                ", generationTime=" + generationTime +
                ", externalId=" + externalId +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", engValue=" + engValue +
                ", sourceValue=" + sourceValue +
                ", receptionTime=" + receptionTime +
                ", route='" + route + '\'' +
                ", validity=" + validity +
                ", alarmState=" + alarmState +
                ", containerId=" + rawDataContainerId +
                '}';
    }
}
