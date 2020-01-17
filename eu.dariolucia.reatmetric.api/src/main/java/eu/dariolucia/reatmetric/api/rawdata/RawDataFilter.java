/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dario
 */
public final class RawDataFilter extends AbstractDataItemFilter<RawData> implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -3381513283926701964L;

	private final boolean withData;

	private final String nameContains;
    
    private final List<String> routeList;
    
    private final List<String> typeList;
    
    private final List<String> sourceList;
    
    private final List<Quality> qualityList;

    public RawDataFilter(boolean withData, String nameContains, List<String> routeList, List<String> typeList, List<String> sourceList, List<Quality> qualityList) {
        this.withData = withData;
        this.nameContains = nameContains;
        if(sourceList != null) {
            this.sourceList = new ArrayList<>(sourceList);
        } else {
            this.sourceList = null;
        }
        if(routeList != null) {
            this.routeList = new ArrayList<>(routeList);
        } else {
            this.routeList = null;
        }
        if(typeList != null) {
            this.typeList = new ArrayList<>(typeList);
        } else {
            this.typeList = null;
        }
        if(qualityList != null) {
            this.qualityList = new ArrayList<>(qualityList);
        } else {
            this.qualityList = null;
        }
    }

    public boolean isWithData() {
        return withData;
    }

    public String getNameContains() {
        return nameContains;
    }

    public List<String> getSourceList() {
        if(sourceList == null) {
            return null;
        }
        return new ArrayList<>(sourceList);
    }

    public List<Quality> getQualityList() {
        if(qualityList == null) {
            return null;
        }
        return new ArrayList<>(qualityList);
    }
    
    public List<String> getTypeList() {
        if(typeList == null) {
            return null;
        }
        return new ArrayList<>(typeList);
    }
    
    public List<String> getRouteList() {
        if(routeList == null) {
            return null;
        }
        return new ArrayList<>(routeList);
    }
    
    @Override
    public boolean isClear() {
        return this.qualityList == null && this.nameContains == null && this.sourceList == null && this.typeList == null && this.routeList == null;
    }

    @Override
    public boolean test(RawData item) {
        if(nameContains != null && !item.getName().contains(nameContains)) {
            return false;
        }
        if(qualityList != null && !qualityList.contains(item.getQuality())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getSource())) {
            return false;
        }
        if(routeList != null && !routeList.contains(item.getRoute())) {
            return false;
        }
        if(typeList != null && !typeList.contains(item.getType())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return true;
    }

    @Override
    public Class<RawData> getDataItemType() {
        return RawData.class;
    }

}
