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

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.incoming.pdus.CltuTransferDataInvocation;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.UtlConfigurationFile;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuServiceInstanceProvider;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuUplinkStatusEnum;
import eu.dariolucia.reatmetric.api.common.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class SpacecraftTcResponder {

    private static final Logger LOG = Logger.getLogger(SpacecraftTcResponder.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: SpacecraftTcResponder <path to SLE configuration file> <CLTU SIID to use>");
            System.exit(1);
        }
        // Load the SLE configuration file
        UtlConfigurationFile sleConfFile = UtlConfigurationFile.load(new FileInputStream(args[0]));
        CltuServiceInstanceConfiguration cltuConf = null;
        for (ServiceInstanceConfiguration sic : sleConfFile.getServiceInstances()) {
            if (sic.getServiceInstanceIdentifier().equals(args[1]) && sic instanceof CltuServiceInstanceConfiguration) {
                cltuConf = (CltuServiceInstanceConfiguration) sic;
                break;
            }
        }
        if (cltuConf == null) {
            System.err.println("Error: cannot find service instance " + args[1] + " in file " + args[0]);
            System.exit(1);
        }
        // Create the CLTU service instance
        CltuServiceInstanceProvider cltuServiceInstanceProvider = new CltuServiceInstanceProvider(sleConfFile.getPeerConfiguration(), cltuConf);
        cltuServiceInstanceProvider.configure();
        // Register the frame producer so that it becomes active when the status of the service instance goes to ACTIVE and stops when it goes to not ACTIVE
        cltuServiceInstanceProvider.register(new TcResponder(cltuServiceInstanceProvider));
        // Wait for BIND
        cltuServiceInstanceProvider.waitForBind(true, null);
        // Exit when the user presses Enter
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();
        // Disconnect and dispose
        cltuServiceInstanceProvider.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
        cltuServiceInstanceProvider.dispose();
        // Bye
    }

    private static class TcResponder implements IServiceInstanceListener {
        private static final int MAX_BUFFER = 10000;

        private final CltuServiceInstanceProvider provider;
        private final BlockingQueue<Pair<Long, byte[]>> cltus = new LinkedBlockingQueue<>();

        public TcResponder(CltuServiceInstanceProvider provider) {
            this.provider = provider;
            this.provider.setTransferDataOperationHandler(this::transferDataReceived);
            new Thread(this::radiateCltus).start();
            sendUplinkStatusOk();
        }

        private void radiateCltus() {
            while (true) {
                try {
                    Pair<Long, byte[]> cltuPair = cltus.take();
                    if (provider.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE) {
                        Date startTime = new Date();
                        provider.cltuProgress(cltuPair.getFirst(), CltuStatusEnum.PRODUCTION_STARTED, startTime, null, computeAvailableBuffer());
                        Thread.sleep(cltuPair.getSecond().length * 8);
                        provider.cltuProgress(cltuPair.getFirst(), CltuStatusEnum.RADIATED, startTime, new Date(), computeAvailableBuffer());
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
        }

        private Long transferDataReceived(CltuTransferDataInvocation cltuTransferDataInvocation) {
            long availBuff = computeAvailableBuffer();
            if (availBuff - cltuTransferDataInvocation.getCltuData().value.length < 0) {
                return -1L; // unable to store
            }
            // Add to queue
            this.cltus.add(Pair.of(cltuTransferDataInvocation.getCltuIdentification().longValue(), cltuTransferDataInvocation.getCltuData().value));
            return computeAvailableBuffer();
        }

        @Override
        public void onStateUpdated(ServiceInstance si, ServiceInstanceState state) {
            switch (state.getState()) {
                case UNBOUND:
                    cltus.clear();
                    break;
                default:
                    break;
            }
        }

        private void sendUplinkStatusOk() {
            provider.updateProductionStatus(CltuProductionStatusEnum.OPERATIONAL, CltuUplinkStatusEnum.NOMINAL, computeAvailableBuffer());
        }

        private long computeAvailableBuffer() {
            int used = cltus.stream().map(Pair::getSecond).map(o -> o.length).reduce(0, Integer::sum);
            return MAX_BUFFER - used;
        }

        @Override
        public void onPduReceived(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
            // Nothing
        }

        @Override
        public void onPduSent(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
            LOG.info("Sending PDU " + name);
        }

        @Override
        public void onPduSentError(ServiceInstance si, Object operation, String name, byte[] encodedOperation, String error, Exception exception) {
            // Nothing
        }

        @Override
        public void onPduDecodingError(ServiceInstance serviceInstance, byte[] encodedOperation) {
            // Nothing
        }

        @Override
        public void onPduHandlingError(ServiceInstance serviceInstance, Object operation, byte[] encodedOperation) {
            // Nothing
        }
    }
}
