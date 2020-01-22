/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.alarms.*;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class AlarmParameterDataAccessManager extends AbstractAccessManager<AlarmParameterData, AlarmParameterDataFilter, IAlarmParameterDataSubscriber> implements IAlarmParameterDataProvisionService {

    public AlarmParameterDataAccessManager(IAlarmParameterDataArchive archive) {
        super(archive);
    }

    @Override
    protected Class<? extends AbstractDataItem> getSupportedClass() {
        return AlarmParameterData.class;
    }

    @Override
    protected String getName() {
        return "Alarm Parameter Access Manager";
    }

    @Override
    protected AbstractAccessSubscriber<AlarmParameterData, AlarmParameterDataFilter, IAlarmParameterDataSubscriber> createSubscriber(IAlarmParameterDataSubscriber subscriber, AlarmParameterDataFilter filter, IProcessingModel model) {
        return new AlarmParameterDataAccessSubscriber(subscriber, filter, model);
    }
}
