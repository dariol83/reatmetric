package eu.dariolucia.reatmetric.core.api;

import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A template implementation of a ReatMetric Core driver.
 */
public abstract class AbstractDriver implements IDriver {

    private static final Logger LOG = Logger.getLogger(AbstractDriver.class.getName());

    // Driver generic properties
    private String name;
    private String driverConfiguration;
    private IServiceCoreContext context;
    private ServiceCoreConfiguration coreConfiguration;
    private IDriverListener driverSubscriber;
    private volatile SystemStatus driverStatus;

    public AbstractDriver() {
        //
    }

    // --------------------------------------------------------------------
    // IDriver methods
    // --------------------------------------------------------------------

    @Override
    public void initialise(String name, String driverConfiguration, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.driverConfiguration = driverConfiguration;
        this.coreConfiguration = coreConfiguration;
        this.driverSubscriber = subscriber;
        setDriverStatus(SystemStatus.NOMINAL);
        try {
            // Read the specific configuration
            setDriverStatus(processConfiguration(driverConfiguration, coreConfiguration, context));
            // Start the driver functions
            setDriverStatus(startProcessing());
        } catch (DriverException e) {
            setDriverStatus(SystemStatus.ALARM);
            LOG.log(Level.SEVERE, "Error while initialising driver " + name + ": " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            setDriverStatus(SystemStatus.ALARM);
            LOG.log(Level.SEVERE, "Error while initialising driver " + name + ": " + e.getMessage(), e);
            throw new DriverException(e);
        }
    }

    protected final String getName() {
        return name;
    }

    protected final String getDriverConfiguration() {
        return driverConfiguration;
    }

    protected final IServiceCoreContext getContext() {
        return context;
    }

    protected final ServiceCoreConfiguration getCoreConfiguration() {
        return coreConfiguration;
    }

    /**
     * This method is used to update the driver status property and to trigger the notification of the driver subscriber
     * in case of different value.
     *
     * Subclasses can override, if needed, as long as they call super.setDriverStatus(newStatus)
     *
     * @param newStatus the new status of the driver
     */
    protected void setDriverStatus(SystemStatus newStatus) {
        if(this.driverStatus != newStatus) {
            this.driverStatus = newStatus;
            this.driverSubscriber.driverStatusUpdate(this.name, this.driverStatus);
        }
    }

    /**
     * This method must be implemented by subclasses, to perform all those activities that must be carried
     * out after the configuration of the driver, to start its internal processing.
     *
     * @return the status of the driver after the initialisation
     * @throws DriverException in case of issues that block the initialisation of the driver
     */
    protected abstract SystemStatus startProcessing() throws DriverException;

    /**
     * This method is called to perform the driver configuration reading/processing. At the end of the invocation,
     * it is expected that the driver is fully configured and its functions ready to be started.
     *
     * @param driverConfiguration the string provided for the configuration of the driver
     * @param coreConfiguration the (read-only) object describing the system configuration
     * @param context the {@link IServiceCoreContext} object
     * @return the status of the driver after the configuration
     * @throws DriverException in case of issues that block the configuration of the driver
     */
    protected abstract SystemStatus processConfiguration(String driverConfiguration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context) throws DriverException;

    @Override
    public SystemStatus getDriverStatus() {
        return this.driverStatus;
    }

    /**
     * Subclasses can override, if needed
     *
     * @return the list of {@link IRawDataRenderer}
     */
    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.emptyList();
    }

    /**
     * Subclasses can override, if needed
     *
     * @return the list of {@link IActivityHandler}
     */
    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.emptyList();
    }

    /**
     * Subclasses can override, if needed
     *
     * @return the list of {@link ITransportConnector}
     */
    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.emptyList();
    }

    /**
     * Subclasses can override, if needed
     */
    @Override
    public void dispose() throws DriverException {
        // Nothing to be done
    }
}
