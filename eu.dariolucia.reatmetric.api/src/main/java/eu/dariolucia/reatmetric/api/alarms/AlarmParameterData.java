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

	public String getName() {
		return name;
	}

	public SystemEntityPath getPath() {
		return path;
	}

	public AlarmState getCurrentAlarmState() {
		return currentAlarmState;
	}

	public Object getCurrentValue() {
		return currentValue;
	}

	public Instant getReceptionTime() {
		return receptionTime;
	}

	public int getExternalId() {
		return externalId;
	}

	public Object getLastNominalValue() {
		return lastNominalValue;
	}

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
