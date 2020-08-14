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


package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class allows to filter/subscribe/retrieve scheduled activities.
 *
 * Objects of this class are immutable.
 */
public final class ScheduledActivityDataFilter extends AbstractDataItemFilter<ScheduledActivityData> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;

    private final Set<SystemEntityPath> activityPathList;

    private final Set<String> resourceList;

    private final Set<String> sourceList;

    private final Set<SchedulingState> schedulingStateList;

    private final Set<Long> externalIdList;

    /**
     * The constructor of the event filter.
     *
     * @param parentPath the parent path to select. It can be null: if so, all paths are selected.
     * @param activityPathList the activity paths to exactly select. It can be null: if so, all activity paths are selected.
     * @param resourceList the resources to select. It can be null: if so, all resources are selected.
     * @param sourceList the sources to select. It can be null: if so, all sources are selected.
     * @param schedulingStateList the scheduling states to select. It can be null: if so, all states are selected.
     * @param externalIdList the external IDs to select. It can be null: if so, all external IDs are selected.
     */
    public ScheduledActivityDataFilter(SystemEntityPath parentPath, Collection<SystemEntityPath> activityPathList, Collection<String> resourceList, Collection<String> sourceList, Collection<SchedulingState> schedulingStateList, Collection<Long> externalIdList) {
        this.parentPath = parentPath;
        if(activityPathList != null) {
            this.activityPathList = Collections.unmodifiableSet(new LinkedHashSet<>(activityPathList));
        } else {
            this.activityPathList = null;
        }
        if(resourceList != null) {
            this.resourceList = Collections.unmodifiableSet(new LinkedHashSet<>(resourceList));
        } else {
            this.resourceList = null;
        }
        if(sourceList != null) {
            this.sourceList = Collections.unmodifiableSet(new LinkedHashSet<>(sourceList));
        } else {
            this.sourceList = null;
        }
        if(schedulingStateList != null) {
            this.schedulingStateList = Collections.unmodifiableSet(new LinkedHashSet<>(schedulingStateList));
        } else {
            this.schedulingStateList = null;
        }
        if(externalIdList != null) {
            this.externalIdList = Collections.unmodifiableSet(new LinkedHashSet<>(externalIdList));
        } else {
            this.externalIdList = null;
        }
    }

    /**
     * The parent path to select: an activity is selected if its path is a descendant of the parent path
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
     * The set of sources to select: an activity is selected if its source is one of those
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
     * The set of states to select: a scheduled activity is selected if its state is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified state
     */
    public Set<SchedulingState> getSchedulingStateList() {
        return schedulingStateList;
    }

    /**
     * The set of resources to select: an activity is selected if at least one of its resources is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified resources
     */
    public Set<String> getResourceList() {
        return resourceList;
    }

    /**
     * The set of external IDs to select: a scheduled item is selected if its external ID is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified external IDs
     */
    public Set<Long> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.activityPathList == null &&this.schedulingStateList == null && this.sourceList == null && this.resourceList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(ScheduledActivityData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getRequest().getPath())) {
            return false;
        }
        if(activityPathList != null && !activityPathList.contains(item.getRequest().getPath())) {
            return false;
        }
        if(schedulingStateList != null && !schedulingStateList.contains(item.getState())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getSource())) {
            return false;
        }
        if(resourceList != null && resourceList.stream().noneMatch(item.getResources()::contains)) {
            return false;
        }
        if(externalIdList != null && !externalIdList.contains(item.getExternalId())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return true;
    }

    @Override
    public Class<ScheduledActivityData> getDataItemType() {
        return ScheduledActivityData.class;
    }

}
