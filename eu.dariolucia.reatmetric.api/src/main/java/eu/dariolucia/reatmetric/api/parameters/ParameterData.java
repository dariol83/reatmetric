/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
	private static final long serialVersionUID = -6541375067096461705L;

	private final String name;

    private final SystemEntityPath path;
    
    private final Object engValue;
    
    private final Object sourceValue;
    
    private final Instant receptionTime;
    
    private final SystemEntityPath parent;

    private final Validity validity;
    
    private final AlarmState alarmState;

    public ParameterData(IUniqueId internalId, String name, SystemEntityPath path, Object engValue, Object sourceValue, Instant receptionTime, SystemEntityPath parent, Validity validity, AlarmState alarmState, Instant generationTime, Object[] additionalFields) {
        super(internalId, generationTime, additionalFields);
        this.name = name;
        this.path = path;
        this.engValue = engValue;
        this.sourceValue = sourceValue;
        this.receptionTime = receptionTime;
        this.parent = parent;
        this.validity = validity;
        this.alarmState = alarmState;
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

    public SystemEntityPath getParent() {
        return parent;
    }

    public Validity getValidity() {
        return validity;
    }

    public AlarmState getAlarmState() {
        return alarmState;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.path);
        hash = 59 * hash + Objects.hashCode(this.engValue);
        hash = 59 * hash + Objects.hashCode(this.sourceValue);
        hash = 59 * hash + Objects.hashCode(this.receptionTime);
        hash = 59 * hash + Objects.hashCode(this.generationTime);
        hash = 59 * hash + Objects.hashCode(this.validity);
        hash = 59 * hash + Objects.hashCode(this.alarmState);
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
        final ParameterData other = (ParameterData) obj;
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (!Objects.equals(this.receptionTime, other.receptionTime)) {
            return false;
        }
        if (!Objects.equals(this.generationTime, other.generationTime)) {
            return false;
        }
        if (!Objects.equals(this.engValue, other.engValue)) {
            return false;
        }
        if (!Objects.equals(this.sourceValue, other.sourceValue)) {
            return false;
        }
        if (this.validity != other.validity) {
            return false;
        }
        if (this.alarmState != other.alarmState) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "ParameterData [name=" + name + ", path=" + path + ", engValue=" + engValue + ", sourceValue="
				+ sourceValue + ", receptionTime=" + receptionTime + ", parent=" + parent + ", validity=" + validity
				+ ", alarmState=" + alarmState + "]";
	}
	
}
