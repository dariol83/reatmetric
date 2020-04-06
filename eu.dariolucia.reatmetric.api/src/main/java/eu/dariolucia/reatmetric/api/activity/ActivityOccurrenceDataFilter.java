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
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author dario
 */
public final class ActivityOccurrenceDataFilter extends AbstractDataItemFilter<ActivityOccurrenceData> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;

    private final Set<String> routeList;

    private final Set<String> sourceList;

    private final Set<String> typeList;

    private final Set<ActivityOccurrenceState> stateList;

    private final Set<Integer> externalIdList;

    public ActivityOccurrenceDataFilter(SystemEntityPath parentPath, List<String> routeList, List<String> typeList, List<ActivityOccurrenceState> stateList, List<String> sourceList, List<Integer> externalIdList) {
        this.parentPath = parentPath;
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

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public Set<ActivityOccurrenceState> getStateList() {
        return stateList;
    }

    public Set<String> getRouteList() {
        return routeList;
    }
    
    public Set<String> getTypeList() {
        return typeList;
    }

    public Set<String> getSourceList() {
        return sourceList;
    }

    public Set<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.stateList == null && this.routeList == null && this.typeList == null && this.sourceList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(ActivityOccurrenceData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
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
