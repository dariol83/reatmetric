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

public final class ParameterSample extends AbstractInputDataItem {

    public static ParameterSample of(int id, Object value) {
        return new ParameterSample(id, null, null, null, value, null, null);
    }

    public static ParameterSample of(int id, Instant generationTime, Instant receptionTime, Object value) {
        return new ParameterSample(id, generationTime, receptionTime, null, value, null, null);
    }

    public static ParameterSample of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, Object value, String route, Object extension) {
        return new ParameterSample(id, generationTime, receptionTime, container, value, route, extension);
    }

    private final int id;
    private final Instant generationTime;
    private final Instant receptionTime;
    private final Object value;
    private final IUniqueId containerId;
    private final String route;
    private final Object extension;

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

    public int getId() {
        return id;
    }

    public Object getExtension() {
        return extension;
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

    public IUniqueId getContainerId() {
        return containerId;
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
                Objects.equals(containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, value, route, containerId);
    }

    @Override
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
