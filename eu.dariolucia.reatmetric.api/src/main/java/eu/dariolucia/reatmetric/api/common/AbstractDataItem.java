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
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author dario
 */
public abstract class AbstractDataItem extends UniqueItem implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -7442923730308757888L;

	protected final Object extension;
    
    protected final Instant generationTime;
    
    public AbstractDataItem(IUniqueId internalId, Instant generationTime, Object extension) {
        super(internalId, null);
        if(generationTime != null) {
            this.generationTime = generationTime;
        } else {
            this.generationTime = Instant.EPOCH;
        }
        this.extension = extension;
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public Object getExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.internalId.hashCode();
        hash = 89 * hash + this.generationTime.hashCode();
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
        final AbstractDataItem other = (AbstractDataItem) obj;
        if (!Objects.equals(this.internalId, other.internalId)) {
            return false;
        }
        if (!Objects.equals(this.generationTime, other.generationTime)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AbstractDataItem [" + "internalId=" + internalId + ", extension=" + extension + ", generationTime=" + generationTime + ']';
    }

}
