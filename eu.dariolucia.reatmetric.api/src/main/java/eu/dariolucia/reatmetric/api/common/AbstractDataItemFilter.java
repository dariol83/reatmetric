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
package eu.dariolucia.reatmetric.api.common;

import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * Abstract filter definition for {@link AbstractDataItem} derived filters.
 *
 * The filter implements the {@link Predicate} interface to allow its easy usage in stream operations.
 */
public abstract class AbstractDataItemFilter<T extends AbstractDataItem> implements Serializable, Predicate<T> {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * If the filter specifies no selection, this method returns true.
	 *
	 * @return true if no filter criterium is specified, otherwise false
	 */
	public abstract boolean isClear();

	/**
	 * Test if the data item satisfies this filter.
	 *
	 * @param item the data item to test
	 * @return true if the data item is selected by the filter, otherwise false
	 */
	public abstract boolean test(T item);

	/**
	 * Test if the provided system entity belongs to the path selection specified by this filter. This method is implemented
	 * only by filters on data items managed by the processing model as {@link SystemEntity}.
	 *
	 * @param entity the system entity to check
	 * @return true if no parent path is specified, or if the system entity path is contained in/contains the specified parent path
	 */
	public abstract boolean select(SystemEntity entity);

	/**
	 * The Java type that this filter can select, i.e. the exact class derived from {@link AbstractDataItem}.
	 *
	 * @return ActivityOccurrenceData {@link Class} object
	 */
	public abstract Class<T> getDataItemType();
}
