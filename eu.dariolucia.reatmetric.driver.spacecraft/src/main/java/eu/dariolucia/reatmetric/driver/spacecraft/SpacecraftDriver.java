/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.spacecraft;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.impl.FieldGroupBasedPacketIdentifier;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.IPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.PacketDefinitionIndexer;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.UtlConfigurationFile;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.rcf.RcfServiceInstanceConfiguration;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IActivityExecutor;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.tcframe.ITcFrameConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket.ITcPacketConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.DataUnitType;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.ExternalConnectorConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.ServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.ITcPacketInjector;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.TcPacketProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.TmPacketProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.replay.TmPacketReplayManager;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.CltuServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.RafServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.RcfServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.sle.SleServiceInstanceManager;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TcDataLinkProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmDataLinkProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
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
 *     Service 5, TM only.</li>
 *     <li>TC Packet Layer: the driver supports the encoding of Space Packets with optional ECSS PUS header, starting from
 *     requests coming from the processing model. Space packets are encoded in TC packets with optional segment header.
 *     Support for PUS services is limited to Service 1 and Service 11, both with limitations</li>
 * </ul>
 *
 */
public class SpacecraftDriver implements IDriver, IRawDataRenderer, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(SpacecraftDriver.class.getName());

    private static final String SLE_FOLDER = "sle";
    private static final String CONFIGURATION_FILE = "configuration.xml";
    private static final String ENCODING_DECODING_DEFINITION_FILE = "tmtc.xml";

    private static final int MAX_TC_PACKET_SIZE = 65536;

    private String name;
    private Instant epoch;
    private SpacecraftConfiguration configuration;
    private ServiceCoreConfiguration coreConfiguration;
    private IServiceCoreContext context;
    private IDriverListener listener;

    private Definition encodingDecodingDefinitions;
    private IPacketIdentifier packetIdentifier;
    private DefaultPacketDecoder packetDecoder;
    private DefaultPacketEncoder packetEncoder;

    private volatile SystemStatus status = SystemStatus.UNKNOWN;

    private List<SleServiceInstanceManager<?,?>> sleManagers;
    private TmPacketReplayManager tmPacketReplayer;
    private TmDataLinkProcessor tmDataLinkProcessor;
    private TmPacketProcessor tmPacketProcessor;

    private TcPacketProcessor tcPacketProcessor;
    private TcDataLinkProcessor tcDataLinkProcessor;

    private ServiceBroker serviceBroker;

    private final Map<String, Function<RawData, LinkedHashMap<String, String>>> rawDataRenderers = new TreeMap<>();
    private IRawDataArchive rawDataArchive; // Needed to retrieve raw data without contents, for rendering

    private final Map<String, List<IActivityExecutor>> activityType2executors = new TreeMap<>();

    private final List<ICltuConnector> cltuConnectors = new LinkedList<>();
    private final List<ITcFrameConnector> tcFrameConnectors = new LinkedList<>();
    private final List<ITcPacketConnector> tcPacketConnectors = new LinkedList<>();
    // The next field is constructed upon getTransportConnectors invocation
    private List<ITransportConnector> allConnectors;


    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.coreConfiguration = coreConfiguration;
        this.listener = subscriber;
        this.rawDataArchive = context.getArchive() != null ? context.getArchive().getArchive(IRawDataArchive.class) : null;
        try {
            // Load the driver configuration
            loadDriverConfiguration(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE);
            // Load encoding/decoding definitions
            loadEncodingDecodingDefinitions(driverConfigurationDirectory + File.separator + ENCODING_DECODING_DEFINITION_FILE);
            // Load the service broker
            loadServiceBroker();
            // Load the different packet services
            loadPacketServices();
            // Load the TM Packet processor
            loadTmPacketProcessor();
            // Load the TM Data Link processor
            loadTmDataLinkProcessor();
            // Load the SLE service instances
            loadSleServiceInstances(driverConfigurationDirectory + File.separator + SLE_FOLDER);
            // Load the external connectors
            loadExternalConnectors();
            // Load the TC Data Link processor
            loadTcDataLinkProcessor();
            // Load the TC Packet processor
            loadTcPacketProcessor();
            // Load packet replayer
            loadTmPacketReplayer();
            // Initialise raw data renderers
            loadRawDataRenderers();
            // Finalise service loading
            finaliseServiceLoading();
            // Ready to go
            updateStatus(SystemStatus.NOMINAL);
        } catch (Exception e) {
            updateStatus(SystemStatus.ALARM);
            throw new DriverException(e);
        }
    }

    private void finaliseServiceLoading() {
        this.serviceBroker.finaliseServiceLoading();
    }

    private void loadExternalConnectors() {
        for(ExternalConnectorConfiguration ecc : configuration.getExternalConnectorConfigurations()) {
            loadExternalConnector(ecc);
        }
    }

    private void loadExternalConnector(ExternalConnectorConfiguration dc) {
        if(dc.getDataUnitType() == DataUnitType.CLTU) {
            ServiceLoader<ICltuConnector> serviceLoader = ServiceLoader.load(ICltuConnector.class);
            Optional<ServiceLoader.Provider<ICltuConnector>> provider = serviceLoader.stream().filter(pr -> pr.type().getName().equals(dc.getType())).findFirst();
            if (provider.isPresent()) {
                ICltuConnector connector = provider.get().get();
                connector.configure(this.name, this.configuration, this.context, dc.getConfiguration());
                connector.prepare();
                cltuConnectors.add(connector);
            } else {
                LOG.log(Level.WARNING, "External CLTU connector for class " + dc.getType() + " and configuration " + dc.getConfiguration() + " not found");
            }
        } else if(dc.getDataUnitType() == DataUnitType.TC_FRAME) {
            ServiceLoader<ITcFrameConnector> serviceLoader = ServiceLoader.load(ITcFrameConnector.class);
            Optional<ServiceLoader.Provider<ITcFrameConnector>> provider = serviceLoader.stream().filter(pr -> pr.type().getName().equals(dc.getType())).findFirst();
            if (provider.isPresent()) {
                ITcFrameConnector connector = provider.get().get();
                connector.configure(this.name, this.configuration, this.context, dc.getConfiguration());
                connector.prepare();
                tcFrameConnectors.add(connector);
            } else {
                LOG.log(Level.WARNING, "External TC frame connector for class " + dc.getType() + " and configuration " + dc.getConfiguration() + " not found");
            }
        } else if(dc.getDataUnitType() == DataUnitType.TC_PACKET) {
            ServiceLoader<ITcPacketConnector> serviceLoader = ServiceLoader.load(ITcPacketConnector.class);
            Optional<ServiceLoader.Provider<ITcPacketConnector>> provider = serviceLoader.stream().filter(pr -> pr.type().getName().equals(dc.getType())).findFirst();
            if (provider.isPresent()) {
                ITcPacketConnector connector = provider.get().get();
                connector.configure(this.name, this.configuration, this.context, this.serviceBroker, this.packetIdentifier, dc.getConfiguration());
                connector.prepare();
                tcPacketConnectors.add(connector);
            } else {
                LOG.log(Level.WARNING, "External TC packet connector for class " + dc.getType() + " and configuration " + dc.getConfiguration() + " not found");
            }
        }
    }

    private void loadTcDataLinkProcessor() {
        tcDataLinkProcessor = new TcDataLinkProcessor(configuration, context, serviceBroker, getCltuConnectors(), getTcFrameConnectors());
        registerActivityExecutor(tcDataLinkProcessor);
    }

    private List<ITcFrameConnector> getTcFrameConnectors() {
        return this.tcFrameConnectors;
    }

    private List<ICltuConnector> getCltuConnectors() {
        List<ICltuConnector> connectors = new LinkedList<>();
        connectors.addAll(this.sleManagers.stream().filter(o -> o instanceof CltuServiceInstanceManager).map(o -> (CltuServiceInstanceManager) o).collect(Collectors.toList()));
        connectors.addAll(this.cltuConnectors);
        return connectors;
    }

    private void loadTcPacketProcessor() {
        this.tcPacketProcessor = new TcPacketProcessor(this.name, this.epoch, this.configuration, this.context, this.serviceBroker, this.encodingDecodingDefinitions, this.tcDataLinkProcessor, getTcPacketConnectors());
        registerActivityExecutor(tcPacketProcessor);
        this.serviceBroker.registerServiceInterface(ITcPacketInjector.class, tcPacketProcessor);
    }

    private List<ITcPacketConnector> getTcPacketConnectors() {
        return this.tcPacketConnectors;
    }

    private void registerActivityExecutor(IActivityExecutor executor) {
        for(String type : executor.getSupportedActivityTypes()) {
            activityType2executors.computeIfAbsent(type, o -> new LinkedList<>()).add(executor);
        }
    }

    private void loadRawDataRenderers() {
        this.rawDataRenderers.put(Constants.T_TM_FRAME, tmDataLinkProcessor::renderTmFrame);
        this.rawDataRenderers.put(Constants.T_BAD_TM, tmDataLinkProcessor::renderBadTm);
        this.rawDataRenderers.put(Constants.T_AOS_FRAME, tmDataLinkProcessor::renderAosFrame);
        this.rawDataRenderers.put(Constants.T_TM_PACKET, tmPacketProcessor::renderTmPacket);
        this.rawDataRenderers.put(Constants.T_IDLE_PACKET, tmPacketProcessor::renderTmPacket);
        this.rawDataRenderers.put(Constants.T_BAD_PACKET, tmPacketProcessor::renderBadPacket);
        this.rawDataRenderers.put(Constants.T_TC_PACKET, tcPacketProcessor::renderTcPacket);

        Map<String, Function<RawData, LinkedHashMap<String, String>>> serviceRenderers = this.serviceBroker.getServiceRenderers();
        this.rawDataRenderers.putAll(serviceRenderers);
    }

    private void loadTmPacketReplayer() {
        this.tmPacketReplayer = new TmPacketReplayManager(name, configuration, context.getRawDataBroker());
        this.tmPacketReplayer.prepare();
    }

    private void loadPacketServices() throws ReatmetricException {
        for(ServiceConfiguration sc : configuration.getPacketServiceConfiguration().getServices()) {
            IService theService = loadService(sc);
            serviceBroker.registerService(theService);
            LOG.info(theService.getName() + " registered");
        }
    }

    private IService loadService(ServiceConfiguration dc) throws ReatmetricException {
        ServiceLoader<IService> serviceLoader = ServiceLoader.load(IService.class);
        Optional<ServiceLoader.Provider<IService>> provider = serviceLoader.stream().filter(pr -> pr.type().getName().equals(dc.getType())).findFirst();
        IService theService = null;
        if(provider.isPresent()) {
            theService = provider.get().get();
            theService.initialise(dc.getConfiguration(), this.name, this.configuration, this.coreConfiguration, this.context, this.serviceBroker);
        }
        return theService;
    }

    private void loadServiceBroker() {
        this.serviceBroker = new ServiceBroker();
        this.serviceBroker.registerServiceInterface(IPacketEncoder.class, this.packetEncoder);
        this.serviceBroker.registerServiceInterface(IPacketDecoder.class, this.packetDecoder);
        this.serviceBroker.registerServiceInterface(IPacketIdentifier.class, this.packetIdentifier);
    }

    private void loadTmPacketProcessor() {
        this.tmPacketProcessor = new TmPacketProcessor(this.configuration,
                this.context,
                this.serviceBroker);
        this.tmPacketProcessor.initialise();
    }

    private void loadEncodingDecodingDefinitions(String filePath) throws IOException {
        LOG.info("Loading TM/TC packet configuration at " + filePath);
        this.encodingDecodingDefinitions = Definition.load(new FileInputStream(filePath));
        this.packetIdentifier = new FieldGroupBasedPacketIdentifier(this.encodingDecodingDefinitions, true, Collections.singletonList(Constants.ENCDEC_TM_PACKET_TYPE));
        this.packetDecoder = new DefaultPacketDecoder(new PacketDefinitionIndexer(encodingDecodingDefinitions), epoch);
        this.packetEncoder = new DefaultPacketEncoder(new PacketDefinitionIndexer(encodingDecodingDefinitions), MAX_TC_PACKET_SIZE, epoch);
    }

    private void loadTmDataLinkProcessor() {
        this.tmDataLinkProcessor = new TmDataLinkProcessor(this.name, this.configuration,
                this.context,
                this.packetIdentifier,
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
                } else if(sic instanceof CltuServiceInstanceConfiguration) {
                    createCltuServiceInstance(confFile.getPeerConfiguration(), (CltuServiceInstanceConfiguration) sic);
                } else {
                    LOG.warning("Driver " + this.name + " cannot load service instance configuration for " + sic.getServiceInstanceIdentifier() + " in file " + sleConfFile + ": SLE service type not supported");
                }
            }
        }
    }

    private void createCltuServiceInstance(PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration sic) {
        LOG.info("Creating SLE CLTU endpoint for " + sic.getServiceInstanceIdentifier());
        CltuServiceInstanceManager m = new CltuServiceInstanceManager(this.name, peerConfiguration, sic, configuration, context);
        m.configure(this.name, configuration, context, null);
        m.prepare();
        this.sleManagers.add(m);
        // Register as activity executor
        registerActivityExecutor(m);
    }

    private void createRcfServiceInstance(PeerConfiguration peerConfiguration, RcfServiceInstanceConfiguration sic) {
        LOG.info("Creating SLE RCF endpoint for " + sic.getServiceInstanceIdentifier());
        RcfServiceInstanceManager m = new RcfServiceInstanceManager(this.name, peerConfiguration, sic, configuration, context);
        m.prepare();
        this.sleManagers.add(m);
    }

    private void createRafServiceInstance(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration sic) {
        LOG.info("Creating SLE RAF endpoint for " + sic.getServiceInstanceIdentifier());
        RafServiceInstanceManager m = new RafServiceInstanceManager(this.name, peerConfiguration, sic, configuration, context);
        m.prepare();
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
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.singletonList(this);
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this);
    }

    @Override
    public synchronized List<ITransportConnector> getTransportConnectors() {
        if(allConnectors == null) {
            allConnectors = this.sleManagers.stream().map(o -> (ITransportConnector) o).collect(Collectors.toCollection(LinkedList::new));
            allConnectors.add(this.tmPacketReplayer);
            allConnectors.addAll(this.cltuConnectors);
            allConnectors.addAll(this.tcFrameConnectors);
            allConnectors.addAll(this.tcPacketConnectors);
        }
        return Collections.unmodifiableList(allConnectors);
    }

    @Override
    public void dispose() {
        this.sleManagers.forEach(SleServiceInstanceManager::abort);
        this.sleManagers.clear();
        this.tmPacketReplayer.dispose();
        this.tmDataLinkProcessor.dispose();
        this.tmPacketProcessor.dispose();
        this.serviceBroker.dispose();
        this.tcPacketProcessor.dispose();
        this.tcDataLinkProcessor.dispose();
        updateStatus(SystemStatus.UNKNOWN);
    }

    private void updateStatus(SystemStatus s) {
        boolean toNotify = s != this.status;
        this.status = s;
        if(toNotify) {
            this.listener.driverStatusUpdate(this.name, this.status);
        }
    }

    @Override
    public String getHandler() {
        return this.name;
    }

    @Override
    public List<String> getSupportedTypes() {
        return new ArrayList<>(rawDataRenderers.keySet());
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException {
        if(!rawData.getHandler().equals(getHandler())) {
            throw new ReatmetricException("Raw data with handler " + rawData.getHandler() + " cannot be processed by driver " + configuration.getName() + ", expecting handler " + getHandler());
        }
        Function<RawData, LinkedHashMap<String, String>> renderingFunction = rawDataRenderers.get(rawData.getType());
        if(renderingFunction == null) {
            throw new ReatmetricException("Raw data with type " + rawData.getType() + " cannot be processed by driver " + configuration.getName() + ", expecting types " + getSupportedTypes());
        }
        // Ok, now check if raw data has contents. If not, retrieve the one with the contents.
        if(!rawData.isContentsSet() && rawDataArchive != null) {
            rawData = rawDataArchive.retrieve(rawData.getInternalId());
        }
        return renderingFunction.apply(rawData);
    }

    @Override
    public void registerModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public List<String> getSupportedRoutes() {
        Set<String> routes = new HashSet<>();
        for(Map.Entry<String, List<IActivityExecutor>> entry : activityType2executors.entrySet()) {
            for(IActivityExecutor ex : entry.getValue()) {
                routes.addAll(ex.getSupportedRoutes());
            }
        }
        return new ArrayList<>(routes);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return new ArrayList<>(activityType2executors.keySet());
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        String type = activityInvocation.getType();
        List<IActivityExecutor> executors = activityType2executors.get(type);
        if(executors == null || executors.isEmpty()) {
            throw new ActivityHandlingException("Cannot find any executor to handle activity " + activityInvocation.getPath() + " type " + type);
        }
        for(IActivityExecutor ex : executors) {
            if(ex.getSupportedRoutes().contains(activityInvocation.getRoute())) {
                ex.executeActivity(activityInvocation);
                return;
            }
        }
        throw new ActivityHandlingException("Cannot find any executor to handle activity " + activityInvocation.getPath() + " type " + type + " for route " + activityInvocation.getRoute());
    }

    @Override
    public boolean getRouteAvailability(String route) {
        Optional<SleServiceInstanceManager<?, ?>> first = this.sleManagers.stream()
                .filter(o -> o instanceof CltuServiceInstanceManager)
                .filter(o -> o.getServiceInstanceIdentifier().equals(route))
                .findFirst();
        if(first.isPresent()) {
            return first.get().getConnectionStatus().equals(TransportConnectionStatus.OPEN);
        } else {
            for(ICltuConnector conn : cltuConnectors) {
                if(conn.getSupportedRoutes().contains(route)) {
                    return conn.getConnectionStatus().equals(TransportConnectionStatus.OPEN);
                }
            }
            for(ITcFrameConnector conn : tcFrameConnectors) {
                if(conn.getSupportedRoutes().contains(route)) {
                    return conn.getConnectionStatus().equals(TransportConnectionStatus.OPEN);
                }
            }
            for(ITcPacketConnector conn : tcPacketConnectors) {
                if(conn.getSupportedRoutes().contains(route)) {
                    return conn.getConnectionStatus().equals(TransportConnectionStatus.OPEN);
                }
            }
        }
        return false;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        List<DebugInformation> toReturn = new ArrayList<>();
        toReturn.addAll(this.tmDataLinkProcessor.currentDebugInfo()); // # of frames and extracted packets per second
        toReturn.addAll(this.tmPacketProcessor.currentDebugInfo()); // # of extracted parameter samples per second
        return toReturn;
    }
}
