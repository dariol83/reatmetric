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
 * Represents parameter data in the ReatMetric system.
 * This class contains all the information related to a parameter value, including its engineering value,
 * source value, validity, alarm state, and routing information.
 *
 * @author dario
 */
public final class ParameterData extends AbstractDataItem implements Serializable {

    /**
     * The serial version UID for serialization compatibility.
     */
	private static final long serialVersionUID = 1L;

    /**
     * The external ID of the parameter.
     */
    private final int externalId;

	/**
     * The name of the parameter.
     */
	private final String name;

    /**
     * The path of the parameter in the system entity hierarchy.
     */
    private final SystemEntityPath path;
    
    /**
     * The engineering value of the parameter (processed/calibrated value).
     */
    private final Object engValue;
    
    /**
     * The source value of the parameter (raw value as received from the source).
     */
    private final Object sourceValue;
    
    /**
     * The reception time of the parameter data.
     */
    private final Instant receptionTime;

    /**
     * The route from which the parameter data was received.
     */
    private final String route;

    /**
     * The validity of the parameter data.
     */
    private final Validity validity;

    /**
     * The unique identifier of the raw data container associated with this parameter data.
     */
    private final IUniqueId rawDataContainerId;

    /**
     * The alarm state of the parameter.
     */
    private final AlarmState alarmState;

    /**
     * Constructor for parameter data.
     *
     * @param internalId the unique internal ID, cannot be null
     * @param generationTime the generation time, cannot be null
     * @param externalId the external ID of the parameter
     * @param name the name of the parameter, cannot be null
     * @param path the path of the parameter, cannot be null
     * @param engValue the engineering value, can be null
     * @param sourceValue the source value, can be null
     * @param route the route, can be null
     * @param validity the validity, cannot be null
     * @param alarmState the alarm state, cannot be null
     * @param rawDataContainerId the raw data container ID, can be null
     * @param receptionTime the reception time, can be null
     * @param extension the extension object, can be null
     */
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

    /**
     * Returns the external ID of the parameter.
     *
     * @return the external ID
     */
    public int getExternalId() {
        return externalId;
    }

    /**
     * Returns the name of the parameter.
     *
     * @return the name (cannot be null)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the path of the parameter in the system entity hierarchy.
     *
     * @return the path (cannot be null)
     */
    public SystemEntityPath getPath() {
        return path;
    }

    /**
     * Returns the engineering value of the parameter (processed/calibrated value).
     *
     * @return the engineering value (can be null)
     */
    public Object getEngValue() {
        return engValue;
    }

    /**
     * Returns the source value of the parameter (raw value as received from the source).
     *
     * @return the source value (can be null)
     */
    public Object getSourceValue() {
        return sourceValue;
    }

    /**
     * Returns the reception time of the parameter data.
     *
     * @return the reception time (can be null)
     */
    public Instant getReceptionTime() {
        return receptionTime;
    }

    /**
     * Returns the validity of the parameter data.
     *
     * @return the validity (cannot be null)
     */
    public Validity getValidity() {
        return validity;
    }

    /**
     * Returns the alarm state of the parameter.
     *
     * @return the alarm state (cannot be null)
     */
    public AlarmState getAlarmState() {
        return alarmState;
    }

    /**
     * Returns the route from which the parameter data was received.
     *
     * @return the route (can be null)
     */
    public String getRoute() {
        return route;
    }

    /**
     * Returns the unique identifier of the raw data container associated with this parameter data.
     *
     * @return the raw data container ID (can be null)
     */
    public IUniqueId getRawDataContainerId() {
        return rawDataContainerId;
    }

    /**
     * Returns a string representation of this parameter data.
     *
     * @return a string representation of this object
     */
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
