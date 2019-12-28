/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class ActivityOccurrenceDataFilter extends AbstractDataItemFilter implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;

    private final List<String> routeList;

    private final List<String> typeList;

    private final List<ActivityOccurrenceState> stateList;

    public ActivityOccurrenceDataFilter(SystemEntityPath parentPath, List<String> routeList, List<String> typeList, List<ActivityOccurrenceState> stateList) {
        this.parentPath = parentPath;
        if(routeList != null) {
            this.routeList = List.copyOf(routeList);
        } else {
            this.routeList = null;
        }
        if(typeList != null) {
            this.typeList = List.copyOf(typeList);
        } else {
            this.typeList = null;
        }
        if(stateList != null) {
            this.stateList = List.copyOf(stateList);
        } else {
            this.stateList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<ActivityOccurrenceState> getStateList() {
        return stateList;
    }

    public List<String> getRouteList() {
        return routeList;
    }
    
    public List<String> getTypeList() {
        return typeList;
    }
    
    @Override
    public boolean isClear() {
        return this.parentPath == null && this.stateList == null && this.routeList == null && this.typeList == null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.parentPath);
        hash = 19 * hash + Objects.hashCode(this.stateList);
        hash = 19 * hash + Objects.hashCode(this.routeList);
        hash = 19 * hash + Objects.hashCode(this.typeList);
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
        final ActivityOccurrenceDataFilter other = (ActivityOccurrenceDataFilter) obj;
        if (!Objects.equals(this.parentPath, other.parentPath)) {
            return false;
        }
        if (!Objects.equals(this.stateList, other.stateList)) {
            return false;
        }
        if (!Objects.equals(this.routeList, other.routeList)) {
            return false;
        }
        if (!Objects.equals(this.typeList, other.typeList)) {
            return false;
        }
        return true;
    }

	@Override
	public String toString() {
		return "EventDataFilter [parentPath=" + parentPath + ", routeList=" + routeList + ", typeList=" + typeList
				+ ", stateList=" + stateList + "]";
	}

}
