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
			Instant lastNominalValueTime, Instant receptionTime, Object[] additionalFields) {
		super(internalId, generationTime, additionalFields);
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
				"externalId=" + externalId +
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
