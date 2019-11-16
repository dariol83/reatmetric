/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.alarms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 *
 * @author dario
 */
public final class AlarmParameterDataFilter extends AbstractDataItemFilter implements Serializable {

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
}
