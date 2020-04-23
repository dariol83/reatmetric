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
import java.util.Objects;

/**
 * This class is the base class for all data items managed and stored by the ReatMetric infrastructure. Such items
 * are characterized by having a generation time and an extension object, that can be serialized and stored.
 *
 * Being a derivation from the {@link UniqueItem} class, objects of this class have an internal ID, which is unique
 * among all the object instances of the same class type.
 *
 * Objects of this class are supposed to be immutable. In particular, the extension object, when used, must be immutable
 * or not modified, as soon as provided in the object constructor. This means, exclusive ownership to modify the object
 * shall be transferred to the {@link AbstractDataItem} object. Failing to do so may result in undefined behaviour
 * during the handling and storage of the instance.
 */
public abstract class AbstractDataItem extends UniqueItem implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected final Object extension;
    
    protected final Instant generationTime;

    /**
     * Constructor of the object. If null generation time is provided, the object generation time is set to
     * Instant.EPOCH.
     *
     * @param internalId the unique internal ID that identifies this specific instance
     * @param generationTime the generation time - if set to null, Instant.EPOCH is used
     * @param extension the extension object (can be null)
     */
    public AbstractDataItem(IUniqueId internalId, Instant generationTime, Object extension) {
        super(internalId, null);
        this.generationTime = Objects.requireNonNullElse(generationTime, Instant.EPOCH);
        this.extension = extension;
    }

    /**
     * Return the data item generation time.
     *
     * @return the generation time (cannot be null)
     */
    public Instant getGenerationTime() {
        return generationTime;
    }

    /**
     * Return the extension object that was specified in the constructor.
     *
     * @return the extension object (can be null)
     */
    public Object getExtension() {
        return extension;
    }

    /**
     * The hashcode defined by this object takes into account only the internal ID and the generation time fields.
     *
     * @return the object hashcode
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.internalId.hashCode();
        hash = 89 * hash + this.generationTime.hashCode();
        return hash;
    }

    /**
     * The equality defined by this object is based on the class type, the equality of the internal ID and the
     * equality of the generation time.
     *
     * @param obj the object, to check equality against
     * @return true if obj is equal to this, otherwise false
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
        final AbstractDataItem other = (AbstractDataItem) obj;
        if (!Objects.equals(this.internalId, other.internalId)) {
            return false;
        }
        return Objects.equals(this.generationTime, other.generationTime);
    }

    @Override
    public String toString() {
        return "AbstractDataItem [" + "internalId=" + internalId + ", extension=" + extension + ", generationTime=" + generationTime + ']';
    }

}
