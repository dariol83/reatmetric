/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.UniqueItem;

/**
 *
 * @author dario
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

    public SystemEntity(IUniqueId internalId, int externalId, SystemEntityPath path, Status status, AlarmState alarmState, SystemEntityType type) {
        super(internalId, null, null);
        this.path = path;
        this.status = status;
        this.alarmState = alarmState;
        this.type = type;
        this.externalId = externalId;
    }

    public SystemEntityPath getPath() {
        return path;
    }

    public String getName() {
        return path.getLastPathElement();
    }

    public Status getStatus() {
        return status;
    }

    public AlarmState getAlarmState() {
        return alarmState;
    }

    public SystemEntityType getType() {
        return type;
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
