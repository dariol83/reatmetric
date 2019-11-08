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

public final class ParameterSample {

    public static final ParameterSample of(SystemEntityPath id, Object value) {
        return new ParameterSample(id, null, null, null, value);
    }

    public static final ParameterSample of(SystemEntityPath id, Instant generationTime, Instant receptionTime, Object value) {
        return new ParameterSample(id, generationTime, receptionTime, null, value);
    }

    public static final ParameterSample of(SystemEntityPath id, Instant generationTime, Instant receptionTime, IUniqueId container, Object value) {
        return new ParameterSample(id, generationTime, receptionTime, container, value);
    }

    private final SystemEntityPath id;
    private final Instant generationTime;
    private final Instant receptionTime;
    private final Object value;
    private final IUniqueId container;

    private ParameterSample(SystemEntityPath id, Instant generationTime, Instant receptionTime, IUniqueId container, Object value) {
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

    public Object getValue() {
        return value;
    }

    public IUniqueId getContainer() {
        return container;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterSample that = (ParameterSample) o;
        return id.equals(that.id) &&
                generationTime.equals(that.generationTime) &&
                receptionTime.equals(that.receptionTime) &&
                Objects.equals(value, that.value) &&
                Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, value, container);
    }

    @Override
    public String toString() {
        return "ParameterSample{" +
                "id=" + id +
                ", generationTime=" + generationTime +
                ", receptionTime=" + receptionTime +
                ", value=" + value +
                ", container=" + container +
                '}';
    }
}
