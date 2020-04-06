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
 *
 * @author dario
 */
public final class AlarmParameterDataFilter extends AbstractDataItemFilter<AlarmParameterData> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3013429180944834538L;

	private final SystemEntityPath parentPath;

    private final Set<SystemEntityPath> parameterPathList;

    private final Set<AlarmState> alarmStateList;

    private final Set<Integer> externalIdList;

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

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public Set<SystemEntityPath> getParameterPathList() {
        return parameterPathList;
    }

    public Set<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }

    public Set<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.parameterPathList == null && this.alarmStateList == null && this.externalIdList == null;
    }

    @Override
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
    public boolean select(SystemEntity entity) {
        return (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<AlarmParameterData> getDataItemType() {
        return AlarmParameterData.class;
    }
}
