/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.events;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class EventDataFilter extends AbstractDataItemFilter<EventData> implements Serializable {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;
    
    private final List<String> routeList;
    
    private final List<String> typeList;
    
    private final List<String> sourceList;
    
    private final List<Severity> severityList;

    public EventDataFilter(SystemEntityPath parentPath, List<String> routeList, List<String> typeList, List<String> sourceList, List<Severity> severityList) {
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
        if(sourceList != null) {
            this.sourceList = List.copyOf(sourceList);
        } else {
            this.sourceList = null;
        }
        if(severityList != null) {
            this.severityList = List.copyOf(severityList);
        } else {
            this.severityList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<String> getSourceList() {
        return sourceList;
    }

    public List<Severity> getSeverityList() {
        return severityList;
    }
    
    public List<String> getRouteList() {
        return routeList;
    }
    
    public List<String> getTypeList() {
        return typeList;
    }
    
    @Override
    public boolean isClear() {
        return this.parentPath == null && this.severityList == null && this.sourceList == null && this.routeList == null && this.typeList == null;
    }

    @Override
    public boolean test(EventData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(severityList != null && !severityList.contains(item.getSeverity())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getSource())) {
            return false;
        }
        if(routeList != null && !routeList.contains(item.getRoute())) {
            return false;
        }
        if(typeList != null && !typeList.contains(item.getType())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return entity.getType() == SystemEntityType.EVENT
                && (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<EventData> getDataItemType() {
        return EventData.class;
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
