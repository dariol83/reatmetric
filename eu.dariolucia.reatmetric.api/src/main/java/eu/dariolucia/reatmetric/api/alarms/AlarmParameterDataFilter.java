/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.alarms;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.List;

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

    private final List<SystemEntityPath> parameterPathList;

    private final List<AlarmState> alarmStateList;

    public AlarmParameterDataFilter(SystemEntityPath parentPath, List<SystemEntityPath> pathList, List<AlarmState> alarmStateList) {
        this.parentPath = parentPath;
        if(pathList != null) {
            this.parameterPathList = List.copyOf(pathList);
        } else {
            this.parameterPathList = null;
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

    public List<SystemEntityPath> getParameterPathList() {
        return parameterPathList;
    }

    public List<AlarmState> getAlarmStateList() {
        return alarmStateList;
    }
    
    @Override
    public boolean isClear() {
        return this.parentPath == null && this.parameterPathList == null && this.alarmStateList == null;
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
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return entity.getType() == SystemEntityType.PARAMETER
                && (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<AlarmParameterData> getDataItemType() {
        return AlarmParameterData.class;
    }
}
