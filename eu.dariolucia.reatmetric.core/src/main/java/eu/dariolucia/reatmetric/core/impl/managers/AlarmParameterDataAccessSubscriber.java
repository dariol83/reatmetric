/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class AlarmParameterDataAccessSubscriber extends AbstractAccessSubscriber<AlarmParameterData, AlarmParameterDataFilter, IAlarmParameterDataSubscriber> {

    public AlarmParameterDataAccessSubscriber(IAlarmParameterDataSubscriber subscriber, AlarmParameterDataFilter filter, IProcessingModel model) {
        super(subscriber, filter, model);
    }

    @Override
    protected Pair<Integer, Long> computeId(AlarmParameterData item) {
        return Pair.of(item.getExternalId(), 0L);
    }

    @Override
    protected String getName() {
        return "Alarm Parameter Access Subscriber";
    }

}
