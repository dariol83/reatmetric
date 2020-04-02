/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author dario
 */
public final class ActivityOccurrenceDataFilter extends AbstractDataItemFilter<ActivityOccurrenceData> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final SystemEntityPath parentPath;

    private final List<String> routeList;

    private final List<String> sourceList;

    private final List<String> typeList;

    private final List<ActivityOccurrenceState> stateList;

    private final List<Integer> externalIdList;

    public ActivityOccurrenceDataFilter(SystemEntityPath parentPath, List<String> routeList, List<String> typeList, List<ActivityOccurrenceState> stateList, List<String> sourceList, List<Integer> externalIdList) {
        this.parentPath = parentPath;
        if(routeList != null) {
            this.routeList = List.copyOf(routeList);
        } else {
            this.routeList = null;
        }
        if(sourceList != null) {
            this.sourceList = List.copyOf(sourceList);
        } else {
            this.sourceList = null;
        }
        if(typeList != null) {
            this.typeList = List.copyOf(typeList);
        } else {
            this.typeList = null;
        }
        if(stateList != null) {
            this.stateList = List.copyOf(stateList);
        } else {
            this.stateList = null;
        }
        if(externalIdList != null) {
            this.externalIdList = List.copyOf(externalIdList);
        } else {
            this.externalIdList = null;
        }
    }

    public SystemEntityPath getParentPath() {
        return parentPath;
    }

    public List<ActivityOccurrenceState> getStateList() {
        return stateList;
    }

    public List<String> getRouteList() {
        return routeList;
    }
    
    public List<String> getTypeList() {
        return typeList;
    }

    public List<String> getSourceList() {
        return sourceList;
    }

    public List<Integer> getExternalIdList() {
        return externalIdList;
    }

    @Override
    public boolean isClear() {
        return this.parentPath == null && this.stateList == null && this.routeList == null && this.typeList == null && this.sourceList == null && this.externalIdList == null;
    }

    @Override
    public boolean test(ActivityOccurrenceData item) {
        if(parentPath != null && !parentPath.isParentOf(item.getPath())) {
            return false;
        }
        if(stateList != null && !stateList.contains(item.getCurrentState())) {
            return false;
        }
        if(routeList != null && !routeList.contains(item.getRoute())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getRoute())) {
            return false;
        }
        if(typeList != null && !typeList.contains(item.getType())) {
            return false;
        }
        if(externalIdList != null && !externalIdList.contains(item.getExternalId())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return entity.getType() == SystemEntityType.ACTIVITY && (parentPath == null || parentPath.isParentOf(entity.getPath()) || entity.getPath().isParentOf(parentPath));
    }

    @Override
    public Class<ActivityOccurrenceData> getDataItemType() {
        return ActivityOccurrenceData.class;
    }

}
