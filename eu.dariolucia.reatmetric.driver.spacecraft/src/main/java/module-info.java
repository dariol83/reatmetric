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

import eu.dariolucia.reatmetric.driver.spacecraft.SpacecraftDriver;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketInfoValueExtensionHandler;
import eu.dariolucia.reatmetric.driver.spacecraft.connectors.CltuCaduTcpConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.connectors.SpacePacketTcpConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.encoding.SpacePacketDecodingExtension;
import eu.dariolucia.reatmetric.driver.spacecraft.encoding.SpacePacketEncodingExtension;
import eu.dariolucia.reatmetric.driver.spacecraft.connectors.TmPacketReplayConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.AesEncryptionService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.*;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptorValueExtensionHandler;

open module eu.dariolucia.reatmetric.driver.spacecraft {
    uses eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
    uses eu.dariolucia.reatmetric.driver.spacecraft.activity.tcframe.ITcFrameConnector;
    uses eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket.ITcPacketConnector;
    uses eu.dariolucia.reatmetric.driver.spacecraft.services.IService;
    uses eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
    uses eu.dariolucia.reatmetric.driver.spacecraft.common.IReceptionOnlyConnector;

    requires java.logging;
    requires jakarta.xml.bind;
    requires java.rmi;
    requires jasn1;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;

    requires eu.dariolucia.ccsds.encdec;
    requires eu.dariolucia.ccsds.tmtc;
    requires eu.dariolucia.ccsds.sle.utl;

    exports eu.dariolucia.reatmetric.driver.spacecraft;
    exports eu.dariolucia.reatmetric.driver.spacecraft.security;
    exports eu.dariolucia.reatmetric.driver.spacecraft.services;
    exports eu.dariolucia.reatmetric.driver.spacecraft.services.impl;
    exports eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu;
    exports eu.dariolucia.reatmetric.driver.spacecraft.activity.tcframe;
    exports eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket;
    exports eu.dariolucia.reatmetric.driver.spacecraft.activity;
    exports eu.dariolucia.reatmetric.driver.spacecraft.common;
    exports eu.dariolucia.reatmetric.driver.spacecraft.connectors;
    exports eu.dariolucia.reatmetric.driver.spacecraft.definition;
    exports eu.dariolucia.reatmetric.driver.spacecraft.definition.services;
    exports eu.dariolucia.reatmetric.driver.spacecraft.definition.security;
    exports eu.dariolucia.reatmetric.driver.spacecraft.security.impl;

    provides eu.dariolucia.reatmetric.core.api.IDriver with SpacecraftDriver;
    provides eu.dariolucia.reatmetric.api.value.IValueExtensionHandler with TmFrameDescriptorValueExtensionHandler, TcPacketInfoValueExtensionHandler;
    provides eu.dariolucia.ccsds.encdec.extension.IDecoderExtension with SpacePacketDecodingExtension;
    provides eu.dariolucia.ccsds.encdec.extension.IEncoderExtension with SpacePacketEncodingExtension;
    provides eu.dariolucia.reatmetric.driver.spacecraft.services.IService with CommandVerificationService, OnboardEventService, OnboardOperationsSchedulingService, TimeCorrelationService, DirectLinkTimeCorrelationService, AesEncryptionService;
    provides eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector with CltuCaduTcpConnector;
    provides eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket.ITcPacketConnector with SpacePacketTcpConnector;
    provides eu.dariolucia.reatmetric.driver.spacecraft.common.IReceptionOnlyConnector with TmPacketReplayConnector;
}