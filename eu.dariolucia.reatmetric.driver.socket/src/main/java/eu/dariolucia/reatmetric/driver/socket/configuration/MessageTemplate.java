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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class MessageTemplate {

    private int id; // Unique identifier, matches action ID

    private String template; // This is a template message, something like "asdas asd asd as ${{param1}} sad ${{param2}}" // NOSONAR

    // private List<SymbolTypeFormat> symbolTypes; // Map symbol to type/Format for correct formatting

    // Move the stuff below to the protocol

    // private List<SymbolMapping> parameterMappings; // Defines which values must be mapped to which parameters in case of inbound message

    // private List<EventMapping> eventMapping; // Events to be raised in case of reception of this message


}
