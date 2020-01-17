/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.common;

import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 *
 * @author dario
 */
public abstract class AbstractDataItemFilter<T extends AbstractDataItem> implements Serializable, Predicate<T> {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 8489715714812772842L;

	public abstract boolean isClear();

	public abstract boolean test(T item);

	public abstract boolean select(SystemEntity entity);

	public abstract Class<T> getDataItemType();
}
