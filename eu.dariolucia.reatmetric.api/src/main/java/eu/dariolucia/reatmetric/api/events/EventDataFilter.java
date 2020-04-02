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

    private final List<SystemEntityPath> eventPathList;

    private final List<String> routeList;
    
    private final List<String> typeList;
    
    private final List<String> sourceList;
    
    private final List<Severity> severityList;

    private final List<Integer> externalIdList;

    public EventDataFilter(SystemEntityPath parentPath, List<SystemEntityPath> eventPathList, List<String> routeList, List<String> typeList, List<String> sourceList, List<Severity> severityList, List<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(eventPathList != null) {
            this.eventPathList = List.copyOf(eventPathList);
        } else {
            this.eventPathList = null;
        }
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
        if(externalIdList != null) {
            this.externalIdList = List.copyOf(externalIdList);
        } else {
            this.externalIdList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<SystemEntityPath> getEventPathList() {
        return eventPathList;
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

    public List<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.eventPathList == null &&this.severityList == null && this.sourceList == null && this.routeList == null && this.typeList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(EventData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(eventPathList != null && !eventPathList.contains(item.getPath())) {
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
        if(externalIdList != null && !externalIdList.contains(item.getExternalId())) {
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

}
