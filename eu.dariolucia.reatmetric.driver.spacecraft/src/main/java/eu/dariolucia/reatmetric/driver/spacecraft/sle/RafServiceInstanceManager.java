/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.sle;

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.bind.types.SleBindReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafSyncNotifyInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstance;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RafServiceInstanceManager extends SleServiceInstanceManager<RafServiceInstance, RafServiceInstanceConfiguration> {

    public RafServiceInstanceManager(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        super(peerConfiguration, siConfiguration, spacecraftConfiguration, broker);
    }

    @Override
    protected RafServiceInstance createServiceInstance(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration siConfiguration) {
        return new RafServiceInstance(peerConfiguration, siConfiguration);
    }

    @Override
    protected boolean isStartReturn(Object operation) {
        return operation instanceof RafStartReturn;
    }

    @Override
    protected void handleOperation(Object operation) {
        // TODO: synch notify, status report, schedule status report return, transfer data, get parameter
    }

    @Override
    protected void finalizeConnection() {
        // TODO: schedule status report
    }

    @Override
    protected void sendStart() {
        serviceInstance.start(null, null, siConfiguration.getRequestedFrameQuality());
    }

    @Override
    protected void sendStop() {
        serviceInstance.stop();
    }
}
