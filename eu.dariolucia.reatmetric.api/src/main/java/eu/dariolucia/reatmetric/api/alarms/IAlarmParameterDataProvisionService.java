/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.alarms;

import eu.dariolucia.reatmetric.api.common.IDataItemProvisionService;

/**
 *
 * @author dario
 */
public interface IAlarmParameterDataProvisionService extends IDataItemProvisionService<IAlarmParameterDataSubscriber, AlarmParameterDataFilter, AlarmParameterData> {

}
