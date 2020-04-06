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

public final class EventOccurrence extends AbstractInputDataItem {

    public static EventOccurrence of(int id) {
        return new EventOccurrence(id, null, null, null, null, null, null, null, null);
    }

    public static EventOccurrence of(int id, String qualifier, Object report) {
        return new EventOccurrence(id, null, null, null, qualifier, report, null, null, null);
    }

    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, String qualifier, Object report) {
        return new EventOccurrence(id, generationTime, receptionTime, null, qualifier, report, null, null, null);
    }

    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, null, null, null);
    }

    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report, String route, String source) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, route, source, null);
    }

    public static EventOccurrence of(int id, Instant generationTime, Instant receptionTime, IUniqueId container, String qualifier, Object report, String route, String source, Object extension) {
        return new EventOccurrence(id, generationTime, receptionTime, container, qualifier, report, route, source, extension);
    }

    private final int id;
    private final Instant generationTime;
    private final Instant receptionTime;
    private final String qualifier;
    private final Object report;
    private final IUniqueId container;
    private final String route;
    private final String source;
    private final Object extension;

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

    public String getQualifier() {
        return qualifier;
    }

    public Object getReport() {
        return report;
    }

    public IUniqueId getContainer() {
        return container;
    }

    public String getRoute() {
        return route;
    }

    public String getSource() {
        return source;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(id, generationTime, receptionTime, qualifier, report, container, route, source);
    }

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
