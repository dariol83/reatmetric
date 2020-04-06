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


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.Serializable;
import java.util.*;

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
    
    private final Set<String> routeList;
    
    private final Set<String> typeList;
    
    private final Set<String> sourceList;
    
    private final Set<Quality> qualityList;

    public RawDataFilter(boolean withData, String nameContains, Collection<String> routeList, Collection<String> typeList, Collection<String> sourceList, Collection<Quality> qualityList) {
        this.withData = withData;
        this.nameContains = nameContains;
        if(sourceList != null) {
            this.sourceList = Collections.unmodifiableSet(new LinkedHashSet<>(sourceList));
        } else {
            this.sourceList = null;
        }
        if(routeList != null) {
            this.routeList = Collections.unmodifiableSet(new LinkedHashSet<>(routeList));
        } else {
            this.routeList = null;
        }
        if(typeList != null) {
            this.typeList = Collections.unmodifiableSet(new LinkedHashSet<>(typeList));
        } else {
            this.typeList = null;
        }
        if(qualityList != null) {
            this.qualityList = Collections.unmodifiableSet(new LinkedHashSet<>(qualityList));
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

    public Set<String> getRouteList() {
        return routeList;
    }

    public Set<String> getTypeList() {
        return typeList;
    }

    public Set<String> getSourceList() {
        return sourceList;
    }

    public Set<Quality> getQualityList() {
        return qualityList;
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
