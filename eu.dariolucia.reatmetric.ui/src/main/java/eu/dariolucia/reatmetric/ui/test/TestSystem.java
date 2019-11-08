/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.IServiceMonitorCallback;
import eu.dariolucia.reatmetric.api.common.IUserMonitorCallback;
import eu.dariolucia.reatmetric.api.common.ServiceType;
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
public class TestSystem implements IServiceFactory {

    private final List<IUserMonitorCallback> userListeners = new CopyOnWriteArrayList<>();
    private final List<IServiceMonitorCallback> serviceListeners = new CopyOnWriteArrayList<>();
    
    private String username;
    
    private TestOperationalMessageService operationalMessageService;
    
    private TestRawDataService rawDataService;
    
    private TestSystemModelService systemModelService;
    
    private TestParameterDataService parameterDataService;
    
    private TestEventDataService eventDataService;
    
    private TestAlarmParameterDataService alarmDataService;
    
    public TestSystem() {
    	this.systemModelService = new TestSystemModelService();
        this.operationalMessageService = new TestOperationalMessageService();
        this.rawDataService = new TestRawDataService();
        this.parameterDataService = new TestParameterDataService();
        this.eventDataService = new TestEventDataService(systemModelService);
        this.alarmDataService = new TestAlarmParameterDataService(systemModelService);
    }
    
    @Override
    public String getSystem() {
        return "Test System 1";
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws MonitoringCentreException {
        if(this.username != null) {
            return this.operationalMessageService;
        }
        throw new MonitoringCentreException("User not connected");
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService() throws MonitoringCentreException {
        if(this.username != null) {
            return this.rawDataService;
        }
        throw new MonitoringCentreException("User not connected");
    }
    
    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() throws MonitoringCentreException {
        if(this.username != null) {
            return this.parameterDataService;
        }
        throw new MonitoringCentreException("User not connected");
    }
    
    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() throws MonitoringCentreException {
        if(this.username != null) {
            return this.systemModelService;
        }
        throw new MonitoringCentreException("User not connected");
    }
    
    @Override
    public IEventDataProvisionService getEventDataMonitorService() throws MonitoringCentreException {
        if(this.username != null) {
            return this.eventDataService;
        }
        throw new MonitoringCentreException("User not connected");
    }
    
    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws MonitoringCentreException {       
    	if(this.username != null) {
            return this.alarmDataService;
        }
        throw new MonitoringCentreException("User not connected");
    }
    
    @Override
    public void login(String username, String password) throws MonitoringCentreException {
        if(this.username != null) {
            String reason = "User already connected";
            this.userListeners.stream().forEach((o) -> o.userConnectionFailed(getSystem(), username, reason));
            throw new MonitoringCentreException(reason);
        }
        this.username = username;
        this.userListeners.stream().forEach((o) -> o.userConnected(getSystem(), username));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.OPERATIONAL_MESSAGES));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.RAW_DATA));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.SYSTEM_MODEL));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.PARAMETERS));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.EVENTS));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getSystem(), ServiceType.ALARMS));
    }

    @Override
    public void logout() {
        String oldUser = this.username;
        this.username = null;
        if(oldUser != null) {
        	this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.ALARMS));
        	this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.EVENTS));
        	this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.PARAMETERS));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.SYSTEM_MODEL));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.OPERATIONAL_MESSAGES));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getSystem(), ServiceType.RAW_DATA));
            this.userListeners.stream().forEach((o) -> o.userDisconnected(getSystem(), oldUser));
        }
    }

    @Override
    public void connect() {
        // Automatic start with this test system
    }

    @Override
    public void disconnect() {
        // Automatic start with this test system
    }

    @Override
    public void register(IServiceMonitorCallback callback) {
        this.serviceListeners.add(callback);
    }

    @Override
    public void register(IUserMonitorCallback callback) {
        this.userListeners.add(callback);
    }

    @Override
    public void deregister(IServiceMonitorCallback callback) {
        this.serviceListeners.remove(callback);
    }

    @Override
    public void deregister(IUserMonitorCallback callback) {
        this.userListeners.remove(callback);
    }
    
}
