/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dario
 */
public final class RawDataFilter extends AbstractDataItemFilter implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -3381513283926701964L;

	private final String nameRegExp;
    
    private final List<String> routeList;
    
    private final List<String> typeList;
    
    private final List<String> sourceList;
    
    private final List<Quality> qualityList;

    public RawDataFilter(String nameRegExp, List<String> routeList, List<String> typeList, List<String> sourceList, List<Quality> qualityList) {
        this.nameRegExp = nameRegExp;
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

    public String getNameRegExp() {
        return nameRegExp;
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
        return this.qualityList == null && this.nameRegExp == null && this.sourceList == null && this.typeList == null && this.routeList == null;
    }

}
