/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.parameters;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author dario
 */
public final class ParameterDataFilter extends AbstractDataItemFilter implements Serializable {
   
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private final SystemEntityPath parentPath;

	private final List<SystemEntityPath> parameterPathList;

    private final List<String> routeList;

    private final List<Validity> validityList;

    private final List<AlarmState> alarmStateList;

    public ParameterDataFilter(SystemEntityPath parentPath, List<SystemEntityPath> pathList, List<String> routeList, List<Validity> validityList, List<AlarmState> alarmStateList) {
        this.parentPath = parentPath;
        if(pathList != null) {
            this.parameterPathList = List.copyOf(pathList);
        } else {
            this.parameterPathList = null;
        }
        if(routeList != null) {
            this.routeList = List.copyOf(routeList);
        } else {
            this.routeList = null;
        }
        if(validityList != null) {
            this.validityList = List.copyOf(validityList);
        } else {
            this.validityList = null;
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

    public List<Validity> getValidityList() {
        return validityList;
    }

    public List<String> getRouteList() {
        return routeList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.parameterPathList == null && this.routeList == null && this.alarmStateList == null && this.validityList == null;
    }
}
