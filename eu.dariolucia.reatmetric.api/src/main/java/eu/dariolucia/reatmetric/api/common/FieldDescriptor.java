/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.common;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class FieldDescriptor implements Serializable {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String name;
    
    private final ValueTypeEnum type;
    
    private final FieldFilterStrategy filterStrategy;

    public FieldDescriptor(String name, ValueTypeEnum type, FieldFilterStrategy filterStrategy) {
        this.name = name;
        this.type = type;
        this.filterStrategy = filterStrategy;
    }

    public String getName() {
        return name;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public FieldFilterStrategy getFilterStrategy() {
        return filterStrategy;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.type);
        hash = 47 * hash + Objects.hashCode(this.filterStrategy);
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
        final FieldDescriptor other = (FieldDescriptor) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (this.filterStrategy != other.filterStrategy) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FieldDescriptor{" + "name=" + name + ", type=" + type + ", filterStrategy=" + filterStrategy + '}';
    }
    
}
