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

package eu.dariolucia.reatmetric.api.processing.input;

import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a sample of a parameter value in the ReatMetric system.
 * This class contains the parameter ID, generation time, reception time, value, container ID, route, and extension.
 */
public final class ParameterSample extends AbstractInputDataItem {

    /**
     * Creates a ParameterSample with ID and value.
     * Generation time and reception time will be set to the current time.
     *
     * @param id the parameter ID
     * @param value the parameter value, can be null
     * @return a new ParameterSample instance
     */
    public static ParameterSample of(int id, Object value) {
        return new ParameterSample(id, null, null, null, value, null, null);
    }

    /**
     * Creates a ParameterSample with ID, generation time, reception time, and value.
     *
     * @param id the parameter ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param value the parameter value, can be null
     * @return a new ParameterSample instance
     */
    public static ParameterSample of(int id, Instant generationTime, Instant receptionTime, Object value) {
        return new ParameterSample(id, generationTime, receptionTime, null, value, null, null);
    }

    /**
     * Creates a ParameterSample with all available information.
     *
     * @param id the parameter ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param container the container ID, can be null
     * @param value the parameter value, can be null
     * @param route the route, can be null
     * @param extension the extension object, can be null
     * @return a new ParameterSample instance
     */
    public static ParameterSample of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, Object value, String route, Object extension) {
        return new ParameterSample(id, generationTime, receptionTime, container, value, route, extension);
    }

    /** The parameter ID. */
    private final int id;
    /** The timestamp when this sample was generated. */
    private final Instant generationTime;
    /** The timestamp when this sample was received. */
    private final Instant receptionTime;
    /** The parameter value. */
    private final Object value;
    /** The container ID for this sample. */
    private final IUniqueId containerId;
    /** The route for this sample. */
    private final String route;
    /** The extension object for additional data. */
    private final Object extension;

    /**
     * Constructs a new ParameterSample.
     *
     * @param id the parameter ID
     * @param generationTime the generation time, will use current time if null
     * @param receptionTime the reception time, will use generation time if null
     * @param containerId the container ID, can be null
     * @param value the parameter value, can be null
     * @param route the route, can be null
     * @param extension the extension object, can be null
     */
    private ParameterSample(int id, Instant generationTime, Instant receptionTime, IUniqueId containerId, Object value, String route, Object extension) {
        this.id = id;
        if(generationTime == null) {
            generationTime = Instant.now();
        }
        this.generationTime = generationTime;
        if(receptionTime == null) {
            receptionTime = this.generationTime;
        }
        this.receptionTime = receptionTime;
        this.value = value;
        this.containerId = containerId;
        this.route = route;
        this.extension = extension;
    }

    /**
     * Returns the parameter ID.
     *
     * @return the parameter ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the extension object.
     *
     * @return the extension object, may be null
     */
    public Object getExtension() {
        return extension;
    }

    /**
     * Returns the generation time of this sample.
     *
     * @return the generation time
     */
    public Instant getGenerationTime() {
        return generationTime;
    }

    /**
     * Returns the reception time of this sample.
     *
     * @return the reception time
     */
    public Instant getReceptionTime() {
        return receptionTime;
    }

    /**
     * Returns the parameter value.
     *
     * @return the parameter value, may be null
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the container ID for this sample.
     *
     * @return the container ID, may be null
     */
    public IUniqueId getContainerId() {
        return containerId;
    }

    /**
     * Returns the route for this sample.
     *
     * @return the route, may be null
     */
    public String getRoute() {
        return route;
    }

    @Override
    /**
     * Checks if this ParameterSample is equal to another object.
     * Two samples are considered equal if they have the same ID, generation time,
     * reception time, value, route, and container ID.
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterSample that = (ParameterSample) o;
        return Objects.equals(this.id, that.id) &&
                generationTime.equals(that.generationTime) &&
                receptionTime.equals(that.receptionTime) &&
                Objects.equals(value, that.value) &&
                Objects.equals(route, that.route) &&
                Objects.equals(containerId, that.containerId);
    }

    @Override
    /**
     * Returns a hash code for this ParameterSample.
     *
     * @return the hash code
     */
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, value, route, containerId);
    }

    @Override
    /**
     * Returns a string representation of this ParameterSample.
     *
     * @return the string representation
     */
    public String toString() {
        return "ParameterSample{" +
                "id=" + id +
                ", generationTime=" + generationTime +
                ", receptionTime=" + receptionTime +
                ", value=" + value +
                ", route=" + route +
                ", containerId=" + containerId +
                '}';
    }
}
