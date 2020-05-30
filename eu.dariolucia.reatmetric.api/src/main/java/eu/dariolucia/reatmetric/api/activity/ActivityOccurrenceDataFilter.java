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


package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class allows to filter/subscribe/retrieve activity occurrences.
 *
 * Objects of this class are immutable.
 */
public final class ActivityOccurrenceDataFilter extends AbstractDataItemFilter<ActivityOccurrenceData> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;

    private final Set<SystemEntityPath> activityPathList;

    private final Set<String> routeList;

    private final Set<String> sourceList;

    private final Set<String> typeList;

    private final Set<ActivityOccurrenceState> stateList;

    private final Set<Integer> externalIdList;

    /**
     * The constructor of the activity occurrence filter.
     *
     * @param parentPath the parent path to select. It can be null: if so, all paths are selected.
     * @param activityPathList the activity paths to exactly select. It can be null: if so, all paths are selected.
     * @param routeList the list of routes to select. It can be null: if so, all routes are selected.
     * @param typeList the list of types to select. It can be null: if so, all types are selected.
     * @param stateList the list of activity occurrence states to select. It can be null: if so, all states are selected.
     * @param sourceList the list of sources to select. It can be null: if so, all sources are selected.
     * @param externalIdList the list of activity IDs to select. It can be null: if so, all activities are selected.
     */
    public ActivityOccurrenceDataFilter(SystemEntityPath parentPath, List<SystemEntityPath> activityPathList, List<String> routeList, List<String> typeList, List<ActivityOccurrenceState> stateList, List<String> sourceList, List<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(activityPathList != null) {
            this.activityPathList = Collections.unmodifiableSet(new LinkedHashSet<>(activityPathList));
        } else {
            this.activityPathList = null;
        }
        if(routeList != null) {
            this.routeList = Collections.unmodifiableSet(new LinkedHashSet<>(routeList));
        } else {
            this.routeList = null;
        }
        if(sourceList != null) {
            this.sourceList = Collections.unmodifiableSet(new LinkedHashSet<>(sourceList));
        } else {
            this.sourceList = null;
        }
        if(typeList != null) {
            this.typeList = Collections.unmodifiableSet(new LinkedHashSet<>(typeList));
        } else {
            this.typeList = null;
        }
        if(stateList != null) {
            this.stateList = Collections.unmodifiableSet(new LinkedHashSet<>(stateList));
        } else {
            this.stateList = null;
        }
        if(externalIdList != null) {
            this.externalIdList = Collections.unmodifiableSet(new LinkedHashSet<>(externalIdList));
        } else {
            this.externalIdList = null;
        }
    }

    /**
     * The parent path to select: an activity occurrence is selected if its path is a descendant of the parent path
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
     * The set of exact paths to select: an activity is selected if its (activity) path is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified paths
     */
    public Set<SystemEntityPath> getActivityPathList() {
        return activityPathList;
    }

    /**
     * The set of activity occurrence states to select: an activity occurrence is selected if its state is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified states
     */
    public Set<ActivityOccurrenceState> getStateList() {
        return stateList;
    }

    /**
     * The set of routes to select: an activity occurrence is selected if its route is one of those
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
     * The set of types to select: an activity occurrence is selected if its type is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified routes
     */
    public Set<String> getTypeList() {
        return typeList;
    }

    /**
     * The set of sources to select: an activity occurrence is selected if its source is one of those
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
     * The set of activity IDs to select: an activity occurrence is selected if its activity ID is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified activity IDs
     */
    public Set<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.activityPathList == null && this.stateList == null && this.routeList == null && this.typeList == null && this.sourceList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(ActivityOccurrenceData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(activityPathList != null && !activityPathList.contains(item.getPath())) {
            return false;
        }
        if(stateList != null && !stateList.contains(item.getCurrentState())) {
            return false;
        }
        if(routeList != null && !routeList.contains(item.getRoute())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getRoute())) {
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
    public Class<ActivityOccurrenceData> getDataItemType() {
        return ActivityOccurrenceData.class;
    }

}
