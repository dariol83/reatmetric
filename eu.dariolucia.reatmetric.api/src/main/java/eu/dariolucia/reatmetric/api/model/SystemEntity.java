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


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.UniqueItem;

/**
 * The state of a system entity in the ReatMetric processing model.
 *
 * Objects of this class are immutable.
 */
public final class SystemEntity extends AbstractDataItem implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final int externalId;

	private final SystemEntityPath path;

    private final Status status;
    
    private final AlarmState alarmState;
    
    private final SystemEntityType type;

    /**
     * Class constructor.
     *
     * @param internalId the internal ID of the system entity state
     * @param externalId the ID of the system entity
     * @param path the path of the system entity
     * @param status the {@link Status} of the system entity
     * @param alarmState the {@link AlarmState} of the system entity
     * @param type the {@link SystemEntityType}
     */
    public SystemEntity(IUniqueId internalId, int externalId, SystemEntityPath path, Status status, AlarmState alarmState, SystemEntityType type) {
        super(internalId, null, null);
        this.path = path;
        this.status = status;
        this.alarmState = alarmState;
        this.type = type;
        this.externalId = externalId;
    }

    /**
     * Return the system entity path.
     *
     * @return the path of the system entity
     */
    public SystemEntityPath getPath() {
        return path;
    }

    /**
     * Return the system entity name (last item in the path).
     *
     * @return the name of the system entity
     */
    public String getName() {
        return path.getLastPathElement();
    }

    /**
     * Return the system entity status.
     *
     * @return the status of the system entity
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Return the system entity alarm state.
     *
     * @return the alarm state of the system entity
     */
    public AlarmState getAlarmState() {
        return alarmState;
    }

    /**
     * Return the system entity type.
     *
     * @return the type of the system entity
     */
    public SystemEntityType getType() {
        return type;
    }

    /**
     * Return the system entity ID.
     *
     * @return the ID of the system entity
     */
    public int getExternalId() {
        return externalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemEntity that = (SystemEntity) o;
        return externalId == that.externalId &&
                Objects.equals(getPath(), that.getPath()) &&
                getStatus() == that.getStatus() &&
                getAlarmState() == that.getAlarmState() &&
                getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId, getPath(), getStatus(), getAlarmState(), getType());
    }

    @Override
    public String toString() {
        return "SystemEntity{" + "path=" + path + ", status=" + status + ", alarmState=" + alarmState + ", type=" + type + '}';
    }
    
}
