/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.identifier.impl.FieldGroupBasedPacketIdentifier;
import eu.dariolucia.ccsds.encdec.structure.PacketDefinitionIndexer;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.UtlConfigurationFile;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.rcf.RcfServiceInstanceConfiguration;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.TmPacketProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.CommandVerificationService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.OnboardEventService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.TimeCorrelationService;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.RafServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.RcfServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.SleServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmDataLinkProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
 *
 * TODO: support to start at time T (e.g. initialisation of the time correlation service) from data coming from a specified archive (reatmetric.core system properties)
 * TODO: support to not process data from specific VCs (up to packet extraction, no packet processing) - avoid that playback data pollutes live data
 * TODO: support for packet replay from a given time, from data in one archive (implement additional transport connector)
 *
 */
public class SpacecraftDriver implements IDriver {

    private static final Logger LOG = Logger.getLogger(SpacecraftDriver.class.getName());

    private static final String SLE_FOLDER = "sle";
    private static final String CONFIGURATION_FILE = "configuration.xml";
    private static final String ENCODING_DECODING_DEFINITION_FILE = "tmtc.xml";

    private String name;
    private Instant epoch;
    private SpacecraftConfiguration configuration;
    private ServiceCoreConfiguration coreConfiguration;
    private IServiceCoreContext context;
    private IDriverListener listener;
    private Definition encodingDecodingDefinitions;

    private volatile SystemStatus status = SystemStatus.UNKNOWN;

    private List<SleServiceInstanceManager<?,?>> sleManagers;
    private TmDataLinkProcessor tmDataLinkProcessor;
    private TmPacketProcessor tmPacketProcessor;

