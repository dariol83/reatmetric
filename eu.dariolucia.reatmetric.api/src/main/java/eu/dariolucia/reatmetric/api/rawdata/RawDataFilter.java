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
 * This class allows to filter, subscribe, and retrieve raw data items.
 * <p>
 * A filter can select raw data items by name substring, route, type, source, quality, and live/repatriated state. The
 * {@code withData} flag does not take part in the item selection: it is a retrieval hint used by archive implementations
 * to decide whether binary contents shall be loaded together with the metadata. A filter with a null selection criterion
 * accepts any value for that criterion.
 * <p>
 * Objects of this class are immutable.
 *
 * @author dario
 */
public final class RawDataFilter extends AbstractDataItemFilter<RawData> implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -3381513283926701964L;

	private final boolean withData;

    private final Boolean live;

	private final String nameContains;
    
    private final Set<String> routeList;
    
    private final Set<String> typeList;
    
    private final Set<String> sourceList;
    
    private final Set<Quality> qualityList;

    /**
     * Constructor of the raw data filter. The constructed filter accepts both live and repatriated raw data items.
     *
     * @param withData true if retrieved raw data shall include binary contents, false for metadata only
     * @param nameContains a string that must be contained in the raw data name. It can be null: if so, all names are selected.
     * @param routeList the routes to select. It can be null: if so, all routes are selected.
     * @param typeList the types to select. It can be null: if so, all types are selected.
     * @param sourceList the sources to select. It can be null: if so, all sources are selected.
     * @param qualityList the qualities to select. It can be null: if so, all qualities are selected.
     */
    public RawDataFilter(boolean withData, String nameContains, Collection<String> routeList, Collection<String> typeList, Collection<String> sourceList, Collection<Quality> qualityList) {
        this(withData, nameContains, routeList, typeList, sourceList, qualityList, null);
    }

    /**
     * Constructor of the raw data filter.
     *
     * @param withData true if retrieved raw data shall include binary contents, false for metadata only
     * @param nameContains a string that must be contained in the raw data name. It can be null: if so, all names are selected.
     * @param routeList the routes to select. It can be null: if so, all routes are selected.
     * @param typeList the types to select. It can be null: if so, all types are selected.
     * @param sourceList the sources to select. It can be null: if so, all sources are selected.
     * @param qualityList the qualities to select. It can be null: if so, all qualities are selected.
     * @param live true to select only live data, false to select only repatriated data. It can be null: if so, both live
     *             and repatriated data are selected.
     */
    public RawDataFilter(boolean withData, String nameContains, Collection<String> routeList, Collection<String> typeList, Collection<String> sourceList, Collection<Quality> qualityList, Boolean live) {
        this.withData = withData;
        this.live = live;
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

    /**
     * Return whether retrieved raw data items shall include their binary contents.
     * This flag is a retrieval hint and does not affect {@link #test(RawData)}.
     *
     * @return true if contents shall be retrieved, false for metadata only
     */
    public boolean isWithData() {
        return withData;
    }

    /**
     * Return the raw data name substring to select.
     * It can be null if all names are selected.
     *
     * @return the name substring, or null if not specified
     */
    public String getNameContains() {
        return nameContains;
    }

    /**
     * Return the set of routes to select.
     * It can be null if all routes are selected.
     *
     * @return the specified routes, or null if not specified
     */
    public Set<String> getRouteList() {
        return routeList;
    }

    /**
     * Return the set of types to select.
     * It can be null if all types are selected.
     *
     * @return the specified types, or null if not specified
     */
    public Set<String> getTypeList() {
        return typeList;
    }

    /**
     * Return the set of sources to select.
     * It can be null if all sources are selected.
     *
     * @return the specified sources, or null if not specified
     */
    public Set<String> getSourceList() {
        return sourceList;
    }

    /**
     * Return the set of qualities to select.
     * It can be null if all qualities are selected.
     *
     * @return the specified qualities, or null if not specified
     */
    public Set<Quality> getQualityList() {
        return qualityList;
    }

    /**
     * Return the live/repatriated selection criterion.
     * It can be null if both live and repatriated raw data shall be selected.
     *
     * @return true for live data only, false for repatriated data only, or null for both
     */
    public Boolean getLive() {
        return live;
    }

    @Override
    public boolean isClear() {
        return this.qualityList == null && this.nameContains == null && this.sourceList == null && this.typeList == null && this.routeList == null && this.live == null;
    }

    @Override
    public boolean test(RawData item) {
        if(live != null && live != item.isLive()) {
            return false;
        }
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
