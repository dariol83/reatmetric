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
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.SleServiceInstanceManager;

import java.io.File;
import java.util.List;

/**
 * The Reatmetric Spacecraft Driver is a driver decomposed in the following functional layers:
 * <ul>
 *     <li>SLE Transport Layer: the driver supports RAF and RCF return services, plus CLTU forward services. Each service
 *     is linked one-to-one to a route. When a return service receives a space data unit (AOS frame, TM frame), the unit is
 *     forwarded to the upper layer via the raw data broker. Randomized frames are derandomized before forwarding.
 *     When a forward service receives a CLTU transmission request, the requested is forwarded via SLE to the remote peer.</li>
 *     <li>TM Data Link Layer: the driver supports TM and AOS frames for telemetry, which are decoded in this layer (packet
 *     extraction and identification, distribution and storage via the raw data broker). OCF data (either extracted from
 *     frames or received via dedicated sle route) is never stored, but distributed using the same approach.</li>
 *     <li>TC Data Link Layer: the driver supports TC frames for telecommand, constructed from a TC packet/TC segment, with
 *     related randomization and CLTU encoding. This layer also support activation of COP-1 for sequence controlled
 *     transmission.</li>
 *     <li>TM Packet Layer: the driver supports processing of CCSDS Space Packet with optional ECSS PUS header. Space packets are
 *     decoded in parameters and injected into the processing model. Support for PUS services is limited to Service 9 and
 *     Service 5,
 *     TM only.</li>
 *     <li>TC Packet Layer: the driver supports the encoding of Space Packets with optional ECSS PUS header, starting from
 *     requests coming from the processing model. Space packets are encoded in TC packets with optional segment header.
 *     Support for PUS services is limited to Service 1 and Service 11, both with limitations</li>
 * </ul>
 */
public class SpacecraftDriver implements IDriver {

    private static final String SLE_FOLDER = "sle";
    private static final String CONFIGURATION_FILE = "configuration.xml";
    private static final String ENCODING_DECODING_DEFINITION_FILE = "tmtc.xml";

    private String name;
    private SpacecraftConfiguration configuration;
    private ServiceCoreConfiguration coreConfiguration;
    private IServiceCoreContext context;
    private IDriverListener listener;

    private List<SleServiceInstanceManager> sleManagers;
    private TmDataLinkProcessor tmDataLinkProcessor;
    private TmPacketProcessor tmPacketProcessor;

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.coreConfiguration = coreConfiguration;
        this.listener = subscriber;
        // Load the driver configuration
        loadDriverConfiguration(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE);
        // Load the SLE service instances
        loadSleServiceInstances(driverConfigurationDirectory + File.separator + SLE_FOLDER);
        // Load the TM Data Link processor
        loadTmDataLinkProcessor();
        // Load the TM Packet processor
        loadTmPacketProcessor();
        // Ready to go
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
