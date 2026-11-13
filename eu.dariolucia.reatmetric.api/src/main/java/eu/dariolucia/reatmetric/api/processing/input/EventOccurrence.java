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
 * Represents an occurrence of an event in the ReatMetric system.
 * This class contains all the information related to an event occurrence, including its ID, generation time,
 * reception time, qualifier, report, container, route, and source.
 */
public final class EventOccurrence extends AbstractInputDataItem {

    /**
     * Creates an EventOccurrence with only the ID.
     * Generation time and reception time will be set to the current time.
     *
     * @param id the event ID
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id) {
        return new EventOccurrence(id, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates an EventOccurrence with ID, qualifier, and report.
     * Generation time and reception time will be set to the current time.
     *
     * @param id the event ID
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id, String qualifier, Object report) {
        return new EventOccurrence(id, null, null, null, qualifier, report, null, null, null);
    }

    /**
     * Creates an EventOccurrence with ID, generation time, reception time, qualifier, and report.
     *
     * @param id the event ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, String qualifier, Object report) {
        return new EventOccurrence(id, generationTime, receptionTime, null, qualifier, report, null, null, null);
    }

    /**
     * Creates an EventOccurrence with ID, generation time, reception time, container, qualifier, and report.
     *
     * @param id the event ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param container the container ID, can be null
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, null, null, null);
    }

    /**
     * Creates an EventOccurrence with ID, generation time, reception time, container, qualifier, report, route, and source.
     *
     * @param id the event ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param container the container ID, can be null
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @param route the route, can be null
     * @param source the source, can be null
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report, String route, String source) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, route, source, null);
    }

    /**
     * Creates an EventOccurrence with all available information.
     *
     * @param id the event ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param container the container ID, can be null
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @param route the route, can be null
     * @param source the source, can be null
     * @param extension the extension object, can be null
     * @return a new EventOccurrence instance
     */
    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report, String route, String source, Object extension) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, route, source, extension);
    }

    /**
     * The event ID.
     */
    private final int id;
    
    /**
     * The generation time of the event occurrence.
     */
    private final Instant generationTime;
    
    /**
     * The reception time of the event occurrence.
     */
    private final Instant receptionTime;
    
    /**
     * The qualifier for this event occurrence.
     */
    private final String qualifier;
    
    /**
     * The report object associated with this event occurrence.
     */
    private final Object report;
    
    /**
     * The container ID associated with this event occurrence.
     */
    private final IUniqueId container;
    
    /**
     * The route from which the event was received.
     */
    private final String route;
    
    /**
     * The source that originated the event.
     */
    private final String source;
    
    /**
     * The extension object associated with this event occurrence.
     */
    private final Object extension;

    /**
     * Private constructor for creating EventOccurrence instances.
     * Use the factory methods instead.
     *
     * @param id the event ID
     * @param generationTime the generation time, can be null (will use current time)
     * @param receptionTime the reception time, can be null (will use generation time or current time)
     * @param container the container ID, can be null
     * @param qualifier the qualifier for this event occurrence, can be null
     * @param report the report object, can be null
     * @param route the route, can be null
     * @param source the source, can be null
     * @param extension the extension object, can be null
     */
    private EventOccurrence(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report, String route, String source, Object extension) {
        this.id = id;
        if(generationTime == null) {
            generationTime = Instant.now();
        }
        this.generationTime = generationTime;
        if(receptionTime == null) {
            receptionTime = this.generationTime;
        }
        this.receptionTime = receptionTime;
        this.qualifier = qualifier;
        this.report = report;
        this.container = container;
        this.route = route;
        this.source = source;
        this.extension = extension;
    }

    /**
     * Returns the event ID.
     *
     * @return the event ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the extension object associated with this event occurrence.
     *
     * @return the extension object (can be null)
     */
    public Object getExtension() {
        return extension;
    }

    /**
     * Returns the generation time of the event occurrence.
     *
     * @return the generation time (cannot be null)
     */
    public Instant getGenerationTime() {
        return generationTime;
    }

    /**
     * Returns the reception time of the event occurrence.
     *
     * @return the reception time (cannot be null)
     */
    public Instant getReceptionTime() {
        return receptionTime;
    }

    /**
     * Returns the qualifier for this event occurrence.
     *
     * @return the qualifier (can be null)
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Returns the report object associated with this event occurrence.
     *
     * @return the report object (can be null)
     */
    public Object getReport() {
        return report;
    }

    /**
     * Returns the container ID associated with this event occurrence.
     *
     * @return the container ID (can be null)
     */
    public IUniqueId getContainer() {
        return container;
    }

    /**
     * Returns the route from which the event was received.
     *
     * @return the route (can be null)
     */
    public String getRoute() {
        return route;
    }

    /**
     * Returns the source that originated the event.
     *
     * @return the source (can be null)
     */
    public String getSource() {
        return source;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two event occurrences are equal if they have the same ID, route, source, generation time,
     * reception time, qualifier, report, and container.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventOccurrence that = (EventOccurrence) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(route, that.route) &&
                Objects.equals(source, that.source) &&
                Objects.equals(generationTime, that.generationTime) &&
                Objects.equals(receptionTime, that.receptionTime) &&
                Objects.equals(qualifier, that.qualifier) &&
                Objects.equals(report, that.report) &&
                Objects.equals(container, that.container);
    }

    /**
     * Returns a hash code value for this event occurrence.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, qualifier, report, container, route, source);
    }

    /**
     * Returns a string representation of this event occurrence.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "EventOccurrence{" +
                "id=" + id +
                ", generationTime=" + generationTime +
                ", receptionTime=" + receptionTime +
                ", qualifier='" + qualifier + '\'' +
                ", report='" + report + '\'' +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                ", container=" + container +
                '}';
    }
}
