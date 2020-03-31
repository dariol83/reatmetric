/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft;

import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.UtlConfigurationFile;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.pdu.PduStringUtil;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstanceProvider;
import eu.dariolucia.reatmetric.api.value.StringUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.logging.Logger;

public class SpacecraftTmReplayer {

    private static final Logger LOG = Logger.getLogger(SpacecraftTmReplayer.class.getName());

    public static void main(String[] args) throws IOException {
        if(args.length != 3) {
            System.err.println("Usage: SpacecraftTmReplayer <path to SLE configuration file> <RAF SIID to use> <TM frame hex file to replay>");
            System.exit(1);
        }
        // Load the SLE configuration file
        UtlConfigurationFile sleConfFile = UtlConfigurationFile.load(new FileInputStream(args[0]));
        RafServiceInstanceConfiguration rafConf = null;
        for(ServiceInstanceConfiguration sic : sleConfFile.getServiceInstances()) {
            if(sic.getServiceInstanceIdentifier().equals(args[1]) && sic instanceof RafServiceInstanceConfiguration) {
                rafConf = (RafServiceInstanceConfiguration) sic;
                break;
            }
        }
        if(rafConf == null) {
            System.err.println("Error: cannot find service instance " + args[1] + " in file " + args[0]);
            System.exit(1);
        }
        // Create the RAF service instance
        RafServiceInstanceProvider rafServiceInstanceProvider = new RafServiceInstanceProvider(sleConfFile.getPeerConfiguration(), rafConf);
        rafServiceInstanceProvider.configure();
        // Register the frame producer so that it becomes active when the status of the service instance goes to ACTIVE and stops when it goes to not ACTIVE
        rafServiceInstanceProvider.register(new FrameReplayer(rafServiceInstanceProvider, args[2]));
        // Wait for BIND
        rafServiceInstanceProvider.waitForBind(true, null);
        // Exit when the user presses Enter
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();
        // Disconnect and dispose
        rafServiceInstanceProvider.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
        rafServiceInstanceProvider.dispose();
        // Bye
    }

    private static class FrameReplayer implements IServiceInstanceListener {
        private final RafServiceInstanceProvider provider;
        private final String tmFrameFile;
        private volatile boolean running = false;
        private volatile Thread fileSender = null;

        public FrameReplayer(RafServiceInstanceProvider provider, String tmFrameFile) {
            this.provider = provider;
            this.tmFrameFile = tmFrameFile;
        }

        @Override
        public void onStateUpdated(ServiceInstance si, ServiceInstanceState state) {
            switch(state.getState()) {
                case ACTIVE:
                    if(!running) {
                        startFileTransmission();
                    }
                    break;
                default:
                    if(running) {
                        stopFileTransmission();
                    }
                    break;
            }
        }

        private void stopFileTransmission() {
            if(!running) {
                return;
            }
            this.running = false;
        }

        private void startFileTransmission() {
            if(running) {
                return;
            }
            running = true;
            // Open file, it must contain 1 frame per line, hexdump, optionally prefixed with Instant toString() output and pipe: i.e. <yyyy-mm-dd'T'hh:mm:ss.SSSSSS'Z'>'|'
            fileSender = new Thread(() -> {
                try {
                    provider.updateProductionStatus(Instant.now(), LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, LockStatusEnum.IN_LOCK, ProductionStatusEnum.RUNNING);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.tmFrameFile)));
                    byte[] frame = null;
                    String line = null;
                    while((line = reader.readLine()) != null && running) {
                        int timeSep = line.indexOf('|');
                        Instant ert = Instant.now();
                        if(timeSep != -1) {
                            // There is a ERT time in the frame
                            ert = Instant.parse(line.substring(0, timeSep));
                            frame = StringUtil.toByteArray(line.substring(timeSep + 1));
                        } else {
                            frame = StringUtil.toByteArray(line);
                        }
                        provider.transferData(frame, ReturnServiceInstanceProvider.FRAME_QUALITY_GOOD, 0, ert, false, PduStringUtil.toHexDump("ANTENNA".getBytes()), false, null);
                        Thread.sleep(1000);
                    }
                    provider.endOfData();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    provider.updateProductionStatus(Instant.now(), LockStatusEnum.OUT_OF_LOCK, LockStatusEnum.OUT_OF_LOCK, LockStatusEnum.OUT_OF_LOCK, LockStatusEnum.OUT_OF_LOCK, ProductionStatusEnum.INTERRUPTED);
                    fileSender = null;
                }
            });
            fileSender.setDaemon(true);
            fileSender.start();
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
