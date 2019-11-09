/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api;

import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.IServiceMonitorCallback;
import eu.dariolucia.reatmetric.api.common.IUserMonitorCallback;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;

/**
 *
 * @author dario
 */
public interface IServiceFactory {
    
    public String getSystem();
    
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException;
    
    public IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException;
    
    public IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException;
    
    public ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException;
    
    public IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException;
    
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException;
    
    public void login(String username, String password) throws ReatmetricException;
    
    public void logout();

    public void connect() throws ReatmetricException;

    public void disconnect() throws ReatmetricException;
    
    public void register(IServiceMonitorCallback callback);
    
    public void register(IUserMonitorCallback callback);
    
    public void deregister(IServiceMonitorCallback callback);
    
    public void deregister(IUserMonitorCallback callback);
}
