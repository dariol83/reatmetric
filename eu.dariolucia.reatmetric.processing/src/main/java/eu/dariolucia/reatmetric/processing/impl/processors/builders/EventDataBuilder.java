/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.Objects;

/**
 * This helper class is used to build a {@link EventData} instance.
 */
public class EventDataBuilder extends AbstractDataItemBuilder<EventData> {

    private Instant generationTime;

    private Object report;

    private String route;

    private String source;

    private Instant receptionTime;

    private String qualifier;

    private final String type;

    private final Severity severity;

    private IUniqueId containerId = null;

    public EventDataBuilder(int id, SystemEntityPath path, Severity severity, String type) {
        super(id, path);
        this.severity = severity;
        this.type = type;
    }

    public void setGenerationTime(Instant generationTime) {
        if (!Objects.equals(this.generationTime, generationTime)) {
            this.generationTime = generationTime;
            this.changedSinceLastBuild = true;
        }
    }

    public void setReceptionTime(Instant receptionTime) {
        if (!Objects.equals(this.receptionTime, receptionTime)) {
            this.receptionTime = receptionTime;
            this.changedSinceLastBuild = true;
        }
    }

    public void setEventState(String qualifier, String source, String route, Object report, IUniqueId containerId) {
        this.qualifier = qualifier;
        this.source = source;
        this.route = route;
        this.report = report;
        this.containerId = containerId;
    }

    @Override
    public EventData build(IUniqueId updateId) {
        return new EventData(updateId, generationTime, id, path.getLastPathElement(), path, qualifier, type, route,  source, severity, report, containerId, receptionTime, null);
    }

    @Override
    public void setInitialisation(EventData item) {
        this.generationTime = item.getGenerationTime();
        this.receptionTime = item.getReceptionTime();
        this.report = item.getReport();
        this.source = item.getSource();
        this.route = item.getRoute();
        this.qualifier = item.getQualifier();
        this.containerId = item.getRawDataContainerId();
    }
}
