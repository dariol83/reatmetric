/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * This class allows to filter/subscribe/retrieve event occurrences.
 *
 * Objects of this class are immutable.
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

    /**
     * The constructor of the event filter.
     *
     * @param parentPath the parent path to select. It can be null: if so, all paths are selected.
     * @param eventPathList the event paths to exactly select. It can be null: if so, all paths are selected.
     * @param routeList the routes to select. It can be null: if so, all routes are selected.
     * @param typeList the types to select. It can be null: if so, all types are selected.
     * @param sourceList the sources to select. It can be null: if so, all sources are selected.
     * @param severityList the severities to select. It can be null: if so, all severities are selected.
     * @param externalIdList the external IDs to select. It can be null: if so, all external IDs are selected.
     */
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

    /**
     * The parent path to select: an event is selected if its path is a descendant of the parent path
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified parent path
     */
    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    /**
     * The set of exact paths to select: an event is selected if its (event) path is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified paths
     */
    public Set<SystemEntityPath> getEventPathList() {
        return eventPathList;
    }

    /**
     * The set of sources to select: an event is selected if its source is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified sources
     */
    public Set<String> getSourceList() {
        return sourceList;
    }

    /**
     * The set of severities to select: an event is selected if its severity is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified severity
     */
    public Set<Severity> getSeverityList() {
        return severityList;
    }

    /**
     * The set of routes to select: an event is selected if its route is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified routes
     */
    public Set<String> getRouteList() {
        return routeList;
    }

    /**
     * The set of types to select: an event is selected if its type is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified types
     */
    public Set<String> getTypeList() {
        return typeList;
    }

    /**
     * The set of external IDs to select: an event is selected if its external ID is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified external IDs
     */
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
        return (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<EventData> getDataItemType() {
        return EventData.class;
    }

}
