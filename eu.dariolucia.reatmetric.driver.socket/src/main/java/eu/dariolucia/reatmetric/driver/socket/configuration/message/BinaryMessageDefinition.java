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

package eu.dariolucia.reatmetric.driver.socket.configuration.message;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.PacketAmbiguityException;
import eu.dariolucia.ccsds.encdec.identifier.PacketNotIdentifiedException;
import eu.dariolucia.ccsds.encdec.identifier.impl.FieldGroupBasedPacketIdentifier;
import eu.dariolucia.ccsds.encdec.structure.*;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.resolvers.DefaultValueFallbackResolver;
import eu.dariolucia.ccsds.encdec.structure.resolvers.DefinitionValueBasedResolver;
import eu.dariolucia.ccsds.encdec.structure.resolvers.PathLocationBasedResolver;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import jakarta.xml.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

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

    @XmlTransient
    private IPacketIdentifier identifier;
    @XmlTransient
    private IPacketEncoder encoder;
    @XmlTransient
    private IPacketDecoder decoder;
    @XmlTransient
    private Definition definition;

    @Override
    public void initialise() throws ReatmetricException {
        try {
            definition = Definition.load(new FileInputStream(getLocation()));
            identifier = new FieldGroupBasedPacketIdentifier(definition, false, markers);
            decoder = new DefaultPacketDecoder(definition);
            encoder = new DefaultPacketEncoder(definition);
        } catch (IOException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public Map<String, Object> decode(String secondaryId, byte[] messageToProcess) throws ReatmetricException {
        try {
            DecodingResult result = decoder.decode(secondaryId, messageToProcess);
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
    public byte[] encode(String secondaryId, Map<String, Object> data) throws ReatmetricException {
        // Replace all entries in data with secondaryId.<key>
        Set<String> keys = new LinkedHashSet<>(data.keySet());
        for(String k : keys) {
            data.put(secondaryId + "." + k, data.get(k));
        }
        // Encode
        try {
            return encoder.encode(secondaryId, new DefaultValueFallbackResolver(new PathLocationBasedResolver(data)));
        } catch (EncodingException e) {
            throw new ReatmetricException(e);
        }
    }
}
