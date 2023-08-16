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

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;

import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class MessageDefinition<T> {

    @XmlID
    @XmlAttribute
    private String id; // Unique identifier for the file

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract void initialise() throws ReatmetricException;

    public abstract Map<String, Object> decode(String id, T messageToProcess) throws ReatmetricException;

    public abstract String identify(T messageToIdentify) throws ReatmetricException;

    public abstract T encode(String id, Map<String, Object> data) throws ReatmetricException;
}
