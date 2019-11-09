/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.events;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class EventDataFilter extends AbstractDataItemFilter implements Serializable {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = -2061173278990530507L;

	private final SystemEntityPath parentPath;
    
    private final List<String> routeList;
    
    private final List<String> typeList;
    
    private final List<String> sourceList;
    
    private final List<Severity> severityList;

    public EventDataFilter(SystemEntityPath parentPath, List<String> routeList, List<String> typeList, List<String> sourceList, List<Severity> severityList) {
        this.parentPath = parentPath;
        if(routeList != null) {
            this.routeList = new ArrayList<>(routeList);
        } else {
            this.routeList = null;
        }
        if(typeList != null) {
            this.typeList = new ArrayList<>(typeList);
        } else {
            this.typeList = null;
        }
        if(sourceList != null) {
            this.sourceList = new ArrayList<>(sourceList);
        } else {
            this.sourceList = null;
        }
        if(severityList != null) {
            this.severityList = new ArrayList<>(severityList);
        } else {
            this.severityList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<String> getSourceList() {
        if(sourceList == null) {
            return null;
        }
        return new ArrayList<>(sourceList);
    }

    public List<Severity> getSeverityList() {
        if(severityList == null) {
            return null;
        }
        return new ArrayList<>(severityList);
    }
    
    public List<String> getRouteList() {
        if(routeList == null) {
            return null;
        }
        return new ArrayList<>(routeList);
    }
    
    public List<String> getTypeList() {
        if(typeList == null) {
            return null;
        }
        return new ArrayList<>(typeList);
    }
    
    @Override
    public boolean isClear() {
        return this.parentPath == null && this.severityList == null && this.sourceList == null && this.routeList == null && this.typeList == null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.parentPath);
        hash = 19 * hash + Objects.hashCode(this.sourceList);
        hash = 19 * hash + Objects.hashCode(this.severityList);
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
        final EventDataFilter other = (EventDataFilter) obj;
        if (!Objects.equals(this.parentPath, other.parentPath)) {
            return false;
        }
        if (!Objects.equals(this.sourceList, other.sourceList)) {
            return false;
        }
        if (!Objects.equals(this.severityList, other.severityList)) {
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
				+ ", sourceList=" + sourceList + ", severityList=" + severityList + "]";
	}

}
