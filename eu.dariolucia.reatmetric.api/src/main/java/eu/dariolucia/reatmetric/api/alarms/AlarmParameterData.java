/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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
	private static final long serialVersionUID = 8068791311138022456L;

	private final String name;

	private final SystemEntityPath path;

	private final AlarmState currentAlarmState;

	private final Object currentValue;

	private final Instant receptionTime;

	private final SystemEntityPath parent;

	private final Object lastNominalValue;

	private final Instant lastNominalValueTime;

	public AlarmParameterData(IUniqueId internalId, String name, SystemEntityPath path, AlarmState currentAlarmState,
			Object currentValue, Instant receptionTime, SystemEntityPath parent, Object lastNominalValue,
			Instant lastNominalValueTime, Instant generationTime, Object[] additionalFields) {
		super(internalId, generationTime, additionalFields);
		this.name = name;
		this.path = path;
		this.currentAlarmState = currentAlarmState;
		this.currentValue = currentValue;
		this.receptionTime = receptionTime;
		this.parent = parent;
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

	public SystemEntityPath getParent() {
		return parent;
	}

	public Object getLastNominalValue() {
		return lastNominalValue;
	}

	public Instant getLastNominalValueTime() {
		return lastNominalValueTime;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 59 * hash + Objects.hashCode(this.path);
		hash = 59 * hash + Objects.hashCode(this.currentAlarmState);
		hash = 59 * hash + Objects.hashCode(this.currentValue);
		hash = 59 * hash + Objects.hashCode(this.receptionTime);
		hash = 59 * hash + Objects.hashCode(this.generationTime);
		hash = 59 * hash + Objects.hashCode(this.lastNominalValue);
		hash = 59 * hash + Objects.hashCode(this.lastNominalValueTime);
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
		final AlarmParameterData other = (AlarmParameterData) obj;
		if (!Objects.equals(this.path, other.path)) {
			return false;
		}
		if (!Objects.equals(this.receptionTime, other.receptionTime)) {
			return false;
		}
		if (!Objects.equals(this.generationTime, other.generationTime)) {
			return false;
		}
		if (!Objects.equals(this.currentAlarmState, other.currentAlarmState)) {
			return false;
		}
		if (!Objects.equals(this.currentValue, other.currentValue)) {
			return false;
		}
		if (!Objects.equals(this.lastNominalValue, other.lastNominalValue)) {
			return false;
		}
		if (!Objects.equals(this.lastNominalValueTime, other.lastNominalValueTime)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AlarmParameterData [name=" + name + ", path=" + path + ", currentAlarmState=" + currentAlarmState
				+ ", currentValue=" + currentValue + ", receptionTime=" + receptionTime + ", parent=" + parent
				+ ", lastNominalValue=" + lastNominalValue + ", lastNominalValueTime=" + lastNominalValueTime + "]";
	}

}
