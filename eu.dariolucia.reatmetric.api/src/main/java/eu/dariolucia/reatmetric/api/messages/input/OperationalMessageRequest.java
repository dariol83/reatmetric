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


package eu.dariolucia.reatmetric.api.messages.input;

import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.processing.input.AbstractInputDataItem;

/**
 * Request for operational messages distribution and storage via the {@link eu.dariolucia.reatmetric.api.messages.IOperationalMessageCollectorService} interface.
 *
 * Objects of this class are immutable.
 */
public final class OperationalMessageRequest extends AbstractInputDataItem {

    /**
     * Serial version UID for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Builds a new {@link OperationalMessageRequest}.
     *
     * @param id the id of the message to raise
     * @param message the message text
     * @param source the source of the message
     * @param severity the severity of the message
     * @param linkedEntityId the linked entity ID, can be null
     * @param extension the extension object
     * @return an instance of the {@link OperationalMessageRequest} class initialized with the provided attributes
     */
    public static OperationalMessageRequest of(String id, String message, String source, Severity severity, Integer linkedEntityId, Object extension) {
        return new OperationalMessageRequest(id, message, source, severity, linkedEntityId, extension);
    }

    /** The unique identifier of the message. */
    private final String id;

    /** The message text. */
    private final String message;

    /** The source of the message. */
    private final String source;

    /** The severity of the message. */
    private final Severity severity;

    /** The linked entity ID, can be null. */
    private final Integer linkedEntityId;

    /** The extension object. */
    private final Object extension;

    /**
     * Constructs an OperationalMessageRequest.
     *
     * @param id the id of the message to raise
     * @param message the message text
     * @param source the source of the message
     * @param severity the severity of the message
     * @param linkedEntityId the linked entity ID, can be null
     * @param extension the extension object
     */
    private OperationalMessageRequest(String id, String message, String source, Severity severity, Integer linkedEntityId, Object extension) {
        this.id = id;
        this.message = message;
        this.source = source;
        this.severity = severity;
        this.linkedEntityId = linkedEntityId;
        this.extension = extension;
    }

    /**
     * Returns the unique identifier of the message.
     *
     * @return the message ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the message text.
     *
     * @return the message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the source of the message.
     *
     * @return the message source
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the severity of the message.
     *
     * @return the message severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the linked entity ID.
     *
     * @return the linked entity ID, or null if not set
     */
    public Integer getLinkedEntityId() {
        return linkedEntityId;
    }

    /**
     * Returns the extension object.
     *
     * @return the extension object
     */
    public Object getExtension() {
        return extension;
    }

    @Override
    /**
     * Returns a string representation of this operational message request.
     *
     * @return a string representation
     */
    public String toString() {
        return "OperationalMessageRequest{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                ", severity=" + severity +
                ", linkedEntityId=" + linkedEntityId +
                ", extension=" + extension +
                "} " + super.toString();
    }
}
