/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.application;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.driver.spacecraft.message.IMessageProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.ITmTcProcessor;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CoreApplication implements IReatmetricSystem {

    private final List<IUserMonitorCallback> userListeners = new CopyOnWriteArrayList<>();
    private final List<IServiceMonitorCallback> serviceListeners = new CopyOnWriteArrayList<>();

    private String username;

    private IMessageProcessor operationalMessageService;

    private ITmTcProcessor rawDataService;

    private ISystemModelProvisionService systemModelService;

    private ProcessingModelImpl parameterDataService;

    private IEventDataProvisionService eventDataService;

    private IAlarmParameterDataProvisionService alarmDataService;

    public CoreApplication() {
        // Do not do anything
    }

    @Override
    public String getName() {
        return "Monitoring Centre CORE 1.0";
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.operationalMessageService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.rawDataService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.parameterDataService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.systemModelService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.eventDataService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException {
        if(this.username != null) {
            return this.alarmDataService;
        }
        throw new ReatmetricException("User not connected");
    }

    @Override
    public void login(String username, String password) throws ReatmetricException {
        if(this.username != null) {
            String reason = "User already connected";
            this.userListeners.stream().forEach((o) -> o.userConnectionFailed(getName(), username, reason));
            throw new ReatmetricException(reason);
        }
        this.username = username;
        // Now create the processing objects: the storer uses the username information to create/point to the
        // correct databases
        // TODO

        this.userListeners.stream().forEach((o) -> o.userConnected(getName(), username));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.OPERATIONAL_MESSAGES));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.RAW_DATA));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.SYSTEM_MODEL));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.PARAMETERS));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.EVENTS));
        this.serviceListeners.stream().forEach((o) -> o.serviceConnected(getName(), ServiceType.ALARMS));
    }

    @Override
    public void logout() {
        String oldUser = this.username;
        this.username = null;
        if(oldUser != null) {
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.ALARMS));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.EVENTS));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.PARAMETERS));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.SYSTEM_MODEL));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.OPERATIONAL_MESSAGES));
            this.serviceListeners.stream().forEach((o) -> o.serviceDisconnected(getName(), ServiceType.RAW_DATA));
            this.userListeners.stream().forEach((o) -> o.userDisconnected(getName(), oldUser));
        }

        // Close and cleanup
        // TODO
    }

    @Override
    public void connect() throws ReatmetricException {
        if(this.username == null) {
            throw new ReatmetricException("User not connected");
        }
        // TODO: start links on the transport layer
    }

    @Override
    public void disconnect() throws ReatmetricException {
        if(this.username == null) {
            throw new ReatmetricException("User not connected");
        }
        // TODO: stop links on the transport layer
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
