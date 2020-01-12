/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core;

import eu.dariolucia.reatmetric.api.IServiceFactory;
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

public class ServiceCoreImpl implements IServiceFactory {

    private static final String INIT_FILE_KEY = "reatmetric.core.config"; // Absolute location of the init file, to configure the core instance

    @Override
    public String getSystem() {
        return null;
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException {
        return null;
    }

    @Override
    public IActivityExecutionService getActivityExecutionService() throws ReatmetricException {
        return null;
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() throws ReatmetricException {
        return null;
    }
}
