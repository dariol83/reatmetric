/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.parentPath);
        hash = 19 * hash + Objects.hashCode(this.statusList);
        hash = 19 * hash + Objects.hashCode(this.alarmStateList);
        hash = 19 * hash + Objects.hashCode(this.typeList);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SystemEntityFilter other = (SystemEntityFilter) obj;
        if (!Objects.equals(this.parentPath, other.parentPath)) {
            return false;
        }
        if (!Objects.equals(this.statusList, other.statusList)) {
            return false;
        }
        if (!Objects.equals(this.alarmStateList, other.alarmStateList)) {
            return false;
        }
        if (!Objects.equals(this.typeList, other.typeList)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SystemEntityFilter [parentPath=" + parentPath + ", typeList=" + typeList
                + ", statusList=" + statusList + ", alarmStateList=" + alarmStateList + "]";
    }

}
