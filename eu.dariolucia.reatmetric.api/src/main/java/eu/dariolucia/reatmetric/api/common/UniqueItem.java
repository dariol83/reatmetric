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
	 * 
	 */
	private static final long serialVersionUID = -4265552582009003032L;

	protected final IUniqueId internalId;

    protected transient volatile Object data;
    
    public UniqueItem(IUniqueId internalId, Object data) {
        this.internalId = internalId;
        this.data = data;
    }

    public IUniqueId getInternalId() {
        return internalId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object o) {
        this.data = o;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.internalId.hashCode();
        return hash;
    }

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

    @Override
    public String toString() {
        return "UniqueItem [" + "internalId=" + internalId + ']';
    }

}
