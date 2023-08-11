package eu.dariolucia.reatmetric.driver.socket.connection;

import eu.dariolucia.reatmetric.driver.socket.configuration.ConnectionConfiguration;

public abstract class AbstractConnectionHandler implements IConnectionHandler {

    private final ConnectionConfiguration configuration;

    public AbstractConnectionHandler(ConnectionConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ConnectionConfiguration getConfiguration() {
        return configuration;
    }
}
