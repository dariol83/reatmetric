/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft;

import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.util.List;

/**
 * The Reatmetric Spacecraft Driver is a driver decomposed in the following functional layers:
 * <ul>
 *     <li>SLE Transport Layer: the driver supports RAF, RCF and ROCF return services, plus CLTU forward services. Each service
 *     is linked one-to-one to a route. When a return service receives a space data unit (AOS frame, TM frame, OCF), the unit is
 *     forwarded to the upper layer. When a forward service receives a CLTU transmission request, the requested is forwarded via
 *     SLE to the remote peer.</li>
 *     <li>TM Data Link Layer: the driver supports TM and AOS frames for telemetry, which are decoded in this layer (derandomization,
 *     packet extraction and identification, distribution and storage via the raw data broker). OCF data (either extracted from
 *     frames or received via dedicated transport route) is never stored, but distributed using the same approach.</li>
 *     <li>TC Data Link Layer: the driver supports TC frames for telecommand, constructed from a TC packet/TC segment, with
 *     related randomization and CLTU encoding. This layer also support activation of COP-1 for sequence controlled transmission.</li>
 *     <li>TM Packet Layer: the driver supports processing of CCSDS Space Packet with optional ECSS PUS header. Space packets are
 *     decoded in parameters and injected into the processing model. Support for PUS services is limited to Service 9 and Service 5,
 *     TM only.</li>
 *     <li>TC Packet Layer: the driver supports the encoding of Space Packets with optional ECSS PUS header, starting from requests
 *     coming from the processing model. Space packets are encoded in TC packets with optional segment header. Support for PUS
 *     services is limited to Service 1 and Service 11, both with limitations</li>
 * </ul>
 */
public class SpacecraftDriver implements IDriver {

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {

    }

    @Override
    public SystemStatus getDriverStatus() {
        return null;
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return null;
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return null;
    }

    @Override
    public void dispose() throws DriverException {

    }
}
