/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.UniqueItem;

/**
 *
 * @author dario
 */
public final class SystemEntityUpdate extends UniqueItem implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -17050372263098762L;

	private final SystemEntity element;
    
    private final UpdateType updateType;

    public SystemEntityUpdate(IUniqueId internalId, SystemEntity element, UpdateType updateType) {
        super(internalId, null);
        this.element = element;
        this.updateType = updateType;
    }

    public SystemEntity getElement() {
        return element;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "SystemEntityUpdate{" + "element=" + element + ", updateType=" + updateType + '}';
    }
    
}
