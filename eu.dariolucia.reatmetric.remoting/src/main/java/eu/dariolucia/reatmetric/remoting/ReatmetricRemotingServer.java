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

package eu.dariolucia.reatmetric.remoting;

import eu.dariolucia.reatmetric.api.IReatmetricRegister;
import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that fetches all the ReatMetric registries, initialises the systems and registers them to a single registry, opened
 * on the specified port (first and only argument).
 */
public class ReatmetricRemotingServer {

    private static final Logger LOG = Logger.getLogger(ReatmetricRemotingServer.class.getName());
    private static final List<ReatmetricSystemRemoting> REMOTED_SYSTEMS = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws RemoteException {
        if(args.length < 1 || args.length > 2) {
            System.err.println("Usage: ReatmetricRemotingServer <port> [system name]");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String message = "ReatMetric Remoting Server launched, creating registry on port " + port;
        String systemName = null;
        if(args.length == 2) {
            systemName = args[1];
            message += " for system " + systemName;
        }
        LOG.info(message);
        Registry registry = LocateRegistry.createRegistry(port);

        LOG.info("Loading systems...");
        ServiceLoader<IReatmetricRegister> loader
                = ServiceLoader.load(IReatmetricRegister.class);
        for (IReatmetricRegister reg : loader) {
            List<IReatmetricSystem> systems;
            try {
                systems = reg.availableSystems();
                for(IReatmetricSystem cp : systems) {
                    String system = null;
                    try {
                        system = cp.getName();
                        if(systemName == null || systemName.equals(system)) {
                            LOG.info("Loading system " + system);
                            cp.initialise(ReatmetricRemotingServer::logSystemStatus);
                            ReatmetricSystemRemoting remoting = new ReatmetricSystemRemoting(registry, system, cp);
                            remoting.activate();
                            REMOTED_SYSTEMS.add(remoting);
                            LOG.info("System " + system + " registered");
                        } else {
                            LOG.info("System " + system + " ignored");
                        }
                    } catch (ReatmetricException | AlreadyBoundException | RemoteException e) {
                        LOG.log(Level.SEVERE, "Cannot load system " + system + " from registry " + reg + ": " + e.getMessage(), e);
                    }
                }
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot load systems from registry " + reg + ": " + e.getMessage(), e);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ReatmetricRemotingServer::shutdown));
    }

    private static void logSystemStatus(SystemStatus systemStatus) {
        LOG.info("Status: " + systemStatus);
    }

    public static void shutdown() {
        LOG.log(Level.INFO, "Shutting down remote systems");
        for (ReatmetricSystemRemoting remotedSystem : REMOTED_SYSTEMS) {
            try {
                remotedSystem.deactivate();
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
            try {
                remotedSystem.getSystem().dispose();
            } catch (ReatmetricException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
