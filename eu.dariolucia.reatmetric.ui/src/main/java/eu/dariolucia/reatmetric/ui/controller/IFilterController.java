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


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;

import java.util.Set;

/**
 *
 * @author dario
 */
public interface IFilterController<T extends AbstractDataItemFilter> {
    
    void setActionAfterSelection(Runnable r);
    
    void setSelectedFilter(T filter);
    
    T getSelectedFilter();
    
    static String toStringList(Set<?> sourceList) {
        if(sourceList == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sourceList.forEach((o) -> {
            sb.append(o.toString()).append(",");
        });
        if(sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
