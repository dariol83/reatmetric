/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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
 */

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.RawTransferFrame;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Shared publication and annotation rules for reception-only frame connectors. */
final class ReceptionFrameMapper {

    enum DataType {
        TM_FRAME,
        AOS_FRAME,
        RAW
    }

    private static final String METADATA_SPACECRAFT_ID_KEY = "spacecraft_id";
    private static final String METADATA_VCID_KEY = "vcid";
    private static final String METADATA_VIRTUAL_CHANNEL_ID_KEY = "virtual_channel_id";
    private static final String METADATA_NAME_KEY = "name";
    private static final String METADATA_PACKET_NAME_KEY = "packet_name";
    private static final String METADATA_FRAME_TYPE_KEY = "frame_type";

    private final SpacecraftConfiguration configuration;
    private final IRawDataBroker broker;
    private final String driverName;
    private final boolean live;

    ReceptionFrameMapper(SpacecraftConfiguration configuration, IRawDataBroker broker, String driverName, boolean live) {
        this.configuration = configuration;
        this.broker = broker;
        this.driverName = driverName;
        this.live = live;
    }

    RawData map(DataType dataType, byte[] payload, Instant receptionTime, Quality quality, String route,
                Map<String, Object> metadata, String frameType) {
        Instant generationTime = receptionTime.minus(configuration.getPropagationDelay(), ChronoUnit.MICROS);
        switch (dataType) {
            case TM_FRAME:
                return mapTmFrame(payload, metadata, generationTime, receptionTime, quality, route);
            case AOS_FRAME:
                return mapAosFrame(payload, metadata, generationTime, receptionTime, quality, route);
            case RAW:
                return mapRaw(payload, metadata, frameType, generationTime, receptionTime, quality, route);
            default:
                throw new IllegalStateException("Unsupported data type " + dataType);
        }
    }

    private RawData mapTmFrame(byte[] payload, Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                               Quality quality, String route) {
        TmTransferFrame frame = new TmTransferFrame(payload, configuration.getTmDataLinkConfigurations().isFecfPresent());
        return mapTransferFrame(frame, Constants.T_TM_FRAME, payload, metadata, generationTime, receptionTime, quality, route);
    }

    private RawData mapAosFrame(byte[] payload, Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                                Quality quality, String route) {
        AosTransferFrame frame = new AosTransferFrame(payload,
                configuration.getTmDataLinkConfigurations().isAosFrameHeaderErrorControlPresent(),
                configuration.getTmDataLinkConfigurations().getAosTransferFrameInsertZoneLength(),
                AosTransferFrame.UserDataType.M_PDU,
                configuration.getTmDataLinkConfigurations().isOcfPresent(),
                configuration.getTmDataLinkConfigurations().isFecfPresent());
        return mapTransferFrame(frame, Constants.T_AOS_FRAME, payload, metadata, generationTime, receptionTime, quality, route);
    }

    private RawData mapTransferFrame(AbstractTransferFrame frame, String frameType, byte[] payload,
                                     Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                                     Quality quality, String route) {
        if (frame.isFecfPresent() && !frame.isValid()) {
            quality = Quality.BAD;
        }
        String source = String.valueOf(numberFromMetadata(metadata, METADATA_SPACECRAFT_ID_KEY, frame.getSpacecraftId()).intValue());
        String type = quality == Quality.BAD ? Constants.T_BAD_TM : frameType;
        RawData rawData = newRawData(generationTime, Constants.N_TM_TRANSFER_FRAME, type, route, source, quality, payload, receptionTime, null);
        annotateFrame(frame, rawData, quality);
        rawData.setData(frame);
        return rawData;
    }

    private RawData mapRaw(byte[] payload, Map<String, Object> metadata, String frameType, Instant generationTime, Instant receptionTime,
                           Quality quality, String route) {
        int spacecraftId = numberFromMetadata(metadata, METADATA_SPACECRAFT_ID_KEY, configuration.getId()).intValue();
        Number vcid = numberFromMetadata(metadata, METADATA_VCID_KEY, null);
        if (vcid == null) {
            vcid = numberFromMetadata(metadata, METADATA_VIRTUAL_CHANNEL_ID_KEY, null);
        }
        String source = String.valueOf(spacecraftId);
        String name = firstString(metadata, Constants.N_UNKNOWN_FRAME, METADATA_NAME_KEY, METADATA_PACKET_NAME_KEY);
        // Opaque data is assumed valid unless the decoder explicitly reported bad quality.
        RawTransferFrame unit = new RawTransferFrame((short) spacecraftId, frameType, payload, false, quality != Quality.BAD);
        if (vcid != null) {
            unit.setAnnotationValue(Constants.ANNOTATION_VCID, vcid.intValue());
        }
        RawData rawData = newRawData(generationTime, name, Constants.T_RAW_FRAME, route, source, quality, payload, receptionTime,
                metadata.getOrDefault(METADATA_FRAME_TYPE_KEY, Constants.N_UNKNOWN_FRAME).toString());
        annotateFrame(unit, rawData, quality);
        rawData.setData(unit);
        return rawData;
    }

    private void annotateFrame(AbstractTransferFrame frame, RawData rawData, Quality frameQuality) {
        frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, rawData.getRoute());
        frame.setAnnotationValue(Constants.ANNOTATION_SOURCE, rawData.getSource());
        frame.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rawData.getGenerationTime());
        frame.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rawData.getReceptionTime());
        frame.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rawData.getInternalId());
        frame.setAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY, frameQuality.ordinal());
    }

    private RawData newRawData(Instant generationTime, String name, String type, String route, String source,
                               Quality quality, byte[] contents, Instant receptionTime, Object extension) {
        return new RawData(broker.nextRawDataId(), generationTime, name, type, route, source, quality, null,
                contents, receptionTime, driverName, live, extension);
    }

    private Number numberFromMetadata(Map<String, Object> metadata, String key, Number fallback) {
        Object value = metadata.get(key);
        return value instanceof Number ? (Number) value : fallback;
    }

    private String firstString(Map<String, Object> metadata, String fallback, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String && !((String) value).isBlank()) {
                return (String) value;
            }
        }
        return fallback;
    }
}
