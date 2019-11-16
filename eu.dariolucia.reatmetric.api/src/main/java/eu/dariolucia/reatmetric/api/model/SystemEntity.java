/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.UniqueItem;

/**
 *
 * @author dario
 */
public final class SystemEntity extends UniqueItem implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath path;
    
    private final String name;
    
    private final Status status;
    
    private final AlarmState alarmState;
    
    private final SystemEntityType type;

    public SystemEntity(IUniqueId internalId, SystemEntityPath path, String name, Status status, AlarmState alarmState, SystemEntityType type) {
        super(internalId, null);
        this.path = path;
        this.name = name;
        this.status = status;
        this.alarmState = alarmState;
        this.type = type;
    }

    public SystemEntityPath getPath() {
        return path;
    }

    public String getName() {
        return name;
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
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.internalId);
        hash = 19 * hash + Objects.hashCode(this.path);
        hash = 19 * hash + Objects.hashCode(this.name);
        hash = 19 * hash + Objects.hashCode(this.status);
        hash = 19 * hash + Objects.hashCode(this.alarmState);
        hash = 19 * hash + Objects.hashCode(this.type);
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
        final SystemEntity other = (SystemEntity) obj;
        if (!Objects.equals(this.internalId, other.internalId)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        if (this.alarmState != other.alarmState) {
            return false;
        }
        return this.type == other.type;
    }

    @Override
    public String toString() {
        return "SystemEntity{" + "path=" + path + ", name=" + name + ", status=" + status + ", alarmState=" + alarmState + ", type=" + type + '}';
    }
    
}
