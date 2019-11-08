/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import java.util.List;

/**
 *
 * @author dario
 */
public interface IFilterController<T extends AbstractDataItemFilter> {
    
    public void setActionAfterSelection(Runnable r);
    
    public void setSelectedFilter(T filter);
    
    public T getSelectedFilter();
    
    public static String toStringList(List<?> sourceList) {
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
