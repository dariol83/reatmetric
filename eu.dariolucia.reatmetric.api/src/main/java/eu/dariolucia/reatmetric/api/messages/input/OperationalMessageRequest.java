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
	 *
	 */
	private static final long serialVersionUID = 1L;

    /**
     * Build a new {@link OperationalMessageRequest}.
     * @param id the id of the message to raise
     * @param message the message text
     * @param source the source of the message
     * @param severity the severity of the message
     * @param linkedEntityId the linked entity ID, can be null
     * @param extension the extension object
     * @return an instance of the {@link OperationalMessageRequest} class initialised with the provided attributes
     */
    public static OperationalMessageRequest of(String id, String message, String source, Severity severity, Integer linkedEntityId, Object extension) {
        return new OperationalMessageRequest(id, message, source, severity, linkedEntityId, extension);
    }

	private final String id;

    private final String message;

    private final String source;

    private final Severity severity;

    private final Integer linkedEntityId;

    private final Object extension;

    private OperationalMessageRequest(String id, String message, String source, Severity severity, Integer linkedEntityId, Object extension) {
        this.id = id;
        this.message = message;
        this.source = source;
        this.severity = severity;
        this.linkedEntityId = linkedEntityId;
        this.extension = extension;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Integer getLinkedEntityId() {
        return linkedEntityId;
    }

    public Object getExtension() {
        return extension;
    }

    @Override
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
