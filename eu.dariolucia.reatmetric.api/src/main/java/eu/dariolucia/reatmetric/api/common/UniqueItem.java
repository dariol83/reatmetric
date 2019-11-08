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
