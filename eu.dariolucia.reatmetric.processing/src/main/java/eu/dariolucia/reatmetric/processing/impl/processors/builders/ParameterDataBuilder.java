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

package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;

import java.time.Instant;
import java.util.Objects;

/**
 * This helper class is used to build a {@link ParameterData} instance.
 */
public class ParameterDataBuilder extends AbstractDataItemBuilder<ParameterData> {

    private Instant generationTime;

    private Object engValue;

    private Object sourceValue;

    private String route;

    private Instant receptionTime;

    private Validity validity = Validity.UNKNOWN;

    private AlarmState alarmState = AlarmState.UNKNOWN;

    private IUniqueId containerId = null;

    public ParameterDataBuilder(int id, SystemEntityPath path) {
        super(id, path);
    }

    public void setGenerationTime(Instant generationTime) {
        if (!Objects.equals(this.generationTime, generationTime)) {
            this.generationTime = generationTime;
            this.changedSinceLastBuild = true;
        }
    }

    public void setEngValue(Object engValue) {
        if (!Objects.equals(this.engValue, engValue)) {
            this.engValue = engValue;
            this.changedSinceLastBuild = true;
        }
    }

    public void setRoute(String route) {
        if (!Objects.equals(this.route, route)) {
            this.route = route;
            this.changedSinceLastBuild = true;
        }
    }

    public void setSourceValue(Object sourceValue) {
        if (!Objects.equals(this.sourceValue, sourceValue)) {
            this.sourceValue = sourceValue;
            this.changedSinceLastBuild = true;
        }
    }

    public void setContainerId(IUniqueId containerId) {
        if (!Objects.equals(this.containerId, containerId)) {
            this.containerId = containerId;
            this.changedSinceLastBuild = true;
        }
    }

    public void setReceptionTime(Instant receptionTime) {
        if (!Objects.equals(this.receptionTime, receptionTime)) {
            this.receptionTime = receptionTime;
            this.changedSinceLastBuild = true;
        }
    }

    public void setValidity(Validity validity) {
        if (!Objects.equals(this.validity, validity)) {
            this.validity = validity;
            this.changedSinceLastBuild = true;
        }
    }

    public void setAlarmState(AlarmState alarmState) {
        if (!Objects.equals(this.alarmState, alarmState)) {
            this.alarmState = alarmState;
            this.changedSinceLastBuild = true;
        }
    }

    @Override
    public ParameterData build(IUniqueId updateId) {
        return new ParameterData(updateId, generationTime, id, path.getLastPathElement(), path, engValue, sourceValue, route, validity, alarmState, containerId, receptionTime, null);
    }

    @Override
    public void setInitialisation(ParameterData item) {
        this.generationTime = item.getGenerationTime();
        this.receptionTime = item.getReceptionTime();
        this.sourceValue = item.getSourceValue();
        this.engValue = item.getEngValue();
        this.validity = item.getValidity();
        this.alarmState = item.getAlarmState();
        this.route = item.getRoute();
        this.containerId = item.getRawDataContainerId();
    }
}
