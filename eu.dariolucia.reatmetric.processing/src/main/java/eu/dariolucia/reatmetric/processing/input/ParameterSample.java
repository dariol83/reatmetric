/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.input;

import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;
import java.util.Objects;

public final class ParameterSample extends AbstractInputDataItem {

    public static final ParameterSample of(int id, Object value) {
        return new ParameterSample(id, null, null, null, value, null);
    }

    public static final ParameterSample of(int id, Instant generationTime, Instant receptionTime, Object value) {
        return new ParameterSample(id, generationTime, receptionTime, null, value, null);
    }

    public static final ParameterSample of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, Object value, String route) {
        return new ParameterSample(id, generationTime, receptionTime, container, value, route);
    }

    private final int id;
    private final Instant generationTime;
    private final Instant receptionTime;
    private final Object value;
    private final IUniqueId parameterContainerId;
    private final String route;

    private ParameterSample(int id, Instant generationTime, Instant receptionTime, IUniqueId parameterContainerId, Object value, String route) {
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
        this.parameterContainerId = parameterContainerId;
        this.route = route;
    }

    public int getId() {
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

    public IUniqueId getParameterContainerId() {
        return parameterContainerId;
    }

    public String getRoute() {
        return route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterSample that = (ParameterSample) o;
        return Objects.equals(this.id, that.id) &&
                generationTime.equals(that.generationTime) &&
                receptionTime.equals(that.receptionTime) &&
                Objects.equals(value, that.value) &&
                Objects.equals(route, that.route) &&
                Objects.equals(parameterContainerId, that.parameterContainerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, value, route, parameterContainerId);
    }

    @Override
    public String toString() {
        return "ParameterSample{" +
                "id=" + id +
                ", generationTime=" + generationTime +
                ", receptionTime=" + receptionTime +
                ", value=" + value +
                ", route=" + route +
                ", container=" + parameterContainerId +
                '}';
    }
}
