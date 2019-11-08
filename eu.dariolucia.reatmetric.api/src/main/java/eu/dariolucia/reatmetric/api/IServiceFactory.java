/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api;

import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.IServiceMonitorCallback;
import eu.dariolucia.reatmetric.api.common.IUserMonitorCallback;
import eu.dariolucia.reatmetric.api.common.exceptions.MonitoringCentreException;
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
    
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws MonitoringCentreException;
    
    public IRawDataProvisionService getRawDataMonitorService() throws MonitoringCentreException;
    
    public IParameterDataProvisionService getParameterDataMonitorService() throws MonitoringCentreException;
    
    public ISystemModelProvisionService getSystemModelMonitorService() throws MonitoringCentreException;
    
    public IEventDataProvisionService getEventDataMonitorService() throws MonitoringCentreException;
    
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws MonitoringCentreException;
    
    public void login(String username, String password) throws MonitoringCentreException;
    
    public void logout();

    public void connect() throws MonitoringCentreException;

    public void disconnect() throws MonitoringCentreException;
    
    public void register(IServiceMonitorCallback callback);
    
    public void register(IUserMonitorCallback callback);
    
    public void deregister(IServiceMonitorCallback callback);
    
    public void deregister(IUserMonitorCallback callback);
}
