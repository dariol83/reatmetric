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

package eu.dariolucia.reatmetric.api.model;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Filter for system entity data items.
 * This filter allows selecting system entities based on various criteria such as parent path, type, status, and alarm state.
 */
public class SystemEntityFilter extends AbstractDataItemFilter<SystemEntity> implements Serializable {

    /**
     * The serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The parent path for filtering. If set, only entities under this path will be selected.
     */
    private final SystemEntityPath parentPath;

    /**
     * The list of system entity types to filter by.
     */
    private final List<SystemEntityType> typeList;

    /**
     * The list of status values to filter by.
     */
    private final List<Status> statusList;

    /**
     * The list of alarm states to filter by.
     */
    private final List<AlarmState> alarmStateList;

    /**
     * Constructor for a system entity filter.
     *
     * @param parentPath the parent path for filtering, can be null
     * @param typeList the list of system entity types to filter by, can be null
     * @param statusList the list of status values to filter by, can be null
     * @param alarmStateList the list of alarm states to filter by, can be null
     */
    public SystemEntityFilter(SystemEntityPath parentPath, List<SystemEntityType> typeList, List<Status> statusList, List<AlarmState> alarmStateList) {
        this.parentPath = parentPath;
        if(typeList != null) {
            this.typeList = List.copyOf(typeList);
        } else {
            this.typeList = null;
        }
        if(statusList != null) {
            this.statusList = List.copyOf(statusList);
        } else {
            this.statusList = null;
        }
        if(alarmStateList != null) {
            this.alarmStateList = List.copyOf(alarmStateList);
        } else {
            this.alarmStateList = null;
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
     * Returns the list of status values to filter by.
     *
     * @return the list of status values (can be null)
     */
    public List<Status> getStatusList() {
        return statusList;
    }

    /**
     * Returns the list of alarm states to filter by.
     *
     * @return the list of alarm states (can be null)
     */
    public List<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }

    /**
     * Returns the list of system entity types to filter by.
     *
     * @return the list of system entity types (can be null)
     */
    public List<SystemEntityType> getTypeList() {
        return typeList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.alarmStateList == null && this.statusList == null && this.typeList == null;
    }

    @Override
    public boolean test(SystemEntity item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(alarmStateList != null && !alarmStateList.contains(item.getAlarmState())) {
            return false;
        }
        if(statusList != null && !statusList.contains(item.getStatus())) {
            return false;
        }
        if(typeList != null && !typeList.contains(item.getType())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<SystemEntity> getDataItemType() {
        return SystemEntity.class;
    }

}
