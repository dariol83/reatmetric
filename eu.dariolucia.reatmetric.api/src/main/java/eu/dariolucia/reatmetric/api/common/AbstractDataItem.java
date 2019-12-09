/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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

	protected final Object[] additionalFields;
    
    protected final Instant generationTime;
    
    public AbstractDataItem(IUniqueId internalId, Instant generationTime, Object[] additionalFields) {
        super(internalId, null);
        this.generationTime = generationTime;
        if(additionalFields != null) {
            this.additionalFields = additionalFields.clone();
        } else {
            this.additionalFields = new Object[0];
        }
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public Object[] getAdditionalFields() {
        return additionalFields.clone();
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
        return "AbstractDataItem [" + "internalId=" + internalId + ", additionalFields=" + Arrays.toString(additionalFields) + ", generationTime=" + generationTime + ']';
    }

}
