/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.alarms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 *
 * @author dario
 */
public final class AlarmParameterDataFilter extends AbstractDataItemFilter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3013429180944834538L;

	private final SystemEntityPath parentPath;  
	
    private final List<AlarmState> alarmStateList;

    public AlarmParameterDataFilter(SystemEntityPath parentPath, List<AlarmState> alarmStateList) {
        this.parentPath = parentPath;
        
        if(alarmStateList != null) {
            this.alarmStateList = new ArrayList<>(alarmStateList);
        } else {
            this.alarmStateList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }
    
    public List<AlarmState> getAlarmStateList() {
        if(alarmStateList == null) {
            return null;
        }
        return new ArrayList<>(alarmStateList);
    }
    
    @Override
    public boolean isClear() {
        return this.parentPath == null && this.alarmStateList == null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.parentPath);
        hash = 19 * hash + Objects.hashCode(this.alarmStateList);
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
        final AlarmParameterDataFilter other = (AlarmParameterDataFilter) obj;
        if (!Objects.equals(this.parentPath, other.parentPath)) {
            return false;
        }
        if (!Objects.equals(this.alarmStateList, other.alarmStateList)) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "AlarmParameterDataFilter [parentPath=" + parentPath + ", alarmStateList=" + alarmStateList + "]";
	}

}
