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
import java.util.*;

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

    private final Set<SystemEntityPath> eventPathList;

    private final Set<String> routeList;
    
    private final Set<String> typeList;
    
    private final Set<String> sourceList;
    
    private final Set<Severity> severityList;

    private final Set<Integer> externalIdList;

    public EventDataFilter(SystemEntityPath parentPath, Collection<SystemEntityPath> eventPathList, Collection<String> routeList, Collection<String> typeList, Collection<String> sourceList, Collection<Severity> severityList, Collection<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(eventPathList != null) {
            this.eventPathList = Collections.unmodifiableSet(new LinkedHashSet<>(eventPathList));
        } else {
            this.eventPathList = null;
        }
        if(routeList != null) {
            this.routeList = Collections.unmodifiableSet(new LinkedHashSet<>(routeList));
        } else {
            this.routeList = null;
        }
        if(typeList != null) {
            this.typeList = Collections.unmodifiableSet(new LinkedHashSet<>(typeList));
        } else {
            this.typeList = null;
        }
        if(sourceList != null) {
            this.sourceList = Collections.unmodifiableSet(new LinkedHashSet<>(sourceList));
        } else {
            this.sourceList = null;
        }
        if(severityList != null) {
            this.severityList = Collections.unmodifiableSet(new LinkedHashSet<>(severityList));
        } else {
            this.severityList = null;
        }
        if(externalIdList != null) {
            this.externalIdList = Collections.unmodifiableSet(new LinkedHashSet<>(externalIdList));
        } else {
            this.externalIdList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public Set<SystemEntityPath> getEventPathList() {
        return eventPathList;
    }

    public Set<String> getSourceList() {
        return sourceList;
    }

    public Set<Severity> getSeverityList() {
        return severityList;
    }
    
    public Set<String> getRouteList() {
        return routeList;
    }
    
    public Set<String> getTypeList() {
        return typeList;
    }

    public Set<Integer> getExternalIdList() {
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
