/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.parameters;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 *
 * @author dario
 */
public final class ParameterData extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private final int externalId;

	private final String name;

    private final SystemEntityPath path;
    
    private final Object engValue;
    
    private final Object sourceValue;
    
    private final Instant receptionTime;

    private final String route;

    private final Validity validity;

    private final IUniqueId rawDataContainerId;

    private final AlarmState alarmState;

    public ParameterData(IUniqueId internalId, Instant generationTime, int externalId, String name, SystemEntityPath path, Object engValue, Object sourceValue, String route, Validity validity, AlarmState alarmState, IUniqueId rawDataContainerId, Instant receptionTime, Object[] additionalFields) {
        super(internalId, generationTime, additionalFields);
        this.name = name;
        this.externalId = externalId;
        this.path = path;
        this.engValue = engValue;
        this.sourceValue = sourceValue;
        this.receptionTime = receptionTime;
        this.validity = validity;
        this.alarmState = alarmState;
        this.route = route;
        this.rawDataContainerId = rawDataContainerId;
    }

    public int getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public SystemEntityPath getPath() {
        return path;
    }

    public Object getEngValue() {
        return engValue;
    }

    public Object getSourceValue() {
        return sourceValue;
    }

    public Instant getReceptionTime() {
        return receptionTime;
    }

    public Validity getValidity() {
        return validity;
    }

    public AlarmState getAlarmState() {
        return alarmState;
    }

    public String getRoute() {
        return route;
    }

    public IUniqueId getRawDataContainerId() {
        return rawDataContainerId;
    }

    @Override
    public String toString() {
        return "ParameterData{" +
                "internalId=" + internalId +
                ", generationTime=" + generationTime +
                ", externalId=" + externalId +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", engValue=" + engValue +
                ", sourceValue=" + sourceValue +
                ", receptionTime=" + receptionTime +
                ", route='" + route + '\'' +
                ", validity=" + validity +
                ", alarmState=" + alarmState +
                ", containerId=" + rawDataContainerId +
                '}';
    }
}
