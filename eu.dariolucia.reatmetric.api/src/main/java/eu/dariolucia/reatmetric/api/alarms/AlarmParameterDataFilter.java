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
package eu.dariolucia.reatmetric.api.alarms;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.*;

/**
 * This class allows to filter/subscribe/retrieve parameter alarms.
 *
 * Objects of this class are immutable.
 */
public final class AlarmParameterDataFilter extends AbstractDataItemFilter<AlarmParameterData> implements Serializable {

	/**
	 * Serial version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	/** The parent path to select. */
	private final SystemEntityPath parentPath;

	/** The set of exact parameter paths to select. */
	private final Set<SystemEntityPath> parameterPathList;

	/** The set of alarm states to select. */
	private final Set<AlarmState> alarmStateList;

	/** The set of parameter IDs to select. */
	private final Set<Integer> externalIdList;

    /**
     * The constructor of the alarm parameter filter.
     *
     * @param parentPath the parent path to select. It can be null: if so, all paths are selected.
     * @param pathList the paths to exactly select. It can be null: if so, all paths are selected.
     * @param alarmStateList the current alarm states to select. It can be null: if so, all alarm states are selected.
     * @param externalIdList the activity IDs to select. It can be null: if so, all activities are selected.
     */
    public AlarmParameterDataFilter(SystemEntityPath parentPath, Collection<SystemEntityPath> pathList, Collection<AlarmState> alarmStateList, Collection<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(pathList != null) {
            this.parameterPathList = Collections.unmodifiableSet(new LinkedHashSet<>(pathList));
        } else {
            this.parameterPathList = null;
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
     * The parent path to select: an alarm parameter is selected if its path is a descendant of the parent path
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
     * The set of exact paths to select: an alarm parameter is selected if its (parameter) path is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified paths
     */
    public Set<SystemEntityPath> getParameterPathList() {
        return parameterPathList;
    }

    /**
     * The set of (current) alarm states to select: an alarm parameter is selected if its (current) alarm state is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified alarm states
     */
    public Set<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }

    /**
     * The set of parameter IDs to select: an alarm parameter is selected if its parameter ID is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified parameter IDs
     */
    public Set<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    /**
     * Checks if this filter is clear (no filtering criteria).
     *
     * @return true if all filter criteria are null, false otherwise
     */
    public boolean isClear() {
        return this.parentPath == null && this.parameterPathList == null && this.alarmStateList == null && this.externalIdList == null;
    }

    @Override
    /**
     * Tests if the specified alarm parameter data matches this filter.
     *
     * @param item the alarm parameter data to test
     * @return true if the item matches the filter, false otherwise
     */
    public boolean test(AlarmParameterData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(parameterPathList != null && !parameterPathList.contains(item.getPath())) {
            return false;
        }
        if(alarmStateList != null && !alarmStateList.contains(item.getCurrentAlarmState())) {
            return false;
        }
        if(externalIdList != null && !externalIdList.contains(item.getExternalId())) {
            return false;
        }
        return true;
    }

    @Override
    /**
     * Checks if the specified system entity should be selected.
     *
     * @param entity the system entity to check
     * @return true if the entity should be selected, false otherwise
     */
    public boolean select(SystemEntity entity) {
        return (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    /**
     * Returns the data item type this filter applies to.
     *
     * @return the AlarmParameterData class
     */
    public Class<AlarmParameterData> getDataItemType() {
        return AlarmParameterData.class;
    }
}
