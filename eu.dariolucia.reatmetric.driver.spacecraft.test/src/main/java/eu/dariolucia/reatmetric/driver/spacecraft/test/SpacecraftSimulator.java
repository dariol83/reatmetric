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

package eu.dariolucia.reatmetric.driver.spacecraft.test;

import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.UtlConfigurationFile;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuServiceInstanceProvider;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstanceProvider;

import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * A simple spacecraft simulator that generates random data, as defined by the processing model and TM/TC packet definition, using hardcoded
 * configuration for the TM/TC datalink and packet layers.
 *
 * With respect to the SLE configuration, the simulator uses a single RAF SI for TM, and a CLTU SI for CLTU reception. These SIs will be the
 * first ones that the system will locate in the provided SLE configuration file.
 *
 *
 */
public class SpacecraftSimulator {

    private static final Logger LOG = Logger.getLogger(SpacecraftSimulator.class.getName());

    public static void main(String[] args) throws IOException, JAXBException {
        if (args.length < 4) {
            System.err.println("Usage: SpacecraftSimulator <path to SLE configuration file> <path to TM/TC configuration file> <spacecraft configuration> <path to processing model> [TCP server port]");
            System.exit(1);
        }
        // Load the SLE configuration file
        UtlConfigurationFile sleConfFile = UtlConfigurationFile.load(new FileInputStream(args[0]));
        CltuServiceInstanceConfiguration cltuConf = null;
        RafServiceInstanceConfiguration rafConf = null;
        for (ServiceInstanceConfiguration sic : sleConfFile.getServiceInstances()) {
            if (cltuConf == null && sic instanceof CltuServiceInstanceConfiguration) {
                cltuConf = (CltuServiceInstanceConfiguration) sic;
            }
            if (rafConf == null && sic instanceof RafServiceInstanceConfiguration) {
                rafConf = (RafServiceInstanceConfiguration) sic;
            }
        }
        if (cltuConf == null) {
            System.err.println("Error: cannot find CLTU service instance in file " + args[0]);
            System.exit(1);
        }
        if (rafConf == null) {
            System.err.println("Error: cannot find RAF service instance in file " + args[0]);
            System.exit(1);
        }
        // Create the CLTU service instance
        CltuServiceInstanceProvider cltuServiceInstanceProvider = new CltuServiceInstanceProvider(sleConfFile.getPeerConfiguration(), cltuConf);
        cltuServiceInstanceProvider.configure();
        // Create the RAF service instance
        RafServiceInstanceProvider rafServiceInstanceProvider = new RafServiceInstanceProvider(sleConfFile.getPeerConfiguration(), rafConf);
        rafServiceInstanceProvider.configure();

        // Create the spacecraft emulator
        int tcpPort = args.length > 4 ? Integer.parseInt(args[4]) : -1;
        SpacecraftModel sm = new SpacecraftModel(args[1], args[2], cltuServiceInstanceProvider, rafServiceInstanceProvider, args[3], tcpPort);
        sm.startProcessing();

        // Exit when the user presses Enter
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();

        //
        sm.stopProcessing();
        // Bye
    }
}
