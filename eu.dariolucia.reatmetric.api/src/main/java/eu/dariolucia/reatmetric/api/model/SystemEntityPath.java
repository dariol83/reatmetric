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


package eu.dariolucia.reatmetric.api.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the path of a system entity in the ReatMetric system.
 * The path is composed of one or more string elements separated by dots (e.g., "spacecraft.subsystem.component").
 *
 * @author dario
 */
public final class SystemEntityPath implements Comparable<SystemEntityPath>, Serializable {

    /**
     * The serial version UID for serialization compatibility.
     */
	private static final long serialVersionUID = 1L;

	/**
     * Creates a SystemEntityPath from a string representation.
     * The string is split by dots to create the path elements.
     *
     * @param systemEntity the string representation of the path, cannot be null
     * @return a new SystemEntityPath instance
     */
	public static SystemEntityPath fromString(String systemEntity) {
        String[] parts = systemEntity.split("\\.", -1);
        return new SystemEntityPath(parts);
    }
    
    /**
     * The array of path elements that make up this path.
     */
    private final String[] pathElements;
    
    /**
     * The string representation of this path (elements joined by dots).
     */
    private final String stringRepresentation;
    
    /**
     * Constructor for a system entity path.
     *
     * @param paths the path elements, cannot be null or empty
     */
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
    
    /**
     * Returns a copy of the path elements array.
     *
     * @return a copy of the path elements array (cannot be null)
     */
    public String[] getPathElements() {
        return this.pathElements.clone();
    }
    
    /**
     * Returns the parent path of this path.
     * If this path has only one element, it has no parent and this method returns null.
     *
     * @return the parent path, or null if this path has only one element
     */
    public SystemEntityPath getParent() {
        if(pathElements.length == 1) {
            // No parent
            return null;
        }
        return new SystemEntityPath(Arrays.copyOfRange(pathElements, 0, pathElements.length - 1));
    }
    
    /**
     * Creates a new path by appending the specified path elements to this path.
     *
     * @param pathElements the path elements to append, cannot be null
     * @return a new SystemEntityPath with the appended elements
     */
    public SystemEntityPath append(String... pathElements) {
        String[] array = new String[this.pathElements.length + pathElements.length];
        System.arraycopy(this.pathElements, 0, array, 0, this.pathElements.length);
        System.arraycopy(pathElements, 0, array, this.pathElements.length, pathElements.length);
        return new SystemEntityPath(array);
    }
    
    /**
     * Returns the last element of this path.
     *
     * @return the last path element (cannot be null)
     */
    public String getLastPathElement() {
        return this.pathElements[this.pathElements.length - 1];
    }
    
    /**
     * Returns the first element of this path.
     *
     * @return the first path element (cannot be null)
     */
    public String getFirstPathElement() {
        return this.pathElements[0];
    }
    
    /**
     * Returns the path element at the specified index.
     *
     * @param i the index of the path element to return
     * @return the path element at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     */
    public String getPathElementAt(int i) {
        return this.pathElements[i];
    }
    
    /**
     * Returns the number of elements in this path.
     *
     * @return the number of path elements (always >= 1)
     */
    public int getPathLength() {
        return this.pathElements.length;
    }
    
    /**
     * Returns the string representation of this path.
     * The elements are joined by dots.
     *
     * @return the string representation (cannot be null)
     */
    public String asString() {
        return this.stringRepresentation;
    }
    
    /**
     * Checks if this path is a descendant of the specified path.
     *
     * @param p the parent path to check against, cannot be null
     * @return true if this path is a descendant of p, false otherwise
     */
    public boolean isDescendantOf(SystemEntityPath p) {
    	return asString().startsWith(p.asString());
    }
    
    /**
     * Checks if this path is a parent of the specified path.
     *
     * @param p the child path to check against, cannot be null
     * @return true if this path is a parent of p, false otherwise
     */
    public boolean isParentOf(SystemEntityPath p) {
    	return p.asString().startsWith(asString());
    }

    /**
     * Returns a hash code value for this system entity path.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.stringRepresentation);
        return hash;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two system entity paths are equal if they have the same string representation.
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
        final SystemEntityPath other = (SystemEntityPath) obj;
        if (!Objects.equals(this.stringRepresentation, other.stringRepresentation)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string representation of this system entity path.
     * This is the same as calling {@link #asString()}.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return asString();
    }

    /**
     * Compares this system entity path with the specified path for order.
     * Paths are compared lexicographically based on their string representation.
     *
     * @param o the system entity path to be compared
     * @return a negative integer, zero, or a positive integer as this path is less than, equal to, or greater than the specified path
     */
    @Override
    public int compareTo(SystemEntityPath o) {
        return asString().compareTo(o.asString());
    }
}
