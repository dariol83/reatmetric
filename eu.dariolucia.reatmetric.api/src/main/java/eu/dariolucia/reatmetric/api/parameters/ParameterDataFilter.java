/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.parameters;

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
public final class ParameterDataFilter extends AbstractDataItemFilter<ParameterData> implements Serializable {
   
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
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return entity.getType() == SystemEntityType.PARAMETER
                && (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<ParameterData> getDataItemType() {
        return ParameterData.class;
    }
}