    private ServiceBroker serviceBroker;
    private TimeCorrelationService timeCorrelationService;
    private OnboardEventService onboardEventService;
    private CommandVerificationService commandVerificationService;

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.coreConfiguration = coreConfiguration;
        this.listener = subscriber;
        try {
            // Load the driver configuration
            loadDriverConfiguration(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE);
            // Load encoding/decoding definitions
            loadEncodingDecodingDefinitions(driverConfigurationDirectory + File.separator + ENCODING_DECODING_DEFINITION_FILE);
            // Load the service broker
            loadServiceBroker();
            // Load the different TM packet services
            loadPacketServices();
            // Load the TM Packet processor
            loadTmPacketProcessor();
            // Load the TM Data Link processor
            loadTmDataLinkProcessor();
            // Load the SLE service instances
            loadSleServiceInstances(driverConfigurationDirectory + File.separator + SLE_FOLDER);
            // Ready to go
            updateStatus(SystemStatus.NOMINAL);
        } catch (Exception e) {
            updateStatus(SystemStatus.ALARM);
            throw new DriverException(e);
        }
    }

    private void loadPacketServices() {
        // Time correlation service (PUS 9) -> if start from time, initialise time coefficients
        this.timeCorrelationService = new TimeCorrelationService(this.configuration, this.context, this.serviceBroker);
        // On-board event service (PUS 5)
        this.onboardEventService = new OnboardEventService(this.configuration, this.context, this.serviceBroker);
        // Command verification service (PUS 1)
        this.commandVerificationService = new CommandVerificationService(this.configuration, this.context, this.serviceBroker);
    }

    private void loadServiceBroker() {
        this.serviceBroker = new ServiceBroker();
    }

    private void loadTmPacketProcessor() {
        this.tmPacketProcessor = new TmPacketProcessor(this.configuration,
                this.context,
                new DefaultPacketDecoder(new PacketDefinitionIndexer(encodingDecodingDefinitions), epoch),
                this.timeCorrelationService,
                this.serviceBroker);
        this.tmPacketProcessor.initialise();
    }

    private void loadEncodingDecodingDefinitions(String filePath) throws IOException {
        LOG.info("Loading TM/TC packet configuration at " + filePath);
        this.encodingDecodingDefinitions = Definition.load(new FileInputStream(filePath));
    }

    private void loadTmDataLinkProcessor() {
        this.tmDataLinkProcessor = new TmDataLinkProcessor(this.configuration,
                this.context,
                new FieldGroupBasedPacketIdentifier(this.encodingDecodingDefinitions, true, Collections.singletonList(Constants.ENCDEC_TM_PACKET_TYPE)),
                tmPacketProcessor::extractPacketGenerationTime,
                tmPacketProcessor::checkPacketQuality
                );
        this.tmDataLinkProcessor.initialise();
    }

    private void loadSleServiceInstances(String sleFolder) throws IOException {
        LOG.info("Loading SLE configuration at " + sleFolder);
        this.sleManagers = new ArrayList<>();
        if(sleFolder == null) {
            LOG.info("Driver " + this.name + " has no SLE folder configured. Skipping SLE configuration.");
            return;
        }
        File sleFolderFile = new File(sleFolder);
        if(!sleFolderFile.exists()) {
            LOG.warning("Driver " + this.name + " points to non-existing SLE folder: " + sleFolderFile.getAbsolutePath());
            return;
        }
        File[] files = sleFolderFile.listFiles();
        if(files == null) {
            LOG.warning("Driver " + this.name + " cannot read contents of SLE folder: " + sleFolderFile.getAbsolutePath());
            return;
        }
        for(File sleConfFile : files) {
            UtlConfigurationFile confFile = UtlConfigurationFile.load(new FileInputStream(sleConfFile));
            for(ServiceInstanceConfiguration sic : confFile.getServiceInstances()) {
                if(sic instanceof RafServiceInstanceConfiguration) {
                    createRafServiceInstance(confFile.getPeerConfiguration(), (RafServiceInstanceConfiguration) sic);
                } else if(sic instanceof RcfServiceInstanceConfiguration) {
                    createRcfServiceInstance(confFile.getPeerConfiguration(), (RcfServiceInstanceConfiguration) sic);
                } else {
                    LOG.warning("Driver " + this.name + " cannot load service instance configuration for " + sic.getServiceInstanceIdentifier() + " in file " + sleConfFile + ": SLE service type not supported");
                }
            }
        }
    }

    private void createRcfServiceInstance(PeerConfiguration peerConfiguration, RcfServiceInstanceConfiguration sic) {
        LOG.info("Creating SLE RCF endpoint for " + sic.getServiceInstanceIdentifier());
        RcfServiceInstanceManager m = new RcfServiceInstanceManager(peerConfiguration, sic, configuration, context.getRawDataBroker());
        this.sleManagers.add(m);
    }

    private void createRafServiceInstance(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration sic) {
        LOG.info("Creating SLE RAF endpoint for " + sic.getServiceInstanceIdentifier());
        RafServiceInstanceManager m = new RafServiceInstanceManager(peerConfiguration, sic, configuration, context.getRawDataBroker());
        this.sleManagers.add(m);
    }

    private void loadDriverConfiguration(String filePath) throws IOException {
        LOG.info("Loading driver configuration at " + filePath);
        this.configuration = SpacecraftConfiguration.load(new FileInputStream(filePath));
        // Optimise PUS configuration
        this.configuration.getTmPacketConfiguration().buildLookupMap();
        // Get SC epoch
        this.epoch = configuration.getEpoch() == null ? null : Instant.ofEpochMilli(configuration.getEpoch().getTime());
    }

    @Override
    public SystemStatus getDriverStatus() {
        return status;
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        // TODO implement command handler
        return Collections.emptyList();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return this.sleManagers.stream().map(o -> (ITransportConnector) o).collect(Collectors.toList());
    }

    @Override
    public void dispose() {
        this.sleManagers.forEach(SleServiceInstanceManager::abort);
        this.sleManagers.clear();
        this.tmDataLinkProcessor.dispose();
        this.tmPacketProcessor.dispose();
        this.timeCorrelationService.dispose();
        this.onboardEventService.dispose();
        this.commandVerificationService.dispose();
        this.serviceBroker.dispose();
        updateStatus(SystemStatus.UNKNOWN);
    }

    private void updateStatus(SystemStatus s) {
        boolean toNotify = s != this.status;
        this.status = s;
        if(toNotify) {
            this.listener.driverStatusUpdate(this.name, this.status);
        }
    }
}
