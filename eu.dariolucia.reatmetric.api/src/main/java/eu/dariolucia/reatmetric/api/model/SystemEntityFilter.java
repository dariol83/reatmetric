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

public class SystemEntityFilter extends AbstractDataItemFilter<SystemEntity> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final SystemEntityPath parentPath;

    private final List<SystemEntityType> typeList;

    private final List<Status> statusList;

    private final List<AlarmState> alarmStateList;

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

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<Status> getStatusList() {
        return statusList;
    }

    public List<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }

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
