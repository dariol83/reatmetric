/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.ccsds.tmtc.coding.decoder.TmRandomizerDecoder;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This connector established a TCP/IP connection to a host on a given port, and uses this connection to:
 * <ul>
 *     <li>Receive CADUs</li>
 *     <li>Send CLTUs</li>
 * </ul>
 */
public class CltuCaduTcpConnector extends AbstractFullDuplexTcpConnector implements ICltuConnector {

    private int caduLength;
    private int asmLength;
    private TmRandomizerDecoder derandomizer;
    private final List<IForwardDataUnitStatusSubscriber> subscribers = new CopyOnWriteArrayList<>();

    public CltuCaduTcpConnector() {
        super("CLTU/CADU Connector", "CLTU/CADU Connector");
    }

    @Override
    protected void processDataUnit(byte[] dataUnit, Instant receptionTime) {
        // Remove ASM and correction codeblock
        dataUnit = Arrays.copyOfRange(dataUnit, asmLength, asmLength + getSpacecraftConfig().getTmDataLinkConfigurations().getFrameLength());
        // If randomisation is active, derandomise
        if(getSpacecraftConfig().getTmDataLinkConfigurations().isDerandomize()) {
            dataUnit = derandomizer.apply(dataUnit);
        }
        // Build transfer frame info with configuration from spacecraft
        AbstractTransferFrame frame = null;
        if(getSpacecraftConfig().getTmDataLinkConfigurations().getType() == TransferFrameType.TM) {
            frame = new TmTransferFrame(dataUnit, getSpacecraftConfig().getTmDataLinkConfigurations().isFecfPresent());
        } else if(getSpacecraftConfig().getTmDataLinkConfigurations().getType() == TransferFrameType.AOS) {
            frame = new AosTransferFrame(dataUnit, getSpacecraftConfig().getTmDataLinkConfigurations().isFecfPresent(),
                    getSpacecraftConfig().getTmDataLinkConfigurations().getAosTransferFrameInsertZoneLength(),
                    AosTransferFrame.UserDataType.M_PDU, getSpacecraftConfig().getTmDataLinkConfigurations().isOcfPresent(),
                    getSpacecraftConfig().getTmDataLinkConfigurations().isFecfPresent());
        } else {
            throw new IllegalArgumentException("Transfer frame type " + getSpacecraftConfig().getTmDataLinkConfigurations().getType() + " not supported");
        }
        // Distribute to raw data broker
        RawData rd = null;
        Instant genTimeInstant = receptionTime.minusNanos(getSpacecraftConfig().getPropagationDelay() * 1000);
        String source = String.valueOf(frame.getSpacecraftId());
        if (frame.isValid()) {
            if(getSpacecraftConfig().getTmDataLinkConfigurations().getProcessVcs() == null ||
                    getSpacecraftConfig().getTmDataLinkConfigurations().getProcessVcs().contains((int) frame.getVirtualChannelId())) {
                String route = String.valueOf(frame.getSpacecraftId()) + '.' + frame.getVirtualChannelId() + ".TCP." + getHost() + '.' + getPort();
                rd = createRawData(genTimeInstant,
                        Constants.N_TM_TRANSFER_FRAME,
                        getSpacecraftConfig().getTmDataLinkConfigurations().getType() == TransferFrameType.TM ? Constants.T_TM_FRAME : Constants.T_AOS_FRAME,
                        route,
                        source,
                        Quality.GOOD, null, frame.getFrame(), receptionTime, null);
            }  // else do not process the frame, discard it
        } else {
            String route = "TCP." + getHost() + '.' + getPort();
            rd = createRawData(genTimeInstant,
                    Constants.N_TM_TRANSFER_FRAME,
                    Constants.T_BAD_TM,
                    route,
                    source,
                    Quality.BAD, null, frame.getFrame(), receptionTime, null);
        }
        // Distribute to broker
        if(rd != null) {
            // Set annotations
            frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, rd.getRoute());
            frame.setAnnotationValue(Constants.ANNOTATION_SOURCE, rd.getSource());
            frame.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rd.getGenerationTime());
            frame.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rd.getReceptionTime());
            frame.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rd.getInternalId());
            rd.setData(frame);
            distributeRawData(rd);
        }
    }

    @Override
    protected byte[] readDataUnit(InputStream inputStream) throws Exception {
        byte[] cadu = inputStream.readNBytes(this.caduLength);
        if(cadu.length == 0) {
            throw new IOException("End of stream");
        }
        return cadu;
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation) throws RemoteException {
        // Call configuration for parent class
        internalConfigure(driverName, configuration, context);
        // Parse configuration for this class
        String[] infoSpl = connectorInformation.split(":", -1); // String with format: "<ip>:<port>:<CADU length>:<asm length>"
        // Set host and port in parent class
        setConnectionInformation(infoSpl[0], Integer.parseInt(infoSpl[1]));
        // Set the remaining configuration data
        this.caduLength = Integer.parseInt(infoSpl[2]);
        this.asmLength = Integer.parseInt(infoSpl[3]);
        this.derandomizer = new TmRandomizerDecoder();
    }

    @Override
    public void register(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException {
        this.subscribers.add(subscriber);
    }

    @Override
    public void deregister(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void sendCltu(byte[] cltu, long externalId) throws RemoteException {
        // Not required to provide activity ID and activity occurrence ID (not needed at this stage)
        sendDataUnit(cltu, externalId, -1, null);
    }

    @Override
    protected void reportActivityRelease(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime, ActivityReportState status) {
        // Report status to CLTU subscribers
        long externalId = (long) trackingInformation;
        informSubscribers(externalId, status == ActivityReportState.OK ? ForwardDataUnitProcessingStatus.RELEASED : ForwardDataUnitProcessingStatus.RELEASE_FAILED, progressTime, null, status == ActivityReportState.OK ? Constants.STAGE_ENDPOINT_RECEPTION : null);
        // No need to inform the processing model, it will be done by the subscriber, super method not invoked
    }

    @Override
    protected void reportActivityReceptionOk(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        // Report status to CLTU subscribers
        long externalId = (long) trackingInformation;
        informSubscribers(externalId, ForwardDataUnitProcessingStatus.UPLINKED, progressTime, Constants.STAGE_ENDPOINT_RECEPTION, null);
        // No need to inform the processing model, it will be done by the subscriber, super method not invoked
    }

    @Override
    protected void reportActivityReceptionFailure(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        // Report status to CLTU subscribers
        long externalId = (long) trackingInformation;
        informSubscribers(externalId, ForwardDataUnitProcessingStatus.UPLINK_FAILED, progressTime, Constants.STAGE_ENDPOINT_RECEPTION, null);
        // No need to inform the processing model, it will be done by the subscriber, super method not invoked
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status, Instant time, String currentState, String nextState) {
        subscribers.forEach(o -> o.informStatusUpdate(externalId, status, time, currentState, nextState));
    }

    @Override
    public List<String> getSupportedRoutes() throws RemoteException {
        return Collections.singletonList("CLTU/CADU @ " + getHost() + ":" + getPort());
    }
}
