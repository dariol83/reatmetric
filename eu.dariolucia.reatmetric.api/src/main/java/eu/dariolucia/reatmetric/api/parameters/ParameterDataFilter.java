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


package eu.dariolucia.reatmetric.api.parameters;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.*;

/**
 * Filter for parameter data items.
 * This filter allows selecting parameter data based on various criteria such as path, route, validity, alarm state, and external ID.
 *
 * @author dario
 */
public final class ParameterDataFilter extends AbstractDataItemFilter<ParameterData> implements Serializable {
   
    /**
     * The serial version UID for serialization compatibility.
     */
	private static final long serialVersionUID = 1L;

    /**
     * The parent path for filtering. If set, only parameters under this path will be selected.
     */
    private final SystemEntityPath parentPath;

	/**
     * The set of specific parameter paths to filter by.
     */
	private final Set<SystemEntityPath> parameterPathList;

    /**
     * The set of routes to filter by.
     */
    private final Set<String> routeList;

    /**
     * The set of validity states to filter by.
     */
    private final Set<Validity> validityList;

    /**
     * The set of alarm states to filter by.
     */
    private final Set<AlarmState> alarmStateList;

    /**
     * The set of external IDs to filter by.
     */
    private final Set<Integer> externalIdList;

    /**
     * Constructor for a parameter data filter.
     *
     * @param parentPath the parent path for filtering, can be null
     * @param pathList the collection of specific parameter paths to filter by, can be null
     * @param routeList the collection of routes to filter by, can be null
     * @param validityList the collection of validity states to filter by, can be null
     * @param alarmStateList the collection of alarm states to filter by, can be null
     * @param externalIdList the collection of external IDs to filter by, can be null
     */
    public ParameterDataFilter(SystemEntityPath parentPath, Collection<SystemEntityPath> pathList, Collection<String> routeList, Collection<Validity> validityList, Collection<AlarmState> alarmStateList, Collection<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(pathList != null) {
            this.parameterPathList = Collections.unmodifiableSet(new LinkedHashSet<>(pathList));
        } else {
            this.parameterPathList = null;
        }
        if(routeList != null) {
            this.routeList = Collections.unmodifiableSet(new LinkedHashSet<>(routeList));
        } else {
            this.routeList = null;
        }
        if(validityList != null) {
            this.validityList = Collections.unmodifiableSet(new LinkedHashSet<>(validityList));
        } else {
            this.validityList = null;
        }
        if(alarmStateList != null) {
            this.alarmStateList = Collections.unmodifiableSet(new LinkedHashSet<>(alarmStateList));
        } else {
            this.alarmStateList = null;
        }
        if(externalIdList != null) {
            this.externalIdList = Collections.unmodifiableSet(new LinkedHashSet<>(externalIdList));
        } else {
            this.externalIdList = null;
        }
    }

    /**
     * Returns the parent path for filtering.
     *
     * @return the parent path (can be null)
     */
    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    /**
     * Returns the set of specific parameter paths to filter by.
     *
     * @return the set of parameter paths (can be null)
     */
    public Set<SystemEntityPath> getParameterPathList() {
        return parameterPathList;
    }

    /**
     * Returns the set of alarm states to filter by.
     *
     * @return the set of alarm states (can be null)
     */
    public Set<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }

    /**
     * Returns the set of validity states to filter by.
     *
     * @return the set of validity states (can be null)
     */
    public Set<Validity> getValidityList() {
        return validityList;
    }

    /**
     * Returns the set of routes to filter by.
     *
     * @return the set of routes (can be null)
     */
    public Set<String> getRouteList() {
        return routeList;
    }

    /**
     * Returns the set of external IDs to filter by.
     *
     * @return the set of external IDs (can be null)
     */
    public Set<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.parameterPathList == null && this.routeList == null && this.alarmStateList == null && this.validityList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(ParameterData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(parameterPathList != null && !parameterPathList.contains(item.getPath())) {
            return false;
        }
        if(alarmStateList != null && !alarmStateList.contains(item.getAlarmState())) {
            return false;
        }
        if(routeList != null && !routeList.contains(item.getRoute())) {
            return false;
        }
        if(validityList != null && !validityList.contains(item.getValidity())) {
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
    public Class<ParameterData> getDataItemType() {
        return ParameterData.class;
    }
}
