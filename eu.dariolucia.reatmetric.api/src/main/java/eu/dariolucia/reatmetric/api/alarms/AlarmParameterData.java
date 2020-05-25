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
package eu.dariolucia.reatmetric.api.alarms;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.time.Instant;

/**
 * An instance of this class represents the alarm status of a parameter at a given point in time. Instances of this class
 * are generated by the processing model when:
 * <ul>
 *     <li>A parameter transitions to an alarm state, when its summary state is one among {@link AlarmState#ALARM}, {@link AlarmState#WARNING}, {@link AlarmState#VIOLATED}, {@link AlarmState#ERROR}</li>
 *     <li>A parameter is updated while remaining in an alarm state</li>
 *     <li>A parameter transitions from an alarm state into a nominal state</li>
 * </ul>
 *
 * Objects of this class are immutable.
 */
public final class AlarmParameterData extends AbstractDataItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final int externalId;

	private final String name;

	private final SystemEntityPath path;

	private final AlarmState currentAlarmState;

	private final Object currentValue;

	private final Instant receptionTime;

	private final Object lastNominalValue;

	private final Instant lastNominalValueTime;

	/**
	 * Constructor of the parameter alarm.
	 *
	 * @param internalId the internal ID of this object
	 * @param generationTime the generation time of the parameter sample causing the alarm generation
	 * @param externalId the ID of the parameter entity in the system entity model
	 * @param name the name of the parameter
	 * @param path the path of the parameter
	 * @param currentAlarmState the current alarm state of the parameter
	 * @param currentValue the current engineering value of the parameter
	 * @param lastNominalValue the last nominal value of the parameter
	 * @param lastNominalValueTime the generation time of the last nominal value of the parameter
	 * @param receptionTime the detection time of the alarm object, i.e. when this object was "received" by the ReatMetric system
	 * @param extension the extension object
	 */
	public AlarmParameterData(IUniqueId internalId, Instant generationTime, int externalId, String name, SystemEntityPath path, AlarmState currentAlarmState,
			Object currentValue, Object lastNominalValue,
			Instant lastNominalValueTime, Instant receptionTime, Object extension) {
		super(internalId, generationTime, extension);
		this.name = name;
		this.path = path;
		this.currentAlarmState = currentAlarmState;
		this.currentValue = currentValue;
		this.receptionTime = receptionTime;
		this.externalId = externalId;
		this.lastNominalValue = lastNominalValue;
		this.lastNominalValueTime = lastNominalValueTime;
	}

	/**
	 * The parameter name, i.e. the last part of the system entity path of the related parameter.
	 *
	 * @return the activity name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The parameter path in the system entity model.
	 *
	 * @return the activity path
	 */
	public SystemEntityPath getPath() {
		return path;
	}

	/**
	 * The alarm state linked to the generation of this parameter alarm object.
	 *
	 * @return the alarm state of this object
	 */
	public AlarmState getCurrentAlarmState() {
		return currentAlarmState;
	}

	/**
	 * The engineering value linked to the current alarm state reported by this object.
	 *
	 * @return the current engineering value
	 */
	public Object getCurrentValue() {
		return currentValue;
	}

	/**
	 * The detection time of the alarm object, i.e. when this object was "received" by the ReatMetric system.
	 *
	 * @return the time
	 */
	public Instant getReceptionTime() {
		return receptionTime;
	}

	/**
	 * The external ID of the parameter: it identifies the parameter in the system entity model, regardless
	 * of its location in the tree.
	 *
	 * @return the external ID
	 */
	public int getExternalId() {
		return externalId;
	}

	/**
	 * The last value of the parameter with a nominal state. This information is useful to characterize the
	 * importance and persistence of the current alarm.
	 *
	 * @return the last parameter value having a non-alarm state
	 */
	public Object getLastNominalValue() {
		return lastNominalValue;
	}

	/**
	 * The generation time of the last value of the parameter with a non-alarm state. This information is useful to characterize the
	 * importance and persistence of the current alarm.
	 *
	 * @return the last parameter value having a non-alarm state
	 */
	public Instant getLastNominalValueTime() {
		return lastNominalValueTime;
	}

	@Override
	public String toString() {
		return "AlarmParameterData{" +
				"internalId=" + internalId +
				", generationTime=" + generationTime +
				", externalId=" + externalId +
				", name='" + name + '\'' +
				", path=" + path +
				", currentAlarmState=" + currentAlarmState +
				", currentValue=" + currentValue +
				", receptionTime=" + receptionTime +
				", lastNominalValue=" + lastNominalValue +
				", lastNominalValueTime=" + lastNominalValueTime +
				'}';
	}
}
