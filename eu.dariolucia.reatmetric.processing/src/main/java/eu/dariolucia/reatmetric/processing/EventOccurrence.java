/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.Objects;

public final class EventOccurrence {

    public static EventOccurrence of(SystemEntityPath id) {
        return new EventOccurrence(id, null, null, null, null, null);
    }

    public static EventOccurrence of(SystemEntityPath id, String qualifier, String type) {
        return new EventOccurrence(id, null, null, null, qualifier, type);
    }

    public static EventOccurrence of(SystemEntityPath id, Instant generationTime, Instant receptionTime, String qualifier, String type) {
        return new EventOccurrence(id, generationTime, receptionTime, null, qualifier, type);
    }

    public static EventOccurrence of(SystemEntityPath id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, String type) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, type);
    }

    private final SystemEntityPath id;
    private final Instant generationTime;
    private final Instant receptionTime;
    private final String qualifier;
    private final String type;
    private final IUniqueId container;

    private EventOccurrence(SystemEntityPath id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, String type) {
        this.id = id;
        this.generationTime = generationTime;
        this.receptionTime = receptionTime;
        this.qualifier = qualifier;
        this.type = type;
        this.container = container;
    }

    public SystemEntityPath getId() {
        return id;
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public Instant getReceptionTime() {
        return receptionTime;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getType() {
        return type;
    }

    public IUniqueId getContainer() {
        return container;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventOccurrence that = (EventOccurrence) o;
        return id.equals(that.id) &&
                Objects.equals(generationTime, that.generationTime) &&
                Objects.equals(receptionTime, that.receptionTime) &&
                Objects.equals(qualifier, that.qualifier) &&
                Objects.equals(type, that.type) &&
                Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, qualifier, type, container);
    }

    @Override
    public String toString() {
        return "EventOccurrence{" +
                "id=" + id +
                ", generationTime=" + generationTime +
                ", receptionTime=" + receptionTime +
                ", qualifier='" + qualifier + '\'' +
                ", type='" + type + '\'' +
                ", container=" + container +
                '}';
    }
}
