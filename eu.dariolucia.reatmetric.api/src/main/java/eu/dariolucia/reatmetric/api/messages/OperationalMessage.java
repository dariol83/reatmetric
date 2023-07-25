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


package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.io.Serializable;
import java.time.Instant;

/**
 * An instance of this class encapsulates the state of an operational message.
 *
 * Objects of this class are immutable.
 */
public final class OperationalMessage extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String id;

    private final String message;

    private final String source;

    private final Severity severity;

    private final Integer linkedEntityId;

    /**
     * Constructor of the class.
     *
     * @param internalId the internal ID identifying the event occurrence
     * @param generationTime the generation time of the event
     * @param id the message ID
     * @param message the message
     * @param source the event source, can be null
     * @param severity the severity of the event as specified in the processing model definition
     * @param linkedEntityId the unique ID of the system entity linked to the message, can be null
     * @param extension an extension object, can be null
     */
    public OperationalMessage(IUniqueId internalId, Instant generationTime, String id, String message, String source, Severity severity, Integer linkedEntityId, Object extension) {
        super(internalId, generationTime, extension);
        this.id = id;
        this.message = message;
        this.source = source;
        this.severity = severity;
        this.linkedEntityId = linkedEntityId;
    }

    /**
     * Return the message ID.
     *
     * @return the message ID
     */
    public String getId() {
        return id;
    }

    /**
     * Return the message text.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the source of the operational message.
     *
     * @return the source of the operational message
     */
    public String getSource() {
        return source;
    }

    /**
     * Return the severity of the operational message.
     *
     * @return the severity of the operational message
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Return the linked system entity ID related to this operational message.
     *
     * @return the system entity ID linked to this operational message
     */
    public Integer getLinkedEntityId() {
        return linkedEntityId;
    }

    @Override
    public String toString() {
        return "OperationalMessage{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                ", severity=" + severity +
                ", linkedEntityId=" + linkedEntityId +
                ", generationTime=" + generationTime +
                ", internalId=" + internalId +
                '}';
    }
}
