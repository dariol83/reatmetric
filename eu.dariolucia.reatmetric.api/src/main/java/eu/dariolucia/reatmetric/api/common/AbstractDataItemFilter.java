/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.common;

import java.io.Serializable;

/**
 *
 * @author dario
 */
public abstract class AbstractDataItemFilter implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 8489715714812772842L;

	public abstract boolean isClear();
    
}
