/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api;

import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;

import java.util.List;

/**
 *
 * @author dario
 */
public interface IServiceFactory {
    
    String getSystem();
    
    IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException;
    
    IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException;
    
    IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException;
    
    ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException;
    
    IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException;
    
    IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException;

    IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException;

    IActivityExecutionService getActivityExecutionService() throws ReatmetricException;

    List<ITransportConnector> getTransportConnectors() throws ReatmetricException;
}
