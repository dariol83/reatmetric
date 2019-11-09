/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.parameters;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dario
 */
public final class ParameterDataFilter extends AbstractDataItemFilter implements Serializable {
   
    /**
	 * 
	 */
	private static final long serialVersionUID = -4775979580773316995L;
	
	private final List<SystemEntityPath> parameterPathList;
    
    public ParameterDataFilter(List<SystemEntityPath> pathList) {
        if(pathList != null) {
            this.parameterPathList = new ArrayList<>(pathList);
        } else {
            this.parameterPathList = null;
        }
    }

    public List<SystemEntityPath> getParameterPathList() {
        if(parameterPathList == null) {
            return null;
        }
        return new ArrayList<>(parameterPathList);
    }
    
    @Override
    public boolean isClear() {
        return this.parameterPathList == null;
    }
}
