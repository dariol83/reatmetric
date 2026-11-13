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

import java.io.Serializable;

/**
 * A unique item is an object that can be identified by a {@link IUniqueId}.
 *
 * This class is immutable except for the data property, which shall be considered a transient volatile field,
 * provided as a convenience placeholder to store volatile user data. The hashCode and equals methods do not take this
 * property into account.
 */
public abstract class UniqueItem implements Serializable {
    
    /**
     * The serial version UID for serialization compatibility.
     */
	private static final long serialVersionUID = -4265552582009003032L;

	/**
	 * The unique identifier for this item.
	 */
	protected final IUniqueId internalId;

    /**
     * Transient volatile field provided as a convenience placeholder to store volatile user data.
     * This field is not considered in hashCode and equals methods.
     */
    protected transient volatile Object data;
    
    /**
     * Constructor for a unique item.
     *
     * @param internalId the unique identifier for this item, cannot be null
     * @param data the volatile user data (can be null)
     */
    public UniqueItem(IUniqueId internalId, Object data) {
        this.internalId = internalId;
        this.data = data;
    }

    /**
     * Returns the unique identifier for this item.
     *
     * @return the unique identifier (cannot be null)
     */
    public IUniqueId getInternalId() {
        return internalId;
    }
    
    /**
     * Returns the volatile user data associated with this item.
     *
     * @return the volatile user data (can be null)
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Sets the volatile user data for this item.
     *
     * @param o the volatile user data to set (can be null)
     */
    public void setData(Object o) {
        this.data = o;
    }
    
    /**
     * Returns a hash code value for this unique item.
     * The hash code is based only on the internal ID.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.internalId.hashCode();
        return hash;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two unique items are equal if they have the same class and the same internal ID.
     *
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UniqueItem other = (UniqueItem) obj;
        return this.internalId == other.internalId;
    }

    /**
     * Returns a string representation of this unique item.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "UniqueItem [" + "internalId=" + internalId + ']';
    }

}
