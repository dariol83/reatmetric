/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
