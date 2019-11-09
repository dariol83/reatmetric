/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class SystemEntityPath implements Comparable<SystemEntityPath>, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8858858517842953497L;

	public static SystemEntityPath fromString(String systemEntity) {
        String[] parts = systemEntity.split("\\.", -1);
        return new SystemEntityPath(parts);
    }
    
    private final String[] pathElements;
    
    private final String stringRepresentation;
    
    public SystemEntityPath(String... paths) {
        this.pathElements = paths.clone();
        StringBuilder sb = new StringBuilder();
        for(String s : paths) {
            sb.append(s).append(".");
        }
        if(sb.length()>0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        this.stringRepresentation = sb.toString();
    }
    
    public String[] getPathElements() {
        return this.pathElements.clone();
    }
    
    public SystemEntityPath getParent() {
        if(pathElements.length == 1) {
            // No parent
            return null;
        }
        return new SystemEntityPath(Arrays.copyOfRange(pathElements, 0, pathElements.length - 1));
    }
    
    public SystemEntityPath append(String... pathElements) {
        String[] array = new String[this.pathElements.length + pathElements.length];
        System.arraycopy(this.pathElements, 0, array, 0, this.pathElements.length);
        System.arraycopy(pathElements, 0, array, this.pathElements.length, pathElements.length);
        return new SystemEntityPath(array);
    }
    
    public String getLastPathElement() {
        return this.pathElements[this.pathElements.length - 1];
    }
    
    public String getFirstPathElement() {
        return this.pathElements[0];
    }
    
    public String getPathElementAt(int i) {
        return this.pathElements[i];
    }
    
    public int getPathLength() {
        return this.pathElements.length;
    }
    
    public String asString() {
        return this.stringRepresentation;
    }
    
    public boolean isDescendantOf(SystemEntityPath p) {
    	return asString().startsWith(p.asString());
    }
    
    public boolean isParentOf(SystemEntityPath p) {
    	return p.asString().startsWith(asString());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.stringRepresentation);
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
        final SystemEntityPath other = (SystemEntityPath) obj;
        if (!Objects.equals(this.stringRepresentation, other.stringRepresentation)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public int compareTo(SystemEntityPath o) {
        return asString().compareTo(o.asString());
    }
}
