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

package eu.dariolucia.reatmetric.driver.socket.configuration;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.PacketAmbiguityException;
import eu.dariolucia.ccsds.encdec.identifier.PacketNotIdentifiedException;
import eu.dariolucia.ccsds.encdec.identifier.impl.FieldGroupBasedPacketIdentifier;
import eu.dariolucia.ccsds.encdec.structure.*;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.resolvers.PathLocationBasedResolver;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class BinaryMessageDefinition extends MessageDefinition<byte[]> {

    @XmlAttribute(required = true)
    private String location;

    @XmlElement(name = "type-marker")
    private List<String> markers = new LinkedList<>();

    public List<String> getMarkers() {
        return markers;
    }

    public void setMarkers(List<String> markers) {
        this.markers = markers;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private IPacketIdentifier identifier;
    private IPacketEncoder encoder;
    private IPacketDecoder decoder;

    @Override
    public void initialise() throws ReatmetricException {
        try {
            Definition definition = Definition.load(new FileInputStream(getLocation()));
            identifier = new FieldGroupBasedPacketIdentifier(definition, false, markers);
            decoder = new DefaultPacketDecoder(definition);
            encoder = new DefaultPacketEncoder(definition);
        } catch (IOException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public Map<String, Object> decode(String id, byte[] messageToProcess) throws ReatmetricException {
        try {
            DecodingResult result = decoder.decode(id, messageToProcess);
            return result.getDecodedItemsAsMap();
        } catch (DecodingException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public String identify(byte[] messageToIdentify) throws ReatmetricException {
        try {
            return identifier.identify(messageToIdentify);
        } catch (PacketNotIdentifiedException e) {
            return null;
        } catch (PacketAmbiguityException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public byte[] encode(String id, Map<String, Object> data) throws ReatmetricException {
        try {
            return encoder.encode(id, new PathLocationBasedResolver(data));
        } catch (EncodingException e) {
            throw new ReatmetricException(e);
        }
    }
}
